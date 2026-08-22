#!/usr/bin/env python3
"""Regenerate the waste code nomenclator seed from the primary source.

Source: Commission Decision 2014/955/EU (Romanian version) on EUR-Lex, which replaces
the list of waste in Decision 2000/532/EC and applies from 1 June 2015. The Romanian
HG 856/2002 Annex 2 list is NOT used: it is the 2002/2007 vintage and the copies in
circulation carry transcription errors (see docs/surse-oficiale.md, final annex).

The Official Journal HTML renders the list as two-column `tr.oj-table` rows:
    "01"          -> chapter heading    (name in capitals)
    "01 01"       -> subchapter heading (name wrapped in span.oj-bold)
    "01 01 01"    -> waste code, optionally suffixed with '*' for hazardous
The separator inside a code is U+00A0 (no-break space), not a plain space.

Both heading levels are written to the CSV as structured comments, so the seed file
mirrors the structure of the official list and WasteCodeSeedTest can re-check, without
this script, that every code sits under the chapter and subchapter its digits claim.

Requires: beautifulsoup4 (pip install beautifulsoup4).

Usage:
    python scripts/generate_waste_codes.py                  # download and rewrite the seed
    python scripts/generate_waste_codes.py --html led.html  # parse a local snapshot
    python scripts/generate_waste_codes.py --check          # validate only, do not write
"""

from __future__ import annotations

import argparse
import datetime as dt
import pathlib
import re
import sys
import urllib.request

from bs4 import BeautifulSoup

SOURCE_URL = "https://eur-lex.europa.eu/legal-content/RO/TXT/HTML/?uri=CELEX:32014D0955"
SOURCE_LABEL = "Decizia 2014/955/UE (versiunea RO), EUR-Lex CELEX 32014D0955"

REPO_ROOT = pathlib.Path(__file__).resolve().parent.parent
CSV_PATH = REPO_ROOT / "backend/src/main/resources/seed/waste_codes.csv"

# Regression fingerprint from docs/surse-oficiale.md, section 3.1. A regeneration that does
# not reproduce it exactly means the source changed - stop and look, do not overwrite.
EXPECTED_TOTAL = 842
EXPECTED_HAZARDOUS = 408
EXPECTED_PER_CHAPTER = {
    "01": 24, "02": 38, "03": 19, "04": 21, "05": 24, "06": 48, "07": 78, "08": 38,
    "09": 13, "10": 173, "11": 27, "12": 23, "13": 34, "14": 5, "15": 12, "16": 72,
    "17": 38, "18": 16, "19": 99, "20": 40,
}

NBSP = chr(0x00A0)  # separator inside a code in the OJ HTML
CHAPTER_RE = re.compile(r"^\d{2}$")
SUBCHAPTER_RE = re.compile(r"^\d{2} \d{2}$")
CODE_RE = re.compile(r"^\d{2} \d{2} \d{2}\*?$")

CHAPTER_MARK = "# =="
SUBCHAPTER_MARK = "# --"


class ParseError(RuntimeError):
    pass


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": "EcoRegistru-seed/1.0"})
    with urllib.request.urlopen(request, timeout=120) as response:
        return response.read().decode("utf-8")


def normalize(text: str) -> str:
    return re.sub(r"\s+", " ", text.replace(NBSP, " ")).strip()


def parse(html: str) -> list[dict]:
    """Returns chapters, subchapters and codes in the order they appear in the Journal."""
    soup = BeautifulSoup(html, "html.parser")
    entries: list[dict] = []
    chapter = None
    subchapter = None

    for row in soup.find_all("tr", class_="oj-table"):
        cells = row.find_all("td")
        if len(cells) != 2:
            continue
        key = normalize(cells[0].get_text())
        name = normalize(cells[1].get_text())
        if not key or not name:
            continue

        if CHAPTER_RE.match(key):
            chapter, subchapter = key, None
            entries.append({"kind": "chapter", "key": key, "name": name})
        elif SUBCHAPTER_RE.match(key):
            subchapter = key
            entries.append({"kind": "subchapter", "key": key, "name": name})
        elif CODE_RE.match(key):
            entries.append({
                "kind": "code",
                "code": key.rstrip("*"),
                "name": name,
                "hazardous": key.endswith("*"),
                # Headings this code was physically listed under - checked below against
                # the code's own digits. A parser that trusts "last heading seen" without
                # that check is exactly what corrupted the secondary sources.
                "under_chapter": chapter,
                "under_subchapter": subchapter,
            })

    if not any(entry["kind"] == "code" for entry in entries):
        raise ParseError("no waste codes found - the source layout probably changed")

    # The document opens with a chapter index (two-digit rows only) before the list proper;
    # drop the headings emitted before the first real code so the CSV starts at chapter 01.
    first_code = next(i for i, e in enumerate(entries) if e["kind"] == "code")
    list_start = max(i for i, e in enumerate(entries[:first_code]) if e["kind"] == "chapter")
    return entries[list_start:]


def codes_of(entries: list[dict]) -> list[dict]:
    return [entry for entry in entries if entry["kind"] == "code"]


