'use strict'
/**
 * Unciv Lua API — 薄拉取器扩展（零依赖，无 node_modules）。
 *
 * 原理：定义文件（lua-api.lua / lua-map-api.lua）由游戏代码生成后部署到文档站
 * （https://club.unciv.cn/Unciv/Modders/，每次发版自动更新），本扩展只负责：
 *   1. 激活时从云端拉取最新定义 → 写入 ~/.unciv/lua-api/（内容不同才写盘）；
 *   2. 一键命令把该目录加入用户级 LuaLS workspace.library（对所有模组生效）；
 *   3. 拉取失败保留本地旧版并提示，不打断任何工作。
 *
 * 数据与逻辑解耦：API 再变本扩展也不用更新（每次激活实时拉取最新版）。
 */

const vscode = require('vscode')
const https = require('node:https')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')

const FILES = ['lua-api.lua', 'lua-map-api.lua']

// 拉取源：文档站优先（国内可达性好），GitHub raw 备用；失败自动切换。
const SOURCES = [
  'https://club.unciv.cn/Unciv/Modders/',
  'https://raw.githubusercontent.com/AutumnPizazz/Unciv/UncivCN/docs/Modders/',
]

/** 定义文件落盘目录（与游戏数据目录一致，绝对路径写入用户设置最稳） */
function targetDir() {
  return path.join(os.homedir(), '.unciv', 'lua-api')
}

function fetchUrl(url, timeoutMs = 10000) {
  return new Promise((resolve, reject) => {
    const req = https.get(url, (res) => {
      if (res.statusCode !== 200) {
        res.resume()
        reject(new Error(`HTTP ${res.statusCode} for ${url}`))
        return
      }
      let data = ''
      res.setEncoding('utf8')
      res.on('data', (chunk) => { data += chunk })
      res.on('end', () => resolve(data))
    })
    req.on('error', reject)
    req.setTimeout(timeoutMs, () => req.destroy(new Error('timeout')))
  })
}

/** 从生成文件头部读取版本标记（-- Unciv version: x.y.z (build n)） */
function versionOf(content) {
  const m = content.match(/-- Unciv version: ([^\n]+)/)
  return m ? m[1] : 'unknown'
}

async function fetchDefinition(file) {
  let lastError = null
  for (const base of SOURCES) {
    try {
      return await fetchUrl(base + file)
    } catch (e) {
      lastError = e
    }
  }
  throw lastError ?? new Error('all sources failed')
}

/**
 * 拉取并同步两份定义文件（内容相同则跳过写盘）。
 * @returns {{updated: string[], failures: string[], dir: string}}
 */
async function updateDefinitions() {
  const dir = targetDir()
  fs.mkdirSync(dir, { recursive: true })
  const updated = []
  const failures = []
  for (const file of FILES) {
    const target = path.join(dir, file)
    try {
      const content = await fetchDefinition(file)
      const current = fs.existsSync(target) ? fs.readFileSync(target, 'utf8') : null
      if (current !== content) {
        fs.writeFileSync(target, content)
        updated.push(`${file} (${versionOf(content)})`)
      }
    } catch {
      failures.push(file)
    }
  }
  return { updated, failures, dir }
}

function activate(context) {
  // 一键配置：把 ~/.unciv/lua-api 加入用户级 LuaLS workspace.library（幂等）
  context.subscriptions.push(vscode.commands.registerCommand('unciv-lua-api.configure', async () => {
    const dir = targetDir()
    const config = vscode.workspace.getConfiguration('Lua')
    const library = config.get('workspace.library', [])
    const next = Array.isArray(library) && library.includes(dir) ? library : [...(Array.isArray(library) ? library : []), dir]
    await config.update('workspace.library', next, vscode.ConfigurationTarget.Global)
    vscode.window.showInformationMessage(
      library.includes(dir)
        ? `Unciv Lua API：${dir} 已在 LuaLS workspace.library 中（无需重复配置）`
        : `Unciv Lua API：已把 ${dir} 加入 LuaLS workspace.library（用户级设置，对所有模组生效）`
    )
  }))

  // 手动立即更新
  context.subscriptions.push(vscode.commands.registerCommand('unciv-lua-api.update', async () => {
    await vscode.window.withProgress(
      { location: vscode.ProgressLocation.Notification, title: 'Unciv Lua API 更新中…' },
      async () => {
        const { updated, failures } = await updateDefinitions()
        if (updated.length > 0) {
          vscode.window.showInformationMessage(`Unciv Lua API 定义已更新：${updated.join('、')}`)
        } else if (failures.length > 0) {
          vscode.window.showWarningMessage(
            `Unciv Lua API 更新失败（${failures.join('、')}），已保留本地版本；请检查网络后重试`
          )
        } else {
          vscode.window.showInformationMessage('Unciv Lua API 定义已是最新')
        }
      }
    )
  }))

  // 后台静默同步：每次 VSCode 启动 / 打开 Lua 文件时自动拉取最新定义
  updateDefinitions().then(({ updated }) => {
    if (updated.length > 0) {
      vscode.window.setStatusBarMessage(`$(sync) Unciv Lua API 已更新：${updated.join('、')}`, 10000)
    }
  }).catch(() => { /* 静默失败：保留本地旧版，不打扰用户 */ })
}

function deactivate() {}

module.exports = { activate, deactivate }
