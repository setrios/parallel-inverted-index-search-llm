#!/usr/bin/env python3
"""
measures how many searches per second the server can do with N clients at the same time
usage: python scripts/bench_search.py no_ollama
       python scripts/bench_search.py with_ollama
"""

import json
import os
import sys
import threading
import time
import urllib.request

SERVER = "http://localhost:8080"
CLIENTS = [1, 2, 4, 8, 16, 32]
SECONDS = 30
OUT = "docs/research/search_throughput.csv"
WORDS = ["government", "people", "music", "water", "history", "city", "school", "market",
         "energy", "health", "car", "river", "computer", "money", "family", "science"]

label = sys.argv[1]
if label == "with_ollama":
    CLIENTS = [1, 2, 4, 8, 16]  # ollama answers one request at a time, 32 clients would take forever


def search(query):
    body = json.dumps({"query": query, "mode": "keyword"}).encode()
    req = urllib.request.Request(SERVER + "/api/search", data=body, headers={"Content-Type": "application/json"})
    urllib.request.urlopen(req, timeout=600).read()


# one client: sends searches one after another until the time is over
def client(n, end, times, lock):
    i = n
    while time.time() < end:
        start = time.time()
        word = WORDS[i % len(WORDS)]
        i += 1
        try:
            search(word)
        except Exception as e:
            print("request failed:", e)
            continue
        with lock:  # all clients add to the same list
            times.append((time.time() - start) * 1000)


search("warmup")

new_file = not os.path.exists(OUT)
f = open(OUT, "a")
if new_file:
    f.write("label,clients,rps,avg_ms,p95_ms\n")

for n in CLIENTS:
    times = []
    lock = threading.Lock()
    end = time.time() + SECONDS
    threads = [threading.Thread(target=client, args=(i, end, times, lock)) for i in range(n)]

    start = time.time()
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    elapsed = time.time() - start

    rps = len(times) / elapsed
    avg = sum(times) / len(times)
    times.sort()
    p95 = times[int(len(times) * 0.95) - 1]
    f.write(f"{label},{n},{rps:.2f},{avg:.1f},{p95:.1f}\n")
    print(f"{n} clients: {rps:.1f} req/s, avg {avg:.0f} ms, p95 {p95:.0f} ms")

f.close()
print("added to", OUT)
