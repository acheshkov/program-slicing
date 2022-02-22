from collections import defaultdict
from program_graphs.adg.adg import ADG  # type: ignore
from tree_sitter import Language, Parser  # type: ignore
from program_graphs.types import ASTNode, NodeID  # type: ignore
from typing import Callable, List, Mapping, Iterator, Optional, Tuple, Set, Dict
from itertools import chain
from enum import Enum

Graph = List[NodeID]
Path = List[NodeID]
Index = int
PathSearchState = Tuple[List[NodeID], Mapping[NodeID, Index]]
AllSuccessorGraphs = List[Graph]


def mk_parser() -> Parser:
    JAVA_LANGUAGE = Language('build/my-languages.so', 'java')
    parser = Parser()
    parser.set_language(JAVA_LANGUAGE)
    return parser


BlockSliceEntry = int
BlockSliceExit = int
LineRange = Tuple[int, int]
BlockSliceLineRange = Optional[LineRange]
BlockSliceSize = int
BlockSlice = Tuple[Set[NodeID], BlockSliceEntry, BlockSliceExit, BlockSliceLineRange, BlockSliceSize]


class State:
    def __init__(self, adg: ADG) -> None:
        self.cfg = adg.to_cfg()
        self.cdg = adg.to_cdg()
        self.ddg = adg.to_ddg()
        self.ast = adg.to_ast()
        self.adg = adg

        self.stops: Dict[NodeID, bool] = defaultdict(bool)
        self.memory: Dict[NodeID, Optional[BlockSlice]] = defaultdict(lambda: None)


class FilterResult(Enum):
    STOP = 1
    CONTINUE = 2
    GOOD = 3


BlockSliceFilter = Callable[[BlockSlice, State], FilterResult]


def merge_line_ranges(r1: BlockSliceLineRange, r2: BlockSliceLineRange) -> BlockSliceLineRange:
    if r1 is None or r2 is None:
        return r1 or r2
    (a, b) = r1
    (c, d) = r2
    return (min(a, c), max(b, d))


def combine_block_slices(bs1: BlockSlice, bs2: BlockSlice) -> BlockSlice:
    nodes_1, bs_entry_1, _, r1, size_1 = bs1
    nodes_2, _, bs_exit_2, r2, size_2 = bs2
    return nodes_1 | nodes_2, bs_entry_1, bs_exit_2, merge_line_ranges(r1, r2), size_1 + size_2


def mk_block_slice(node: NodeID, state: State) -> Optional[BlockSlice]:
    if state.stops[node] is True:
        return None
    if state.memory[node] is not None:
        return state.memory[node]

    syntax_ancestors: Set[NodeID] = traverse_all_syntax_dependent(state.ast, node)
    mb_exit_node: Optional[BlockSliceExit] = check_validity_cf(state.cfg, syntax_ancestors)

    if mb_exit_node is None:
        state.stops[node] = True
        return None

    more_nodes: List[NodeID] = safe_cfg_continuation(state.cfg, mb_exit_node)
    if len(more_nodes) > 0:
        syntax_ancestors |= set(more_nodes)
        mb_exit_node = more_nodes[-1]

    entry_node = node
    exit_node: NodeID = mb_exit_node
    block_slice_line_range = get_occupied_line_range(state.ast, node)
    comment_and_blank_lines: Set[int] = find_blank_and_full_comment_lines(state.ast, node)
    block_slice_size = count_ncss(block_slice_line_range, comment_and_blank_lines)
    bs = syntax_ancestors, entry_node, exit_node, block_slice_line_range, block_slice_size
    state.memory[node] = bs
    return bs


def safe_cfg_continuation(cfg: ADG, node: NodeID) -> List[NodeID]:
    ''' Iteratevely take non-AST CFG successors while in-degree = 1 and out-degree <= 1 '''
    if cfg.out_degree(node) == 0:
        return []
    assert cfg.out_degree(node) == 1
    [current_node] = cfg.successors(node)
    cfg_continuation = []
    while current_node is not None:
        if cfg.nodes[current_node].get('ast_node', None) is not None:
            break
        if cfg.in_degree(current_node) != 1:
            break
        if cfg.out_degree(current_node) > 1:
            break
        cfg_continuation.append(current_node)
        current_node = next(cfg.successors(current_node), None)

    return cfg_continuation


