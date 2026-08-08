import { defineConfig } from 'vitepress'
import { fileURLToPath, URL } from 'node:url'
// @ts-ignore
import taskLists from 'markdown-it-task-lists'
import mathjax3 from 'markdown-it-mathjax3'

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
        logo: '/Unciv/Icon.png',
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
            ]
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
        logo: '/Unciv/Icon.png',
        // 语言切换器：列出全部 locale（themeConfig.locales 存在时显示）
        locales: {
          root: { label: 'English' },
          zh: { label: '简体中文' },
        },
        nav: [
          { text: '首页', link: '/zh/' },
          { text: 'UncivCN 专区', link: '/zh/UncivCN/' },
          { text: '原版专区', link: '/zh/原版专区/Unciv基础术语/' },
          { text: '模组专区', link: '/zh/模组专区/CoeHarMod/' },
          { text: '开发者专区', link: '/zh/开发者专区/模组开发/模组' },
          { text: '更新日志', link: '/zh/更新日志/' },
        ],
        sidebar: [
          {
            text: 'UncivCN 专区',
            collapsed: false,
            items: [
              { text: '分支介绍', link: '/zh/UncivCN/' },
              { text: '新特性', link: '/zh/UncivCN/新特性' },
              { text: '更新日志', link: '/zh/UncivCN/更新日志' },
              { text: '与上游差异', link: '/zh/UncivCN/与上游差异' },
            ],
          },
          {
            text: '原版专区',
            collapsed: true,
            items: [
              { text: 'Unciv 基础术语', link: '/zh/原版专区/Unciv基础术语/' },
              { text: '众神与国王入门指南', link: '/zh/原版专区/众神与国王入门指南/' },
              { text: '自主左二前应该做什么', link: '/zh/原版专区/自主左二前应该做什么/' },
              { text: 'Unciv 鳇脯菌校：全局篇', link: '/zh/原版专区/Unciv鳇脯菌校/' },
              { text: '联机教程', link: '/zh/原版专区/联机教程/' },
            ],
          },
          {
            text: '模组专区',
            collapsed: true,
            items: [
              {
                text: 'CoeHarMod',
                items: [
                  { text: '模组介绍', link: '/zh/模组专区/CoeHarMod/' },
                  { text: '更新日志', link: '/zh/模组专区/CoeHarMod/更新日志/' },
                  { text: '更新计划', link: '/zh/模组专区/CoeHarMod/更新日志/更新计划' },
                ],
              },
              { text: 'Emperors and Deities', link: '/zh/模组专区/Emperors and Deities/' },
            ],
          },
          {
            text: '开发者专区',
            collapsed: true,
            items: [
              {
                text: '模组开发',
                items: [
                  { text: '模组总览', link: '/zh/开发者专区/模组开发/模组' },
                  {
                    text: '模组文件结构',
                    items: [
                      { text: '概述', link: '/zh/开发者专区/模组开发/模组文件结构/概述' },
                      { text: '文明相关 JSON 文件', link: '/zh/开发者专区/模组开发/模组文件结构/文明相关JSON文件' },
                      { text: '地图相关 JSON 文件', link: '/zh/开发者专区/模组开发/模组文件结构/地图相关JSON文件' },
                      { text: '单位相关 JSON 文件', link: '/zh/开发者专区/模组开发/模组文件结构/单位相关JSON文件' },
                      { text: '其他 JSON 文件', link: '/zh/开发者专区/模组开发/模组文件结构/其他JSON文件' },
                    ],
                  },
                  { text: '创建新文明', link: '/zh/开发者专区/模组开发/创建新文明' },
                  { text: '图像和音频资源', link: '/zh/开发者专区/模组开发/图像和音频资源' },
                  { text: '自定义地形集', link: '/zh/开发者专区/模组开发/自定义地形集' },
                  { text: '创建 UI 皮肤', link: '/zh/开发者专区/模组开发/创建UI皮肤' },
                  { text: '场景制作', link: '/zh/开发者专区/模组开发/场景制作' },
                  { text: 'Unique 参数详解', link: '/zh/开发者专区/模组开发/Unique参数详解' },
                  { text: 'Unique 能力列表', link: '/zh/开发者专区/模组开发/Unique能力列表' },
                  { text: '类型检查', link: '/zh/开发者专区/模组开发/类型检查' },
                  { text: '自动更新指南', link: '/zh/开发者专区/模组开发/自动更新指南' },
                  { text: 'UncivCN MergeAction 教程', link: '/zh/开发者专区/模组开发/UncivCN扩展JSON-MergeAction教程' },
                  { text: 'Lua 脚本', link: '/zh/开发者专区/模组开发/Lua脚本' },
                ],
              },
              {
                text: '翻译本地化',
                items: [
                  { text: '翻译指南', link: '/zh/开发者专区/翻译本地化/翻译指南' },
                  { text: '翻译生成', link: '/zh/开发者专区/翻译本地化/翻译生成' },
                  { text: '模组翻译', link: '/zh/开发者专区/翻译本地化/模组翻译' },
                ],
              },
              {
                text: '代码贡献',
                items: [
                  { text: '项目结构', link: '/zh/开发者专区/代码贡献/项目结构/' },
                  { text: '代码标准', link: '/zh/开发者专区/代码贡献/代码标准/' },
                  { text: '构建和部署', link: '/zh/开发者专区/代码贡献/构建和部署' },
                  { text: 'UI 开发', link: '/zh/开发者专区/代码贡献/UI 开发' },
                  { text: 'Uniques 机制', link: '/zh/开发者专区/代码贡献/Uniques 机制' },
                ],
              },
              {
                text: '源码分析',
                items: [
                  { text: '军事实力计算方式', link: '/zh/开发者专区/源码分析/军事实力计算方式/' },
                  { text: '文明积分计算', link: '/zh/开发者专区/源码分析/文明积分计算/' },
                ],
              },
            ],
          },
          {
            text: '更新日志',
            collapsed: false,
            items: [
              { text: '总览', link: '/zh/更新日志/' },
              { text: 'Unciv 原版', link: '/zh/更新日志/Unciv原版/' },
              {
                text: 'UncivCN',
                items: [
                  { text: '更新日志', link: '/zh/更新日志/UncivCN/' },
                  { text: '轮询联机', link: '/zh/更新日志/UncivCN/轮询联机' },
                ],
              },
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
