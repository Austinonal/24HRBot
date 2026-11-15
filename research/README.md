# Persistent scheduled research workspace

Place this folder in the research bot's **persistent working folder** or copy its templates there. Do not save the only copy on the phone. OpenMausBot's persistent server storage and bot workspace should own the archive.

Suggested layout inside the bot workspace:

```text
research/
  PROJECT_BRIEF.md       # stable question, scope and source-quality rules
  MEMORY_LOG.md          # rolling compact summary for the next run
  RESEARCH_LOG.md        # readable append-only findings
  SOURCES.jsonl          # one source record per line
  INSIGHTS.md            # durable observations
  DEAD_ENDS.md           # failed/weak approaches to avoid repeating
  reports/                # dated user-facing summaries
```

The skill in `SKILL.md` provides a THINK → EXECUTE → REFLECT cycle. Use the daily routine prompt in `routines/daily-research.md` to schedule a single run through OpenMausBot Automations.

## Storage behaviour

- `RESEARCH_LOG.md`, `SOURCES.jsonl`, `INSIGHTS.md` and `DEAD_ENDS.md` are append-oriented. Do not rewrite the entire history for each run.
- `MEMORY_LOG.md` is a compact rolling summary; keep it under about 4,000 characters.
- Keep only a small source/finding summary in the model context; open older source records by date/topic as needed.
- Record timestamps in ISO 8601 UTC and present user reports in `Asia/Bangkok`.
- If the same URL already exists, do not duplicate it. If a source changes, record a new observation with a checked-at timestamp.
- Keep full reports and source URLs; never invent a title, author, publication date or URL.
