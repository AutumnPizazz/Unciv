#!/usr/bin/env python3
"""
一次性修复：迁移后 docs/zh/ 镜像目录中的链接指向。
restructure_zh.py 的链接重写因源文件已移动而按新位置解析，产生
`/zh/Modders/模组文件结构/概述` 这类「新目录前缀 + 旧中文文件名」的链接。
本脚本把镜像目录（Modders/Developers/Translating）下所有 md 中的此类链接
统一修正为英文文件名路径，并顺带修复社区原有的死链（Uniques 系统/Unique 参数）。
"""
import re
import urllib.parse
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
ZH = REPO_ROOT / 'docs' / 'zh'

# 旧 basename（去 .md）-> 新 basename
BASENAME = {
    '模组': 'Mods',
    '模组文件结构': 'Mod-file-structure',
    '概述': '1-Overview',
    '文明相关JSON文件': '2-Civilization-related-JSON-files',
    '地图相关JSON文件': '3-Map-related-JSON-files',
    '单位相关JSON文件': '4-Unit-related-JSON-files',
    '其他JSON文件': '5-Miscellaneous-JSON-files',
    '创建新文明': 'Making-a-new-Civilization',
    '创建UI皮肤': 'Creating-a-UI-skin',
    '自定义地形集': 'Creating-a-custom-tileset',
    '图像和音频资源': 'Images-and-Audio',
    'Lua脚本': 'Lua-Modding',
    '场景制作': 'Scenarios',
    '类型检查': 'Type-checking',
    'Unique参数详解': 'Unique-parameters',
    'Unique能力列表': 'uniques',
    '自动更新指南': 'Autoupdates',
    'UncivCN扩展JSON-MergeAction教程': '6-MergeActions',
    '项目结构': 'Project-structure-and-major-classes',
    '代码标准': 'Coding-standards',
    'UI 开发': 'UI-development',
    'Uniques 机制': 'Uniques',
    'Uniques 系统': 'Uniques',  # 社区原有死链，顺带修正
    'Unique 参数': 'Unique-parameters',  # 社区原有死链
    '构建和部署': 'From-code-to-deployment',
    '翻译指南': 'Translating',
    '翻译生成': 'Translation-generation',
    '模组翻译': 'Translation-mods',
}

# 旧目录前缀 -> 新目录前缀
PREFIX = {
    '开发者专区/模组开发': 'Modders',
    '开发者专区/代码贡献': 'Developers',
    '开发者专区/翻译本地化': 'Translating',
    '开发者专区/源码分析': 'Developers/源码分析',
    '模组开发': 'Modders',
    '代码贡献': 'Developers',
    '翻译本地化': 'Translating',
    'Modders': 'Modders',
    'Developers': 'Developers',
    'Translating': 'Translating',
}


def fix_path(rel: str) -> str | None:
    """修正 /zh/ 下的路径段。返回修正后的路径，无变化返回 None。"""
    if rel.endswith('/'):
        rel = rel[:-1]
    parts = rel.split('/')
    changed = False
    # 前缀映射
    for plen in (3, 2, 1):
        prefix = '/'.join(parts[:plen])
        if prefix in PREFIX:
            new_prefix = PREFIX[prefix]
            parts = new_prefix.split('/') + parts[plen:]
            changed = True
            break
    # basename 映射（每个路径段，先去掉 .md 后缀）
    for i, part in enumerate(parts):
        key = part[:-3] if part.endswith('.md') else part
        if key in BASENAME:
            parts[i] = BASENAME[key]
            changed = True
    if not changed:
        return None
    return '/'.join(parts)


def rewrite(content: str) -> str:
    def repl(m: re.Match) -> str:
        open_, link, close = m.group(1), m.group(2), m.group(3)
        anchor = ''
        if '#' in link:
            link, anchor = link.split('#', 1)
        if not link.startswith('/zh/'):
            return m.group(0)
        rel = urllib.parse.unquote(link[len('/zh/'):])
        fixed = fix_path(rel)
        if fixed is None:
            return m.group(0)
        out = f'{open_}/zh/{fixed}'
        if anchor:
            out += '#' + anchor
        return out + close

    return re.sub(r'(\]\(|<a href=")([^)"\r\n]+)(\)|")', repl, content)


def main() -> int:
    total = 0
    for file in ZH.rglob('*.md'):
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
