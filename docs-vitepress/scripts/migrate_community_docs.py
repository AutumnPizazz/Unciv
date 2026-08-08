#!/usr/bin/env python3
"""
一次性脚本：将 unciv-chinese-community 仓库内容全量移植到本仓库 docs/zh/。

移植范围（共 40 页）：
    原版专区/   （5 页）   模组专区/   （4 页）
    开发者专区/ （26 页）  更新日志/   （4 页）

处理内容：
1. 复制全部文件（含 md 引用的图片等资源）
2. md 内部绝对路径链接加 /zh 前缀（如 /模组专区/... → /zh/模组专区/...）
3. <Badge type="tip" text="最新" /> 组件转纯文本（VitePress 默认主题无 Badge 组件）
4. frontmatter 保留原样（社区页面只有 title）

社区仓库停更留档，本脚本只在初始移植时使用一次。
"""
import re
import shutil
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
COMMUNITY_ROOT = Path(r'C:\Users\27162\Documents\GitHub\unciv-chinese-community')

DIRS = ['原版专区', '开发者专区', '模组专区', '更新日志']

# 社区内部绝对路径链接（部署在根路径），移植到 /zh/ 下需加前缀；
# 路径允许以 / 结尾或以 .md / .md#锚点 结尾（如 /更新日志/UncivCN/轮询联机.md）
ABSOLUTE_LINK_RE = re.compile(
    r'(?P<open>\]\(|link: |href=")(?P<path>/(?:原版专区|开发者专区|模组专区|更新日志)/[^)\s"}]*)')

BADGE_RE = re.compile(r'<Badge\s+type="(?P<type>[^"]*)"\s+text="(?P<text>[^"]*)"\s*/>')


def rewrite_markdown(content: str, rel: str) -> str:
    original = content

    # 1. 绝对路径链接加 /zh 前缀（frontmatter 的 link: 字段同样处理）
    content = ABSOLUTE_LINK_RE.sub(lambda m: m.group('open') + '/zh' + m.group('path'), content)

    # 2. Badge 组件转纯文本
    content = BADGE_RE.sub(lambda m: f'**{m.group("text")}**', content)

    if content != original:
        print(f'  [rewrite] {rel}')
    return content


def main() -> None:
    if not COMMUNITY_ROOT.exists():
        print(f'错误：社区仓库不存在 {COMMUNITY_ROOT}')
        return 1

    total_md = 0
    for dirname in DIRS:
        src = COMMUNITY_ROOT / dirname
        dst = REPO_ROOT / 'docs' / 'zh' / dirname
        if not src.exists():
            print(f'[skip] {dirname} 不存在')
            continue
        # 先删旧目标（幂等），再整体复制
        if dst.exists():
            shutil.rmtree(dst)
        shutil.copytree(src, dst)
        print(f'[copied] {dirname}')

        for file in sorted(dst.rglob('*')):
            if file.is_file() and file.suffix.lower() == '.md':
                rel = file.relative_to(REPO_ROOT / 'docs')
                rewritten = rewrite_markdown(file.read_text(encoding='utf-8'), str(rel))
                file.write_text(rewritten, encoding='utf-8')
                total_md += 1

    print(f'\n共移植 {total_md} 个 md 文件到 docs/zh/')
    return 0


if __name__ == '__main__':
    sys.exit(main())
