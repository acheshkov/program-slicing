from functools import reduce
from typing import Set
from unittest import TestCase, main

from program_graphs.adg.parser.java.parser import parse  # type: ignore

from slicing.block.block import gen_block_slices
from slicing.block.block import get_entry_candidates, get_node_lines, mk_declared_variables_table
from slicing.block.filters import at_least_one_block_stmt, last_or_next_statement_is_control
from slicing.block.state import State
from slicing.block.utils import get_occupied_line_range, count_ncss, find_blank_and_full_comment_lines
import numpy as np
from scipy.stats import ttest_ind
from pathlib import Path
import argparse


def is_avg_larger(cur_csv: Path, prev_csv: Path) -> None:
    prev_df = pd.read_csv(cur_csv)
    cur_df = pd.read_csv(prev_csv)
    prev_mean = prev_df['diff'].mean()
    cur_mean = cur_df['diff'].mean()
    prev_mean_scaled = np.interp(prev_mean, (prev_mean.min(), prev_mean.max()), (-1, +1))
    cur_mean_scaled = np.interp(cur_mean, (cur_mean.min(), cur_mean.max()), (-1, +1))
    print('not scaled prev ', prev_df['diff'])
    print('scaled prev', prev_mean_scaled)
    print('not scaled cur ', cur_df['diff'])
    print('scaled cur', cur_mean_scaled)
    t_statistic, pvalue = ttest_ind(prev_mean_scaled, cur_mean_scaled)
    print(f'Pval {pvalue}')
    # self.assertLess(pvalue, 0.01)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Run performance testing for program-slicing repo with certain commit id'
    )
    parser.add_argument(
        '--prev_csv',
        type=Path,
        required=True
    )
    parser.add_argument(
        '--cur_csv',
        type=Path,
        required=True
    )
    args = parser.parse_args()
    is_avg_larger(args.cur_csv, args.prev_csv)
