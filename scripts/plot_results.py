#!/usr/bin/env python3

"""Makes the charts for the report from the csv files in docs/research"""

import csv
import statistics

import matplotlib
matplotlib.use("Agg")  # just save png files, don't open windows
import matplotlib.pyplot as plt

DIR = "docs/research/"

# index build
times = {}
for row in csv.DictReader(open(DIR + "index_build.csv")):
    times.setdefault(int(row["threads"]), []).append(int(row["time_ms"]))

threads = sorted(times)
seconds = [statistics.median(times[t]) / 1000 for t in threads]
speedup = [seconds[0] / s for s in seconds]

print("threads  time,s  speedup  efficiency")
for t, s, sp in zip(threads, seconds, speedup):
    print(f"{t:7}  {s:6.1f}  {sp:7.2f}  {sp / t:10.2f}")

plt.figure(figsize=(6, 4))
plt.plot(threads, seconds, "o-")
plt.xticks(threads)
plt.xlabel("threads")
plt.ylabel("build time, s")
plt.title("Index build time (5378 documents)")
plt.grid(alpha=0.3)
plt.savefig(DIR + "build_time.png", dpi=150, bbox_inches="tight")

plt.figure(figsize=(6, 4))
plt.plot(threads, speedup, "o-", label="measured")
plt.plot(threads, threads, "--", color="gray", label="ideal")
plt.xticks(threads)
plt.xlabel("threads")
plt.ylabel("speedup T(1) / T(n)")
plt.title("Index build speedup")
plt.legend()
plt.grid(alpha=0.3)
plt.savefig(DIR + "speedup.png", dpi=150, bbox_inches="tight")

# search
modes = {}
for row in csv.DictReader(open(DIR + "search_throughput.csv")):
    modes.setdefault(row["label"], []).append(row)

for column, ylabel, title, file in [("rps", "requests per second", "Search throughput", "throughput.png"),
                                    ("avg_ms", "average latency, ms", "Search latency", "latency.png")]:
    plt.figure(figsize=(6, 4))
    for label, rows in modes.items():
        clients = [int(r["clients"]) for r in rows]
        plt.plot(clients, [float(r[column]) for r in rows], "o-", label=label)
    plt.xscale("log", base=2)
    plt.yscale("log")  # with ollama about 200 times slower, on a normal axis just shows falt line
    plt.xticks([1, 2, 4, 8, 16, 32], ["1", "2", "4", "8", "16", "32"])
    plt.xlabel("concurrent clients")
    plt.ylabel(ylabel)
    plt.title(title)
    plt.legend()
    plt.grid(alpha=0.3)
    plt.savefig(DIR + file, dpi=150, bbox_inches="tight")

print("charts saved to", DIR)
