# Triage Labels

The skills speak in terms of five canonical triage roles. This repo tracks issues as local markdown, so a role is recorded as the `Status:` line near the top of the issue file rather than as a label.

| Label in mattpocock/skills | `Status:` value in our tracker | Meaning                                  |
| -------------------------- | ------------------------------ | ---------------------------------------- |
| `needs-triage`             | `needs-triage`                 | Maintainer needs to evaluate this issue  |
| `needs-info`               | `needs-info`                   | Waiting on reporter for more information |
| `ready-for-agent`          | `ready-for-agent`              | Fully specified, ready for an AFK agent  |
| `ready-for-human`          | `ready-for-human`              | Requires human implementation            |
| `wontfix`                  | `wontfix`                      | Will not be actioned                     |

When a skill mentions a role (e.g. "apply the AFK-ready triage label"), set the issue file's `Status:` line to the corresponding value from this table.

Edit the "`Status:` value" column to match whatever vocabulary you actually use.
