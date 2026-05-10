# /// script
# requires-python = ">=3.12"
# dependencies = [
#  "matplotlib",
#  "numpy",
# ]
# ///

import enum
import re
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

# Example pattern:
#
# -- Statistics:
#  Average time: 4.9121 seconds
#  Std Dev time: 1.3374 seconds
#         Total:   100 runs
#     Successes:    99 runs
#     Succ Rate: 0.990000
#      Timeouts:     1 runs
#  Timeout Rate: 0.010000
#      Failures:     0 runs
#  Failure Rate: 0.000000


def main():

    CURR = Path(__file__).parent
    LOG_DIR = CURR / "logs" / "2026-05-09"

    re_avgtime = re.compile(r"Average time:\s*([\d.]+)\s*seconds")
    re_stddev = re.compile(r"Std Dev time:\s*([\d.]+)\s*seconds")
    re_succ = re.compile(r"Successes:\s*(\d+)")
    re_timeout = re.compile(r"Timeouts:\s*(\d+)")
    re_fail = re.compile(r"Failures:\s*(\d+)")

    machines = ["dev-server", "nuc-server"]

    variants = ["jvm", "juc-atomic", "libc-stdatomic"]

    test_suites = [
        "Loops1Test",
        "Loops2Test",
        "Loops3Test",
        "Loops4Test",
    ]

    for machine in machines:
        data = {}
        for suite in test_suites:
            data[suite] = {v: None for v in variants}

        for v in variants:
            for suite in test_suites:
                log_path = LOG_DIR / machine / "standalone" / v / suite

                if not log_path.exists():
                    print(f"Log file {log_path} does not exist.")
                    continue

                print(log_path)
                log_files = tuple(log_path.glob("*.log"))
                num_runs = len(log_files)

                avg_arr = np.zeros(num_runs, dtype=float)
                std_arr = np.zeros(num_runs, dtype=float)
                succ_arr = np.zeros(num_runs, dtype=int)
                timeout_arr = np.zeros(num_runs, dtype=int)
                fail_arr = np.zeros(num_runs, dtype=int)

                for i, log in enumerate(log_files):
                    with log.open() as fh:
                        content = fh.read()
                        avg_match = re_avgtime.search(content)
                        std_match = re_stddev.search(content)
                        succ_match = re_succ.search(content)
                        timeout_match = re_timeout.search(content)
                        fail_match = re_fail.search(content)

                        avg = float(avg_match.group(1) if avg_match else "nan")
                        std = float(std_match.group(1) if std_match else "nan")
                        succ = int(succ_match.group(1) if succ_match else "0")
                        timeout = int(timeout_match.group(1) if timeout_match else "0")
                        fail = int(fail_match.group(1) if fail_match else "0")
                        print(
                            f"{avg} +- {std}, succ={succ}, timeout={timeout}, fail={fail}"
                        )
                        avg_arr[i] = avg
                        std_arr[i] = std
                        succ_arr[i] = succ
                        timeout_arr[i] = timeout
                        fail_arr[i] = fail

                if num_runs > 10:
                    fig_path = log_path / f"{suite}-{v}-times.svg"
                    fig, ax = plt.subplots(figsize=(10, 5))
                    x = np.arange(num_runs) + 1
                    ax.errorbar(
                        x,
                        avg_arr,
                        yerr=std_arr,
                        label=f"{suite} - {v}",
                        marker="o",
                        markersize=5,
                    )
                    ax.set_title(
                        f"{machine} - {suite} - {v} - Avg Time with Std Dev per run (lower is better)"
                    )
                    ax.set_xlabel("Run Index")
                    ax.set_xticks(x[::2])
                    ax.set_xlim(1, num_runs + 1)
                    ax.set_ylabel("Time (seconds)")
                    ax.legend(edgecolor="black")
                    fig.tight_layout()
                    fig.savefig(fig_path, transparent=False, dpi=300)
                    plt.close(fig)

                    fig_path = log_path / f"{suite}-{v}-succ.svg"
                    fig, ax = plt.subplots(figsize=(10, 5))
                    x = np.arange(num_runs) + 1
                    ax.bar(x, succ_arr, label="Successes")
                    ax.bar(x, timeout_arr, bottom=succ_arr, label="Timeouts")
                    ax.bar(x, fail_arr, bottom=succ_arr + timeout_arr, label="Failures")
                    ax.set_title(
                        f"{machine} - {suite} - {v} - Success/Timeout/Failure Stacked Bar"
                    )
                    ax.set_xlabel("Run Index")
                    ax.set_xticks(x[::2])
                    ax.set_xlim(0, num_runs + 1)
                    ax.set_ylabel("Count")
                    ax.legend(edgecolor="black")
                    fig.tight_layout()
                    fig.savefig(fig_path, transparent=False, dpi=300)
                    plt.close(fig)

                data[suite][v] = {
                    "avg_arr": avg_arr,
                    "std_arr": std_arr,
                    "succ_arr": succ_arr,
                    "timeout_arr": timeout_arr,
                    "fail_arr": fail_arr,
                }

        for suite in test_suites:
            fig_path = LOG_DIR / machine / f"{suite}.svg"
            fig, ax = plt.subplots(1, 1, figsize=(12, 6))

            for v in variants:
                avg_arr = data[suite][v]["avg_arr"]
                std_arr = data[suite][v]["std_arr"]

                x = np.arange(len(avg_arr)) + 1
                ax.errorbar(
                    x,
                    avg_arr,
                    yerr=std_arr,
                    label=f"{v}, final avg: {np.nanmean(avg_arr):.4f} secs",
                    marker="o",
                    markersize=5,
                )

            ax.set_title(
                f"{machine} - {suite} - Avg Time with Std Dev per run (lower is better)"
            )
            ax.set_xlabel("Run Index")
            ax.set_xticks(np.arange(15) + 1)
            ax.set_ylabel("Time (seconds)")
            ax.set_xlim(1, 15)
            ax.legend(edgecolor="black")
            fig.tight_layout()
            fig.savefig(fig_path, transparent=False, dpi=300)
            plt.close(fig)


if __name__ == "__main__":
    main()
