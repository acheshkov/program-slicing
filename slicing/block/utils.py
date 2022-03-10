from program_graphs.types import ASTNode  # type: ignore
from typing import Iterator


def traverse_leafs_tree_sitter(node: ASTNode) -> Iterator[ASTNode]:
    if len(node.children) == 0:
        yield node
    for child in node.children:
        yield from traverse_leafs_tree_sitter(child)