def traverse_leafs_tree_sitter(node: ASTNode) -> Iterator[ASTNode]:
    # if node is None: return
    if len(node.children) == 0:
        yield node
    for child in node.children:
        yield from traverse_leafs_tree_sitter(child)

# def count_comment_and_empty_lines_in_range(line_range: Tuple[int, int], ) -> BlockSliceSize:
#     ast_node = ast.nodes[node].get('ast_node', None)
#     if ast_node is None:
#         return 0
#     return count_ncss_tree_sitter(ast_node)

# def count_ncss_tree_sitter(ast_node: ASTNode) -> BlockSliceSize:
#     leafs = traverse_leafs_tree_sitter(ast_node)
#     leafs_without_comments = [leaf.start_point[0] for leaf in leafs if leaf.type != 'comment']
#     return len(set(leafs_without_comments))


def count_ncss(line_range: BlockSliceLineRange, comment_and_lines: Set[int]) -> BlockSliceSize:
    if line_range is None:
        return 0
    (s, e) = line_range
    lines = set(range(s, e + 1))
    lines = lines - comment_and_lines
    return len(lines)


def find_blank_and_full_comment_lines(ast: ADG, node: NodeID) -> Set[int]:
    ast_node = ast.nodes[node].get('ast_node', None)
    if ast_node is None:
        return set()

    all_lines = set(range(ast_node.start_point[0], ast_node.end_point[0] + 1))
    for n in traverse_leafs_tree_sitter(ast_node):  # we need traverse whole AST, it's takes time
        if n.type not in ['line_comment', 'block_comment']:
            all_lines -= set(range(n.start_point[0], n.end_point[0] + 1))
    return all_lines


def get_occupied_line_range(ast: ADG, node: NodeID) -> BlockSliceLineRange:
    ast_node = ast.nodes[node].get('ast_node', None)
    if ast_node is None:
        return None
    return ast_node.start_point[0], ast_node.end_point[0]


def check_validity_cf(cfg: ADG, nodes: Set[NodeID]) -> Optional[BlockSliceExit]:
    # check-1: one or zero CF entries from outside
    # check-2: one or zero CF exits to outside
    # combined with block-slices's exit node search
    cf_entries = 0
    cf_exits = 0
    bs_exit_candidates: List[NodeID] = []
    for n in nodes:
        if cfg.nodes.get(n) is None:
            continue
        edges_out_out = [1 for node_to in cfg.successors(n,) if node_to not in nodes]
        cf_exits += len(edges_out_out)
        cf_entries += len([1 for node_from in cfg.predecessors(n) if node_from not in nodes])
        if cf_entries > 1 or cf_exits > 1:
            return None
        if cfg.out_degree(n) == len(edges_out_out):
            bs_exit_candidates.append(n)

    if len(bs_exit_candidates) == 0 and len(nodes) == 1:
        return list(nodes)[0]  # type: ignore
    assert len(bs_exit_candidates) == 1
    return bs_exit_candidates[0]  # type: ignore


def check_dd(ddg: ADG, nodes: Set[NodeID]) -> bool:
    # check-1: no more than one unique written variable is used outside
    only_ddg_nodes = nodes & set(ddg.nodes())
    vars_used_outside = set()
    for n in only_ddg_nodes:
        for node_to in ddg.successors(n):
            if node_to in only_ddg_nodes:
                continue
            vars_used_outside |= ddg.edges[n, node_to]['vars']
            if len(vars_used_outside) > 1:
                return False
    return len(vars_used_outside) < 2


def get_cfg_successors(cfg: ADG, node: NodeID) -> Set[NodeID]:
    return set(list(cfg.successors(node)))


def next_block_slice(block_slice: BlockSlice, state: State) -> Optional[BlockSlice]:
    _, _, bs_exit, _, _ = block_slice
    cfg_successors = get_cfg_successors(state.cfg, bs_exit)
    if len(cfg_successors) == 0:
        return None
    [next_node_cfg] = cfg_successors
    bs = mk_block_slice(next_node_cfg, state)
    if bs is None:
        return None
    return combine_block_slices(block_slice, bs)


