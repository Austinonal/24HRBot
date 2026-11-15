# Skill: Always-on research with durable memory

Use this workflow whenever the user asks for recurring research, scheduled monitoring, or a daily/weekly report.

## Before each run

1. Read `PROJECT_BRIEF.md`, the latest `MEMORY_LOG.md`, the most recent `RESEARCH_LOG.md` entries, `INSIGHTS.md` and `DEAD_ENDS.md`.
2. Identify the exact time window and question. If the task says "latest", check the publication date and the date the event actually happened.
3. Reuse prior source URLs and findings. Do not repeat unchanged content just to fill a report.
4. If no browser/search tool or needed connector is configured, say what is unavailable; do not pretend to have searched.

## THINK

- Write a short plan with the research question, what is already known, what changed since the last run and what evidence would change the conclusion.
- Prefer primary sources: official documentation, release notes, papers, standards, public filings and direct statements.
- Use independent corroboration for important claims. A search snippet alone is not enough when the source page can be opened.
- Keep the planned work bounded. Do not create extra tasks or run an endless loop while idle.

## EXECUTE

- Search only for information relevant to the defined question and time window.
- Record each source with canonical URL, title as published, publisher, publication date (when known), access/check date and the specific claim it supports.
- Distinguish confirmed facts, estimates, interpretation and unresolved questions.
- If two sources conflict, show the disagreement instead of silently choosing one.
- Never send emails, post publicly, buy, book, delete or change external accounts unless the user explicitly requests and approves that individual action.

## REFLECT

- Compare new evidence with the prior report and mark each item as new, changed, confirmed, stale or unresolved.
- Append new findings to `RESEARCH_LOG.md` and new source records to `SOURCES.jsonl` (one JSON object per line).
- Update `INSIGHTS.md` only with durable, well-supported observations. Add failed searches or approaches to `DEAD_ENDS.md` so the next run does not repeat them.
- Compact `MEMORY_LOG.md` to a short summary of the question, current conclusion, most useful sources and next open question. Keep it under 4,000 characters.
- Write a dated report under `reports/YYYY-MM-DD.md` and send the user a concise summary with source links.
- If nothing material changed, say "ไม่พบความเปลี่ยนแปลงที่มีนัยสำคัญ" and avoid reposting the previous report.

## Run budget and safety

- Default to one scheduled run per day. For time-sensitive monitoring, use a deliberate interval and a human-defined maximum number of runs per hour.
- Stop after the research objective is met or a provider quota/error blocks progress. Do not keep retrying a permanent authentication, billing or permission error.
- Do not store API keys, passwords, session cookies or pairing tokens in research files, prompts or source logs.
- Use isolated bot workspaces; do not read unrelated user folders.
- A report is informational, not a guarantee that a search found every relevant source.

## JSONL source record format

```json
{"checked_at":"2026-11-15T01:00:00Z","url":"https://example.org/report","title":"Title as published","publisher":"Publisher","published_at":"2026-11-14","claim":"What this source supports","status":"primary"}
```

Use one JSON object per line; preserve previous lines.
