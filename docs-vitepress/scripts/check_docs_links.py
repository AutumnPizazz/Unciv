#!/usr/bin/env python3
"""检查 VitePress 构建产物中的内部链接有效性（/Unciv/ 前缀链接）。"""
import re
import pathlib
import html as htmllib
import urllib.parse
import sys

dist = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else '.')

files = set()
for p in dist.rglob('*'):
    if p.is_file():
        files.add(str(p.relative_to(dist)).replace('\\', '/'))


def resolve(href: str):
    href = href.split('#')[0].split('?')
    href = urllib.parse.unquote(href[0])
    if not href.startswith('/Unciv/'):
        return None
    rel = href[len('/Unciv/'):]
    if rel == '' or rel.endswith('/'):
        rel += 'index.html'
    if not rel.endswith('.html'):
        rel += '.html'
    return rel


def is_locale_switcher_link(page_rel: str, href: str) -> bool:
    """语言切换器链接 = 当前页面的另一个 locale 版本（目标页面可能尚未翻译，404 属预期）。"""
    if not href.startswith('/Unciv/'):
        return False
    rel = href[len('/Unciv/'):]
    if rel.endswith('.html'):
        rel = rel[:-5]
    if rel.endswith('index'):
        rel = rel[:-5]
    page = page_rel
    if page.endswith('.html'):
        page = page[:-5]
    if page.endswith('index'):
        page = page[:-5]
    if page.startswith('zh/'):
        return rel == page[3:]
    return rel == 'zh/' + page


bad = []
count = 0
for p in dist.rglob('*.html'):
    text = p.read_text(encoding='utf-8', errors='ignore')
    page_rel = str(p.relative_to(dist)).replace('\\', '/')
    for m in re.finditer(r'href="([^"]+)"', text):
        href = htmllib.unescape(m.group(1))
        # 跳过资源文件（css/js/图片等）
        if href.startswith(('/Unciv/assets/', '/Unciv/vp-icons')) or href.endswith(
                ('.css', '.js', '.woff2', '.png', '.ico', '.svg', '.jpg', '.webp', '.gif')):
            continue
        rel = resolve(href)
        if rel is None:
            continue
        count += 1
        if rel not in files:
            if is_locale_switcher_link(page_rel, href):
                continue
            bad.append((str(p), href))

print(f'内部链接总数: {count}')
print(f'坏链接数: {len(bad)}')
for page, href in bad[:20]:
    print(f'  {page} -> {href}')
sys.exit(1 if bad else 0)
