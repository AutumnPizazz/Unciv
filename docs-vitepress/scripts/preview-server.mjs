#!/usr/bin/env node
/**
 * UncivCN 文档站本地预览服务器（Node 版，替代原 preview_server.py）。
 *
 * - 服务 docs-vitepress/.vitepress/dist，映射 /Unciv/ 前缀（与 GitHub Pages 路径一致）
 * - 无扩展名路径自动补 .html（如 /zh/UncivCN/新特性 -> 新特性.html）
 * - 空闲超时（默认 5 分钟无 HTTP 访问）自动退出，Ctrl+C 立即退出
 * - 关闭浏览器后无需手动清理进程
 *
 * 用法：node scripts/preview-server.mjs [--port 4173] [--idle-timeout 300]
 */
import http from 'node:http'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const DIST = path.resolve(__dirname, '../.vitepress/dist')
const BASE = '/Unciv/'

const args = process.argv.slice(2)
const port = Number(args[args.indexOf('--port') + 1] ?? 4173)
const idleTimeout = Number(args[args.indexOf('--idle-timeout') + 1] ?? 300)

let lastAccess = Date.now()

const MIME = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8', '.map': 'application/json',
  '.png': 'image/png', '.svg': 'image/svg+xml', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg',
  '.gif': 'image/gif', '.webp': 'image/webp', '.ico': 'image/x-icon',
  '.woff2': 'font/woff2', '.txt': 'text/plain; charset=utf-8', '.xml': 'text/xml; charset=utf-8',
}

/** 去掉 /Unciv/ 前缀并解析为 DIST 内的文件路径；越界（../）返回 null */
function resolveFile(urlPath) {
  let p = urlPath
  if (p === BASE || p === BASE.slice(0, -1)) p = '/'
  else if (p.startsWith(BASE)) p = '/' + p.slice(BASE.length)
  let decoded
  try { decoded = decodeURIComponent(p) } catch { return null }
  const full = path.normalize(path.join(DIST, decoded))
  if (full !== DIST && !full.startsWith(DIST + path.sep)) return null
  return full
}

const server = http.createServer((req, res) => {
  lastAccess = Date.now()
  const url = new URL(req.url ?? '/', 'http://localhost')

  let filePath = resolveFile(url.pathname)
  if (filePath === null) { res.writeHead(403).end('Forbidden'); return }

  // 无扩展名的路径自动尝试 .html（如 /zh/UncivCN/新特性）
  if (!fs.existsSync(filePath) && !url.pathname.endsWith('/') &&
      !path.basename(url.pathname).includes('.')) {
    filePath = resolveFile(url.pathname + '.html') ?? filePath
  }
  // 目录请求 -> index.html
  if (fs.existsSync(filePath) && fs.statSync(filePath).isDirectory()) {
    filePath = path.join(filePath, 'index.html')
  }
  if (!fs.existsSync(filePath)) { res.writeHead(404).end('Not Found'); return }

  const ext = path.extname(filePath).toLowerCase()
  res.writeHead(200, { 'Content-Type': MIME[ext] ?? 'application/octet-stream' })
  fs.createReadStream(filePath).pipe(res)
})

server.on('error', (e) => {
  if (e.code === 'EADDRINUSE') {
    console.error(`[错误] 端口 ${port} 启动失败：可能已有预览服务器在运行（直接访问 http://localhost:${port}/Unciv/ 即可）。`)
    process.exit(1)
  }
  console.error(e)
  process.exit(1)
})

server.listen(port, '127.0.0.1', () => {
  console.log(`预览地址: http://localhost:${port}/Unciv/`)
  console.log(`空闲 ${idleTimeout} 秒无访问将自动退出；Ctrl+C 立即停止。`)
  console.log('关闭本窗口或浏览器后无需手动清理进程。')
})

// 空闲超时自动退出
setInterval(() => {
  if (Date.now() - lastAccess > idleTimeout * 1000) {
    console.log('[提示] 已空闲超时，自动退出。')
    process.exit(0)
  }
}, 2000)
