#!/usr/bin/env node
/**
 * 检查 VitePress 构建产物中的内部链接有效性（/Unciv/ 前缀链接）。
 * 用法：node scripts/check-docs-links.mjs [dist 目录，默认当前目录]
 * 坏链时退出码为 1（CI 中用于阻止部署）。
 */
import fs from 'node:fs'
import path from 'node:path'

const dist = path.resolve(process.argv[2] ?? '.')

/** 递归收集 dist 下所有文件（相对路径，正斜杠） */
function* walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) yield* walk(full)
    else yield full.slice(dist.length + 1).replaceAll('\\', '/')
  }
}

/** 把 /Unciv/xxx 链接解析为 dist 内的相对文件路径；非站内链接返回 null */
function resolve(href) {
  href = href.split('#')[0].split('?')[0]
  let decoded
  try { decoded = decodeURIComponent(href) } catch { return null }
  if (!decoded.startsWith('/Unciv/')) return null
  let rel = decoded.slice('/Unciv/'.length)
  if (rel === '' || rel.endsWith('/')) rel += 'index.html'
  if (!rel.endsWith('.html')) rel += '.html'
  return rel
}

/** 语言切换器链接 = 当前页面的另一个 locale 版本（目标页面可能尚未翻译，404 属预期） */
function isLocaleSwitcherLink(pageRel, href) {
  if (!href.startsWith('/Unciv/')) return false
  let rel = href.slice('/Unciv/'.length)
  if (rel.endsWith('.html')) rel = rel.slice(0, -5)
  if (rel.endsWith('index')) rel = rel.slice(0, -5)
  let page = pageRel
  if (page.endsWith('.html')) page = page.slice(0, -5)
  if (page.endsWith('index')) page = page.slice(0, -5)
  if (page.startsWith('zh/')) return rel === page.slice(3)
  return rel === 'zh/' + page
}

const allFiles = [...walk(dist)]
const files = new Set(allFiles)

const bad = []
let count = 0
for (const rel of allFiles) {
  if (!rel.endsWith('.html')) continue
  const text = fs.readFileSync(path.join(dist, rel), 'utf-8')
  for (const m of text.matchAll(/href="([^"]+)"/g)) {
    // 最常见的 HTML 实体反转义（链接中出现 & 时 VitePress 会编码）
    const href = m[1]
      .replaceAll('&amp;', '&').replaceAll('&lt;', '<')
      .replaceAll('&gt;', '>').replaceAll('&quot;', '"').replaceAll('&#39;', "'")
    // 跳过资源文件（css/js/图片等）
    if (href.startsWith('/Unciv/assets/') || href.startsWith('/Unciv/vp-icons') ||
        /\.(css|js|woff2|png|ico|svg|jpg|webp|gif)$/.test(href)) continue
    const target = resolve(href)
    if (target === null) continue
    count++
    if (!files.has(target) && !isLocaleSwitcherLink(rel, href)) bad.push([rel, href])
  }
}

console.log(`内部链接总数: ${count}`)
console.log(`坏链接数: ${bad.length}`)
for (const [page, href] of bad.slice(0, 20)) console.log(`  ${page} -> ${href}`)
process.exit(bad.length ? 1 : 0)
