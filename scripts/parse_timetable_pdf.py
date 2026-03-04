#!/usr/bin/env python3
"""Offline timetable parser for campus schedule PDFs.

Usage:
  python scripts/parse_timetable_pdf.py --input "E:\\MyCode\\SB-courselist\\sample.pdf"
  python scripts/parse_timetable_pdf.py --input-dir "E:\\MyCode\\SB-courselist" --out-dir "parse_outputs"
"""

from __future__ import annotations

import argparse
import csv
import json
import re
import sys
from collections import Counter, defaultdict
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

import pdfplumber


if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")


DAY_MAP = {
    "\u4e00": 1,  # Monday
    "\u4e8c": 2,
    "\u4e09": 3,
    "\u56db": 4,
    "\u4e94": 5,
    "\u516d": 6,
    "\u65e5": 7,  # Sunday
    "\u5929": 7,
}

DAY_EN_MAP = {
    "mon": 1,
    "tue": 2,
    "wed": 3,
    "thu": 4,
    "fri": 5,
    "sat": 6,
    "sun": 7,
}

PERIOD_RE = re.compile(
    r"[\(\uff08]\s*(\d{1,2})\s*[-~\u301c\u2013\u2014\u2015\uff0d\u81f3]\s*(\d{1,2})\s*\u8282\s*[\)\uff09]"
)
WEEK_RE = re.compile(r"(\d{1,2}(?:-\d{1,2})?\u5468(?:[,\uff0c]\d{1,2}(?:-\d{1,2})?\u5468)*)")
LOCATION_LABEL_RE = re.compile(r"(?:\u573a\u5730|\u5730\u70b9|\u6559\u5ba4)[:\uff1a]\s*")
TEACHER_LABEL_RE = re.compile(r"(?:\u6559\u5e08|\u8001\u5e08)[:\uff1a]?\s*")

FIELD_STOP_MARKERS = (
    "/",
    "\uff0f",
    "\u6559\u5e08",
    "\u8001\u5e08",
    "\u6559\u5b66\u73ed",
    "\u8bfe\u7a0b\u5b66\u65f6",
    "\u8003\u6838\u65b9\u5f0f",
    "\u9009\u8bfe\u5907\u6ce8",
)

NAME_NOISE = (
    "\u8bfe\u7a0b\u5b66\u65f6",  # 课程学时
    "\u8003\u6838\u65b9\u5f0f",  # 考核方式
    "\u6559\u5b66\u73ed",  # 教学班
    "\u9009\u8bfe\u5907\u6ce8",  # 选课备注
    "\u5468\u5b66\u65f6",  # 周学时
    "\u5b66\u5206",  # 学分
    "\u6821\u533a",  # 校区
    "\u573a\u5730",  # 场地
    "\u6559\u5e08",  # 教师
)

WEAK_NAME_EXACT = {
    "\u5b9e\u9a8c",  # 实验
    "\u5b9e\u8df5",  # 实践
    "\u7406\u8bba",  # 理论
    "\u8df5\u8bfe",  # 践课
    "\u6307\u5bfc",  # 指导
    "\u6307\u5bfc1",
    "\u6307\u5bfc2",
    "\u8bfe\u7a0b",  # 课程
    "\u5b66\u5206",  # 学分
    "\u5b66\u65f6",  # 学时
}


@dataclass
class ParsedCourse:
    page: int
    day: int
    start_period: int
    end_period: int
    weeks: str
    name: str
    location: str
    teacher: str
    raw: str


@dataclass
class ParseStats:
    total: int
    unknown_name: int
    missing_weeks: int
    missing_location: int
    missing_teacher: int


@dataclass
class LineItem:
    top: float
    bottom: float
    text: str


def normalize_text(text: str) -> str:
    return (
        text.replace("\u3000", " ")
        .replace(" ", "")
        .replace("|", "")
        .replace("\t", "")
        .replace("\r", "")
        .replace("\n", "")
        .strip()
    )


