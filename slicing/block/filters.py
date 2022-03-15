from slicing.block.declaration import BlockSlice, BlockSliceState
from slicing.block.listing_map import is_line_empty_left, is_line_empty_right
# from enum import Enum
from typing import Callable, List
from slicing.block.state import State


# class BlockSliceState(Enum):
#     INVALID = 1
#     MAYBE_VALID_FURTHER = 2
#     LAST_TIME_VALID = 3
#     VALID = 4


BlockSliceFilter = Callable[[BlockSlice, State], BlockSliceState]


def at_least_one_block_stmt(bs: BlockSlice, state: State) -> BlockSliceState:
    if not bs.has_block_stmt:
        return BlockSliceState.MAYBE_VALID_FURTHER
    return BlockSliceState.VALID


def shared_line_filter(bs: BlockSlice, state: State) -> BlockSliceState:
    mb_line_range = bs.line_range
    if mb_line_range is None:
        return BlockSliceState.VALID
    start_point, end_point = mb_line_range
    if not is_line_empty_left(state.row_col_map, start_point):
        return BlockSliceState.INVALID
    if not is_line_empty_right(state.row_col_map, end_point):
        return BlockSliceState.MAYBE_VALID_FURTHER
    return BlockSliceState.VALID


def mk_max_min_ncss_filter(max_ncss: int = 50, min_ncss: int = 0) -> BlockSliceFilter:
    def line_size_filter(bs: BlockSlice, state: State) -> BlockSliceState:
        bs_line_size = bs.size
        if bs_line_size < min_ncss:
            return BlockSliceState.MAYBE_VALID_FURTHER
        if bs_line_size > max_ncss:
            return BlockSliceState.INVALID
        return BlockSliceState.VALID
    return line_size_filter


def combine_filters(bs: BlockSlice, state: State, filters: List[BlockSliceFilter]) -> BlockSliceState:
    for filter in filters:
        filter_result = filter(bs, state)
        if filter_result == BlockSliceState.INVALID:
            return BlockSliceState.INVALID
        if filter_result == BlockSliceState.MAYBE_VALID_FURTHER:
            return BlockSliceState.MAYBE_VALID_FURTHER
    return BlockSliceState.VALID
