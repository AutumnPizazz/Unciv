import { defineConfig } from 'vitepress'
import { fileURLToPath, URL } from 'node:url'
import { readFileSync } from 'node:fs'
// @ts-ignore
import taskLists from 'markdown-it-task-lists'
import mathjax3 from 'markdown-it-mathjax3'
import container from 'markdown-it-container'

// 仓库根目录的上游官方更新日志（uncivbot 自动生成，merge 上游时自动更新）
const upstreamChangelogPath = fileURLToPath(new URL('../../changelog.md', import.meta.url))

/**
 * UncivCN 文档站配置
 *
 * - srcDir 指向仓库 docs/：英文区 = docs/ 根（上游原样），中文区 = docs/zh/
 * - base '/Unciv/'：部署到 GitHub Pages 子路径 autumnpizazz.github.io/Unciv
 * - i18n：`/` 英文、`/zh/` 简体中文，右上角语言切换器
 * - 搜索：VitePress 内置 localSearch（minisearch），支持中文按字索引
 */
export default defineConfig({
  base: '/Unciv/',
  srcDir: '../docs',

  title: 'UncivCN Docs',
  description: 'UncivCN - open source Civ V remake documentation (English)',
  lang: 'en',

  head: [
    ['link', { rel: 'icon', href: '/Unciv/favicon.png' }],
    ['meta', { name: 'theme-color', content: '#d98c3a' }],
    ['meta', { name: 'og:title', content: 'UncivCN Docs' }],
    ['meta', { name: 'og:description', content: 'UncivCN - open source Civ V remake documentation' }],
  ],

  // 顶层 themeConfig：localSearch 的 provider 必须在此声明（构建期常量
  // __VP_LOCAL_SEARCH__ 只读取顶层 themeConfig.search，locale 级配置不会生效）；
  // 各 locale 的 search.translations（按钮/弹窗文案）在 locale themeConfig 中覆盖。
  themeConfig: {
    search: {
      provider: 'local',
    },
  },

  // srcDir 位于 docs-vitepress 之外（../docs），md 编译产物中的 vue 导入
  // 会从 md 所在目录向上查找 node_modules 而失败，这里显式指回本工程依赖。
  vite: {
    resolve: {
      alias: {
        vue: fileURLToPath(new URL('../node_modules/vue', import.meta.url)),
      },
    },
  },

  markdown: {
    config: (md) => {
      md.use(taskLists)
      md.use(mathjax3)
      // 自定义 note 容器：mkdocs 的 `!!! note` 无标题提示块，VitePress 未内置 note 类型。
      // 渲染为无标题的提示块（复用 info 配色，见 custom.css 的 .custom-block.note）。
      md.use(container, 'note', {
        render(tokens, idx) {
          if (tokens[idx].nesting === 1) {
            return '<div class="custom-block note">\n'
          }
          return '</div>\n'
        },
      })
      // 覆盖内置 details 容器：summary 中注入 CopyButton 组件（一键复制标题文本，
      // 模组作者需要复制 unique 语句；VitePress 默认 summary 不可选中）。
      md.use(container, 'details', {
        render(tokens, idx) {
          const title = tokens[idx].info.trim().replace(/^details\s*/, '')
          if (tokens[idx].nesting === 1) {
            const escaped = title
              .replace(/&/g, '&amp;')
              .replace(/"/g, '&quot;')
              .replace(/</g, '&lt;')
              .replace(/>/g, '&gt;')
            return `<details class="details custom-block"><summary>${title}<CopyButton text="${escaped}" /></summary>\n`
          }
          return '</details>\n'
        },
      })
      // 上游更新日志自动嵌入：`::: upstream-changelog` 容器构建时直接读取
      // 仓库根 changelog.md 全文渲染（中文站上游日志页使用），保证与上游
      // 同步最新，无需手动维护英文原文副本。
      // 注意点：
      // - 裸尖括号（如 <for every [resource]>）会被当作 HTML 标签，导致 Vue
      //   模板编译报「未闭合标签」，嵌入前须转义；
      // - 标题统一降级为 h4 并加 Upstream 前缀：避免与翻译区标题重复导致
      //   localSearch 索引崩溃，同时不进入页面大纲（outline 级别 2-3）。
      md.use(container, 'upstream-changelog', {
        render(tokens, idx) {
          if (tokens[idx].nesting === 1) {
            const raw = readFileSync(upstreamChangelogPath, 'utf-8')
              .replace(/</g, '&lt;')
              .replace(/>/g, '&gt;')
              .replace(/^#{1,6} /gm, '#### Upstream ')
            return md.render(raw)
          }
          return ''
        },
      })
    }
  },

  // 构建结束后把本工程的 public/ 静态资源复制到输出目录。
  // （vite root = srcDir = ../docs，其默认 public 目录 docs/public 不存在；
  //  本文件位于 .vitepress/ 下，故 public 相对路径为 ../public）
  async buildEnd(siteConfig) {
    const { cpSync, existsSync } = await import('node:fs')
    const publicDir = fileURLToPath(new URL('../public', import.meta.url))
    if (existsSync(publicDir)) {
      cpSync(publicDir, siteConfig.outDir, { recursive: true })
    }
  },

  locales: {
    root: {
      label: 'English',
      lang: 'en',
      title: 'UncivCN Docs',
      description: 'UncivCN - open source Civ V remake documentation',
      themeConfig: {
        logo: '/Icon.png',
        // 语言切换器：列出全部 locale（themeConfig.locales 存在时显示）
        locales: {
          root: { label: 'English' },
          zh: { label: '简体中文' },
        },
        nav: [
          { text: 'Home', link: '/' },
          { text: 'Modders', link: '/Modders/Mods' },
          { text: 'Developers', link: '/Developers/Building-Locally' },
          { text: 'Translating', link: '/Translating/Translating' },
          { text: 'Other', link: '/Other/Multiplayer' },
          { text: 'UncivCN', link: '/UncivCN/' },
          { text: 'Community', link: '/Community/Guides/Unciv-basics/' },
        ],
        sidebar: [
          {
            text: 'Modders',
            collapsed: false,
            items: [
              { text: 'Introduction to Mods', link: '/Modders/Mods' },
              {
                text: 'Mod File Structure',
                items: [
                  { text: 'Overview', link: '/Modders/Mod-file-structure/1-Overview' },
                  { text: 'Civilization-related JSON files', link: '/Modders/Mod-file-structure/2-Civilization-related-JSON-files' },
                  { text: 'Map-related JSON files', link: '/Modders/Mod-file-structure/3-Map-related-JSON-files' },
                  { text: 'Unit-related JSON files', link: '/Modders/Mod-file-structure/4-Unit-related-JSON-files' },
                  { text: 'Miscellaneous JSON files', link: '/Modders/Mod-file-structure/5-Miscellaneous-JSON-files' },
                  { text: 'Merge Actions', link: '/Modders/Mod-file-structure/6-MergeActions' },
                ]
              },
              { text: 'Making a New Civilization', link: '/Modders/Making-a-new-Civilization' },
              { text: 'Creating a Custom Tileset', link: '/Modders/Creating-a-custom-tileset' },
              { text: 'Creating a UI Skin', link: '/Modders/Creating-a-UI-skin' },
              { text: 'Images and Audio', link: '/Modders/Images-and-Audio' },
              { text: 'Lua Modding', link: '/Modders/Lua-Modding' },
              { text: 'Scenarios', link: '/Modders/Scenarios' },
              { text: 'Type Checking', link: '/Modders/Type-checking' },
              { text: 'Unique Parameters', link: '/Modders/Unique-parameters' },
              { text: 'Uniques', link: '/Modders/uniques' },
              { text: 'Autoupdates', link: '/Modders/Autoupdates' },
            ]
          },
          {
            text: 'Developers',
            collapsed: true,
            items: [
              { text: 'Building Locally', link: '/Developers/Building-Locally' },
              { text: 'Coding Standards', link: '/Developers/Coding-standards' },
              { text: 'From Code to Deployment', link: '/Developers/From-code-to-deployment' },
              { text: 'Game Making Tips', link: '/Developers/Game-Making-Tips' },
              { text: 'Map Rendering', link: '/Developers/Map-rendering' },
              { text: 'Project Structure', link: '/Developers/Project-structure-and-major-classes' },
              { text: 'Saved Games and Transients', link: '/Developers/Saved-games-and-transients' },
              { text: 'Building for and Testing on Android', link: '/Developers/Testing-Android-Builds' },
              { text: 'Translations, Mods, and Modding Freedom', link: '/Developers/Translations,-mods,-and-modding-freedom-in-Open-Source' },
              { text: 'UI Development', link: '/Developers/UI-development' },
              { text: 'Unique Replacement Process', link: '/Developers/Unique-replacement-process' },
              { text: 'Uniques', link: '/Developers/Uniques' },
            ]
          },
          {
            text: 'Translating',
            collapsed: true,
            items: [
              { text: 'Translating - for translators', link: '/Translating/Translating' },
              { text: 'Translation Generation - for developers', link: '/Translating/Translation-generation' },
              { text: 'Translation Generation - for modders', link: '/Translating/Translation-mods' },
            ]
          },
          {
            text: 'Other',
            collapsed: true,
            items: [
              { text: 'Force Rating Calculation', link: '/Other/Force-rating-calculation' },
              { text: 'Installing on macOS', link: '/Other/Installing-on-macOS' },
              { text: 'Intentional Departures from Civ V', link: '/Other/Intentional-departures-from-Civ-V' },
              { text: 'Multiplayer', link: '/Other/Multiplayer' },
              { text: 'Regions', link: '/Other/Regions' },
              { text: 'Simulations', link: '/Other/Simulations' },
            ]
          },
          {
            text: 'About',
            collapsed: true,
            items: [
              { text: 'Credits', link: '/Credits' },
              { text: 'Trailer Audio Credits', link: '/Credits_trailer' },
              { text: 'Guiding Principles', link: '/Guiding-Principles' },
              { text: 'Privacy Policy', link: '/Privacy-Policy' },
            ],
          },
          {
            text: 'UncivCN',
            collapsed: true,
            items: [
              { text: 'About the branch', link: '/UncivCN/' },
              { text: 'Features', link: '/UncivCN/Features' },
              { text: 'Changelog', link: '/UncivCN/Changelog' },
              { text: 'Differences vs upstream', link: '/UncivCN/Differences' },
              { text: 'Coding standards', link: '/UncivCN/Coding-standards' },
              { text: 'Polling multiplayer', link: '/UncivCN/Polling-multiplayer' },
            ],
          },
          {
            text: 'Community',
            collapsed: true,
            items: [
              {
                text: 'Guides',
                items: [
                  { text: 'Basic terminology', link: '/Community/Guides/Unciv-basics/' },
                  { text: 'Gods & Kings guide', link: '/Community/Guides/Gods-and-Kings-guide/' },
                  { text: 'Liberty opening strategy', link: '/Community/Guides/Liberty-opening-strategy/' },
                  { text: 'PVP arena guide', link: '/Community/Guides/PVP-arena-guide/' },
                  { text: 'Multiplayer tutorial', link: '/Community/Guides/Multiplayer-tutorial/' },
                ],
              },
              {
                text: 'Mods',
                items: [
                  { text: 'CoeHarMod', link: '/Community/Mods/CoeHarMod/' },
                  { text: 'CoeHarMod changelog', link: '/Community/Mods/CoeHarMod/更新日志/' },
                  { text: 'CoeHarMod roadmap', link: '/Community/Mods/CoeHarMod/更新日志/更新计划' },
                  { text: 'Emperors and Deities', link: '/Community/Mods/Emperors-and-Deities/' },
                ],
              },
              {
                text: 'Code analysis',
                items: [
                  { text: 'Military strength', link: '/Community/Code-analysis/Military-strength/' },
                  { text: 'Score calculation', link: '/Community/Code-analysis/Score-calculation/' },
                ],
              },
              { text: 'Upstream changelog (zh)', link: '/Community/Upstream-changelog' },
            ],
          },
        ],
        outline: { label: 'On this page', level: [2, 3] },
        docFooter: { prev: 'Previous', next: 'Next' },
        darkModeSwitchLabel: 'Theme',
        sidebarMenuLabel: 'Menu',
        returnToTopLabel: 'Back to top',
        lastUpdated: { text: 'Last updated' },
        editLink: {
          pattern: 'https://github.com/AutumnPizazz/Unciv/edit/UncivCN/docs/:path',
          text: 'Edit this page on GitHub',
        },
        search: {
          provider: 'local',
          options: {
            translations: {
              button: { buttonText: 'Search docs', buttonAriaLabel: 'Search docs' },
              modal: {
                noResultsText: 'No results found',
                resetButtonTitle: 'Clear query',
                footer: { selectText: 'Select', navigateText: 'Switch', closeText: 'Close' },
              },
            },
          },
        },
        socialLinks: [{ icon: 'github', link: 'https://github.com/AutumnPizazz/Unciv' }],
      },
    },
    zh: {
      label: '简体中文',
      lang: 'zh-CN',
      title: 'UncivCN 文档',
      description: 'UncivCN - 开源策略游戏 Unciv 的中文文档站',
      themeConfig: {
        logo: '/Icon.png',
        // 语言切换器：列出全部 locale（themeConfig.locales 存在时显示）
        locales: {
          root: { label: 'English' },
          zh: { label: '简体中文' },
        },
        nav: [
          { text: '首页', link: '/zh/' },
          { text: 'UncivCN 专区', link: '/zh/UncivCN/' },
          { text: '模组制作', link: '/zh/Modders/Mods' },
          { text: '开发者', link: '/zh/Developers/Building-Locally' },
          { text: '翻译', link: '/zh/Translating/Translating' },
          { text: '其他', link: '/zh/Other/Multiplayer' },
        ],
        sidebar: [
          {
            text: 'UncivCN 专区',
            collapsed: false,
            items: [
              { text: '分支介绍', link: '/zh/UncivCN/' },
              { text: '新特性', link: '/zh/UncivCN/Features' },
              { text: '更新日志', link: '/zh/UncivCN/Changelog' },
              { text: '与上游差异', link: '/zh/UncivCN/Differences' },
              { text: '代码规范', link: '/zh/UncivCN/Coding-standards' },
              { text: '轮询联机', link: '/zh/UncivCN/Polling-multiplayer' },
            ],
          },
          {
            text: '社区内容（Community）',
            collapsed: true,
            items: [
              {
                text: '原版攻略（Guides）',
                items: [
                  { text: '基础术语', link: '/zh/Community/Guides/Unciv-basics/' },
                  { text: '众神与国王入门指南', link: '/zh/Community/Guides/Gods-and-Kings-guide/' },
                  { text: '自主左二前应该做什么', link: '/zh/Community/Guides/Liberty-opening-strategy/' },
                  { text: '鳇脯菌校：全局篇', link: '/zh/Community/Guides/PVP-arena-guide/' },
                  { text: '联机教程', link: '/zh/Community/Guides/Multiplayer-tutorial/' },
                ],
              },
              {
                text: '模组专区（Mods）',
                items: [
                  { text: 'CoeHarMod 介绍', link: '/zh/Community/Mods/CoeHarMod/' },
                  { text: 'CoeHarMod 更新日志', link: '/zh/Community/Mods/CoeHarMod/更新日志/' },
                  { text: 'CoeHarMod 更新计划', link: '/zh/Community/Mods/CoeHarMod/更新日志/更新计划' },
                  { text: 'Emperors and Deities', link: '/zh/Community/Mods/Emperors-and-Deities/' },
                ],
              },
              {
                text: '源码分析（Code analysis）',
                items: [
                  { text: '军事实力计算方式', link: '/zh/Community/Code-analysis/Military-strength/' },
                  { text: '文明积分计算', link: '/zh/Community/Code-analysis/Score-calculation/' },
                ],
              },
              { text: 'Unciv 原版更新日志', link: '/zh/Community/Upstream-changelog' },
            ],
          },
          {
            text: 'Modders（模组制作）',
            collapsed: true,
            items: [
              { text: '模组简介', link: '/zh/Modders/Mods' },
              {
                text: '模组文件结构',
                items: [
                  { text: '总览', link: '/zh/Modders/Mod-file-structure/1-Overview' },
                  { text: '文明相关 JSON 文件', link: '/zh/Modders/Mod-file-structure/2-Civilization-related-JSON-files' },
                  { text: '地图相关 JSON 文件', link: '/zh/Modders/Mod-file-structure/3-Map-related-JSON-files' },
                  { text: '单位相关 JSON 文件', link: '/zh/Modders/Mod-file-structure/4-Unit-related-JSON-files' },
                  { text: '其他 JSON 文件', link: '/zh/Modders/Mod-file-structure/5-Miscellaneous-JSON-files' },
                  { text: 'Merge Actions', link: '/zh/Modders/Mod-file-structure/6-MergeActions' },
                ],
              },
              { text: '创建新文明', link: '/zh/Modders/Making-a-new-Civilization' },
              { text: '创建自定义地形集', link: '/zh/Modders/Creating-a-custom-tileset' },
              { text: '创建 UI 皮肤', link: '/zh/Modders/Creating-a-UI-skin' },
              { text: '图像和音频', link: '/zh/Modders/Images-and-Audio' },
              { text: 'Lua 模组', link: '/zh/Modders/Lua-Modding' },
              { text: '场景', link: '/zh/Modders/Scenarios' },
              { text: '类型检查', link: '/zh/Modders/Type-checking' },
              { text: 'Unique 参数类型', link: '/zh/Modders/Unique-parameters' },
              { text: 'Uniques', link: '/zh/Modders/uniques' },
              { text: '自动更新', link: '/zh/Modders/Autoupdates' },
            ],
          },
          {
            text: 'Developers（开发者）',
            collapsed: true,
            items: [
              { text: '本地构建', link: '/zh/Developers/Building-Locally' },
              { text: '代码标准', link: '/zh/Developers/Coding-standards' },
              { text: '从代码到部署', link: '/zh/Developers/From-code-to-deployment' },
              { text: '游戏制作技巧', link: '/zh/Developers/Game-Making-Tips' },
              { text: '地图渲染', link: '/zh/Developers/Map-rendering' },
              { text: '项目结构', link: '/zh/Developers/Project-structure-and-major-classes' },
              { text: '存档与瞬态', link: '/zh/Developers/Saved-games-and-transients' },
              { text: '构建与测试 Android 版', link: '/zh/Developers/Testing-Android-Builds' },
              { text: '翻译、模组与模组自由', link: '/zh/Developers/Translations,-mods,-and-modding-freedom-in-Open-Source' },
              { text: 'UI 开发', link: '/zh/Developers/UI-development' },
              { text: 'Unique 替换流程', link: '/zh/Developers/Unique-replacement-process' },
              { text: 'Uniques', link: '/zh/Developers/Uniques' },
            ],
          },
          {
            text: 'Translating（翻译）',
            collapsed: true,
            items: [
              { text: '翻译指南', link: '/zh/Translating/Translating' },
              { text: '翻译生成', link: '/zh/Translating/Translation-generation' },
              { text: '模组翻译', link: '/zh/Translating/Translation-mods' },
            ],
          },
          {
            text: 'Other（其他）',
            collapsed: true,
            items: [
              { text: '军事实力计算', link: '/zh/Other/Force-rating-calculation' },
              { text: 'macOS 安装', link: '/zh/Other/Installing-on-macOS' },
              { text: '与文明5的有意差异', link: '/zh/Other/Intentional-departures-from-Civ-V' },
              { text: '多人游戏', link: '/zh/Other/Multiplayer' },
              { text: '区域', link: '/zh/Other/Regions' },
              { text: '模拟', link: '/zh/Other/Simulations' },
            ],
          },
          {
            text: 'About（关于）',
            collapsed: true,
            items: [
              { text: '致谢', link: '/zh/Credits' },
              { text: '预告片音频致谢', link: '/zh/Credits_trailer' },
              { text: '指导原则', link: '/zh/Guiding-Principles' },
              { text: '隐私政策', link: '/zh/Privacy-Policy' },
            ],
          },
        ],
        outline: { label: '页面导航', level: [2, 3] },
        docFooter: { prev: '上一页', next: '下一页' },
        darkModeSwitchLabel: '主题',
        sidebarMenuLabel: '菜单',
        returnToTopLabel: '回到顶部',
        lastUpdated: { text: '最后更新于' },
        editLink: {
          pattern: 'https://github.com/AutumnPizazz/Unciv/edit/UncivCN/docs/:path',
          text: '在 GitHub 上编辑此页',
        },
        search: {
          provider: 'local',
          options: {
            translations: {
              button: { buttonText: '搜索文档', buttonAriaLabel: '搜索文档' },
              modal: {
                noResultsText: '无法找到相关结果',
                resetButtonTitle: '清除查询条件',
                footer: { selectText: '选择', navigateText: '切换', closeText: '关闭' },
              },
            },
          },
        },
        socialLinks: [{ icon: 'github', link: 'https://github.com/AutumnPizazz/Unciv' }],
      },
    },
  },
})
