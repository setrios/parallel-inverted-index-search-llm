#!/usr/bin/env python3

"""Measures how long the index build takes with different numbers of threads"""

import json
import statistics
import urllib.request

SERVER = "http://localhost:8080"
THREADS = [1, 2, 4, 6, 8, 12, 16]
RUNS = 3
OUT = "docs/research/index_build.csv"


def build_index(threads):
    body = json.dumps({"threads": threads}).encode()
    req = urllib.request.Request(SERVER + "/api/index", data=body, headers={"Content-Type": "application/json"})
    resp = urllib.request.urlopen(req, timeout=3600)
    return json.load(resp)["time_ms"]


# the first build is always slower (jvm warm up, files not in disk cache yet), so it is not saved
print("warm up...")
build_index(8)

f = open(OUT, "w")
f.write("threads,run,time_ms\n")
for t in THREADS:
    times = []
    for run in range(1, RUNS + 1):
        ms = build_index(t)
        times.append(ms)
        f.write(f"{t},{run},{ms}\n")
    print(f"{t} threads: {times}, median {statistics.median(times)} ms")
f.close()
print("saved to", OUT)
