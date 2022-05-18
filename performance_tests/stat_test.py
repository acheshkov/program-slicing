from unittest import TestCase, main

from scipy.stats import ttest_1samp
from pathlib import Path
import numpy as np
import pandas as pd
import argparse


def is_avg_larger(cur_csv: Path, prev_csv: Path) -> None:
    prev_samples = pd.read_csv(cur_csv)['secs']
    cur_samples = pd.read_csv(prev_csv)['secs'] + 0.03

    print('not scaled prev ', prev_samples)
    print('not scaled cur ', cur_samples)
    print('prev mean', prev_samples.mean())
    print('cur mean', cur_samples.mean())
    _, pvalue = ttest_1samp(cur_samples.tolist(), prev_samples.mean(), alternative='greater')
    print(f'Pval orig {pvalue}')

    if pvalue < 0.01:
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
