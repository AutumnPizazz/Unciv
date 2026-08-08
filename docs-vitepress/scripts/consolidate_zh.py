#!/usr/bin/env python3
"""
一次性脚本：中文独有内容全部归入 UncivCN/ 目录，实现中英结构一一对应。

移动：
  Developers/源码分析/          -> UncivCN/源码分析/
  原版专区/                     -> UncivCN/原版专区/
  模组专区/                     -> UncivCN/模组专区/
  更新日志/UncivCN/轮询联机.md   -> UncivCN/轮询联机.md
  更新日志/Unciv原版/index.md    -> UncivCN/Unciv原版更新日志.md

合并：社区版 UncivCN 更新日志的早期版本（v4.20.7.2 及以前）并入 UncivCN/更新日志.md
删除：更新日志/ 目录

链接修正：/zh/开发者专区... 旧前缀已修过，这里处理 /zh/源码分析、/zh/原版专区、
/zh/模组专区、/zh/更新日志 前缀 -> /zh/UncivCN/ 前缀，以及 UncivCN 页面内的相对链接。
"""
import re
import shutil
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
ZH = REPO_ROOT / 'docs' / 'zh'
UCN = ZH / 'UncivCN'

PREFIX_FIX = [
    ('/zh/开发者专区/源码分析', '/zh/UncivCN/源码分析'),
    ('/zh/Developers/源码分析', '/zh/UncivCN/源码分析'),
    ('/zh/原版专区', '/zh/UncivCN/原版专区'),
    ('/zh/模组专区', '/zh/UncivCN/模组专区'),
]


def move(src: Path, dst: Path) -> None:
    if not src.exists():
        print(f'[skip] {src}')
        return
    if dst.exists():
        shutil.rmtree(dst) if dst.is_dir() else dst.unlink()
    if src.is_dir():
        shutil.copytree(src, dst)
        shutil.rmtree(src)
    else:
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(src), str(dst))
    print(f'[move] {src.relative_to(ZH)} -> {dst.relative_to(ZH)}')


def rewrite_links_in(file: Path) -> None:
    content = file.read_text(encoding='utf-8')
    original = content
    for old, new in PREFIX_FIX:
        content = content.replace(old, new)
    # UncivCN 页内旧相对链接
    content = content.replace('](../更新日志/UncivCN/轮询联机.md)', '](./轮询联机)')
    content = content.replace('](/zh/更新日志/UncivCN/轮询联机.md)', '](/zh/UncivCN/轮询联机)')
    content = content.replace('](/zh/更新日志/UncivCN/轮询联机)', '](/zh/UncivCN/轮询联机)')
    if content != original:
        file.write_text(content, encoding='utf-8')
        print(f'[links] {file.relative_to(ZH)}')


def main() -> int:
    # 1. 移动独有内容到 UncivCN/
    move(ZH / 'Developers' / '源码分析', UCN / '源码分析')
    move(ZH / '原版专区', UCN / '原版专区')
    move(ZH / '模组专区', UCN / '模组专区')
    move(ZH / '更新日志' / 'UncivCN' / '轮询联机.md', UCN / '轮询联机.md')
    move(ZH / '更新日志' / 'Unciv原版' / 'index.md', UCN / 'Unciv原版更新日志.md')

    # 2. 合并社区版早期版本（v4.20.7.2 及以前）到 更新日志.md
    community_log = ZH / '更新日志' / 'UncivCN' / 'index.md'
    if community_log.exists():
        lines = community_log.read_text(encoding='utf-8').split('\n')
        # 找 "## v4.20.7.2" 开始，到 "::: tip 参与反馈" 结束
        start = next((i for i, l in enumerate(lines) if l.startswith('## v4.20.7.2')), None)
        end = next((i for i, l in enumerate(lines) if l.startswith('::: tip 参与反馈')), None)
        if start is not None:
            early = lines[start:end]
            # 标题带日期转换：## vX.Y.Z + **发布日期**：2026.5.19 -> ## vX.Y.Z（2026.5.19）
            out = []
            i = 0
            while i < len(early):
                line = early[i]
                if line.startswith('## v'):
                    date = ''
                    if i + 1 < len(early) and early[i + 1].strip().startswith('**发布日期**'):
                        date = early[i + 1].strip().split('：', 1)[-1].strip()
                        i += 1
                    out.append(f'## {line[4:]}{f"（{date}）" if date else ""}')
                elif line.strip() == '---':
                    pass  # 去掉分隔线
                else:
                    out.append(line)
                i += 1
            target = UCN / '更新日志.md'
            content = target.read_text(encoding='utf-8')
            if '## v4.20.7.2' not in content:
                content = content.rstrip() + '\n\n---\n\n' + '\n'.join(out) + '\n'
                target.write_text(content, encoding='utf-8')
                print(f'[merge] 早期版本 {len(out)} 行并入 更新日志.md')
            else:
                print('[skip] 早期版本已存在')
        # 3. 删除社区日志目录
        shutil.rmtree(ZH / '更新日志')
        print('[remove] 更新日志/')

    # 4. 链接修正（UncivCN 全部页面 + 首页）
    for file in UCN.rglob('*.md'):
        rewrite_links_in(file)
    rewrite_links_in(ZH / 'index.md')
    return 0


if __name__ == '__main__':
    sys.exit(main())
