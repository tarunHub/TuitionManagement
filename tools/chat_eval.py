#!/usr/bin/env python3
"""Accuracy check for the "Ask" assistant against the real local model and the imported data.

    python3 tools/chat_eval.py                      # all questions
    python3 tools/chat_eval.py 3 7                  # only questions 3 and 7

Each question lists answer fragments that must appear (case-insensitive; a tuple means "any of these") and
fragments that must not. Expected values reflect the data imported from the Excel workbook on 2026-10-06 with
"today" in October 2026; update them when the data changes. Uses only the standard library; read-only.
"""
import json
import os
import sys
import time
import urllib.request

API = os.environ.get("API_URL", "http://localhost:8080/api/v1")
USER = os.environ.get("ADMIN_USERNAME", "admin")
PASSWORD = os.environ.get("ADMIN_PASSWORD", "admin")

CASES = [
    ("How many active students do we have?", ["8"], []),
    ("How many students are inactive?", ["6"], []),
    ("Who still owes fees this month?", ["Fateh", "Alaina", "Kaya", "Gurshabd"], ["Avneet", "Baani", "Ravjodh"]),
    ("How much fee did we collect in September?", [("1,285", "1285")], []),
    ("What was the total fee billed in August 2026?", ["930"], []),
    ("Who has not paid their September fees?", ["Tanisha"], ["Fateh"]),
    ("How many active students are in Batch3?", ["4"], []),
    ("What is Kaya's balance for October?", ["10"], []),
    ("What was Fateh's attendance in September?", ["12"], []),
    ("Which students were absent in August?", ["Fateh", "Ravjodh", "Kaya"], ["Alaina"]),
    ("What topics did Alaina cover in October?", ["Patterns"], []),
    ("What did Ravjodh need to improve in September?", ["reading"], []),
    ("When does the current term end?", [("December 19", "Dec 19", "2026-12-19", "19 December")], []),
    ("What grade is Avneet in?", ["Grade 5"], []),
    ("Who is Gurshabd's guardian?", ["Ashwinder"], []),
    ("Compare the fees collected in August and October.", ["960", "540"], []),
    # The Attendance page (active students only) says 88.16%; the Dashboard (all students) says 88.61%.
    ("What was the average attendance in September?", [("88.16", "88.2", "88.61", "88.6")], []),
    ("Which batch is Tanisha in?", ["Batch4"], []),
    ("Which classes are on the schedule this week?", [("no ", "not ", "haven't", "none")], []),
    ("What's the weather today?", [("only answer", "can't", "cannot", "don't", "unable", "not able")], []),
]


def post(path, body, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(API + path, json.dumps(body).encode(), headers)
    return json.load(urllib.request.urlopen(req, timeout=600))


def check(answer, must, must_not):
    low = answer.lower()
    missing = [m for m in must if not any(alt.lower() in low for alt in (m if isinstance(m, tuple) else (m,)))]
    forbidden = [m for m in must_not if m.lower() in low]
    return missing, forbidden


def main(selected):
    token = post("/auth/login", {"username": USER, "password": PASSWORD})["token"]
    passed, total_time = 0, 0.0
    cases = [(i, c) for i, c in enumerate(CASES, 1) if not selected or i in selected]
    for i, (question, must, must_not) in cases:
        start = time.time()
        try:
            r = post("/chat", {"messages": [{"role": "USER", "content": question}]}, token)
            answer = r["answer"]
            missing, forbidden = check(answer, must, must_not)
            ok = not missing and not forbidden
            info = f"tools={r['toolsUsed']} grounded={r['grounded']}"
        except Exception as e:  # report and keep going
            answer, ok, missing, forbidden, info = f"ERROR {e}", False, must, [], ""
        took = time.time() - start
        total_time += took
        passed += ok
        print(f"{'PASS' if ok else 'FAIL'} {i:2d} ({took:4.0f}s) {question}")
        if not ok:
            print(f"      answer: {' '.join(answer.split())[:400]}")
            print(f"      missing={missing} forbidden={forbidden} {info}")
    print(f"\n{passed}/{len(cases)} passed, average {total_time / max(len(cases), 1):.0f}s per question")
    return 0 if passed == len(cases) else 1


if __name__ == "__main__":
    sys.exit(main({int(a) for a in sys.argv[1:]}))