def parse_day_label(text: str) -> Optional[int]:
    text = normalize_text(text)
    if not text:
        return None

    if text.startswith("\u661f\u671f") and len(text) >= 3:
        return DAY_MAP.get(text[2])
    if text.startswith("\u5468") and len(text) >= 2:
        return DAY_MAP.get(text[1])
    if text.startswith("\u793c\u62dc") and len(text) >= 3:
        return DAY_MAP.get(text[2])
    if len(text) == 1:
        return DAY_MAP.get(text)

    low = text.lower()
    for key, value in DAY_EN_MAP.items():
        if low.startswith(key):
            return value
    return None


def looks_like_name(text: str) -> bool:
    if not text or len(text) < 2:
        return False
    if PERIOD_RE.search(text):
        return False
    if "/" in text or ":" in text or "\uff1a" in text:
        return False
    if re.fullmatch(r"\d{1,2}(?::\d{2})?", text):
        return False
    if not re.search(r"[A-Za-z0-9\u4e00-\u9fff]", text):
        return False
    if any(token in text for token in NAME_NOISE):
        return False
    if is_weak_name(text):
        return False
    return True


def clean_name(text: str) -> str:
    text = normalize_text(text)
    text = re.sub(r"^[^A-Za-z0-9\u4e00-\u9fff]+", "", text)
    text = re.sub(r"[*&@]+$", "", text).strip()
    # Balance trailing brackets in extracted names, e.g. "xxx（慕课" -> "xxx（慕课）".
    full_open = text.count("\uff08")
    full_close = text.count("\uff09")
    if full_open > full_close:
        text += "\uff09" * (full_open - full_close)
    ascii_open = text.count("(")
    ascii_close = text.count(")")
    if ascii_open > ascii_close:
        text += ")" * (ascii_open - ascii_close)
    return text


def clean_value(text: str) -> str:
    text = normalize_text(text)
    text = text.strip("/")
    text = text.replace("\uff0f", "/")
    return text


def is_weak_name(text: str) -> bool:
    text = clean_name(text)
    if not text:
        return True
    if text in WEAK_NAME_EXACT:
        return True
    if re.fullmatch(r"[:\uff1a0-9.,\uff0c]+", text):
        return True
    if len(text) <= 3 and re.fullmatch(r"[\u4e00-\u9fff0-9]+", text):
        if text.endswith(("\u8bfe", "\u9a8c", "\u8df5", "\u5bfc", "\u8bba")):
            return True
    return False


def extract_day_centers(words: Sequence[dict]) -> Tuple[Dict[int, float], float]:
    best_by_day: Dict[int, dict] = {}
    for w in words:
        day = parse_day_label(w["text"])
        if day is None:
            continue
        prev = best_by_day.get(day)
        if prev is None or w["top"] < prev["top"]:
            best_by_day[day] = w

    if len(best_by_day) < 5:
        return {}, float("-inf")

    centers = {day: (w["x0"] + w["x1"]) / 2.0 for day, w in best_by_day.items()}
    header_bottom = max(w["bottom"] for w in best_by_day.values())
    return centers, header_bottom


def build_day_bounds(day_centers: Dict[int, float]) -> Dict[int, Tuple[float, float]]:
    ordered = sorted(day_centers.items(), key=lambda kv: kv[1])
    if len(ordered) < 5:
        return {}

    bounds: Dict[int, Tuple[float, float]] = {}
    for idx, (day, x) in enumerate(ordered):
        if idx == 0:
            next_x = ordered[idx + 1][1]
            left = x - (next_x - x) / 2.0
        else:
            prev_x = ordered[idx - 1][1]
            left = (prev_x + x) / 2.0

        if idx == len(ordered) - 1:
            prev_x = ordered[idx - 1][1]
            right = x + (x - prev_x) / 2.0
        else:
            next_x = ordered[idx + 1][1]
            right = (x + next_x) / 2.0

        bounds[day] = (left, right)
    return bounds


