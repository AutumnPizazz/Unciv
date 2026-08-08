#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
UncivCN 游戏仓库 ↔ unciv-chinese-community 社区仓库联动同步脚本。

功能（本地与 CI 通用，仅依赖 Python 标准库 + git 命令行）：
  1. changelog       —— 从 git log 提取当前版本与上一版本之间的提交，生成更新日志草稿
  2. docs-report     —— 对比游戏仓库 docs/Modders/ 与社区开发者专区对应文档的新鲜度
  3. translation     —— 统计游戏简体中文翻译的完成度（未翻译 key 数、合并后新增 key 数）
  4. mod-report      —— 报告游戏内置 CoeHarMod 的最近修改时间，提示核对社区模组专区记录
  5. all             —— 以上全部（默认）

用法：
  python sync.py [--game-repo PATH] [--community-repo PATH] [--output DIR] [--version X.Y.Z] [--prev COMMIT_OR_TAG] [功能名]

  --game-repo       游戏仓库路径（默认：脚本所在目录的上级上级，即仓库根）
  --community-repo  社区仓库路径（默认：../unciv-chinese-community）
  --output          草稿输出目录（默认：community-repo 的 更新日志/UncivCN/ 与仓库根）
  --version         目标版本号（默认：从游戏仓库 BuildConfig.kt 自动读取）
  --prev            上一版本对应的 commit/tag（默认：自动查找上一版本号提交）
