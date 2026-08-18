---
name: github-issues
description: Create and triage GitHub issues for the task-manager-api repo via the gh CLI, including real linked GitHub sub-issues for work big enough to split up. Use whenever the user asks to file a bug, request a feature, open an issue, wants to see what's open, or asks to triage/organize/clean up the issue tracker. Every commit and PR in this repo references an issue, so this is also the first step before any implementation work that doesn't already have one.
---

# Issues in task-manager-api

## Creating an issue

### 1. Gather the content

- **Title** - short imperative phrase, e.g. "Add due-date filter to task list endpoint".
- **Description** - a paragraph explaining the issue.
- **Sub-issues** - titles for discrete pieces of follow-up work, if the issue is big enough to
  warrant breaking up (e.g. "implement the use case", "add the controller endpoint", "cover it
  in ArchUnit if it introduces a new package"). Not every issue needs these - small, single-step
  issues can have an empty list.
- **Acceptance criteria** - a checklist of concrete, verifiable conditions. Prefer "`GET
  /api/tasks?dueBefore=` returns 400 for an unparsable date" over "handle bad input" - something
  you could literally check a box on after doing the work.

Ask the user directly for anything unclear rather than guessing at scope.

### 2. Format the body

```markdown
## Description
What the problem or request is, referencing the relevant architecture layer if useful.

## Sub-issues
<title of sub-issue 1>
<title of sub-issue 2>

## Acceptance Criteria
- [ ] Concrete, checkable condition
- [ ] Another one

## Related
Closes #123 / relates to #456, if applicable.
```

Omit the "Sub-issues" section entirely if there are none - don't leave it as an empty header.
Each sub-issue gets its own short body (a sentence or two is enough).

### 3. Preview and get explicit confirmation

Filing an issue publishes public content, same bar as opening a PR - show the user the fully
rendered title, body, and (if any) sub-issue titles exactly as they'll be created, and **do not
create anything until they explicitly confirm.** Moving on to another topic is not confirmation.

### 4. Create it

Once confirmed, build a JSON spec and hand it to the bundled script - it creates the issue *and*
links any sub-issues via GitHub's native sub-issues API (which needs each issue's numeric id, not
just its number, and can't be done through `gh issue create` flags alone; sub-issues linked this
way show up as a real progress checklist on the parent, trackable/closable independently):

```json
{
  "repo": "<owner>/<repo>",
  "title": "Add due-date filter to task list endpoint",
  "body": "## Description\n...\n\n## Acceptance Criteria\n- [ ] ...\n",
  "labels": ["enhancement", "web"],
  "sub_issues": [
    { "title": "Add dueBefore/dueAfter to ListTasksUseCase", "body": "..." },
    { "title": "Add query params to TaskController", "body": "..." }
  ]
}
```

Write it to a temp file, then run:

```bash
.claude/skills/github-issues/scripts/create_issue.sh /path/to/spec.json
```

The script checks `gh auth status` itself and fails with a clear message if not logged in -
don't attempt to authenticate on the user's behalf, just relay the message. On success it prints
a JSON summary with the parent URL and each sub-issue URL - relay those links back.

`labels` defaults to none. A label must already exist on the repo before it can be applied -
check first with `gh label list`; if missing, create it
(`gh label create "X" --description "..." --color "RRGGBB"`) or ask the user, rather than letting
the whole script fail partway through.

## Labels

This repo's taxonomy, kept small on purpose:

- **Type**: `bug`, `enhancement`, `refactor`, `docs`, `testing`
- **Area**: `domain`, `application`, `infrastructure`, `web` - mirrors the onion architecture
  layers, so `gh issue list --label domain` shows everything touching the Task aggregate
  regardless of type

Don't invent new labels ad hoc without checking what already exists (`gh label list`).

## Triaging

```bash
gh issue list                                   # what's open
gh issue list --label "needs-triage"            # what needs a first pass
gh issue view <number>                          # read one in full
gh issue edit <number> --add-label "bug,domain" # label it
gh issue close <number> --comment "<why>"        # close with a reason, never silently
```

When triaging a batch: read each issue, propose type + area labels, and apply the unambiguous
ones. For anything genuinely unclear (unreproducible bug report, vague feature ask), summarize it
back to the user instead of guessing at a label or closing it - triage should surface judgment
calls, not paper over them.