def traverse_all_syntax_dependent(ast: ADG, node: NodeID) -> Set[NodeID]:
    return set(chain.from_iterable(
        [traverse_all_syntax_dependent(ast, out) for out in ast.successors(node)]
    )) | set([node])


def get_node_lines(g: ADG, node: NodeID) -> Set[int]:
    mb_ast_node = g.nodes[node].get('ast_node', None)
    if mb_ast_node is None:
        return set([])
    return set([mb_ast_node.start_point[0]])
    # better to traverse ast tree and get all lines this AST node allocate
    # do this only for ADG leafs


def block_slice_line_size(bs: BlockSlice) -> Optional[int]:
    _, _, _, _, size = bs
    return size


def block_slice_lines(g: ADG, bs: BlockSlice) -> Set[int]:
    _, _, _, line_range, _ = bs
    if line_range is None:
        return set()
    (s, e) = line_range
    return set(range(s, e + 1))


def combine_filters(bs: BlockSlice, state: State, filters: List[BlockSliceFilter]) -> FilterResult:
    for filter in filters:
        filter_result = filter(bs, state)
        if filter_result == FilterResult.STOP:
            return FilterResult.STOP
        if filter_result == FilterResult.CONTINUE:
            return FilterResult.CONTINUE
    return FilterResult.GOOD


def gen_block_slices_from_single_node(node: NodeID, state: State, filters: List[BlockSliceFilter]) -> Iterator[BlockSlice]:
    # print('start slicing from node', node)
    bs = mk_block_slice(node, state)
    counter = 0
    while bs is not None:
        nodes, _, _, _, _ = bs
        # print('next slice has length', len(nodes))
        filter_result = combine_filters(bs, state, filters)
        if filter_result == FilterResult.STOP:
            break
        if filter_result == FilterResult.CONTINUE:
            bs = next_block_slice(bs, state)
            continue

        if check_dd(state.ddg, nodes):
            yield bs

        bs = next_block_slice(bs, state)
        counter += 1


def get_entry_candidates_for_node(node: NodeID, ast: ADG, cfg: ADG) -> Set[NodeID]:
    entries: Set[NodeID] = set()
    ast_node = ast.nodes[node].get('ast_node')
    if ast_node is None:
        return set()
    if ast_node.type in ['if_statement', 'for_statement']:
        entries.add(node)
        exit_nodes = [n for (_, n, exit) in ast.out_edges(node, data='exit') if exit is True]
        if len(exit_nodes) == 1:
            cf_succesors = get_cfg_successors(cfg, exit_nodes[0])
            assert len(cf_succesors) == 1
            entries |= {n for n in cf_succesors if cfg.nodes[n].get('ast_node') is not None}

    if ast_node.type in ['block', 'program']:
        cf_succesors = get_cfg_successors(cfg, node)
        assert len(cf_succesors) == 1
        entries |= {n for n in cf_succesors if cfg.nodes[n].get('ast_node') is not None}
    return entries


def get_entry_candidates(state: State) -> Set[NodeID]:
    entry_candidates: Set[NodeID] = set()

    for node in state.ast.nodes():
        # if ast_node is None: continue
        entry_candidates |= get_entry_candidates_for_node(node, state.ast, state.cfg)

    return entry_candidates


def mk_max_min_ncss_filter(max_ncss: int = 50, min_ncss: int = 0) -> BlockSliceFilter:
    def line_size_filter(bs: BlockSlice, state: State) -> FilterResult:
        bs_line_size = block_slice_line_size(bs)
        if bs_line_size is None:
            return FilterResult.CONTINUE
        if bs_line_size < min_ncss:
            return FilterResult.CONTINUE
        if bs_line_size > max_ncss:
            return FilterResult.STOP
        return FilterResult.GOOD
    return line_size_filter


def gen_block_slices(g: ADG, filters: List[BlockSliceFilter] = []) -> Iterator[BlockSlice]:
    state = State(g)
    entry_candidates = get_entry_candidates(state)
    for entry in entry_candidates:
        yield from gen_block_slices_from_single_node(entry, state, filters)
