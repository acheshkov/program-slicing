from slicing.block.listing_map import ListingMap
from program_graphs.adg.adg import ADG  # type: ignore
from collections import defaultdict
from typing import Dict, Optional, Tuple
from slicing.block.declaration import BlockSlice, BlockSliceState


NodeID = int


class State:
    def __init__(self, adg: ADG, row_col_map: ListingMap) -> None:
        self.cfg = adg.to_cfg()
        self.cdg = adg.to_cdg()
        self.ddg = adg.to_ddg()
        self.ast = adg.to_ast()
        self.adg = adg
        self.row_col_map = row_col_map
        self.cfg_nodes = set(self.cfg.nodes)

        self.stops: Dict[NodeID, bool] = defaultdict(bool)
        self.memory: Dict[NodeID, Optional[Tuple[BlockSlice, BlockSliceState]]] = defaultdict(lambda: None)
