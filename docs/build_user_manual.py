"""Build the printable manual from USER_MANUAL.md using ReportLab.

Run with the Codex bundled Python, or Python with reportlab installed.
Pass --verify to also extract/check text and render page proof images.
"""

from pathlib import Path
import argparse
import hashlib
import re
from html import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate, Frame, PageBreak, PageTemplate, Paragraph, Preformatted,
    Spacer, Table, TableStyle,
)
from reportlab.platypus.tableofcontents import TableOfContents

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / 'docs' / 'USER_MANUAL.md'
OUTPUT = ROOT / 'output' / 'pdf' / 'Saaf_Milk_User_Manual.pdf'
PROOFS = ROOT / 'tmp' / 'pdfs' / 'manual_qa'
INK = colors.HexColor('#233337')
GREEN = colors.HexColor('#225B4C')
MUTED = colors.HexColor('#536469')
PALE = colors.HexColor('#F0F5F3')
RULE = colors.HexColor('#D9E3DF')


def register_fonts():
    font_dir = Path('C:/Windows/Fonts')
    candidates = [('arial.ttf', 'arialbd.ttf'), ('calibri.ttf', 'calibrib.ttf')]
    for regular, bold in candidates:
        if (font_dir / regular).is_file() and (font_dir / bold).is_file():
            pdfmetrics.registerFont(TTFont('Manual', str(font_dir / regular)))
            pdfmetrics.registerFont(TTFont('ManualBold', str(font_dir / bold)))
            pdfmetrics.registerFontFamily('Manual', normal='Manual', bold='ManualBold',
                                         italic='Manual', boldItalic='ManualBold')
            return 'Manual', 'ManualBold'
    return 'Helvetica', 'Helvetica-Bold'


NORMAL, BOLD = register_fonts()
styles = getSampleStyleSheet()
styles.add(ParagraphStyle('ManualBody', fontName=NORMAL, fontSize=10.2, leading=14,
                         textColor=INK, spaceAfter=7, allowWidows=0, allowOrphans=0))
styles.add(ParagraphStyle('ManualChapter', fontName=BOLD, fontSize=19, leading=24,
                         textColor=GREEN, spaceAfter=17, keepWithNext=True))
styles.add(ParagraphStyle('ManualSub', fontName=BOLD, fontSize=12.3, leading=17,
                         textColor=GREEN, spaceBefore=9, spaceAfter=7, keepWithNext=True))
styles.add(ParagraphStyle('ManualList', parent=styles['ManualBody'], leftIndent=13,
                         firstLineIndent=-10, spaceAfter=4))
styles.add(ParagraphStyle('ManualCode', fontName='Courier', fontSize=8.3, leading=12,
                         textColor=INK, backColor=PALE, borderPadding=10,
                         spaceBefore=4, spaceAfter=13))
styles.add(ParagraphStyle('ManualCell', fontName=NORMAL, fontSize=9.1, leading=12.5,
                         textColor=INK, wordWrap='CJK'))
styles.add(ParagraphStyle('ManualCellHead', parent=styles['ManualCell'], fontName=BOLD,
                         textColor=colors.white))
styles.add(ParagraphStyle('ManualCoverTitle', fontName=BOLD, fontSize=34, leading=40,
                         textColor=GREEN, spaceAfter=17))
styles.add(ParagraphStyle('ManualCoverSub', fontName=NORMAL, fontSize=17, leading=24,
                         textColor=INK, spaceAfter=22))
styles.add(ParagraphStyle('ManualTOC', fontName=NORMAL, fontSize=11, leading=16,
                         textColor=INK, leftIndent=0, firstLineIndent=0, spaceBefore=6))


def inline(value):
    value = escape(value)
    value = re.sub(r'\*\*(.+?)\*\*', r'<b>\1</b>', value)
    value = re.sub(r'`([^`]+)`', r'<font name="Courier" size="8.5">\1</font>', value)
    return value


class ManualDoc(BaseDocTemplate):
    def afterFlowable(self, flowable):
        if isinstance(flowable, Paragraph) and getattr(flowable, 'toc_title', None):
            label = flowable.toc_title
            key = 'chapter-' + hashlib.sha1(label.encode()).hexdigest()[:12]
            self.canv.bookmarkPage(key)
            self.canv.addOutlineEntry(label, key, level=0)
            self.notify('TOCEntry', (0, label, self.page, key))


def footer(canvas, doc):
    if doc.page == 1:
        return
    width, height = A4
    canvas.saveState()
    canvas.setStrokeColor(RULE)
    canvas.line(20 * mm, height - 16 * mm, width - 20 * mm, height - 16 * mm)
    canvas.setFont(BOLD, 8)
    canvas.setFillColor(GREEN)
    canvas.drawString(20 * mm, height - 12 * mm, 'SAAF MILK  /  USER MANUAL')
    canvas.setFont(NORMAL, 8)
    canvas.setFillColor(MUTED)
    canvas.drawRightString(width - 20 * mm, height - 12 * mm, '11 October 2026')
    canvas.line(20 * mm, 17 * mm, width - 20 * mm, 17 * mm)
    canvas.drawString(20 * mm, 12 * mm, 'Current-source edition | PKR | Android')
    canvas.drawRightString(width - 20 * mm, 12 * mm, str(doc.page))
    canvas.restoreState()


