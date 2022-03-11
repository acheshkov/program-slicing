from slicing.block.declaration import BlockSlice
from slicing.block.listing_map import is_line_empty_left, is_line_empty_right
from enum import Enum
from typing import Callable, List
from slicing.block.state import State


class FilterResult(Enum):
    STOP = 1
    CONTINUE = 2
    GOOD = 3


BlockSliceFilter = Callable[[BlockSlice, State], FilterResult]


def at_least_one_block_stmt(bs: BlockSlice, state: State) -> FilterResult:
    if not bs.has_block_stmt:
        return FilterResult.CONTINUE
    return FilterResult.GOOD


def shared_line_filter(bs: BlockSlice, state: State) -> FilterResult:
    mb_line_range = bs.line_range
    if mb_line_range is None:
        return FilterResult.GOOD
    start_point, end_point = mb_line_range
    if not is_line_empty_left(state.row_col_map, start_point):
        return FilterResult.STOP
    if not is_line_empty_right(state.row_col_map, end_point):
        return FilterResult.CONTINUE
    return FilterResult.GOOD


def mk_max_min_ncss_filter(max_ncss: int = 50, min_ncss: int = 0) -> BlockSliceFilter:
    def line_size_filter(bs: BlockSlice, state: State) -> FilterResult:
        bs_line_size = bs.size
        # if bs_line_size is None:
        #     return FilterResult.CONTINUE
        if bs_line_size < min_ncss:
            return FilterResult.CONTINUE
        if bs_line_size > max_ncss:
            return FilterResult.STOP
        return FilterResult.GOOD
    return line_size_filter


def combine_filters(bs: BlockSlice, state: State, filters: List[BlockSliceFilter]) -> FilterResult:
    for filter in filters:
        filter_result = filter(bs, state)
        if filter_result == FilterResult.STOP:
            return FilterResult.STOP
        if filter_result == FilterResult.CONTINUE:
            return FilterResult.CONTINUE
    return FilterResult.GOOD