def assign_words_by_day(
    words: Sequence[dict],
    day_bounds: Dict[int, Tuple[float, float]],
    header_bottom: float,
) -> Dict[int, List[dict]]:
    assigned: Dict[int, List[dict]] = {day: [] for day in day_bounds}
    for w in words:
        if w["top"] <= header_bottom + 1.5:
            continue
        x_center = (w["x0"] + w["x1"]) / 2.0
        for day, (left, right) in day_bounds.items():
            if left <= x_center < right:
                assigned[day].append(w)
                break
    return assigned


def words_to_lines(words: Sequence[dict], y_tol: float = 3.4) -> List[LineItem]:
    if not words:
        return []

    sorted_words = sorted(words, key=lambda w: (w["top"], w["x0"]))
    lines: List[List[dict]] = []

    for w in sorted_words:
        if not lines:
            lines.append([w])
            continue
        prev_line = lines[-1]
        prev_top = prev_line[0]["top"]
        if abs(w["top"] - prev_top) <= y_tol:
            prev_line.append(w)
        else:
            lines.append([w])

    result: List[LineItem] = []
    for line_words in lines:
        ordered = sorted(line_words, key=lambda w: w["x0"])
        text = "".join(normalize_text(w["text"]) for w in ordered)
        text = clean_value(text)
        if not text:
            continue
        result.append(
            LineItem(
                top=min(w["top"] for w in ordered),
                bottom=max(w["bottom"] for w in ordered),
                text=text,
            )
        )
    return result


def pick_name_from_prelude(lines: Sequence[LineItem]) -> str:
    cleaned = [clean_name(line.text) for line in lines if clean_name(line.text)]
    if not cleaned:
        return ""

    for idx in range(len(cleaned) - 1, -1, -1):
        current = cleaned[idx]
        if idx > 0:
            merged = clean_name(cleaned[idx - 1] + current)
            if looks_like_name(cleaned[idx - 1]) and looks_like_name(merged):
                return merged
        if looks_like_name(current):
            return current

    # Last-resort merge for split names like "大学生心理健康教育实" + "践课".
    if len(cleaned) >= 2:
        merged = clean_name(cleaned[-2] + cleaned[-1])
        if looks_like_name(merged):
            return merged
    return ""


def extract_weeks(detail_text: str, joined: str) -> str:
    match = PERIOD_RE.search(detail_text)
    if match:
        tail = detail_text[match.end() :]
        week_match = WEEK_RE.search(tail)
        if week_match:
            return clean_value(week_match.group(1))

    joined_match = PERIOD_RE.search(joined)
    if joined_match:
        tail = joined[joined_match.end() : joined_match.end() + 160]
        week_match = WEEK_RE.search(tail)
        if week_match:
            return clean_value(week_match.group(1))
    return ""


def extract_field(text: str, regex: re.Pattern[str]) -> str:
    match = regex.search(text)
    if not match:
        return ""
    value = clean_value(match.group(1))
    value = re.split(
        r"(?:\u6559\u5b66\u73ed|\u8bfe\u7a0b\u5b66\u65f6|\u8003\u6838\u65b9\u5f0f|\u9009\u8bfe\u5907\u6ce8)",
        value,
    )[0]
    return clean_value(value)


def extract_segment(text: str, label_regex: re.Pattern[str]) -> str:
    match = label_regex.search(text)
    if not match:
        return ""
    tail = text[match.end() :]
    cut_positions = [pos for marker in FIELD_STOP_MARKERS if (pos := tail.find(marker)) >= 0]
    end = min(cut_positions) if cut_positions else len(tail)
    return clean_value(tail[:end])


