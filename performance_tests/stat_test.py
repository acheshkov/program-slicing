from unittest import TestCase, main

from pingouin import ttest, power_ttest
from pathlib import Path
import numpy as np
import pandas as pd
import argparse
import math
from collections import defaultdict
import json
from sklearn.preprocessing import MinMaxScaler


def is_avg_larger(cur_csv: Path, prev_csv: Path) -> None:
    cur_samples = defaultdict(list)
    prev_samples = defaultdict(list)
    scaler_prev = MinMaxScaler()
    scaler_cur = MinMaxScaler()

    df_cur = pd.read_csv(cur_csv)
    df_prev = pd.read_csv(prev_csv)
    df_cur[['secs']] = scaler_cur.fit_transform(df_cur[['secs']])
    df_prev[['secs']] = scaler_prev.fit_transform(df_prev[['secs']])
    for _, x in df_cur.iterrows():
        cur_samples[x['file']].append(x['secs'])

    for _, x in df_prev.iterrows():
        prev_samples[x['file']].append(x['secs'])

    output_d = {}
    for filename, prev_lst_times in prev_samples.items():
        current_lst_times = cur_samples.get(filename)
        res = ttest(current_lst_times, prev_lst_times, alternative='greater')
        pvalue = res['p-val'].astype(float).tolist()[0]
        mean_cur = np.mean(current_lst_times)
        mean_prev = np.mean(prev_lst_times)
        cohend = res['cohen-d'].tolist()[0]
        need_number = power_ttest(d=cohend, power=0.80, alternative='greater')
        t_val = {'mean_prev': mean_prev, 'mean_cur': mean_cur, 'diff': mean_cur - mean_prev, 'ttest': False,
                 'pval': pvalue, 'power': res['power'].tolist()[0],
                 'cohen': cohend,
                 'var_prev': np.var(prev_lst_times),
                 'var_cur': np.var(current_lst_times),
                 'nob': need_number}
        alfa = 0.05 / 2
        hyp = np.less_equal(pvalue, alfa)
        if hyp:
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


def draw_table(output_d):
    print('''| Filename | Mean previous | Mean current | Diff | Pvalue | Power | Var previous | Var current | Cohen-d | Number of observations | Ttest ''')
    print('''| ------------ | ----------------- | ---------------- | -------- | ----------------- | ----------------- | ----------------- | ----------------- | ----------------- | ----------------- | ----------------- |''')
    for filename, temp_d in output_d.items():
        diff = np.round(temp_d['diff'], 5)
        hyp = temp_d['ttest']
        mean_prev = np.round(temp_d['mean_prev'], 5)
        mean_cur = np.round(temp_d['mean_cur'], 5)
        cur_str = f'''|  {filename}  |  {mean_prev}  |   {mean_cur}  |'''
        print(cur_str, end='')
        column = f'''    {diff} '''
        if hyp:
            column = f'''    <span style="color:red">+{diff}</span> '''
        elif np.isclose(diff, 0.00000000000000000000001):
            column = f'''    {diff} |'''
        elif np.less(diff, 0.00000000000000000000001):
            column = f'''    <span style="color:green">{diff}</span> '''

        print(column, end='')
        var_prev = round(temp_d['var_prev'], 10)
        var_cur = round(temp_d['var_cur'], 10)
        pval = round(temp_d['pval'], 5)
        power = round(temp_d['power'], 5)
        cohen = round(temp_d['cohen'], 5)
        nob = int(temp_d['nob'])
        print(f'''|     {pval} |    {power}  |  {var_prev}  |   {var_cur}  |    {cohen} |   {nob}  |''', end='')
        if temp_d['ttest']:
            print(f'''  &#x274C;   |''')
        else:
            print(f'''  &#10004;   |''')


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
