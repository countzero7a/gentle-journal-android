#!/usr/bin/env python3
"""Checks app/src/main/res/values-*/strings.xml against the English values/strings.xml.

- every file is well-formed XML
- every language has exactly the English string and plurals keys
- each translation uses the same format placeholders as the English text
- apostrophes and double quotes are escaped (aapt rejects bare ones)

Exits non-zero and lists the problems if anything is off.
"""
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")
PLACEHOLDER = re.compile(r"%(?:(\d+)\$)?([sd])")


def load(path):
    root = ET.parse(path).getroot()
    strings = {}
    for el in root:
        name = el.get("name")
        if el.tag == "string":
            strings[name] = [el.text or ""]
        elif el.tag == "plurals":
            strings[name] = [(item.text or "") for item in el.findall("item")]
    return strings


def placeholders(text):
    """Placeholders as sorted (position, type); unnumbered ones count from 1 in order."""
    found, position = [], 0
    for index, kind in PLACEHOLDER.findall(text.replace("%%", "")):
        position = int(index) if index else position + 1
        found.append((position, kind))
    return sorted(set(found))


def unescaped_quotes(path):
    problems = []
    for number, line in enumerate(open(path, encoding="utf-8"), 1):
        body = re.sub(r"^\s*<[^>]+>|</[^>]+>\s*$", "", line.rstrip("\n"))
        if not line.lstrip().startswith(("<string", "<item")):
            continue
        if re.search(r"(?<!\\)'", body) or re.search(r'(?<!\\)"', body):
            problems.append(f"line {number}: unescaped quote in {body.strip()[:60]!r}")
    return problems


def main():
    english = load(os.path.join(RES, "values", "strings.xml"))
    problems = []
    for path in sorted(glob.glob(os.path.join(RES, "values-*", "strings.xml"))):
        lang = os.path.basename(os.path.dirname(path))
        try:
            translated = load(path)
        except ET.ParseError as e:
            problems.append(f"{lang}: not well-formed: {e}")
            continue
        for key in english.keys() - translated.keys():
            problems.append(f"{lang}: missing {key}")
        for key in translated.keys() - english.keys():
            problems.append(f"{lang}: {key} is not in the English file")
        for key in english.keys() & translated.keys():
            expected = placeholders(english[key][-1])
            for text in translated[key]:
                if placeholders(text) != expected:
                    problems.append(f"{lang}: {key} placeholders {placeholders(text)} != English {expected}")
        problems += [f"{lang}: {p}" for p in unescaped_quotes(path)]

    for p in problems:
        print(p)
    print(f"{len(problems)} problem(s)" if problems else "All translations complete and consistent.")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