def make_table(lines, available, section_title=None):
    rows = [[cell.strip() for cell in line.strip().strip('|').split('|')] for line in lines]
    rows = [row for row in rows if not all(re.fullmatch(r':?-+:?', cell) for cell in row)]
    count = len(rows[0])
    if count == 2:
        fractions = [0.34, 0.66]
    elif count == 3:
        fractions = [0.27, 0.24, 0.49]
    else:
        fractions = [1 / count] * count
    rendered = [[Paragraph(inline(cell), styles['ManualCellHead' if i == 0 else 'ManualCell'])
                 for cell in row] for i, row in enumerate(rows)]
    header = 0
    if section_title:
        rendered.insert(0, [Paragraph(inline(section_title), styles['ManualSub'])] + [''] * (count - 1))
        header = 1
    result = Table(rendered, colWidths=[available * frac for frac in fractions],
                   repeatRows=header + 1, hAlign='LEFT', spaceAfter=12)
    commands = [
        ('BACKGROUND', (0, header), (-1, header), GREEN),
        ('ROWBACKGROUNDS', (0, header + 1), (-1, -1), [colors.white, PALE]),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('TOPPADDING', (0, 0), (-1, -1), 7),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 7),
        ('LEFTPADDING', (0, 0), (-1, -1), 8),
        ('RIGHTPADDING', (0, 0), (-1, -1), 8),
        ('LINEBELOW', (0, header), (-1, header), 0.6, GREEN),
        ('LINEBELOW', (0, header + 1), (-1, -1), 0.25, RULE),
    ]
    if section_title:
        commands.extend([
            ('SPAN', (0, 0), (-1, 0)),
            ('LEFTPADDING', (0, 0), (-1, 0), 0),
            ('TOPPADDING', (0, 0), (-1, 0), 3),
            ('BOTTOMPADDING', (0, 0), (-1, 0), 4),
        ])
    result.setStyle(TableStyle(commands))
    return result


def build():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    doc = ManualDoc(str(OUTPUT), pagesize=A4, leftMargin=20 * mm, rightMargin=20 * mm,
                    topMargin=23 * mm, bottomMargin=23 * mm,
                    title='Saaf Milk User Manual', author='Saaf Milk project',
                    subject='Setup, daily operations, money, closing, recovery and 24/7 suitability')
    frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height,
                  leftPadding=0, bottomPadding=0, rightPadding=0, topPadding=0)
    doc.addPageTemplates(PageTemplate(id='manual', frames=[frame], onPage=footer))
    lines = SOURCE.read_text(encoding='utf-8').splitlines()
    story = [Spacer(1, 24 * mm)]
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if not line:
            i += 1
            continue
        if line.startswith('# '):
            story.append(Paragraph('Saaf Milk<br/>User Manual', styles['ManualCoverTitle']))
            i += 1
            continue
        if line.startswith('## Contents'):
            story.append(PageBreak())
            story.append(Paragraph('Contents', styles['ManualChapter']))
            toc = TableOfContents()
            toc.levelStyles = [styles['ManualTOC']]
            story.append(toc)
            i += 1
            while i < len(lines) and not lines[i].startswith('## '):
                i += 1
            continue
        if line.startswith('## '):
            story.append(PageBreak())
            title = line[3:]
            heading = Paragraph(inline(title), styles['ManualChapter'])
            heading.toc_title = title
            story.append(heading)
            i += 1
            continue
        if line.startswith('### '):
            next_nonblank = i + 1
            while next_nonblank < len(lines) and not lines[next_nonblank].strip():
                next_nonblank += 1
            if next_nonblank < len(lines) and lines[next_nonblank].strip().startswith('|'):
                block = []
                i = next_nonblank
                while i < len(lines) and lines[i].strip().startswith('|'):
                    block.append(lines[i])
                    i += 1
                story.append(make_table(block, doc.width, section_title=line[4:]))
                continue
            story.append(Paragraph(inline(line[4:]), styles['ManualSub']))
            i += 1
            continue
        if line.startswith('```'):
            block = []
            i += 1
            while i < len(lines) and not lines[i].startswith('```'):
                block.append(lines[i])
                i += 1
            assert max(map(len, block), default=0) <= 88, 'Formula too wide'
            story.append(Preformatted('\n'.join(block), styles['ManualCode']))
            i += 1
            continue
        if line.startswith('|'):
            block = []
            while i < len(lines) and lines[i].strip().startswith('|'):
                block.append(lines[i])
                i += 1
            story.append(make_table(block, doc.width))
            continue
        if line.startswith('- ') or re.match(r'^\d+\. ', line):
            numbered = re.match(r'^(\d+)\. (.*)', line)
            prefix, content = (numbered[1] + '.', numbered[2]) if numbered else ('-', line[2:])
            story.append(Paragraph(escape(prefix) + ' ' + inline(content), styles['ManualList']))
            i += 1
            continue
        paragraph = [line]
        i += 1
        while i < len(lines) and lines[i].strip() and not re.match(r'^(#|\||```|- |\d+\. )', lines[i]):
            paragraph.append(lines[i].strip())
            i += 1
        style = 'ManualCoverSub' if len(story) == 2 else 'ManualBody'
        story.append(Paragraph(inline(' '.join(paragraph)), styles[style]))
    doc.multiBuild(story)
    print(f'PDF created: {OUTPUT}')


