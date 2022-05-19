from unittest import TestCase, main

from scipy.stats import ttest_1samp
from pathlib import Path
import numpy as np
import pandas as pd
import argparse
import math


def is_avg_larger(cur_csv: Path, prev_csv: Path) -> None:
    prev_samples = pd.read_csv(cur_csv)
    cur_samples = {x['file']: x['secs'] for _, x in pd.read_csv(prev_csv).iterrows()}
    was_at_least_one_degradation = False
    res = {}
    for _, row in prev_samples.iterrows():
        current_time = cur_samples.get(row['file'])
        last_commit_time = row['secs']
        was_degraded = not np.less_equal(current_time, last_commit_time)
        if was_degraded:
            was_at_least_one_degradation = True
            res[row['file']] = (last_commit_time, current_time)

    if was_at_least_one_degradation:
        for file, times in res.items():
            print(f'Performance degradation for {file} from {times[0]} secs to {times[1]}')
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
