# BOSS Git Changes

Working tree status and the staging area, in the left sidebar.

Stage, unstage and discard changes without leaving BOSS. Everything goes through the host's
`GitDataProvider`, so this plugin never shells out to `git` or touches the filesystem itself.

It is also the reference implementation for plugin authors: the smallest plugin that exercises
every extension point at once. See [Notes](#notes).

## What it does

- **Two sections**, Staged Changes and Changes, each with a live count.
- **Stage or unstage a single file** with a per-row toggle, or **Discard Changes** on it.
- **Toolbar**: Stage All, Unstage All, Refresh.
- **Click a file** to open it in the editor.
- **Toast messages** for every operation outcome, dismissable.
- **Distinct empty states** for "Not a Git repository", "Working tree clean", and "Git data
  provider not available".

Untracked files appear in the Changes section rather than in a section of their own.

## MCP tools

| Tool | Purpose |
|---|---|
| `git_status` | Working tree status, in git porcelain `XY path` format |
| `git_stage` | Stage one file |
| `git_unstage` | Unstage one file |
| `git_stage_all` | Stage everything |
| `git_unstage_all` | Unstage everything |
| `git_discard` | Discard working tree changes to a file. Irreversible |
| `git_checkout` | Check out a commit, branch or tag |

Everything except `git_status` mutates the working tree, and none of them is permission-gated.

`git_checkout` lives here rather than in [Git
Log](https://github.com/risa-labs-inc/boss-plugin-git-log), even though the panel's own
Checkout button is over there.

## Requirements

- BOSS >= 9.2.20, boss-plugin-api >= 1.0.20
- `gitDataProvider` from the host, which is what needs a working `git`. The plugin itself has
  no `java.io.File` or process usage at all.

## Notes

Worth copying from if you are writing a plugin. In about 900 lines it covers `PanelInfo` and
panel registration, consuming host providers, re-exporting host `StateFlow`s instead of
mirroring them, an `McpToolProvider` with both read-only and mutating tools, and a `dispose()`
that nulls its provider so tools are removed on disable or unload.

The ViewModel is the pattern to copy: re-export provider flows directly
(`gitDataProvider?.fileStatus ?: MutableStateFlow(emptyList())`), keep only local UI state as
private `MutableStateFlow` exposed through `asStateFlow()`, and follow every mutation with a
`refreshStatus()`.

## Build

```bash
./gradlew buildPluginJar
cp build/libs/boss-plugin-git-status-*.jar ~/.boss/plugins/
```

See [AGENTS.md](AGENTS.md) for architecture and conventions.

## License

Proprietary - Risa Labs Inc.
