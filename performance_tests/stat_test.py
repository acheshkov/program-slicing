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
    prev_samples = pd.read_csv(cur_csv)['diff']
    cur_samples = pd.read_csv(prev_csv)['diff']

    prev_mean_scaled = np.interp(prev_samples, (prev_samples.min(), prev_samples.max()), (-1, +1))
    cur_mean_scaled = np.interp(cur_samples, (cur_samples.min(), cur_samples.max()), (-1, +1))

    prev_mean = prev_mean_scaled.mean()

    print('not scaled prev ', prev_samples)
    print('scaled prev', prev_mean_scaled)
    print('not scaled cur ', cur_samples)
    print('scaled cur', cur_mean_scaled)

    # H_0: the mean time of current version is less or equal to mean of previous version
    # H_1: the mean time of current version is greater than mean of previous version
    test_res = ttest_ind(cur_samples, prev_mean)
    # since ttest_ind is written for two-tailed, we correct pval
    corr_pvalue = 1 - test_res.pvalue / 2

    print(f'Pval {corr_pvalue}')
    if corr_pvalue < 0.05:
        raise Exception("we reject null hypothesis; cur version is slower than prev version")
    else:
        print("we accept null hypothesis; cur version has the same performance or it is faster than previous version")


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
