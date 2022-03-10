from collections import defaultdict
from typing import Dict, Tuple
from program_graphs.types import ASTNode  # type: ignore
from slicing.block.block import traverse_leafs_tree_sitter  # type: ignore

Row = int
Col = int
ListingMap = Dict[Row, Dict[Col, bool]]
RowCol = Tuple[Row, Col]


def mk_listing_pixel_map(node: ASTNode) -> ListingMap:
    listing_map: ListingMap = defaultdict(lambda: defaultdict(bool))
    for n in traverse_leafs_tree_sitter(node):
        [start_row, start_col] = n.start_point
        [end_row, end_col] = n.end_point
        if start_row != end_row:
            print(n, n.children)
        if start_row == end_row:
            for c in range(start_col, end_col + 1):
                listing_map[start_row][c] = True
        else:
            for c in range(start_col, start_col + 1):
                listing_map[start_row][c] = True
            for c in range(0, end_col + 1):
                listing_map[end_row][c] = True

    return listing_map


def is_line_empty_left(row_col_map: ListingMap, line_row: RowCol) -> bool:
    row, col = line_row
    for i in range(0, col):
        if i in row_col_map[row]:
            return False
    return True


def is_line_empty_right(row_col_map: ListingMap, line_row: RowCol) -> bool:
    row, col = line_row
    for k in row_col_map[row].keys():
        if k > col:
            return False
    return True