def extract_tail_name(text: str) -> str:
    tail_start = 0
    for marker in (
        "\u8bfe\u7a0b\u5b66\u65f6",
        "\u9009\u8bfe\u5907\u6ce8",
        "\u8003\u6838\u65b9\u5f0f",
        "\u6559\u5b66\u73ed\u7ec4\u6210",
        "\u6559\u5b66\u73ed",
        "\u5b66\u5206",
        "\u603b\u5b66\u65f6",
        "\u5468\u5b66\u65f6",
    ):
        idx = text.rfind(marker)
        if idx >= 0:
            tail_start = max(tail_start, idx + len(marker))

    tail = clean_name(text[tail_start:])
    tail = re.sub(r"^[:\uff1a0-9.,\uff0c]+", "", tail)
    tail = clean_name(tail)
    if looks_like_name(tail):
        return tail

    parts = [clean_name(part) for part in re.split(r"[/\uff0f]", text) if part]
    for part in reversed(parts):
        if looks_like_name(part):
            return part
    return ""


def pick_stable(counter: Counter[str]) -> str:
    if not counter:
        return ""
    common = counter.most_common(2)
    if len(common) == 1:
        return common[0][0]
    first, second = common[0], common[1]
    if first[1] >= second[1] + 2:
        return first[0]
    return ""


def fill_missing_fields(courses: Sequence[ParsedCourse]) -> List[ParsedCourse]:
    if not courses:
        return []

    by_name_teacher: Dict[str, Counter[str]] = defaultdict(Counter)
    by_name_location: Dict[str, Counter[str]] = defaultdict(Counter)
    by_teacher_name: Dict[Tuple[str, int, int], Counter[str]] = defaultdict(Counter)
    by_teacher_any_name: Dict[str, Counter[str]] = defaultdict(Counter)
    by_teacher_location_name: Dict[Tuple[str, str], Counter[str]] = defaultdict(Counter)

    for c in courses:
        if c.name and c.name != "unknown_course":
            if c.teacher:
                by_name_teacher[c.name][c.teacher] += 1
                by_teacher_name[(c.teacher, c.start_period, c.end_period)][c.name] += 1
                by_teacher_any_name[c.teacher][c.name] += 1
            if c.location:
                by_name_location[c.name][c.location] += 1
            if c.teacher and c.location:
                by_teacher_location_name[(c.teacher, c.location)][c.name] += 1

    enriched = [ParsedCourse(**asdict(c)) for c in courses]
    for c in enriched:
        if c.name == "unknown_course":
            guessed = ""
            if c.teacher and c.location:
                guessed = pick_stable(by_teacher_location_name[(c.teacher, c.location)])
            if not guessed and c.teacher:
                guessed = pick_stable(by_teacher_name[(c.teacher, c.start_period, c.end_period)])
            if not guessed and c.teacher:
                guessed = pick_stable(by_teacher_any_name[c.teacher])
            if guessed:
                c.name = guessed

        if c.name and c.name != "unknown_course":
            if not c.teacher:
                guessed_teacher = pick_stable(by_name_teacher[c.name])
                if guessed_teacher:
                    c.teacher = guessed_teacher
            elif len(c.teacher) <= 1:
                candidates = by_name_teacher[c.name]
                better = [
                    teacher
                    for teacher in candidates
                    if teacher.startswith(c.teacher) and len(teacher) > len(c.teacher)
                ]
                if better:
                    better.sort(key=lambda teacher: (candidates[teacher], len(teacher)), reverse=True)
                    c.teacher = better[0]
            if not c.location:
                guessed_location = pick_stable(by_name_location[c.name])
                if guessed_location:
                    c.location = guessed_location

        if c.name == "unknown_course":
            tail_guess = extract_tail_name(c.raw)
            if tail_guess:
                c.name = tail_guess

        if c.name and c.name != "unknown_course" and not c.name.endswith("\u5b9e\u9a8c"):
            has_lab_only = (
                "\u8bfe\u7a0b\u5b66\u65f6\u7ec4\u6210:\u5b9e\u9a8c" in c.raw
                and "\u7406\u8bba" not in c.raw
            )
            if has_lab_only:
                c.name = f"{c.name}\u5b9e\u9a8c"

    return enriched


