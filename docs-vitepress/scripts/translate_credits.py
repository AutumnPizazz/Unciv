#!/usr/bin/env python3
"""
一次性脚本：生成中文版 Credits.md —— 翻译标题与说明段落，保留全部贡献名单。
"""
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
SRC = REPO_ROOT / 'docs' / 'Credits.md'
DST = REPO_ROOT / 'docs' / 'zh' / 'Credits.md'

HEADINGS = {
    '# Credits': '# 致谢',
    '## License summary': '## 许可证摘要',
    '## Icon Credits': '## 图标致谢',
    '### Stat icons': '### 统计图标',
    '### Units': '### 单位',
    '#### Ancient Era': '#### 远古时代',
    '#### Classical Era': '#### 古典时代',
    '#### Medieval Era': '#### 中世纪',
    '#### Renaissance Era': '#### 文艺复兴',
    '#### Industrial Era': '#### 工业时代',
    '#### Modern Era': '#### 现代',
    '#### Atomic Era': '#### 原子能时代',
    '#### Information Era': '#### 信息时代',
    '#### All Eras': '#### 全部时代',
    '### Units - AbsoluteUnits unitset images': '### 单位 - AbsoluteUnits 单位集图片',
    '### HexaRealm': '### HexaRealm',
    '### Resources': '### 资源',
    '### Improvements': '### 改良设施',
    '### Buildings': '### 建筑',
    "#### All Era's": "#### 全部时代",
    '### Social Policies': '### 社会政策',
    '#### Tradition': '#### 传统',
    '#### Liberty': '#### 自主',
    '#### Honor': '#### 荣誉',
    '#### Piety': '#### 虔信',
    '#### Patronage': '#### 赞助',
    '#### Commerce': '#### 商业',
    '#### Rationalism': '#### 理性',
    '#### Freedom': '#### 自由',
    '#### Autocracy': '#### 独裁',
    '#### Order': '#### 秩序',
    '### Technologies': '### 科技',
    '#### Ancient': '#### 远古',
    '#### Classical': '#### 古典',
    '#### Medieval': '#### 中世纪',
    '#### Renaissance': '#### 文艺复兴',
    '#### Industrial': '#### 工业',
    '#### Modern': '#### 现代',
    '#### Atomic': '#### 原子能',
    '#### Information': '#### 信息',
    '#### Future': '#### 未来',
    '### Terrain': '### 地形',
    '### Nations': '### 文明',
    '### Promotions': '### 晋升',
    '### Religions': '### 宗教',
    '### Others': '### 其他',
    '### Main menu': '### 主菜单',
    '### WorldScreen': '### 世界屏幕',
    '## Sound credits': '## 音效致谢',
    '## Music': '## 音乐',
    '## Trailer audio': '## 预告片音频',
    '## Visual effects': '## 视觉效果',
}

PARAGRAPHS = {
    "The works listed in this document fall under various licenses. Here’s a list of licenses or legal conditions that works have been released under:": "本文档列出的作品遵循多种许可证。以下是这些作品发布的许可证或法律条件列表：",
    "Flag Icons made by [Freepik](https://www.flaticon.com/authors/freepik) from [www.flaticon.com](https://www.flaticon.com) and licensed under CC BY 3.0, except for:": "旗帜图标由 [Freepik](https://www.flaticon.com/authors/freepik) 制作，来自 [www.flaticon.com](https://www.flaticon.com)，采用 CC BY 3.0 许可，以下除外：",
    'New Unciv logo made by u-ndefined on Discord': '新 Unciv logo 由 Discord 上的 u-ndefined 制作',
    'The base tile icons for the "Fantasy Hex" tileset were created CuddlyClover at <https://cuddlyclover.itch.io/fantasy-hex-tiles> with a few additions by various contributors, licensed CC BY 4.0.': '"Fantasy Hex" 地形集的基础地块图标由 CuddlyClover 在 <https://cuddlyclover.itch.io/fantasy-hex-tiles> 创建，并由多位贡献者做了一些补充，采用 CC BY 4.0 许可。',
    'Promotional trailer for Steam and other storefronts made by [letstalkaboutdune](https://github.com/letstalkaboutdune)': 'Steam 及其他商店的推广预告片由 [letstalkaboutdune](https://github.com/letstalkaboutdune) 制作',
    'Unless otherwise specified, all the following are from [the Noun Project](https://thenounproject.com) either licenced under CC BY 3.0 or released into the Public Domain.': '除非另有说明，以下全部来自 [the Noun Project](https://thenounproject.com)，采用 CC BY 3.0 许可或已发布到公共领域。',
    'Unless otherwise specified, units for the [AbsoluteUnits unitset](https://github.com/letstalkaboutdune/AbsoluteUnits) are made by letstalkaboutdune:': '除非另有说明，[AbsoluteUnits 单位集](https://github.com/letstalkaboutdune/AbsoluteUnits) 的单位由 letstalkaboutdune 制作：',
    'Barbarian variants by Pelo, made for [Playable Barbarians](https://github.com/Jaba583/Playable-Barbarians):': '野蛮人变体由 Pelo 为 [Playable Barbarians](https://github.com/Jaba583/Playable-Barbarians) 制作：',
    'By Basil:': '由 Basil 制作：',
    'Unless otherwise specified, tile improvements and units, as well as the terrains and improvements for HexaRealm tileset, are made by Penguinologist:': '除非另有说明，HexaRealm 地形集的地块改良设施和单位，以及地形和改良设施，由 Penguinologist 制作：',
    'HexaRealm tileset images by legacymtgsalvationuser69544 [here](https://github.com/legacymtgsalvationuser69544/Edges-Tileset)': 'HexaRealm 地形集图片由 legacymtgsalvationuser69544 制作，见[这里](https://github.com/legacymtgsalvationuser69544/Edges-Tileset)',
    'With some exceptions, most sounds are from FreeSound.org. The sounds are either licensed CC0/Public Domain, CC BY 3.0 or are original creations of the creator.': '除少数例外，大部分声音来自 FreeSound.org。这些声音要么采用 CC0/公共领域、CC BY 3.0 许可，要么是创作者的原创作品。',
    'The following music is from https://filmmusic.io:': '以下音乐来自 https://filmmusic.io：',
    'Unciv has released a Unciv Gameplay Trailer video separately, not part of this repository. See `Credits_trailer.md` for the audio credits.': 'Unciv 已单独发布了一部游戏玩法预告片视频，不属于本仓库。音频致谢见 `Credits_trailer.md`。',
    'The fireworks on the City Screen of a WLTK-celebrating city are loosely based on the Fireworks.p file included in [Particle Park](https://github.com/gamestuffbyben/Particle-Park):': 'WLTK 庆祝城市城市屏幕上的烟花大致基于 [Particle Park](https://github.com/gamestuffbyben/Particle-Park) 中的 Fireworks.p 文件：',
    'All differences and edits done by the Unciv team.': '所有修改和编辑由 Unciv 团队完成。',
    'License quoted:': '许可证原文：',
}


def main() -> int:
    lines = SRC.read_text(encoding='utf-8').split('\n')
    out = []
    for line in lines:
        s = line.strip()
        if s in HEADINGS:
            out.append(HEADINGS[s])
        elif s in PARAGRAPHS:
            indent = line[:len(line) - len(line.lstrip())]
            out.append(indent + PARAGRAPHS[s])
        else:
            out.append(line)
    DST.write_text('\n'.join(out), encoding='utf-8')
    print(f'[ok] {DST} ({len(lines)} 行)')
    return 0


if __name__ == '__main__':
    sys.exit(main())
