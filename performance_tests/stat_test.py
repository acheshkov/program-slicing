from unittest import TestCase, main

from pingouin import ttest
from pathlib import Path
import numpy as np
import pandas as pd
import argparse
import math
from collections import defaultdict


def is_avg_larger(cur_csv: Path, prev_csv: Path) -> None:
    cur_samples = defaultdict(list)
    prev_samples = defaultdict(list)

    for _, x in pd.read_csv(cur_csv).iterrows():
        cur_samples[x['file']].append(x['secs'])

    for _, x in pd.read_csv(prev_csv).iterrows():
        prev_samples[x['file']].append(x['secs'])

    was_at_least_one_degradation = False
    res = {}
    for filename, prev_lst_times in prev_samples.items():
        current_lst_times = cur_samples.get(filename)
        print(current_lst_times)
        print(prev_lst_times)
        _, pvalue = ttest(current_lst_times, prev_lst_times, alternative='greater')
        print(f'Pval orig {pvalue}')
        if pvalue < 0.01:
            was_at_least_one_degradation = True
            mean_cur = np.mean(current_lst_times)
            mean_prev = np.mean(prev_lst_times)
            res[filename] = (mean_cur, mean_prev)
            print("we reject null hypothesis; cur version is slower than prev version")
            print(f"{filename} prev: {mean_prev}; cur: {mean_cur}")
        # else:
        #     print(
        #         "we accept null hypothesis; cur version has the same performance or it is faster than previous version")

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