def extract_name_from_joined(joined: str, period_start: int) -> str:
    prefix = joined[:period_start]
    chunks = [c for c in re.split(r"[/\uff0f]", prefix) if c]
    for chunk in reversed(chunks):
        chunk = clean_name(chunk)
        if looks_like_name(chunk):
            return chunk
    prefix = clean_name(prefix)
    if looks_like_name(prefix):
        return prefix
    return "unknown_course"


def parse_lines_into_courses(day: int, page_index: int, lines: Sequence[LineItem]) -> List[ParsedCourse]:
    if not lines:
        return []

    detail_indices = [i for i, line in enumerate(lines) if PERIOD_RE.search(line.text)]
    if not detail_indices:
        return []

    courses: List[ParsedCourse] = []
    for idx_pos, detail_idx in enumerate(detail_indices):
        prev_idx = detail_indices[idx_pos - 1] if idx_pos > 0 else -1
        next_idx = detail_indices[idx_pos + 1] if idx_pos + 1 < len(detail_indices) else len(lines)

        prelude = lines[prev_idx + 1 : detail_idx]
        block = lines[detail_idx:next_idx]
        if not block:
            continue

        detail_text = block[0].text
        joined = "".join(line.text for line in block)
        period_match = PERIOD_RE.search(joined)
        if not period_match:
            continue

        start_period = int(period_match.group(1))
        end_period = int(period_match.group(2))
        if start_period > end_period:
            start_period, end_period = end_period, start_period

        name_hint = pick_name_from_prelude(prelude)
        name = name_hint if name_hint else extract_name_from_joined(joined, period_match.start())
        name = clean_name(name) or "unknown_course"

        weeks = extract_weeks(detail_text, joined)
        location = extract_segment(joined, LOCATION_LABEL_RE)
        teacher = extract_segment(joined, TEACHER_LABEL_RE)

        courses.append(
            ParsedCourse(
                page=page_index,
                day=day,
                start_period=max(1, min(14, start_period)),
                end_period=max(1, min(14, end_period)),
                weeks=weeks,
                name=name,
                location=location,
                teacher=teacher,
                raw=joined,
            )
        )
    return courses


def dedupe_courses(courses: Sequence[ParsedCourse]) -> List[ParsedCourse]:
    seen = set()
    unique: List[ParsedCourse] = []
    for c in sorted(courses, key=lambda item: (item.day, item.start_period, item.end_period, item.name)):
        key = (c.day, c.start_period, c.end_period, c.weeks, c.name, c.location, c.teacher)
        if key in seen:
            continue
        seen.add(key)
        unique.append(c)
    return unique


def parse_pdf(pdf_path: Path) -> List[ParsedCourse]:
    parsed: List[ParsedCourse] = []
    reference_day_centers: Dict[int, float] = {}
    reference_header_bottom = float("-inf")

    with pdfplumber.open(str(pdf_path)) as pdf:
        for page_index, page in enumerate(pdf.pages, start=1):
            words = page.extract_words(x_tolerance=2, y_tolerance=2)
            words = [w for w in words if normalize_text(w["text"])]
            if not words:
                continue

            day_centers, header_bottom = extract_day_centers(words)
            if len(day_centers) >= 5:
                reference_day_centers = day_centers
                reference_header_bottom = header_bottom
            else:
                day_centers = reference_day_centers
                header_bottom = reference_header_bottom

            if len(day_centers) < 5:
                continue

            day_bounds = build_day_bounds(day_centers)
            if len(day_bounds) < 5:
                continue

            by_day = assign_words_by_day(words, day_bounds, header_bottom)
            for day, day_words in by_day.items():
                lines = words_to_lines(day_words)
                parsed.extend(parse_lines_into_courses(day=day, page_index=page_index, lines=lines))

    enriched = fill_missing_fields(parsed)
    return dedupe_courses(enriched)


