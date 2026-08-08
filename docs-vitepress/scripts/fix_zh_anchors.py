#!/usr/bin/env python3
"""
一次性修复：restructure_zh.py 把页内锚点链接（#xxx）误转成了目录路径链接
（/zh/Modders#xxx、/zh/Modders/Mod-file-structure#xxx 等）。恢复为页内锚点。
另修正 /zh/Modders/Uniques -> /zh/Developers/Uniques（Uniques 机制文档在 Developers/）。
"""
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
ZH = REPO_ROOT / 'docs' / 'zh'

# 被误转的目录前缀锚点 -> 页内锚点（原链接就是 #anchor 形式）
DIR_ANCHOR = [
    re.compile(r'\]\(/zh/Modders#([^)]+)\)'),
    re.compile(r'\]\(/zh/Modders/Mod-file-structure#([^)]+)\)'),
    re.compile(r'\]\(/zh/Modders/模组文件结构#([^)]+)\)'),
    re.compile(r'\]\(/zh/Translating#([^)]+)\)'),
    re.compile(r'\]\(/zh/Developers#([^)]+)\)'),
]

SPECIAL = {
    '/zh/Modders/Uniques)': '/zh/Developers/Uniques)',
}


def rewrite(content: str) -> str:
    for pat in DIR_ANCHOR:
        content = pat.sub(r'](#\1)', content)
    for old, new in SPECIAL.items():
        content = content.replace(old, new)
    return content


def main() -> int:
    total = 0
    for dirname in ('Modders', 'Developers', 'Translating', 'Other'):
        base = ZH / dirname
        if not base.exists():
            continue
        for file in base.rglob('*.md'):
            content = file.read_text(encoding='utf-8')
            new = rewrite(content)
            if new != content:
                file.write_text(new, encoding='utf-8')
                print(f'[fixed] {file.relative_to(ZH)}')
                total += 1
    print(f'\n共修正 {total} 个文件')
    return 0


if __name__ == '__main__':
    sys.exit(main())
