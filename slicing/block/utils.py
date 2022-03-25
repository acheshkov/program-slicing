from program_graphs.types import ASTNode, NodeID  # type: ignore
from typing import Iterator, Set
from slicing.block.declaration import BlockSliceLineRange, get_start_line, get_end_line
from program_graphs.adg import ADG  # type: ignore


def traverse_leafs_tree_sitter(node: ASTNode) -> Iterator[ASTNode]:
    if len(node.children) == 0:
        yield node
    for child in node.children:
        yield from traverse_leafs_tree_sitter(child)


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
    for n in traverse_leafs_tree_sitter(ast_node):  # we need traverse whole AST; it takes time
        if n.type not in ['line_comment', 'block_comment']:
            all_lines -= set(range(n.start_point[0], n.end_point[0] + 1))
    return all_lines


def get_ast_node_lines(ast: ADG, node: NodeID) -> Set[int]:
    ''' return set of line numbers given ast node occupied in the source-code listing '''
    mb_line_range = get_occupied_line_range(ast, node)
    if mb_line_range is None:
        return set()

    (start_line, _), (end_line, _) = mb_line_range
    return set(range(start_line, end_line))


def get_occupied_line_range(ast: ADG, node: NodeID) -> BlockSliceLineRange:
    ast_node = ast.nodes[node].get('ast_node', None)
    if ast_node is None:
        return None
    return ast_node.start_point, ast_node.end_point