def verify():
    import json
    from decimal import Decimal, ROUND_HALF_UP
    import pdfplumber
    import pypdfium2 as pdfium
    from PIL import Image, ImageDraw
    from pypdf import PdfReader

    PROOFS.mkdir(parents=True, exist_ok=True)
    reader = PdfReader(OUTPUT)
    all_text = '\n'.join(page.extract_text() or '' for page in reader.pages)
    normalized_pdf = re.sub(r'\s+', '', all_text)
    missing_source = []
    for line in SOURCE.read_text(encoding='utf-8').splitlines():
        if not line.strip() or line.startswith('```'):
            continue
        if line.startswith('|'):
            fragments = [cell.strip() for cell in line.strip().strip('|').split('|')]
        else:
            fragments = [re.sub(r'^(?:#{1,3} |\- )', '', line)]
        for fragment in fragments:
            plain = fragment.replace('**', '').replace('`', '')
            plain = re.sub(r'\s+', '', plain)
            if len(plain) > 25 and plain not in normalized_pdf:
                missing_source.append(fragment)
    assert not missing_source, f'Source text missing from PDF: {missing_source[:5]}'
    raw, fat, lr, standard, rate, fee = map(Decimal, ['100', '4', '30', '13', '170', '11'])
    snf = Decimal('0.25') * lr + Decimal('0.22') * fat + Decimal('0.72')
    owed = (raw * ((fat + snf) / standard) * rate + raw * fee).quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)
    assert owed == Decimal('18230.77')
    assert 10 + 100 - 20 - 30 - 10 - 2 == 48
    assert 5 + 17 - 4 - 3 - 1 == 14
    assert 5000 + 6600 + 1120 + 1000 - 5000 - 300 - 1500 == 6920
    assert 1500 + 2200 + 890 - 1000 == 3590
    assert Decimal(2000) + owed - Decimal(5000) == Decimal('15230.77')
    assert 10000 + 100 - 7000 - 200 - 500 - 800 == 1600
    for phrase in ['24/7', 'PKR 6,920', 'PKR 15,230.77', '14 days', 'Owner drawing',
                   'Contents', 'Current limits and verification notes']:
        assert phrase in all_text, f'Missing expected text: {phrase}'
    with pdfplumber.open(OUTPUT) as pdf:
        page_report = []
        for number, page in enumerate(pdf.pages, 1):
            chars = page.chars
            out = [c for c in chars if c['x0'] < 0 or c['x1'] > page.width + 0.5 or
                   c['top'] < 0 or c['bottom'] > page.height + 0.5]
            assert not out, f'Text outside page {number}'
            body = [c for c in chars if c['top'] > 52 and c['bottom'] < page.height - 54]
            page_report.append({'page': number, 'body_chars': len(body),
                                'last_lines': (page.extract_text() or '').splitlines()[-4:]})
    pdf = pdfium.PdfDocument(str(OUTPUT))
    thumbnails = []
    for number in range(len(pdf)):
        page = pdf[number]
        bitmap = page.render(scale=1.35)
        rendered = bitmap.to_pil().convert('RGB')
        rendered.save(PROOFS / f'page-{number + 1:02d}.png')
        thumb = rendered.copy()
        thumb.thumbnail((360, 510))
        tile = Image.new('RGB', (380, 540), '#dde5e1')
        tile.paste(thumb, ((380 - thumb.width) // 2, 20))
        ImageDraw.Draw(tile).text((14, 522), f'Page {number + 1}', fill='#233337')
        thumbnails.append(tile)
        bitmap.close()
        page.close()
    pdf.close()
    for start in range(0, len(thumbnails), 6):
        chunk = thumbnails[start:start + 6]
        sheet = Image.new('RGB', (1140, 1080), 'white')
        for slot, tile in enumerate(chunk):
            sheet.paste(tile, ((slot % 3) * 380, (slot // 3) * 540))
        sheet.save(PROOFS / f'contact-{start // 6 + 1:02d}.png')
    report = {'pages': len(reader.pages), 'words': len(all_text.split()),
              'sha256': hashlib.sha256(OUTPUT.read_bytes()).hexdigest(),
              'checks': 'All substantive source fragments preserved; example arithmetic checked; no text beyond page bounds',
              'page_details': page_report}
    (PROOFS / 'verification.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps({k: v for k, v in report.items() if k != 'page_details'}, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--verify', action='store_true')
    args = parser.parse_args()
    build()
    if args.verify:
        verify()
