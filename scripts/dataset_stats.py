#!/usr/bin/env python3

"""Counts numbers about the dataset in data/ for the report"""

import re
import statistics
from pathlib import Path

DIR = Path("data")

# same rule as Tokenizer.java: lower case, split on everything that is not a letter or a digit
WORD = re.compile(r"[^\W_]+")

sizes = []
words_per_doc = []
distinct_per_doc = []
vocabulary = set()

files = sorted(DIR.glob("*.txt"))
for i, file in enumerate(files, 1):
    sizes.append(file.stat().st_size)
    words = WORD.findall(file.read_text(encoding="utf-8").lower())
    distinct = set(words)
    words_per_doc.append(len(words))
    distinct_per_doc.append(len(distinct))
    vocabulary.update(distinct)
    if i % 500 == 0:
        print(f"{i}/{len(files)} files")

postings = sum(distinct_per_doc)  # one posting for every distinct word of every document

print()
print("documents:                ", len(files))
print("total size, bytes:        ", sum(sizes))
print("file size, bytes:          min", min(sizes), " median", int(statistics.median(sizes)), " max", max(sizes))
print("words per document:        avg", round(statistics.mean(words_per_doc)), " median", int(statistics.median(words_per_doc)))
print("distinct words per doc:    avg", round(statistics.mean(distinct_per_doc)), " median", int(statistics.median(distinct_per_doc)))
print("words in all documents:   ", sum(words_per_doc))
print("postings (index entries): ", postings)
print("distinct words in index:  ", len(vocabulary))
print("words / postings:         ", round(sum(words_per_doc) / postings, 2))
