from typing import Set, Optional, Tuple
from enum import IntEnum, Enum
# from slicing.block.utils import TREE_SITTER_BLOCK_STATEMENTS

RowCol = Tuple[int, int]
LineRange = Tuple[RowCol, RowCol]
BlockSliceLineRange = Optional[LineRange]
BlockSliceSize = int
NodeID = int


TREE_SITTER_BLOCK_STATEMENTS = [
    'if_statement', 'for_statement', 'while_statement', 'do_statement', 'enhanced_for_statement',
    'switch_expression', 'try_statement', 'try_with_resources_statement', 'synchronized_statement', 'labeled_statement'
]


class ReturnState(IntEnum):
    NONE = 1  # there are no return statements
    INCOMPLETE = 2  # there are return statments but makes slice INVALID
    COMPLETE = 3  # Every CF path has return


class BlockSliceState(Enum):
    INVALID = 1
    MAYBE_VALID_FURTHER = 2
    LAST_TIME_VALID = 3
    VALID = 4


class BlockSlice:
    def __init__(self, nodes: Set[NodeID], entry: NodeID, exit: Optional[NodeID]) -> None:
        self._nodes: Set[NodeID] = nodes
        self._entry: NodeID = entry
        self._exit: Optional[NodeID] = exit
        self._line_range: BlockSliceLineRange = None
        self._size: int = 0
        self._has_block_stmt: bool = False
        self._return_state: ReturnState = ReturnState.NONE
        self._entry_stmt_name: str = 'None'
        self._exit_stmt_name: str = 'None'

    @property
    def nodes(self) -> Set[NodeID]:
        return self._nodes

    @property
    def exit(self) -> Optional[NodeID]:
        return self._exit

    @property
    def size(self) -> int:
        return self._size

    @size.setter
    def size(self, size: int) -> None:
        self._size = size

    @property
    def line_range(self) -> BlockSliceLineRange:
        return self._line_range

    @line_range.setter
    def line_range(self, line_range: LineRange) -> None:
        self._line_range = line_range

    @property
    def has_block_stmt(self) -> bool:
        return self._has_block_stmt

    @has_block_stmt.setter
    def has_block_stmt(self, has_block_stmt: bool) -> None:
        self._has_block_stmt = has_block_stmt

    @property
    def return_state(self) -> ReturnState:
        return self._return_state

    @return_state.setter
    def return_state(self, state: ReturnState) -> None:
        self._return_state = state

    @property
    def entry_stmt_name(self) -> str:
        return self._entry_stmt_name

    @entry_stmt_name.setter
    def entry_stmt_name(self, name: str) -> None:
        self._entry_stmt_name = name

    @property
    def exit_stmt_name(self) -> str:
        return self._exit_stmt_name

    @exit_stmt_name.setter
    def exit_stmt_name(self, name: str) -> None:
        self._exit_stmt_name = name

    def block_slice_lines(self) -> Set[int]:
        if self._line_range is None:
            return set()
        (s, e) = get_start_line(self._line_range), get_end_line(self._line_range)
        return set(range(s, e + 1))

    def __lt__(self, other: 'BlockSlice') -> bool:
        return get_end_line(self.line_range) <= get_start_line(other.line_range)  # type: ignore


def mk_block_slice_ex(
    nodes: Set[NodeID],
    entry_node: NodeID,
    exit_node: Optional[NodeID],
    line_range: Optional[LineRange],
    size: int,
    entry_stmt_name: str
) -> BlockSlice:
    bs = BlockSlice(nodes, entry_node, exit_node)
    bs.line_range = line_range
    bs.size = size
    bs.entry_stmt_name = entry_stmt_name
    bs.exit_stmt_name = entry_stmt_name
    bs.has_block_stmt = entry_stmt_name in TREE_SITTER_BLOCK_STATEMENTS
    return bs


def get_start_line(lr: LineRange) -> int:
    return lr[0][0]


def get_end_line(lr: LineRange) -> int:
    return lr[1][0]


def merge_line_ranges(r1: Optional[LineRange], r2: Optional[LineRange]) -> Optional[LineRange]:
    if r1 is None or r2 is None:
        return r1 or r2
    (a, b) = r1
    (c, d) = r2
    return (min(a, c), max(b, d))


def combine_block_slices(bs1: BlockSlice, bs2: BlockSlice) -> BlockSlice:
    bs = BlockSlice(bs1._nodes | bs2._nodes, bs1._entry, bs2._exit)
    bs.line_range = merge_line_ranges(bs1._line_range, bs2._line_range)
    bs.size = bs1._size + bs2._size
    bs.has_block_stmt = bs1.has_block_stmt or bs2.has_block_stmt
    bs.exit_stmt_name = bs2.exit_stmt_name
    bs.return_state = max(bs1.return_state, bs2.return_state)
    return bs
