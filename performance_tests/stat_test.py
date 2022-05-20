from unittest import TestCase, main

from pingouin import ttest
from pathlib import Path
import numpy as np
import pandas as pd
import argparse
import math
from collections import defaultdict
import json


def is_avg_larger(cur_csv: Path, prev_csv: Path) -> None:
    cur_samples = defaultdict(list)
    prev_samples = defaultdict(list)

    for _, x in pd.read_csv(cur_csv).iterrows():
        cur_samples[x['file']].append(x['secs'])

    for _, x in pd.read_csv(prev_csv).iterrows():
        prev_samples[x['file']].append(x['secs'])

    was_at_least_one_degradation = False
    output_d = {}
    for filename, prev_lst_times in prev_samples.items():
        current_lst_times = cur_samples.get(filename)
        res = ttest(current_lst_times, prev_lst_times, alternative='greater')
        pvalue = res['p-val'].astype(float).tolist()[0]
        mean_cur = np.mean(current_lst_times)
        mean_prev = np.mean(prev_lst_times)
        t_val = {'mean_prev': mean_prev, 'mean_cur': mean_cur, 'diff': mean_cur - mean_prev, 'ttest': False}
        hyp = np.less_equal(pvalue, 0.05)
        if hyp:
            was_at_least_one_degradation = True
            t_val['ttest'] = True
            t_val['pvalue'] = pvalue
            output_d[filename] = t_val
            # print(
            #     f"we reject null hypothesis; cur version is slower than prev version;"
            #     f" {filename} prev: {mean_prev}; cur: {mean_cur}; pval: {pvalue}")
        else:
            t_val['ttest'] = False
            t_val['pvalue'] = pvalue
            output_d[filename] = t_val

    draw_table(output_d)

    if was_at_least_one_degradation:
        exit(1)
    else:
        print('There is no performance degradation')
        exit(0)


def draw_table(output_d):
    with open('output.md', 'w') as w:
        header = '''| Filename | Mean previous | Mean current | Diff |
        | ------------ | ----------------- | ---------------- | -------- |'''
        w.write(header + '\n')
        for filename, temp_d in output_d.items():
            diff = np.round(temp_d['diff'], 5)
            hyp = temp_d['ttest']
            mean_prev = np.round(temp_d['mean_prev'], 5)
            mean_cur = np.round(temp_d['mean_cur'], 5)
            cur_str = f'''|  {filename}  |  {mean_prev}  |   {mean_cur}  |'''
            w.write(cur_str)
            column = f'''    {diff} |'''
            if hyp:
                column = f'''    <span style="color:red">+{diff}</span> |'''
            elif np.isclose(diff, 0.00000000000000000000001):
                column = f'''    {diff} |'''
            elif np.less(diff, 0.00000000000000000000001):
                column = f'''    <span style="color:green">{diff}</span> |'''

            w.write(column + '\n')


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
