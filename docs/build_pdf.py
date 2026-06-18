import markdown, weasyprint, re

with open("INTERVIEW_PREP.md") as f:
    md = f.read()

# Drop the trailing stray code fence if present
md = md.rstrip()
if md.endswith("```"):
    md = md[:-3].rstrip()

html_body = markdown.markdown(md, extensions=["tables", "fenced_code", "sane_lists"])

css = """
@page { size: A4; margin: 1.6cm 1.5cm 1.8cm 1.5cm;
  @bottom-center { content: "PayLedger — Interview Prep   ·   K C Darshan Siddarth   ·   Page " counter(page) " of " counter(pages);
    font-size: 8pt; color: #8a94a6; } }
* { box-sizing: border-box; }
body { font-family: 'Helvetica Neue', Arial, sans-serif; font-size: 10.2pt; line-height: 1.5;
  color: #1f2733; }
h1 { font-size: 22pt; color: #0b66c3; margin: 0 0 4px 0; border-bottom: 3px solid #0b66c3; padding-bottom: 6px; }
h2 { font-size: 14pt; color: #0b3a66; margin: 22px 0 8px 0; padding: 6px 10px;
  background: #eef4fb; border-left: 4px solid #0b66c3; border-radius: 3px; page-break-after: avoid; }
h3 { font-size: 11.5pt; color: #14507f; margin: 14px 0 5px 0; page-break-after: avoid; }
p { margin: 6px 0; }
strong { color: #0b3a66; }
a { color: #0b66c3; text-decoration: none; }
blockquote { margin: 8px 0; padding: 10px 14px; background: #f6f8fb;
  border-left: 4px solid #5b9bd5; border-radius: 4px; font-style: normal; color: #2a3340; }
blockquote p { margin: 5px 0; }
table { border-collapse: collapse; width: 100%; margin: 10px 0; font-size: 9.2pt; page-break-inside: avoid; }
th { background: #0b66c3; color: #fff; text-align: left; padding: 6px 8px; font-weight: 600; }
td { border: 1px solid #d6dde6; padding: 5px 8px; vertical-align: top; }
tr:nth-child(even) td { background: #f4f7fb; }
code { background: #eef1f5; color: #b5005a; padding: 1px 4px; border-radius: 3px;
  font-family: 'Menlo','Consolas',monospace; font-size: 8.8pt; }
pre { background: #1e2430; color: #e6edf3; padding: 12px 14px; border-radius: 6px;
  overflow-x: auto; font-size: 8.4pt; line-height: 1.4; page-break-inside: avoid; }
pre code { background: none; color: #e6edf3; padding: 0; }
hr { border: none; border-top: 1px solid #dfe5ec; margin: 16px 0; }
ul, ol { margin: 6px 0 6px 0; padding-left: 22px; }
li { margin: 3px 0; }
"""

html = f"<html><head><meta charset='utf-8'><style>{css}</style></head><body>{html_body}</body></html>"
weasyprint.HTML(string=html).write_pdf("PayLedger_Interview_Prep.pdf")
print("PDF written")
