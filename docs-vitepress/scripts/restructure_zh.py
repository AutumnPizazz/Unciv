#!/usr/bin/env python3
"""
一次性脚本：将 docs/zh/开发者专区/ 下已有的社区翻译迁移到英文镜像结构。

旧结构（社区仓库）:
    docs/zh/开发者专区/{模组开发,代码贡献,翻译本地化,源码分析}/
新结构（镜像英文 docs/）:
    docs/zh/Modders/        —— 模组开发翻译（英文文件名）
    docs/zh/Developers/     —— 代码贡献翻译 + 源码分析（独有）
    docs/zh/Translating/    —— 翻译本地化翻译

处理：
1. 文件移动（含 schemas/ 等资源目录）
2. md 内部链接重写为新的 /zh/ 绝对路径（相对链接先按旧位置解析）
3. 删除已搬空的旧目录
"""
import re
import shutil
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent  # docs-vitepress/scripts/ -> 仓库根
ZH = REPO_ROOT / 'docs' / 'zh'

# 旧相对路径(docs/zh/ 下, 无前导斜杠, 目录型含 index.md) -> 新路径(docs/zh/ 下)
MAPPING = {
    # 模组开发 -> Modders
    '开发者专区/模组开发/模组.md': 'Modders/Mods.md',
    '开发者专区/模组开发/模组文件结构/概述.md': 'Modders/Mod-file-structure/1-Overview.md',
    '开发者专区/模组开发/模组文件结构/文明相关JSON文件.md': 'Modders/Mod-file-structure/2-Civilization-related-JSON-files.md',
    '开发者专区/模组开发/模组文件结构/地图相关JSON文件.md': 'Modders/Mod-file-structure/3-Map-related-JSON-files.md',
    '开发者专区/模组开发/模组文件结构/单位相关JSON文件.md': 'Modders/Mod-file-structure/4-Unit-related-JSON-files.md',
    '开发者专区/模组开发/模组文件结构/其他JSON文件.md': 'Modders/Mod-file-structure/5-Miscellaneous-JSON-files.md',
    '开发者专区/模组开发/模组文件结构/schemas': 'Modders/Mod-file-structure/schemas',
    '开发者专区/模组开发/创建新文明.md': 'Modders/Making-a-new-Civilization.md',
    '开发者专区/模组开发/创建UI皮肤.md': 'Modders/Creating-a-UI-skin.md',
    '开发者专区/模组开发/自定义地形集.md': 'Modders/Creating-a-custom-tileset.md',
    '开发者专区/模组开发/图像和音频资源.md': 'Modders/Images-and-Audio.md',
    '开发者专区/模组开发/Lua脚本.md': 'Modders/Lua-Modding.md',
    '开发者专区/模组开发/场景制作.md': 'Modders/Scenarios.md',
    '开发者专区/模组开发/类型检查.md': 'Modders/Type-checking.md',
    '开发者专区/模组开发/Unique参数详解.md': 'Modders/Unique-parameters.md',
    '开发者专区/模组开发/Unique能力列表.md': 'Modders/uniques.md',
    '开发者专区/模组开发/自动更新指南.md': 'Modders/Autoupdates.md',
    '开发者专区/模组开发/UncivCN扩展JSON-MergeAction教程.md': 'Modders/6-MergeActions.md',
    # 代码贡献 -> Developers
    '开发者专区/代码贡献/项目结构/index.md': 'Developers/Project-structure-and-major-classes.md',
    '开发者专区/代码贡献/代码标准/index.md': 'Developers/Coding-standards.md',
    '开发者专区/代码贡献/UI 开发.md': 'Developers/UI-development.md',
    '开发者专区/代码贡献/Uniques 机制.md': 'Developers/Uniques.md',
    '开发者专区/代码贡献/构建和部署.md': 'Developers/From-code-to-deployment.md',
    # 翻译本地化 -> Translating
    '开发者专区/翻译本地化/翻译指南.md': 'Translating/Translating.md',
    '开发者专区/翻译本地化/翻译生成.md': 'Translating/Translation-generation.md',
    '开发者专区/翻译本地化/模组翻译.md': 'Translating/Translation-mods.md',
    # 源码分析（独有内容，保留中文名）
    '开发者专区/源码分析/军事实力计算方式/index.md': 'Developers/源码分析/军事实力计算方式/index.md',
    '开发者专区/源码分析/文明积分计算/index.md': 'Developers/源码分析/文明积分计算/index.md',
}


