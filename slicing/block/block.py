from program_graphs.adg.adg import ADG  # type: ignore
from tree_sitter import Language, Parser  # type: ignore
from program_graphs.types import NodeID  # type: ignore
from typing import List, Mapping, Iterator, Optional, Tuple, Set
from slicing.block.utils import traverse_leafs_tree_sitter
from slicing.block.listing_map import mk_listing_pixel_map
from itertools import chain
from slicing.block.declaration import BlockSlice, BlockSliceLineRange, mk_block_slice_ex, combine_block_slices
from slicing.block.declaration import get_start_line, get_end_line
from slicing.block.state import State
from slicing.block.filters import combine_filters, FilterResult, BlockSliceFilter, shared_line_filter

Graph = List[NodeID]
Path = List[NodeID]
Index = int
PathSearchState = Tuple[List[NodeID], Mapping[NodeID, Index]]
AllSuccessorGraphs = List[Graph]


TREE_SITTER_BLOCK_STATEMENTS = [
    'if_statement', 'for_statement', 'while_statement', 'do_statement', 'enhanced_for_statement',
    'switch_expression', 'try_statement', 'try_with_resources_statement'
]


def mk_parser() -> Parser:
    JAVA_LANGUAGE = Language('build/my-languages.so', 'java')
    parser = Parser()
    parser.set_language(JAVA_LANGUAGE)
    return parser


def mk_block_slice(node: NodeID, state: State) -> Optional[BlockSlice]:
    if state.stops[node] is True:
        # print('stop')
        return None
    if state.memory[node] is not None:
        return state.memory[node]

    syntax_ancestors: Set[NodeID] = traverse_all_syntax_dependent(state.ast, node)
    mb_exit_node: Optional[NodeID] = check_validity_cf(state.cfg, syntax_ancestors)

    if mb_exit_node is None:
        # print('exit is not found for node', node, syntax_ancestors)
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
    bs = mk_block_slice_ex(syntax_ancestors, entry_node, exit_node, block_slice_line_range, block_slice_size)
    bs.has_block_stmt = state.ast.nodes[node].get('ast_node').type in TREE_SITTER_BLOCK_STATEMENTS
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


def ncss_from_node(ast: ADG, node: NodeID) -> int:
    block_slice_line_range = get_occupied_line_range(ast, node)
    comment_and_blank_lines: Set[int] = find_blank_and_full_comment_lines(ast, node)
    block_slice_size = count_ncss(block_slice_line_range, comment_and_blank_lines)
    return block_slice_size


def count_ncss(line_range: BlockSliceLineRange, comment_and_lines: Set[int]) -> int:
    if line_range is None:
        return 0
    lines = set(range(get_start_line(line_range), get_end_line(line_range) + 1))
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
    return ast_node.start_point, ast_node.end_point


def check_validity_cf(cfg: ADG, nodes: Set[NodeID]) -> Optional[NodeID]:
    # check-1: one or zero CF entries from outside
    # check-2: one or zero CF exits to outside
    # check-3: if there are return statements the main entry also should be among nodes
    # combined with block-slices's exit node search
    cf_entries = 0
    cf_exits = 0
    bs_exit_candidates: List[NodeID] = []
    for n in nodes:
        if cfg.nodes.get(n) is None:
            continue

        edges_out_out = [1 for node_to in cfg.successors(n) if node_to not in nodes]
        cf_exits += len(edges_out_out)
        cf_entries += len([1 for node_from in cfg.predecessors(n) if node_from not in nodes])
        if cf_entries > 1 or cf_exits > 1:
            return None
        if cfg.out_degree(n) == len(edges_out_out):
            bs_exit_candidates.append(n)

    if len(bs_exit_candidates) == 0 and len(nodes) == 1:
        return list(nodes)[0]  # type: ignore
    # if return_counter > 0 and cfg.get_exit_node() not in nodes:
    #     return None
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
    bs_exit = block_slice.exit
    cfg_successors = get_cfg_successors(state.cfg, bs_exit)
    if len(cfg_successors) == 0:
        return None
    [next_node_cfg] = cfg_successors
    bs = mk_block_slice(next_node_cfg, state)
    if bs is None:
        return None
    if block_slice > bs:
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


def gen_block_slices_from_single_node(node: NodeID, state: State, filters: List[BlockSliceFilter]) -> Iterator[BlockSlice]:
    # print('start slicing from node', node)
    bs = mk_block_slice(node, state)
    counter = 0
    while bs is not None:
        # print("bs: ", bs)
        nodes = bs.nodes
        # print('next slice has length', len(nodes))
        filter_result = combine_filters(bs, state, filters)
        # print("filter_result", filter_result)
        if filter_result == FilterResult.STOP:
            break
        if filter_result == FilterResult.CONTINUE:
            bs = next_block_slice(bs, state)
            continue

        if check_dd(state.ddg, nodes):
            yield bs
        # print('DD check is not passed', nodes)
        bs = next_block_slice(bs, state)
        counter += 1


def get_entry_candidates_for_node(node: NodeID, ast: ADG, cfg: ADG) -> Set[NodeID]:
    entries: Set[NodeID] = set()
    ast_node = ast.nodes[node].get('ast_node')
    if ast_node is None:
        return set()
    if ast_node.type == 'local_variable_declaration':
        entries.add(node)
    if ast_node.type in TREE_SITTER_BLOCK_STATEMENTS:
        entries.add(node)
        exit_nodes = [n for (_, n, exit) in ast.out_edges(node, data='exit') if exit is True]
        if len(exit_nodes) == 1:
            cf_succesors = get_cfg_successors(cfg, exit_nodes[0])
            assert len(cf_succesors) == 1
            entries |= {n for n in cf_succesors if cfg.nodes[n].get('ast_node') is not None}

    if ast_node.type in ['block', 'program']:
        cf_succesors = get_cfg_successors(cfg, node)
        # if len(cf_succesors) > 1:
        #     return entries
        entries |= {n for n in cf_succesors if cfg.nodes[n].get('ast_node') is not None}
    return entries


def get_entry_candidates(state: State) -> Set[NodeID]:
    entry_candidates: Set[NodeID] = set()

    for node in state.ast.nodes():
        entry_candidates |= get_entry_candidates_for_node(node, state.ast, state.cfg)

    return entry_candidates


def gen_block_slices(g: ADG, filters: List[BlockSliceFilter] = []) -> Iterator[BlockSlice]:
    ast = g.nodes[g.get_entry_node()].get('ast_node')
    row_col_map = mk_listing_pixel_map(ast)
    state = State(g, row_col_map)
    entry_candidates = get_entry_candidates(state)
    for entry in entry_candidates:
        # print('start from node', entry)
        yield from gen_block_slices_from_single_node(entry, state, filters + [shared_line_filter])
