from program_graphs.adg.adg import ADG  # type: ignore
from program_graphs.types import NodeID  # type: ignore
from typing import List, Mapping, Iterator, Optional, Tuple, Set, Dict
from slicing.block.utils import find_blank_and_full_comment_lines, get_occupied_line_range, count_ncss, get_ast_node_lines
from slicing.block.listing_map import mk_listing_pixel_map
from itertools import chain
from slicing.block.declaration import BlockSlice, ReturnState, mk_block_slice_ex, combine_block_slices
from slicing.block.declaration import TREE_SITTER_BLOCK_STATEMENTS
from slicing.block.state import State
from slicing.block.filters import BlockSliceFilter, combine_filters, BlockSliceState, shared_line_filter
from program_graphs.types import ASTNode
import networkx as nx  # type: ignore
from program_graphs.ddg.parser.java.utils import get_declared_variables  # type: ignore

Graph = List[NodeID]
Path = List[NodeID]
Index = int
PathSearchState = Tuple[List[NodeID], Mapping[NodeID, Index]]
AllSuccessorGraphs = List[Graph]
VarName = str
VarType = str
Variable = Tuple[VarName, VarType]


# def mk_parser() -> Parser:
#     JAVA_LANGUAGE = Language('build/my-languages.so', 'java')
#     parser = Parser()
#     parser.set_language(JAVA_LANGUAGE)
#     return parser


def find_all_exits(
    entry_node: NodeID,
    g: nx.DiGraph,
    nodes: Set[NodeID],
    visited: Set[NodeID]
) -> Set[NodeID]:
    if entry_node in visited:
        return set([])
    # visited = visited | {entry_node}
    visited.add(entry_node)
    allowed_moves = [n for n in g.successors(entry_node)]
    if len(allowed_moves) == 0:
        return set([entry_node])
    if len(allowed_moves) == 1 and allowed_moves[0] not in nodes:
        return set([entry_node])
    res = set()
    for n in allowed_moves:
        if n not in nodes:
            return set([])
        res |= find_all_exits(n, g, nodes, visited)
    return res


def check_entry_single(state: State, nodes: Set[NodeID]) -> bool:
    ''' Check set of nodes has no more than one external inbound edge'''
    cf_entries = 0
    for n in nodes:
        if n not in state.cfg_nodes:
            continue
        for node_from in state.cfg.predecessors(n):
            if node_from in nodes:
                continue
            cf_entries += 1
            if cf_entries > 1:
                return False
    return True


def is_return_stmt(node: Optional[ASTNode]) -> bool:
    if node is None:
        return False
    return node.type in ['return_statement', 'throw_statement']  # type: ignore


def check_exits(
    entry_node: NodeID,
    cfg: ADG,
    nodes: Set[NodeID]
) -> Tuple[Optional[NodeID], BlockSliceState, ReturnState]:
    _exits = find_all_exits(entry_node, cfg, nodes, set())
    exits: List[Tuple[NodeID, bool]] = [(n, is_return_stmt(cfg.nodes[n].get('ast_node'))) for n in _exits]
    returns_counter = 0
    non_return_exits = []
    for (node, is_return) in exits:
        if is_return:
            returns_counter += 1
        else:
            non_return_exits.append(node)

    if len(exits) == 0:
        return (None, BlockSliceState.LAST_TIME_VALID, ReturnState.NONE)
    if len(exits) == 1 and returns_counter == 0:
        return (exits[0][0], BlockSliceState.VALID, ReturnState.NONE)
    if len(exits) == returns_counter:
        return (None, BlockSliceState.LAST_TIME_VALID, ReturnState.COMPLETE)
    if len(non_return_exits) == 1:
        if has_any_executable_statement_after(cfg, non_return_exits[0]):
            return (non_return_exits[0], BlockSliceState.MAYBE_VALID_FURTHER, ReturnState.INCOMPLETE)
        else:
            return (non_return_exits[0], BlockSliceState.VALID, ReturnState.COMPLETE)
    return (None, BlockSliceState.INVALID, ReturnState.INCOMPLETE)


