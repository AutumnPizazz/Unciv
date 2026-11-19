import { execFileSync } from 'node:child_process'
import { mkdirSync, writeFileSync } from 'node:fs'

const [, , ref, ...extra] = process.argv
if (!ref || extra.length) throw new Error('Usage: node docs-vitepress/scripts/snapshot-upstream-uniques.mjs <upstream-release-ref>')

const repo = new URL('../../', import.meta.url)
const git = (...args) => execFileSync('git', args, { cwd: repo, encoding: 'utf8' })
const commit = git('rev-parse', '--verify', '--end-of-options', `${ref}^{commit}`).trim()
const docs = git('show', `${commit}:docs/Modders/uniques.md`)
const templates = new Set()
for (const line of docs.split(/\r?\n/)) {
    if (!line.startsWith('??? example')) continue
    const match = line.match(/^\?\?\? example\s+"(.+)"$/)
    if (!match) throw new Error(`Unexpected upstream Unique heading: ${line}`)
    templates.add(match[1].replace(/^&lt;(.+)&gt;$/, '$1'))
}
if (templates.size === 0) throw new Error('No upstream Unique templates found; check the upstream document format.')

const directory = new URL('desktop/src/main/resources/', repo)
mkdirSync(directory, { recursive: true })
writeFileSync(new URL('upstream-unique-templates.txt', directory),
    [`# Upstream Unciv ${ref} (${commit})`, ...[...templates].sort(), ''].join('\n'))
console.log(`Saved ${templates.size} Unique templates from upstream ${ref}.`)
