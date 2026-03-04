# Timetable Parser Algorithm Notes

## Ready-to-use references researched
- `Ta1al/timetable-parser` (PDF timetable to JSON): https://github.com/Ta1al/timetable-parser
- `pdfplumber` (PDF text + coordinates extraction): https://github.com/jsvine/pdfplumber
- `Camelot` (table extraction for text PDFs): https://camelot-py.readthedocs.io/
- `PaddleOCR` (OCR + layout parsing fallback): https://github.com/PaddlePaddle/PaddleOCR

## Chosen approach for this project
- Primary path: `pdfplumber` + rule-based extraction (fast and stable for text-based campus timetable PDFs).
- Fallback idea (not enabled in this script): OCR for scanned/image PDFs.

## Why this path
- Fast: no OCR for text PDFs.
- Accurate on current samples: extraction with column anchors + period-line blocks + cross-record completion.
- Generalizable: rules are not hardcoded for one exact file; they use weekday headers, period patterns, and field markers.

## Local tool
- Script: `scripts/parse_timetable_pdf.py`
- Batch run:
  - `python scripts/parse_timetable_pdf.py --input-dir . --pattern "*.pdf" --out-dir parse_outputs_final`