def has_any_executable_statement_after(cfg: ADG, node: NodeID) -> bool:
    ss = get_cfg_successors(cfg, node)
    if len([s for s in ss if cfg.nodes[s].get('ast_node') is not None]) > 0:
        return True
    for s in ss:
        # if is_return_stmt(cfg.nodes[s].get('ast_node')):
        #     continue
        if has_any_executable_statement_after(cfg, s):
            return True
    return False


def mk_block_slice(node: NodeID, state: State) -> Tuple[Optional[BlockSlice], BlockSliceState]:
    if state.stops[node] is True:
        return None, BlockSliceState.INVALID
    if state.memory[node] is not None:
        return state.memory[node]  # type: ignore
    syntax_ancestors: Set[NodeID] = traverse_all_syntax_dependent(state.ast, node)
    if not check_entry_single(state, syntax_ancestors - {state.adg.get_exit_node()}):
        state.stops[node] = True
        return None, BlockSliceState.INVALID
    mb_exit_node, status, return_state = check_exits(node, state.cfg, syntax_ancestors)
    if status == BlockSliceState.INVALID:
        state.stops[node] = True
        return None, BlockSliceState.INVALID

    if mb_exit_node is not None:
        more_nodes: List[NodeID] = safe_cfg_continuation(state.cfg, mb_exit_node)
        if len(more_nodes) > 0:
            syntax_ancestors |= set(more_nodes)
            mb_exit_node = more_nodes[-1]

    # if mb_exit_node == state.adg.get_exit_node():
    if mb_exit_node == state.exit_node:
        # if block slice ends on PROGRAM's exit
        return_state = ReturnState.COMPLETE

    entry_node = node
    exit_node: NodeID = mb_exit_node
    block_slice_line_range = get_occupied_line_range(state.ast, node)
    comment_and_blank_lines: Set[int] = get_ast_node_lines(state.ast, node) & state.blank_and_full_comment_lines
    block_slice_size = count_ncss(block_slice_line_range, comment_and_blank_lines)
    bs = mk_block_slice_ex(
        syntax_ancestors,
        entry_node,
        exit_node,
        block_slice_line_range,
        block_slice_size,
        entry_stmt_name=state.ast.nodes[entry_node].get('ast_node').type
    )
    bs.return_state = return_state
    state.memory[node] = (bs, status)
    return bs, status


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
        inbound_cf_edges = cfg.in_edges(current_node, data='program_return')
        in_cf_except_return = [_ for (_, _, is_return) in inbound_cf_edges if is_return is not True]
        if len(in_cf_except_return) != 1:
            break
        if cfg.out_degree(current_node) > 1:
            break
        cfg_continuation.append(current_node)
        current_node = next(cfg.successors(current_node), None)

    return cfg_continuation


def check_dd(state: State, nodes: Set[NodeID]) -> bool:
    # check-1: no more than one unique written variable is used outside (exluding class fields and non-primitive )
    ddg = state.ddg
    only_ddg_nodes = nodes & set(ddg.nodes())
    declared_vars: Set[VarName] = {var for n in only_ddg_nodes for var in state.declared_vars[n]}
    vars_used_outside = set()
    for n in only_ddg_nodes:
        for node_to in ddg.successors(n):
            if node_to in only_ddg_nodes:
                continue  # if dependency internal then skip
            for var_name in ddg.edges[n, node_to]['vars']:
                # if var_name in declared_vars:
                #     return False  # we can't extract and then duplicate declaration
                if var_name in state.vars_not_need_to_return:
                    if var_name not in declared_vars:
                        continue  # these vars not need to return
                vars_used_outside.add(var_name)
            if len(vars_used_outside) > 1:
                return False
    return len(vars_used_outside) < 2


def get_cfg_successors(cfg: ADG, node: NodeID) -> Set[NodeID]:
    return set(list(cfg.successors(node)))


def has_any_intersection(nodes_1: Set[NodeID], nodes_2: Set[NodeID]) -> bool:
    if len(nodes_1) > len(nodes_2):
        return has_any_intersection(nodes_2, nodes_1)
    for node in nodes_1:
        if node in nodes_2:
            return True
    return False


