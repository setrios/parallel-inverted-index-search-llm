#!/usr/bin/env python3

"""
Compares TF-IDF and BM25 with different weights of the LLM words on queries
where we know which book should be found.
usage: python3 scripts/ranking_experiment.py
"""

import math
import re
from collections import Counter
from pathlib import Path

DIR = Path("data")

# query, words from the LLM (copied from the server log), right books
QUERIES = [
    ("alice rabbit", [], ["11"]),
    ("whale", ["mammal", "marine mammal", "cetacean", "sea creature", "aquatic animal"], ["2701"]),
    ("sherlock holmes", ["detective", "investigator", "sleuth"], ["108", "48320", "3070", "2852"]),
    ("time traveller machine", ["time machine", "chronal accelerator", "temporal displacement device",
                                "chronometer", "temporal portal"], ["35"]),
    ("dracula", ["vampire", "count", "nosferatu", "bloodsucker", "undead"], ["10150"]),
    ("scrooge", ["miser", "miserly", "cheapskate", "penny-pincher", "miserableness"], ["30368", "28198", "40729"]),
    ("dorian gray", ["vanity", "morality", "beauty", "corruption", "decadence"], ["174", "26740"]),
    ("natural selection species", ["evolution", "adaptation", "survival", "genetics", "biodiversity"], ["2009"]),
    ("hamlet ophelia", ["tragedy", "shakespearean", "madness", "love", "despair"], ["2265"]),
    ("fierce bad rabbit", ["fierce", "ferocious", "savage", "intense", "formidable"], ["45265"]),
]


def tokenize(text):
    # same as Tokenizer.java: lower case, split on everything that is not a letter or a digit
    return re.findall(r"[^\W_]+", text.lower())


def unique(words):
    result = []
    for w in words:
        if w not in result:
            result.append(w)
    return result


# words of every query like in SearchService: llm words that repeat the query are dropped
queries = []
needed = set()
for text, llm, right in QUERIES:
    original = unique(tokenize(text))
    expanded = [w for w in unique(tokenize(" ".join(llm))) if w not in original]
    queries.append((text, original, expanded, right))
    needed.update(original + expanded)

# read every book once, keep only its length and counts of the words we need
files = sorted(DIR.glob("*.txt"))  # same order as the server, so docId is the same
names = []
lengths = []
counts = {}  # word -> {docId: count}
for w in needed:
    counts[w] = {}

for doc_id, file in enumerate(files):
    words = tokenize(file.read_text(encoding="utf-8"))
    names.append(file.stem)
    lengths.append(len(words))
    for w, n in Counter(x for x in words if x in needed).items():
        counts[w][doc_id] = n
    if (doc_id + 1) % 1000 == 0:
        print(f"{doc_id + 1}/{len(files)} files")

N = len(files)
avg_length = sum(lengths) / N


def tfidf_scores(word, weight, b):
    docs = counts[word]
    idf = math.log(1 + N / len(docs))
    return {d: weight * tf * idf for d, tf in docs.items()}


def bm25_scores(word, weight, b):
    k1 = 1.2
    docs = counts[word]
    idf = math.log(1 + (N - len(docs) + 0.5) / (len(docs) + 0.5))
    scores = {}
    for d, tf in docs.items():
        length_norm = 1 - b + b * lengths[d] / avg_length
        scores[d] = weight * idf * tf * (k1 + 1) / (tf + k1 * length_norm)
    return scores


# returns the 10 best (book, score) like the server does
def search(formula, original, expanded, expanded_weight, b):
    totals = Counter()
    for w in original:
        if counts[w]:
            totals.update(formula(w, 1.0, b))
    for w in expanded:
        if counts[w] and expanded_weight > 0:
            totals.update(formula(w, expanded_weight, b))

    best = sorted(totals.items(), key=lambda e: (-e[1], e[0]))[:10]  # equal scores by docId
    if not best:
        return []
    top = best[0][1]
    return [(names[d], score / top) for d, score in best]


# "shared" = 0.5 for all llm words together, like the server now
tests = [("tfidf", 0.5, 0), ("tfidf", 0, 0)]
for b in [0.75, 0.5, 0.3]:
    for weight in [0.5, 0.3, 0.2, 0.1, "shared", 0]:
        tests.append(("bm25", weight, b))

print()
print("place of the right book in the top 10 (- = not in the top 10)")
header = f"{'formula':26}"
for text, *_ in queries:
    header += f"{text[:9]:>10}"
print(header + "   top1  top10   MRR")

for formula, weight, b in tests:
    places = []
    for text, original, expanded, right in queries:
        w = weight
        if weight == "shared":
            w = 0.5 / len(expanded) if expanded else 0
        results = search(tfidf_scores if formula == "tfidf" else bm25_scores, original, expanded, w, b)

        place = None
        for i, (name, score) in enumerate(results):
            if name in right:
                place = i + 1
                break
        places.append(place)

    label = f"{formula} llm={weight}"
    if formula == "bm25":
        label += f" b={b}"
    line = f"{label:26}"
    for p in places:
        line += f"{p if p else '-':>10}"
    top1 = places.count(1)
    top10 = len([p for p in places if p])
    mrr = sum(1 / p for p in places if p) / len(places)
    print(line + f"   {top1:4}  {top10:5}  {mrr:.2f}")

# the first results for "whale" must be the same as the server gives
print()
text, original, expanded, right = queries[1]
for formula, name in [(tfidf_scores, "tfidf llm=0.5"), (bm25_scores, "bm25 llm=0.5 b=0.75")]:
    top3 = search(formula, original, expanded, 0.5, 0.75)[:3]
    print(f"check {name}, whale:", ", ".join(f"{n}.txt {s:.3f}" for n, s in top3))
