/**
 * Drop headings whose section has no content.
 *
 * The UncivCN changelogs keep empty `## Unreleased` / `### <audience>` stubs so
 * contributors know where to add entries, and `bumpVersion` re-creates those
 * stubs after every release (`audienceHeadings` in GameVersionSync.kt). Rendered
 * as-is, the docs site showed an "Unreleased" section made of two empty group
 * headings, which also entered the page outline as no-op entries. This transform
 * hides such empty sections from the rendered page only: the source file keeps
 * its stubs, so the release pipeline and contributors are unaffected.
 *
 * A section counts as empty when every line between its heading and the next
 * heading of the same or a higher level is blank, an HTML comment, or another
 * heading. Nesting is handled by repeating the pass, so an empty `###` also
 * clears its empty `##` parent.
 */

const HEADING_RE = /^(#{1,6})\s+(.*)$/
const FENCE_RE = /^\s*(```|~~~)/

export function stripEmptySections(src) {
  let current = src
  // Each pass removes at least one nesting level of newly emptied parents.
  for (let pass = 0; pass < 10; pass++) {
    const next = stripOnce(current)
    if (next === current) return current
    current = next
  }
  return current
}

function stripOnce(src) {
  const lines = src.split('\n')
  const headings = []
  let fenced = false
  for (let i = 0; i < lines.length; i++) {
    if (FENCE_RE.test(lines[i])) {
      fenced = !fenced
      continue
    }
    if (fenced) continue
    const match = HEADING_RE.exec(lines[i])
    if (match) headings.push({ index: i, level: match[1].length })
  }

  const drop = new Array(lines.length).fill(false)
  for (let h = 0; h < headings.length; h++) {
    const { index, level } = headings[h]
    // The section ends at the next heading of the same or a higher level.
    let end = lines.length
    for (let j = h + 1; j < headings.length; j++) {
      if (headings[j].level <= level) {
        end = headings[j].index
        break
      }
    }

    let hasContent = false
    for (let i = index + 1; i < end; i++) {
      const text = lines[i].trim()
      if (text === '' || text.startsWith('<!--')) continue
      if (HEADING_RE.test(lines[i])) continue
      hasContent = true
      break
    }
    if (hasContent) continue

    // Keep the blank separator above the heading: it is what terminates any
    // block container (e.g. `::: tip`) that precedes the removed section.
    for (let i = index; i < end; i++) drop[i] = true
  }

  return lines.filter((_, i) => !drop[i]).join('\n')
}