"""
import argparse
import datetime
import os
import re
import subprocess
import sys

# ============================================================
# 常量与路径
# ============================================================

GAME_REPO_DEFAULT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
COMMUNITY_REPO_DEFAULT = os.path.normpath(os.path.join(GAME_REPO_DEFAULT, "..", "unciv-chinese-community"))
BUILD_CONFIG = "buildSrc/src/main/kotlin/BuildConfig.kt"
GAME_VERSION_FILE = "core/src/com/unciv/UncivGame.kt"

# 游戏 docs/Modders 与社区开发者专区的文档映射（社区文档为人工翻译/重写，仅提示核对）
DOCS_MAP = {
    "Mods.md": "模组.md",
    "Mod-file-structure": "模组文件结构",
    "Type-checking.md": "类型检查.md",
    "Creating-a-UI-skin.md": "创建UI皮肤.md",
    "Creating-a-custom-tileset.md": "自定义地形集.md",
    "Images-and-Audio.md": "图像和音频资源.md",
    "Scenarios.md": "场景制作.md",
    "Autoupdates.md": "自动更新指南.md",
    "Making-a-new-Civilization.md": "创建新文明.md",
    "Lua-Modding.md": "Lua脚本.md",
    "Unique-parameters.md": "Unique参数详解.md",
    "uniques.md": "Unique能力列表.md",
    "Merge-Actions.md": "UncivCN扩展JSON-MergeAction教程.md",
}


# ============================================================
# git 辅助
# ============================================================

def git(repo, *args):
    """运行 git 命令，返回 stdout 文本。"""
    result = subprocess.run(
        ["git", "-C", repo, *args],
        capture_output=True, text=True, encoding="utf-8", errors="replace"
    )
    if result.returncode != 0:
        raise RuntimeError(f"git {args[0]} 失败: {result.stderr.strip()}")
    return result.stdout


def read_version(repo):
    """从 BuildConfig.kt 读取 appVersion / appCodeNumber。"""
    path = os.path.join(repo, BUILD_CONFIG)
    with open(path, encoding="utf-8") as f:
        text = f.read()
    version = re.search(r'appVersion = "([^"]+)"', text).group(1)
    code = re.search(r"appCodeNumber = (\d+)", text).group(1)
    return version, int(code)


def find_previous_version_commit(repo, version):
    """定位版本提交。

    返回 (intro_commit, prev_commit)：
      intro_commit —— 最近一个 BuildConfig.kt 中 appVersion == version 的提交（当前版本引入点）
      prev_commit  —— 该提交之前最近的 appVersion != version 的提交（上一版本状态）
    更新日志范围 = prev_commit..intro_commit
    """
    log = git(repo, "log", "--first-parent", "--format=%H", "--", BUILD_CONFIG).strip().splitlines()
    intro_commit = None
    prev_commit = None
    for commit in log:
        content = git(repo, "show", f"{commit}:{BUILD_CONFIG}")
        m = re.search(r'appVersion = "([^"]+)"', content)
        if m is None:
            continue
        if m.group(1) == version and intro_commit is None:
            intro_commit = commit
        elif m.group(1) != version and intro_commit is not None:
            prev_commit = commit
            break
    return intro_commit, prev_commit


def commits_between(repo, prev, head="HEAD"):
    """返回 (commit, 单行信息) 列表，按时间从旧到新。"""
    if not prev:
        raise RuntimeError("无法确定上一版本提交，请用 --prev 指定")
    lines = git(repo, "log", "--format=%H%x09%s", f"{prev}..{head}").strip().splitlines()
    result = []
    for line in lines:
        if not line:
            continue
        sha, _, subject = line.partition("\t")
        result.append((sha, subject))
    return result


def last_modified(repo, path):
    """文件/目录在仓库中的最后修改提交时间（ISO）。"""
    try:
        date = git(repo, "log", "-1", "--format=%ci", "--", path).strip()
        return date or "未知"
    except RuntimeError:
        return "未知"


# ============================================================
# 更新日志生成
# ============================================================

def classify(subject):
    """按提交信息关键字粗分类（中文优先）。"""
    s = subject.lower()
    if s.startswith("merge") or "merge" in s or "合并" in s:
        return "合并/同步"
    if "view implementation" in s or "view 实现" in s:
        return "重构"
    if s.startswith("fix") or "修复" in s or "修" in s[:6]:
        return "修复"
    if s.startswith("perf") or "性能" in s or "优化" in s:
        return "性能优化"
    if s.startswith("feat") or "新增" in s or "添加" in s or "支持" in s[:6] or "实现" in s[:6]:
        return "新功能"
    if "翻译" in s or "translation" in s:
        return "翻译"
    if "版本" in s or "version" in s:
        return "版本"
    return "其他"


def summarize_upstream(commits):
    """把上游（merge 引入的）提交归并成简短摘要。"""
    if not commits:
        return []
    counts = {}
    samples = {}
    for _, subject in commits:
        s = subject.lower()
        key = None
        if "view implementation" in s:
            key = "View 大规模重构"
        elif s.startswith("perf") or "性能" in s or "优化" in s:
            key = "性能优化"
        elif s.startswith("fix") or "修复" in s:
            key = "bug 修复"
        elif "version rollout" in s or re.match(r"^4\.\d+", subject):
            continue  # 版本标记，跳过
        else:
            key = "其他改动"
        counts[key] = counts.get(key, 0) + 1
        if key not in samples and len(subject) < 90:
            samples[key] = subject
    result = []
    for key in ["View 大规模重构", "性能优化", "bug 修复", "其他改动"]:
        if key in counts:
            line = f"- {key}（{counts[key]} 个提交）"
            if key in samples:
                line += f"：{samples[key]}"
            result.append(line)
    return result


def generate_changelog(game_repo, version, prev, intro_commit, head="HEAD"):
    """生成更新日志草稿 markdown。

    主列表取 CN 主干（--first-parent）提交；merge 提交引入的上游提交
    自动归并为简短摘要，避免罗列全部上游提交。
    """
    lines = []
    if intro_commit:
        head = intro_commit  # 版本号提交本身作为范围终点，包含其改动

    # CN 主干提交（含 merge 提交本身）
    trunk = git(game_repo, "log", "--first-parent", "--format=%H%x09%s", f"{prev}..{head}").strip().splitlines()
    trunk_commits = []
    for line in trunk:
        if not line:
            continue
        sha, _, subject = line.partition("\t")
        trunk_commits.append((sha, subject))

    today = datetime.date.today()
    today_str = f"{today.year}.{today.month}.{today.day}"
    lines.append(f"## v{version}<Badge type=\"tip\" text=\"最新\" />")
    lines.append("")
    lines.append(f"**发布日期**：{today_str}")
    lines.append("")
    lines.append("> ⚠️ 以下内容由联动脚本自动生成草稿，请人工整理后发布。")
    lines.append("")

    # 按分类分组（保持出现顺序）
    categories = {}
    order = []
    upstream_total = 0
    for sha, subject in trunk_commits:
        if subject.lower().startswith("merge"):
            # 提取该 merge 引入的上游提交（第二父分支）
            try:
                up = git(game_repo, "log", "--format=%H%x09%s", f"{sha}^1..{sha}^2").strip().splitlines()
                up_commits = []
                for uline in up:
                    if not uline:
                        continue
                    usha, _, usubject = uline.partition("\t")
                    up_commits.append((usha, usubject))
                upstream_total += len(up_commits)
                summary = summarize_upstream(up_commits)
                cat = "合并上游"
                if cat not in categories:
                    categories[cat] = []
                    order.append(cat)
                categories[cat].append((sha, subject, summary))
            except RuntimeError:
                categories.setdefault("合并/同步", []).append((sha, subject, []))
                if "合并/同步" not in order:
                    order.append("合并/同步")
            continue
        cat = classify(subject)
        if cat == "版本":
            continue  # 版本标记提交不单独列出
        if cat not in categories:
            categories[cat] = []
            order.append(cat)
        categories[cat].append((sha, subject, []))

    for cat in order:
        lines.append(f"### {cat}")
        lines.append("")
        for sha, subject, summary in categories[cat]:
            if summary:
                lines.append(f"- {subject}")
                lines.extend(summary)
            else:
                lines.append(f"- {subject} (`{sha[:8]}`)")
        lines.append("")

    lines.append("### 完整提交列表")
    lines.append("")
    for sha, subject in trunk_commits:
        lines.append(f"- [{subject}](https://github.com/AutumnPizazz/Unciv/commit/{sha})")
    if upstream_total:
        lines.append("")
        lines.append(f"> 注：另有上游 {upstream_total} 个提交随合并进入，详见合并提交的父分支。")
    lines.append("")
    return "\n".join(lines)


# ============================================================
# docs 同步报告
# ============================================================

def generate_docs_report(game_repo, community_repo, version):
    """对比 docs/Modders 与社区开发者专区的文档新鲜度。"""
    lines = [f"# 开发者文档同步报告 v{version}", ""]
    lines.append("> ⚠️ 社区文档为人工翻译/重写，本报告仅提示『游戏侧已更新、社区侧可能过时』的文档，请人工核对内容。")
    lines.append("")
    lines.append("| 游戏 docs/Modders | 社区开发者专区 | 游戏侧最后修改 | 建议 |")
    lines.append("|---|---|---|---|")
    game_docs = os.path.join(game_repo, "docs", "Modders")
    community_dev = os.path.join(community_repo, "开发者专区", "模组开发")
    for game_name, community_name in DOCS_MAP.items():
        game_path = os.path.join(game_docs, game_name)
        community_path = os.path.join(community_dev, community_name)
        if not os.path.exists(game_path):
            continue
        game_mtime = last_modified(game_repo, os.path.join("docs", "Modders", game_name))
        if os.path.exists(community_path):
            community_mtime = last_modified(community_repo, os.path.join("开发者专区", "模组开发", community_name))
            # 粗略比较：以游戏侧修改时间晚于社区侧为『需核对』（日期字符串可比较）
            suggestion = "⚠️ 需核对" if game_mtime > community_mtime else "ok"
        else:
            community_mtime = "（不存在）"
            suggestion = "⚠️ 社区无对应文档"
        lines.append(f"| {game_name} | {community_name} | {game_mtime[:10]} | {suggestion} |")
    lines.append("")
    return "\n".join(lines)


# ============================================================
# 翻译状态报告
# ============================================================

def count_properties(path):
    """统计 properties 文件中的 key 数。"""
    if not os.path.exists(path):
        return 0
    count = 0
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line.strip():
                continue
            if line.lstrip().startswith("#"):
                continue
            if " = " in line:
                count += 1
    return count


def generate_translation_report(game_repo, version, prev):
    """统计翻译完成度与合并新增的 key。"""
    translations = os.path.join(game_repo, "android", "assets", "jsons", "translations")
    template = os.path.join(translations, "template.properties")
    zh = os.path.join(translations, "Simplified_Chinese.properties")
    template_count = count_properties(template)
    zh_count = count_properties(zh)
    untranslated = max(0, template_count - zh_count)

    lines = [f"# 翻译状态报告 v{version}", ""]
    lines.append(f"- 模板 key 总数：{template_count}")
    lines.append(f"- 简体中文已有翻译：{zh_count}")
    lines.append(f"- 未翻译 key（约）：{untranslated}（含故意留空模板，仅供参考）")
    lines.append("")

    # 合并后新增的 key（template.properties 在本版本范围内的变更）
    if prev:
        diff = git(game_repo, "diff", "--numstat", prev, "HEAD", "--", "android/assets/jsons/translations/template.properties")
        if diff.strip():
            lines.append("本版本对翻译模板的变更（template.properties）：")
            lines.append("")
            lines.append("```")
            lines.append(diff.strip())
            lines.append("```")
            lines.append("")
    return "\n".join(lines)


# ============================================================
# CoeHarMod 版本报告
# ============================================================

def generate_mod_report(game_repo, community_repo, version):
    """报告游戏内置 CoeHarMod 的状态，提示核对社区模组专区。"""
    mod_dir = os.path.join(game_repo, "android", "assets", "mods", "CoeHarMod")
    community_log = os.path.join(community_repo, "模组专区", "CoeHarMod", "更新日志", "index.md")
    lines = [f"# CoeHarMod 模组同步报告 v{version}", ""]
    if not os.path.exists(mod_dir):
        lines.append("⚠️ 游戏仓库未包含内置 CoeHarMod（可能已取消跟踪）。")
        return "\n".join(lines)

    lines.append(f"- 游戏内置 CoeHarMod 最后修改：`{last_modified(game_repo, 'android/assets/mods/CoeHarMod')}`")
    if os.path.exists(community_log):
        with open(community_log, encoding="utf-8") as f:
            text = f.read()
        m = re.search(r"最新版本\s*\*?\*?v([\d.]+)", text) or re.search(r"v([\d.]+)\s*-\s*适配", text)
        if m:
            lines.append(f"- 社区模组专区记录版本：v{m.group(1)}")
            lines.append("- ⚠️ 请人工核对游戏内置版本与社区记录是否一致，若有更新请在社区『模组专区/CoeHarMod/更新日志』补充条目。")
        else:
            lines.append("- 社区模组专区未找到版本号（格式变化），请人工核对。")
    lines.append("")
    return "\n".join(lines)


# ============================================================
# 主流程
# ============================================================

def main():
    parser = argparse.ArgumentParser(description="UncivCN ↔ 社区仓库联动同步脚本")
    parser.add_argument("--game-repo", default=GAME_REPO_DEFAULT)
    parser.add_argument("--community-repo", default=COMMUNITY_REPO_DEFAULT)
    parser.add_argument("--output", default=None, help="草稿输出目录（默认：社区仓库对应位置）")
    parser.add_argument("--version", default=None)
    parser.add_argument("--prev", default=None, help="上一版本 commit/tag")
    parser.add_argument("tasks", nargs="*", default=["all"], help="all/changelog/docs-report/translation/mod-report")
    args = parser.parse_args()

    game_repo = os.path.abspath(args.game_repo)
    community_repo = os.path.abspath(args.community_repo)

    version, code = read_version(game_repo)
    if args.version:
        version = args.version

    # 定位上一版本提交
    prev = args.prev
    intro_commit = None
    if prev is None:
        intro_commit, prev = find_previous_version_commit(game_repo, version)
        if prev:
            print(f"上一版本提交：{prev[:8]}（当前版本 {version} 引入于 {intro_commit[:8] if intro_commit else '?'}）")
        else:
            print("⚠️ 未找到上一版本提交，更新日志将仅含当前版本引入提交之后的改动；可用 --prev 指定")

    tasks = args.tasks if args.tasks != ["all"] else ["changelog", "docs-report", "translation", "mod-report"]
    outputs = {}

    if "changelog" in tasks:
        draft = generate_changelog(game_repo, version, prev, intro_commit)
        outputs["changelog"] = (os.path.join("更新日志", "UncivCN", f"草稿-v{version}.md"), draft)
    if "docs-report" in tasks:
        outputs["docs-report"] = (os.path.join("同步报告", f"文档同步报告-v{version}.md"), generate_docs_report(game_repo, community_repo, version))
    if "translation" in tasks:
        outputs["translation"] = (os.path.join("同步报告", f"翻译状态报告-v{version}.md"), generate_translation_report(game_repo, version, prev))
    if "mod-report" in tasks:
        outputs["mod-report"] = (os.path.join("同步报告", f"模组同步报告-v{version}.md"), generate_mod_report(game_repo, community_repo, version))

    for name, (relpath, content) in outputs.items():
        if args.output:
            target = os.path.join(args.output, name + ".md")
        else:
            target = os.path.join(community_repo, *relpath.split(os.sep))
        os.makedirs(os.path.dirname(target), exist_ok=True)
        with open(target, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"✓ 已生成 {name}: {target}")


if __name__ == "__main__":
    main()