def normalize_link(link: str) -> str:
    """链接 -> (无锚点路径, 锚点)。目录型补 index.md，去掉 .md 后缀待映射比较。"""
    anchor = ''
    if '#' in link:
        link, anchor = link.split('#', 1)
    if link.endswith('/'):
        link += 'index.md'
    return link, anchor


def resolve_old_path(from_file: Path, link: str) -> str | None:
    """相对旧文件位置解析链接 -> 旧相对路径（docs/zh/ 下，含 .md）。外部/锚点返回 None。"""
    if link.startswith('http') or link.startswith('mailto:') or link.startswith('#'):
        return None
    if link.startswith('/zh/'):
        rel = link[len('/zh/'):]
        if rel.endswith('/'):
            rel += 'index.md'
        return rel
    if link.startswith('/'):
        return None  # 其他绝对路径（/Unciv/ 等）
    # 相对链接
    base = from_file.parent
    target = (base / link).resolve().relative_to(ZH.resolve())
    return str(target).replace('\\', '/')


def rewrite_links(content: str, from_file: Path) -> str:
    def repl(m: re.Match) -> str:
        prefix, link, suffix = m.group(1), m.group(2), m.group(3)
        path, anchor = normalize_link(link)
        old = resolve_old_path(from_file, path)
        if old is None:
            return m.group(0)
        new = MAPPING.get(old)
        if new is None:
            # 未迁移的目标（原版专区/模组专区/更新日志/UncivCN）：转 /zh/ 绝对链接
            new = old
        if new.endswith('.md'):
            new = new[:-3]
        if new.endswith('/index'):
            new = new[:-6]
        out = f'{prefix}/zh/{new}'
        if anchor:
            out += '#' + anchor
        out += suffix
        return out

    return re.sub(r'(\]\(|<a href=")([^)"\s]+)(\)|")', repl, content)


def main() -> int:
    moved = []
    # 1. 移动文件（资源目录用 copytree/rmtree）
    for old_rel, new_rel in sorted(MAPPING.items()):
        src = ZH / old_rel
        dst = ZH / new_rel
        if not src.exists():
            print(f'[skip 缺失] {old_rel}')
            continue
        dst.parent.mkdir(parents=True, exist_ok=True)
        if src.is_dir():
            if dst.exists():
                shutil.rmtree(dst)
            shutil.copytree(src, dst)
            shutil.rmtree(src)
        else:
            dst.parent.mkdir(parents=True, exist_ok=True)
            if dst.exists():
                dst.unlink()
            shutil.move(str(src), str(dst))
        moved.append((old_rel, new_rel))
        print(f'[move] {old_rel} -> {new_rel}')

    # 2. 重写所有迁移目标 md 的内部链接
    for old_rel, new_rel in moved:
        dst = ZH / new_rel
        if not dst.is_file() or dst.suffix != '.md':
            continue
        # 链接重写基于旧位置（相对链接语义）
        src_ref = ZH / old_rel
        if old_rel.endswith('/index.md') or (not old_rel.endswith('.md')):
            src_ref = ZH / (old_rel.rstrip('/') + '/index.md') if not old_rel.endswith('.md') else ZH / old_rel
        content = dst.read_text(encoding='utf-8')
        rewritten = rewrite_links(content, src_ref if src_ref.exists() else dst)
        if rewritten != content:
            dst.write_text(rewritten, encoding='utf-8')
            print(f'[links] {new_rel}')

    # 3. 清理空目录
    for dirpath in sorted([p for p in (ZH / '开发者专区').rglob('*') if p.is_dir()], reverse=True):
        try:
            dirpath.rmdir()
        except OSError:
            pass
    try:
        (ZH / '开发者专区').rmdir()
        print('[clean] 开发者专区/ 已删除')
    except OSError as e:
        print(f'[warn] 开发者专区/ 非空: {e}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
