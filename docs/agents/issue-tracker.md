# Issue tracker: GitHub + local planning

This repo uses two surfaces, split by what is being recorded:

| What | Where |
|---|---|
| Standalone work: bugs, review findings, feature requests, anything someone should pick up later on its own | **GitHub Issues** (`gh`) |
| Planning: a spec, the implementation tickets it breaks into, wayfinder maps and their tickets | **Local markdown** under `.scratch/` |

Implementation tickets only make sense next to their spec, and filed on GitHub they bury the real backlog. So `/to-spec`, `/to-tickets`, `/wayfinder`, `/implement` and `/implement-spec` work in `.scratch/`. `/triage` and any "file an issue" request work on GitHub.

When a GitHub issue starts a piece of planned work, put its number at the top of the spec (`Issue: #<n>`). The PR that finishes the spec closes that issue (`Closes #<n>`). The spec's tickets are never filed on GitHub.

## GitHub Issues

Infer the repo from `git remote -v`; `gh` does this automatically inside a clone.

- **Create an issue**: `gh issue create --title "..." --body "..."`. Use a heredoc for multi-line bodies.
- **Read an issue**: `gh issue view <number> --json number,title,body,labels,comments`.
- **List issues**: `gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'`, with `--label` and `--state` filters as needed.
- **Comment**: `gh issue comment <number> --body "..."`
- **Apply / remove labels**: `gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **Close**: `gh issue close <number> --comment "..."`

Repo conventions for new issues:

- **Severity:** give every issue exactly one `severity:` label (critical / major / minor / trivial) when you file it. The legend is in #952.
- **Self-contained bodies:** write every caveat a fixer needs into the issue body, such as coupling, fix-together groups, or "not as small as it looks". Use a `> [!NOTE]` or `> [!WARNING]` callout at the top.
- **Titles:** scoped conventional style, like PR titles (`fix(pipes): …`, `docs(fluids): …`).

### Pull requests as a triage surface

**PRs as a request surface: no.** _(Set to `yes` if this repo treats external PRs as feature requests; `/triage` reads this flag.)_

When set to `yes`, PRs go through the same labels and states as issues, using the `gh pr` equivalents:

- **Read a PR**: `gh pr view <number> --comments`, and `gh pr diff <number>` for the diff.
- **List external PRs for triage**: `gh api --paginate 'repos/{owner}/{repo}/pulls?state=open' --jq '.[] | select(.author_association | IN("OWNER","MEMBER","COLLABORATOR") | not) | {number, title, author: .user.login, author_association, labels: [.labels[].name]}'`.
- **Comment / label / close**: `gh pr comment`, `gh pr edit --add-label` / `--remove-label`, `gh pr close`.

GitHub shares one number space across issues and PRs, so a bare `#42` may be either. Resolve it with `gh pr view 42`, falling back to `gh issue view 42`.

## Local planning (`.scratch/`)

`.scratch/` is gitignored. Planning files are local working notes in the worktree that created them, so pass another session an absolute path.

- One feature per directory: `.scratch/<feature-slug>/`
- The spec is `.scratch/<feature-slug>/spec.md`, with `Issue: #<n>` at the top when a GitHub issue prompted it
- Implementation tickets are one file each at `.scratch/<feature-slug>/issues/<NN>-<slug>.md`, numbered from `01`. Never put all tickets in one file.
- Triage state is a `Status:` line near the top of each file (see `triage-labels.md`)
- Comments and history go at the bottom of the file under a `## Comments` heading
- A ticket is closed by setting `Status: resolved`. The PR closes the spec's GitHub issue, if it has one.

### Wayfinding operations

Used by `/wayfinder`. The **map** is a file with one **child** file per ticket.

- **Map**: `.scratch/<effort>/map.md` (the Notes / Decisions-so-far / Fog body).
- **Child ticket**: `.scratch/<effort>/issues/NN-<slug>.md`, numbered from `01`, with the question in the body. A `Type:` line gives the ticket type (`research` / `prototype` / `grilling` / `task`). A `Status:` line gives `claimed` / `resolved`.
- **Blocking**: a `Blocked by: NN, NN` line near the top. A ticket is unblocked when every file it lists is `resolved`.
- **Frontier**: scan `.scratch/<effort>/issues/` for files that are open, unblocked and unclaimed. The lowest number goes first.
- **Claim**: set `Status: claimed` and save before doing any work.
- **Resolve**: add the answer under an `## Answer` heading, set `Status: resolved`, then add a context pointer (gist + link) to Decisions-so-far in `map.md`.

## When a skill says "publish to the issue tracker"

- **A spec, its tickets, or a wayfinder map/ticket** (`/to-spec`, `/to-tickets`, `/wayfinder`): write a file under `.scratch/<feature-slug>/`, creating the directory if needed.
- **Anything else**, such as a bug, a finding, or a request to file an issue: create a GitHub issue.

## When a skill says "fetch the relevant ticket"

- A path, or a bare `NN` within a known feature: read the `.scratch/` file.
- A `#<n>` number: read it as in **Read an issue** above.
