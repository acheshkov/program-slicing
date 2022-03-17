from program_graphs.types import ASTNode  # type: ignore
from typing import Iterator


TREE_SITTER_BLOCK_STATEMENTS = [
    'if_statement', 'for_statement', 'while_statement', 'do_statement', 'enhanced_for_statement',
    'switch_expression', 'try_statement', 'try_with_resources_statement'
]


def traverse_leafs_tree_sitter(node: ASTNode) -> Iterator[ASTNode]:
    if len(node.children) == 0:
        yield node
    for child in node.children:
        yield from traverse_leafs_tree_sitter(child)
