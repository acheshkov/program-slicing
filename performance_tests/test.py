import json
import os
import warnings
from collections import defaultdict
from pathlib import Path
from time import time
from typing import Optional, List
from tqdm import tqdm

import pandas as pd
from program_graphs.adg import parse_java
from slicing.block.block import gen_block_slices
from slicing.block.filters import mk_max_min_ncss_filter
from datetime import datetime
import subprocess
import os
import argparse
import numpy as np
warnings.filterwarnings('ignore')  # Ignore everything


def run_perf_test(output_file: Optional[str], path: Path) -> None:
    df = pd.DataFrame(columns=['file', 'secs'])
    files = [x for x in path.rglob('**/*.java') if x.is_file()]
    for dataset_file in tqdm(files):
        with open(dataset_file) as f:
            filename = dataset_file.name
            method_code = "class Foo {" + f.read() + "}"
            adg = parse_java(method_code)
            times_to_repeat = 20
            for i in range(times_to_repeat):
                start = time()
                list(gen_block_slices(adg, method_code, [mk_max_min_ncss_filter(50, 4)]))
                end = time()
                diff = end - start
                df = df.append(
                    {'file': filename, 'secs': diff, 'iter': i},
                    ignore_index=True)

    print(f'Saving output to {output_file}')
    df.to_csv(output_file)


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
        '--output_file',
        type=str,
        required=True
    )

    args = parser.parse_args()

    time_arr = []
    results = []

    out_f = Path(args.output_file).resolve()
    run_perf_test(out_f, args.dataset_path)