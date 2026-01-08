#!/usr/bin/env python3
import os
import sys
from heapq import nlargest

ROOT = sys.argv[1] if len(sys.argv) > 1 else "."

results = []

for root, dirs, files in os.walk(ROOT):
    # optional speed-up filter: skip .git
    dirs[:] = [d for d in dirs if d != ".git"]

    for fname in files:
        path = os.path.join(root, fname)
        try:
            with open(path, "rb") as f:
                # count number of newline bytes
                line_count = sum(1 for _ in f)
        except Exception:
            continue  # skip unreadable files

        results.append((line_count, path))

# keep only the top 100
top100 = nlargest(100, results, key=lambda x: x[0])

for count, path in top100:
    print(f"{count:10d}  {path}")
