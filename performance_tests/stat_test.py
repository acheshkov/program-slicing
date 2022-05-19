from unittest import TestCase, main

from scipy.stats import ttest_ind
from pathlib import Path
import numpy as np
import pandas as pd
import argparse
import math
from collections import defaultdict


def is_avg_larger(cur_csv: Path, prev_csv: Path) -> None:
    prev_samples = defaultdict(list)
    cur_samples = defaultdict(list)
    for _, row in pd.read_csv(prev_csv).iterrows():
        prev_samples[row['file']].append(row['secs'])
    for _, row in pd.read_csv(cur_csv).iterrows():
        cur_samples[row['file']].append(row['secs'])

    res = {}
    was_at_least_one_degradation = False
    for filename, lst_with_secs in cur_samples.items():
        secs_for_prev_commit = prev_samples.get(filename)
        print(lst_with_secs)
        print(secs_for_prev_commit)
        _, pvalue = ttest_ind(lst_with_secs, secs_for_prev_commit, alternative='greater')
        print(f'Pval orig {pvalue}')

        if pvalue < 0.01:
            was_at_least_one_degradation = False
            print(f'{filename}: prev avg: {np.mean(secs_for_prev_commit)}; current avg: {np.mean(lst_with_secs)}')
            print("we reject null hypothesis; cur version is slower than prev version")
        # else:
        #     print("we accept null hypothesis; cur version has the same performance"
        #           " or it is faster than previous version")

    if was_at_least_one_degradation:
        exit(1)
    else:
        print('There is no performance degradation')
        exit(0)


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
