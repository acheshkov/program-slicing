from slicing.block.listing_map import ListingMap
from program_graphs.adg.adg import ADG  # type: ignore
from collections import defaultdict
from typing import Dict, Optional, Tuple, Set
from slicing.block.declaration import BlockSlice, BlockSliceState


NodeID = int
VarName = str
VarType = str
JAVA_PRIMITIVE_TYPES = ['int', 'char', 'short', 'byte', 'boolean', 'long', 'float', 'double']


class State:
    def __init__(
        self, adg: ADG,
        ddg: ADG,
        row_col_map: ListingMap,
        var_types: Dict[VarName, Optional[VarType]],
        declared_vars: Dict[NodeID, Set[VarName]],
        blank_and_full_comment_lines: Set[int]
    ) -> None:
        self.cfg = adg.to_cfg()
        self.cdg = adg.to_cdg()
        self.ddg = ddg
        self.ast = adg.to_ast()
        self.adg = adg
        self.row_col_map = row_col_map
        self.cfg_nodes = set(self.cfg.nodes)
        self.class_fields_vars: Set[VarName] = set()
        self.non_primitve_type_vars: Set[VarName] = set()
        self._sort_variables(var_types)
        self.vars_not_need_to_return = self.class_fields_vars | self.non_primitve_type_vars
        self.declared_vars: Dict[NodeID, Set[VarName]] = declared_vars
        self.stops: Dict[NodeID, bool] = defaultdict(bool)
        self.memory: Dict[NodeID, Optional[Tuple[BlockSlice, BlockSliceState]]] = defaultdict(lambda: None)
        self.exit_node = adg.get_exit_node()
        self.blank_and_full_comment_lines = blank_and_full_comment_lines

    def _sort_variables(self, var_types: Dict[VarName, Optional[VarType]]) -> None:
        for var_name, var_type in var_types.items():
            if var_type is None:
                self.class_fields_vars.add(var_name)
            elif var_type not in JAVA_PRIMITIVE_TYPES:
                self.non_primitve_type_vars.add(var_name)
