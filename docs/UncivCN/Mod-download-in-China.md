# Installing mods from mainland China

> Author: UncivCN
>
> Last updated: see git history

## Background

Almost all Unciv mods are published on GitHub, and the in-game mod browser searches, previews and downloads mods through GitHub's services. Access to GitHub from mainland China is unreliable: the mod list often fails to load and downloads frequently break. This makes installing mods very hard for players in China.

UncivCN ships a built-in **download source** switch to solve exactly this problem. The source is not only used for the mod list, preview images and downloads - the in-game update check (querying the latest GitHub release) goes through the same source.

## In-game solution: switch the download source

Open **Options → Advanced** from the main menu and find **Download source**:

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

## Game update check

On entering the main menu, the game queries the latest release of the UncivCN repository in the background (also through the active download source). When a newer version exists, the version label at the bottom of the main menu shows a "New version available" hint; clicking the version label shows the new version info and a link to the download page. If the check fails due to network problems, a confirmation popup offers a one-click switch to a mirror download source (same interaction as the mod list failure).

## Manual way: paste an accelerated link

You can also install mods without switching the source. In Mod Management → "Download mod from URL", paste an accelerated zip link, for example:

```
https://gh-proxy.com/https://github.com/author/mod-repo/archive/refs/heads/main.zip
```

Unciv can download and extract a mod zip from any URL, so repository links on other platforms such as Gitee work as well.

::: warning Note
Public proxy services are free community offerings - they may be rate-limited, go down or change domains at any time. If a built-in mirror stops working, try another built-in one first, or search for a currently working GitHub accelerator and fill it in as a custom prefix.
:::
