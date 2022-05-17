import json
import os
import warnings
from collections import defaultdict
from pathlib import Path
from time import time
from typing import Optional, List

import pandas as pd
from program_graphs.adg import parse_java
from slicing.block.block import gen_block_slices
from slicing.block.filters import mk_max_min_ncss_filter
from datetime import datetime
import subprocess
# import isodate
import os
import argparse


warnings.filterwarnings('ignore')  # Ignore everything


def run_perf_test(commit_id: Optional[str], commit_time: Optional[datetime], path: Path) -> Dict[str, List[float]]:
    print(f'Running {commit_id} from {commit_time}...')
    files = [x for x in path.rglob('**/*.java') if x.is_file()]
    # print(path, files)
    times = defaultdict(list)
    for x in range(100):
        for dataset_file in files:
            filename = dataset_file.name
            # try:
            with open(dataset_file) as f:
                print(dataset_file)
                method_code = "class Foo {" + f.read() + "}"
                start = time()
                adg = parse_java(method_code)
                list(gen_block_slices(adg, method_code, [mk_max_min_ncss_filter(50, 4)]))
                end = time()
                diff = end - start
                times[filename].append(diff)

    return times
    # for filename, time_lst in
    #     # except Exception as e:
    #         # print(f'Err for {dataset_file}: {str(e)}')
    #         # continue


def run_cmd_and_print_output(cmd: List[str]):
    cmd_t = subprocess.run(
        cmd,
        check=True,
        stdout=subprocess.PIPE)
    if cmd_t.stdout:
        print(cmd_t.stdout.decode('utf-8'))
    if cmd_t.stderr:
        print(cmd_t.stderr.decode('utf-8'))


def git_checkout(commit_id):
    # cmd_t = subprocess.run(
    #     ["git", "checkout", "master"],
    #     check=True,
    #     stdout=subprocess.PIPE)
    # print(cmd_t.stdout.decode('utf-8'))
    # print(cmd_t.stderr.decode('utf-8'))
    # cmd_t = subprocess.run(
    #     ["git", "reset", "--hard"],
    #     check=True,
    #     stdout=subprocess.PIPE)
    run_cmd_and_print_output(["git", "checkout", "-"])
    run_cmd_and_print_output(["git", "checkout", commit_id])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Run performance testing for program-slicing repo with certain commit id'
    )
    parser.add_argument(
        '--dataset_path',
        type=Path,
        required=True
    )
    parser.add_argument(
        '--commit_id',
        type=str,
        required=True
    )
    args = parser.parse_args()
    # path = Path(args.repo_path)
    # os.chdir(path)

    time_arr = []
    results = []

    # output = subprocess.run(
    #     ["git", "log", "--merges", "--first-parent", "master", '--pretty=format:"%H %cI"'],
    #     check=True,
    #     stdout=subprocess.PIPE).stdout.decode('utf-8')
    # top_n = 10
    # merge_requests_lst = output.split('\n')[0:top_n]
    # df = pd.DataFrame(columns=['file', 'secs', 'commit_id', 'commit_time'])
    # run_cmd_and_print_output('git clone ')
    json_d = run_perf_test(None, None, args.dataset_path)
    with open(args.commit_id + '.csv', encoding='utf-8') as w:
        json.dump(json_d)
    # for mr_i in merge_requests_lst:
    #     commit_id, time_str = mr_i.split(' ', maxsplit=1)
    #     datetime_for_commit = isodate.parse_datetime(time_str.strip())
        # git_checkout(commit_id)
        # run_perf_test(commit_id, datetime_for_commit, df)

# resp = response.content
# end = time()
# diff = end - start
# time_arr.append(diff)
# print(f'Time for {dataset_file}: {diff} secs;')
# resp = json.loads(resp.decode('utf-8'))
# print(resp)
# if resp:
# if resp.get('result'):
# results.append(resp['result'])
# print(f'{dataset_file}: {resp}')

# with open('out.ccs_top1', 'w') as w:
# json.dump(results, w)
# print(f'Total time: {sum(time_arr)}')
# print(f'Avg time: {mean(time_arr)}, {median(time_arr)}, {percentile(time_arr, 70)} , {percentile(time_arr, 95)}')
