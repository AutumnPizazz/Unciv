# Installing mods from mainland China

> Author: UncivCN
>
> Last updated: see git history

## Background

Almost all Unciv mods are published on GitHub, and the in-game mod browser searches, previews and downloads mods through GitHub's services. Access to GitHub from mainland China is unreliable: the mod list often fails to load and downloads frequently break. This makes installing mods very hard for players in China.

UncivCN ships a built-in **mod download source** switch to solve exactly this problem.

## In-game solution: switch the mod download source

Open **Options → Advanced** from the main menu and find **Mod download source**:

| Option | Description |
|--------|-------------|
| GitHub (official) | Default; connects directly to GitHub, usually unstable in mainland China |
| gh-proxy.com | Public GitHub proxy (tested to work for search, preview images and downloads) |
| ghfast.top | Public GitHub proxy |
| ghproxy.net | Public GitHub proxy |
| Custom | Enter your own proxy prefix in the input field below |

After switching, go back to the **Mod Management** screen (re-entering the screen refreshes it) - the mod list, preview images and downloads will all go through the selected mirror.

### Automatic guidance

If loading the mod list fails, the game checks the currently active download source:

- If it is still the official source, a confirmation popup offers to **switch to the mirror source (gh-proxy.com) and retry automatically** - just confirm;
- If a mirror is already active and it still fails, a toast points to Options → Advanced to try another mirror.

### Custom prefix

Some players may run their own proxy or prefer a specific accelerator. Select Custom and type the prefix (e.g. `https://gh-proxy.com/`) into the input field, then press Enter to save. The prefix is prepended to GitHub URLs, i.e.:

```
https://your-proxy-host/https://github.com/author/mod-repo/archive/refs/heads/main.zip
```

Only GitHub-hosted URLs (github.com, raw.githubusercontent.com, avatars.githubusercontent.com, api.github.com etc.) get the prefix; other addresses (e.g. direct zip links on Gitee) are never touched.

## Manual way: paste an accelerated link

You can also install mods without switching the source. In Mod Management → "Download mod from URL", paste an accelerated zip link, for example:

```
https://gh-proxy.com/https://github.com/author/mod-repo/archive/refs/heads/main.zip
```

Unciv can download and extract a mod zip from any URL, so repository links on other platforms such as Gitee work as well.

::: warning Note
Public proxy services are free community offerings - they may be rate-limited, go down or change domains at any time. If a built-in mirror stops working, try another built-in one first, or search for a currently working GitHub accelerator and fill it in as a custom prefix.
:::
