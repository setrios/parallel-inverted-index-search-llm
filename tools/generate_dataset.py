'''
Generate a synthetic English text dataset: doc_00001.txt, doc_00002.txt, ... in --out.
'''
import argparse
import random
from itertools import accumulate
from pathlib import Path

from wordfreq import top_n_list

WORDS_PER_SENTENCE = 12

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--out', default='data', help='output directory (default: data)')
parser.add_argument('--docs', type=int, default=20000, help='number of documents (default: 20000)')
parser.add_argument('--sentences', type=int, default=1550, help='sentences per document (default: 1550, about 100 KB)')
parser.add_argument('--seed', type=int, default=80085, help='random seed (default: 80085)')
args = parser.parse_args()

vocab = top_n_list('en', 20000)
cum_weights = list(accumulate(1 / n for n in range(1, len(vocab) + 1)))  # the n-th most frequent word has weight 1/n

rng = random.Random(args.seed)
out = Path(args.out)
out.mkdir(parents=True, exist_ok=True)

for i in range(1, args.docs + 1):
    n = int(args.sentences * rng.uniform(0.5, 1.5))  # document size varies from 50% to 150% of the average

    words = rng.choices(vocab, cum_weights=cum_weights, k=n * WORDS_PER_SENTENCE)
    sentences = [' '.join(words[j:j + WORDS_PER_SENTENCE]).capitalize() + '.' for j in range(0, len(words), WORDS_PER_SENTENCE)]
    paragraphs, pos = [], 0

    while pos < n:  # paragraphs of 4-8 sentences
        size = rng.randint(4, 8)
        paragraphs.append(' '.join(sentences[pos:pos + size]))
        pos += size

    file_name = out / f'doc_{i:05d}.txt'
    file_name.write_text('\n\n'.join(paragraphs) + '\n', encoding='utf-8')

    if i % 1000 == 0:
        print(f'{i}/{args.docs} documents')

print(f'done: {args.docs} documents in {out}/')