def next_block_slice(block_slice: BlockSlice, state: State) -> Tuple[Optional[BlockSlice], BlockSliceState]:
    mb_bs_exit = block_slice.exit
    if mb_bs_exit is None:
        return None, BlockSliceState.INVALID
    cfg_successors = get_cfg_successors(state.cfg, mb_bs_exit)
    if len(cfg_successors) == 0:
        return None, BlockSliceState.INVALID
    [next_node_cfg] = cfg_successors
    bs, status = mk_block_slice(next_node_cfg, state)
    if status == BlockSliceState.INVALID:
        return None, BlockSliceState.INVALID
    if bs is None:
        return None, BlockSliceState.INVALID
    if has_any_intersection(bs.nodes, block_slice.nodes):
        return None, BlockSliceState.INVALID
    next_bs = combine_block_slices(block_slice, bs)
    if next_bs.return_state == ReturnState.INCOMPLETE:
        return next_bs, BlockSliceState.MAYBE_VALID_FURTHER
    if next_bs.return_state == ReturnState.COMPLETE:
        return next_bs, BlockSliceState.LAST_TIME_VALID

    return next_bs, status


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


def bs_gen_base(node: NodeID, state: State) -> Iterator[Tuple[BlockSlice, BlockSliceState]]:
    bs, status = mk_block_slice(node, state)
    while status is not BlockSliceState.INVALID:
        yield bs, status  # type: ignore
        if status == BlockSliceState.LAST_TIME_VALID:
            break
        bs, status = next_block_slice(bs, state)  # type: ignore


def bs_gen_l2(node: NodeID, state: State, filters: List[BlockSliceFilter]) -> Iterator[BlockSlice]:
    for bs, status in bs_gen_base(node, state):
        if status == BlockSliceState.MAYBE_VALID_FURTHER:
            continue
        filter_result = combine_filters(bs, state, filters)
        if filter_result == BlockSliceState.INVALID:
            break
        if filter_result == BlockSliceState.MAYBE_VALID_FURTHER:
            continue
        yield bs


def bs_gen_l3(node: NodeID, state: State, filters: List[BlockSliceFilter]) -> Iterator[BlockSlice]:
    for bs in bs_gen_l2(node, state, filters):
        nodes = bs.nodes
        if check_dd(state, nodes):
            yield bs


def gen_block_slices_from_single_node(node: NodeID, state: State, filters: List[BlockSliceFilter]) -> Iterator[BlockSlice]:
    yield from bs_gen_l3(node, state, filters)


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
        entries |= {n for n in cf_succesors if cfg.nodes[n].get('ast_node') is not None}
    return entries


def get_entry_candidates(state: State) -> Set[NodeID]:
    entry_candidates: Set[NodeID] = set()

    for node in state.ast.nodes():
        entry_candidates |= get_entry_candidates_for_node(node, state.ast, state.cfg)

    return entry_candidates


def gen_block_slices(adg: ADG, source_code: str, filters: List[BlockSliceFilter] = []) -> Iterator[BlockSlice]:
    entry_node_ast = adg.nodes[adg.get_entry_node()].get('ast_node')
    blank_and_full_comment_lines: Set[int] = find_blank_and_full_comment_lines(adg, adg.get_entry_node())
    row_col_map = mk_listing_pixel_map(entry_node_ast)
    ddg = adg.to_ddg()
    var_types: Dict[VarName, Optional[VarType]] = mk_var_types_table(ddg)
    node_to_declared_vars = mk_declared_variables_table(ddg, source_code)
    state = State(adg, ddg, row_col_map, var_types, node_to_declared_vars, blank_and_full_comment_lines)
    entry_candidates = get_entry_candidates(state)
    for entry in entry_candidates:
        yield from gen_block_slices_from_single_node(entry, state, filters + [shared_line_filter])


def mk_var_types_table(ddg: ADG) -> Dict[VarName, Optional[VarType]]:
    res: Dict[VarName, Optional[VarType]] = {}
    for _, write_vars in ddg.nodes(data='write_vars'):
        for var_name, var_type in write_vars:
            if var_name in res:
                continue
            res[var_name] = var_type
    return res


def mk_declared_variables_table(ddg: ADG, source_code: str) -> Dict[NodeID, Set[VarName]]:
    res: Dict[NodeID, Set[VarName]] = {}
    for node in ddg.nodes():
        res[node] = get_declared_variables(ddg.nodes[node].get('ast_node'), source_code.encode())
    return res
