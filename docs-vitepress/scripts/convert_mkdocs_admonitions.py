#!/usr/bin/env python3
"""
一次性脚本：将 mkdocs 特有 admonition 语法（!!! / ???）转换为 VitePress 容器（:::）语法。

mkdocs:
    !!! note
    ??? example "Gain a free [buildingName]"
    !!! note ""
    内容以 4 空格（或 tab）缩进

VitePress:
    ::: note
    ::: details 标题          # 可折叠块用 details
    内容以 4 空格（或 tab）缩进
    块结束需显式 :::

注意：uniques.md / Unique-parameters.md / 6-MergeActions.md 由 Kotlin 生成器输出，
不在此脚本处理（生成器已改为直接输出 VitePress 格式）。
"""
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

# mkdocs 的 admonition 起始行：!!! / ??? + 类型 + 可选 "标题"
ADMONITION_RE = re.compile(r'^(?P<marker>!!!|\?\?\?)\s+(?P<type>\S+)\s*(?P<title>".*")?\s*$')


def convert_text(text: str) -> str:
    lines = text.split('\n')
    out: list[str] = []
    in_block = False  # 当前是否在 admonition 块内

    for line in lines:
        stripped = line.strip()
        m = ADMONITION_RE.match(stripped)
        if m:
            # 新块开始；上一个块若未闭合则先闭合
            if in_block:
                out.append(':::')
            in_block = True
            marker, atype, title = m.group('marker'), m.group('type'), m.group('title')
            if marker == '???':
                # 可折叠：映射为 VitePress details 容器
                vtype = 'details'
            else:
                vtype = atype
            if title and title != '""':
                out.append(f'::: {vtype} {title[1:-1]}')
            else:
                out.append(f'::: {vtype}')
            continue

        if in_block:
            # 块内行：空行或缩进行（mkdocs 要求 4 空格，生成器产物用 tab）
            if line == '' or line.startswith(' ') or line.startswith('\t'):
                out.append(line)
                continue
            # 遇到非缩进内容行 = 块结束
            out.append(':::')
            in_block = False
            out.append(line)
            continue

        out.append(line)

    if in_block:
        out.append(':::')

    return '\n'.join(out)


def main() -> None:
    files = [
        'docs/Modders/Making-a-new-Civilization.md',
        'docs/Modders/Mod-file-structure/5-Miscellaneous-JSON-files.md',
        'docs/DocsSite-Plan.md',  # 规划文档，不进导航，但顺手转换避免渲染异常
    ]
    for rel in files:
        path = REPO_ROOT / rel
        if not path.exists():
            print(f'[skip] {rel} (不存在)')
            continue
        original = path.read_text(encoding='utf-8')
        converted = convert_text(original)
        if converted != original:
            path.write_text(converted, encoding='utf-8')
            print(f'[converted] {rel}')
        else:
            print(f'[unchanged] {rel}')


if __name__ == '__main__':
    sys.exit(main())