def validate(entries: list[dict]) -> list[str]:
    errors: list[str] = []
    codes = codes_of(entries)

    seen: dict[str, int] = {}
    for entry in codes:
        seen[entry["code"]] = seen.get(entry["code"], 0) + 1
    for code, count in sorted(seen.items()):
        if count > 1:
            errors.append("duplicate code %s (%d occurrences)" % (code, count))

    for entry in codes:
        code = entry["code"]
        if not re.fullmatch(r"\d{2} \d{2} \d{2}", code):
            errors.append("invalid code format: %r" % code)
            continue
        if entry["under_subchapter"] != code[:5]:
            errors.append("code %s listed under subchapter %s" % (code, entry["under_subchapter"]))
        if entry["under_chapter"] != code[:2]:
            errors.append("code %s listed under chapter %s" % (code, entry["under_chapter"]))
        if not entry["name"]:
            errors.append("code %s has an empty name" % code)

    per_chapter: dict[str, int] = {}
    for entry in codes:
        chapter = entry["code"][:2]
        per_chapter[chapter] = per_chapter.get(chapter, 0) + 1
    if per_chapter != EXPECTED_PER_CHAPTER:
        for chapter in sorted(set(per_chapter) | set(EXPECTED_PER_CHAPTER)):
            got = per_chapter.get(chapter, 0)
            want = EXPECTED_PER_CHAPTER.get(chapter, 0)
            if got != want:
                errors.append("chapter %s: %d codes, fingerprint expects %d" % (chapter, got, want))

    hazardous = sum(1 for entry in codes if entry["hazardous"])
    if len(codes) != EXPECTED_TOTAL:
        errors.append("total %d codes, fingerprint expects %d" % (len(codes), EXPECTED_TOTAL))
    if hazardous != EXPECTED_HAZARDOUS:
        errors.append("%d hazardous codes, fingerprint expects %d" % (hazardous, EXPECTED_HAZARDOUS))

    return errors


def render_csv(entries: list[dict], source: str, accessed: str) -> str:
    codes = codes_of(entries)
    hazardous = sum(1 for entry in codes if entry["hazardous"])
    lines = [
        "# EcoRegistru — nomenclator coduri deșeuri (Lista Europeană a Deșeurilor)",
        "#",
        "# Sursă: " + SOURCE_LABEL,
        "#   " + source,
        "#   accesat: " + accessed + ". Se aplică de la 1 iunie 2015; înlocuiește lista din",
        "#   Decizia 2000/532/CE. NU se folosește anexa 2 la HG 856/2002 (versiunea 2002/2007,",
        "#   iar copiile în circulație au erori de transcriere — vezi docs/surse-oficiale.md).",
        "#   Amprentă: %d coduri, din care %d periculoase." % (len(codes), hazardous),
        "#",
        "# GENERAT — nu edita manual. Regenerare: python scripts/generate_waste_codes.py",
        "# Validat de script și de testul WasteCodeSeedTest (unicitate, format, capitol,",
        "# subcapitol, amprenta pe capitole).",
        "#",
        "# Format: code,name,hazardous",
        '#   code      = 6 cifre formatate "NN NN NN" (FĂRĂ asteriscul de periculozitate)',
        "#   name      = denumirea oficială în română, verbatim; poate conține virgule, deci",
        "#               la citire: codul e până la prima virgulă, hazardous după ultima",
        "#   hazardous = true dacă în LED codul e marcat cu '*' (deșeu periculos), altfel false",
        "#",
        "# Titlurile de capitol (" + CHAPTER_MARK + ") și de subcapitol (" + SUBCHAPTER_MARK + ")",
        "# păstrează structura listei oficiale: fiecare cod trebuie să stea sub capitolul și",
        "# subcapitolul date de propriile lui cifre. Sunt comentarii — seed-ul le ignoră.",
    ]
    for entry in entries:
        if entry["kind"] == "chapter":
            lines.append("")
            lines.append("%s %s %s" % (CHAPTER_MARK, entry["key"], entry["name"]))
        elif entry["kind"] == "subchapter":
            lines.append("%s %s %s" % (SUBCHAPTER_MARK, entry["key"], entry["name"]))
        else:
            lines.append("%s,%s,%s" % (entry["code"], entry["name"], str(entry["hazardous"]).lower()))
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--html", help="parse this local HTML snapshot instead of downloading")
    parser.add_argument("--check", action="store_true", help="validate only, do not write the CSV")
    parser.add_argument("--out", default=str(CSV_PATH), help="output CSV path")
    args = parser.parse_args()

    if args.html:
        html = pathlib.Path(args.html).read_text(encoding="utf-8")
        source = "snapshot local: " + args.html
    else:
        html = fetch(SOURCE_URL)
        source = SOURCE_URL

    entries = parse(html)
    errors = validate(entries)
    if errors:
        print("VALIDATION FAILED (%d problems):" % len(errors), file=sys.stderr)
        for error in errors[:40]:
            print("  - " + error, file=sys.stderr)
        return 1

    codes = codes_of(entries)
    hazardous = sum(1 for entry in codes if entry["hazardous"])
    print("OK: %d codes, %d hazardous" % (len(codes), hazardous))
    if args.check:
        return 0

    out = pathlib.Path(args.out)
    out.write_text(render_csv(entries, source, dt.date.today().strftime("%d.%m.%Y")), encoding="utf-8")
    print("written: " + str(out))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