def calc_stats(courses: Sequence[ParsedCourse]) -> ParseStats:
    return ParseStats(
        total=len(courses),
        unknown_name=sum(1 for c in courses if c.name == "unknown_course"),
        missing_weeks=sum(1 for c in courses if not c.weeks),
        missing_location=sum(1 for c in courses if not c.location),
        missing_teacher=sum(1 for c in courses if not c.teacher),
    )


def write_json(path: Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")


def write_csv(path: Path, courses: Sequence[ParsedCourse]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(
            [
                "page",
                "day",
                "start_period",
                "end_period",
                "weeks",
                "name",
                "location",
                "teacher",
            ]
        )
        for c in courses:
            writer.writerow(
                [
                    c.page,
                    c.day,
                    c.start_period,
                    c.end_period,
                    c.weeks,
                    c.name,
                    c.location,
                    c.teacher,
                ]
            )


def print_stats(pdf_path: Path, stats: ParseStats) -> None:
    print(
        f"{pdf_path.name}: total={stats.total}, "
        f"unknown_name={stats.unknown_name}, missing_weeks={stats.missing_weeks}, "
        f"missing_location={stats.missing_location}, missing_teacher={stats.missing_teacher}"
    )


def parse_single(input_path: Path, output_path: Path, export_csv: bool) -> None:
    courses = parse_pdf(input_path)
    stats = calc_stats(courses)
    write_json(output_path, [asdict(c) for c in courses])
    if export_csv:
        write_csv(output_path.with_suffix(".csv"), courses)
    print_stats(input_path, stats)
    print(f"output => {output_path}")


def parse_batch(input_dir: Path, pattern: str, out_dir: Path, export_csv: bool) -> None:
    pdfs = sorted(input_dir.glob(pattern))
    if not pdfs:
        raise FileNotFoundError(f"No PDF found under {input_dir} with pattern: {pattern}")

    summary = []
    for pdf_path in pdfs:
        courses = parse_pdf(pdf_path)
        stats = calc_stats(courses)
        output_file = out_dir / f"{pdf_path.stem}.parsed.json"
        write_json(output_file, [asdict(c) for c in courses])
        if export_csv:
            write_csv(out_dir / f"{pdf_path.stem}.parsed.csv", courses)
        print_stats(pdf_path, stats)
        summary.append({"pdf": pdf_path.name, **asdict(stats), "output": str(output_file)})

    summary_path = out_dir / "summary.json"
    write_json(summary_path, summary)
    print(f"summary => {summary_path}")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Parse timetable PDFs into structured JSON.")
    parser.add_argument("--input", type=Path, help="Single PDF path to parse.")
    parser.add_argument("--output", type=Path, help="Output JSON path for --input mode.")
    parser.add_argument(
        "--input-dir",
        type=Path,
        default=Path.cwd(),
        help="Directory for batch mode (default: current directory).",
    )
    parser.add_argument(
        "--pattern",
        default="*.pdf",
        help="Glob pattern for batch mode (default: *.pdf).",
    )
    parser.add_argument(
        "--out-dir",
        type=Path,
        default=Path("parse_outputs"),
        help="Output directory for batch mode (default: parse_outputs).",
    )
    parser.add_argument(
        "--csv",
        action="store_true",
        help="Also export CSV files for manual review.",
    )
    return parser


def main() -> None:
    args = build_parser().parse_args()
    if args.input:
        output = args.output or args.input.with_suffix(".parsed.json")
        parse_single(args.input, output, export_csv=args.csv)
        return
    parse_batch(args.input_dir, args.pattern, args.out_dir, export_csv=args.csv)


if __name__ == "__main__":
    main()
