# Beads Agent Instructions - CrimsonCode2026

*Last updated: February 2026*

You are an autonomous development agent using beads for task tracking on the **CrimsonCode2026 Emergency Response App**.

## Project Overview

**DOCS ARE SOURCE OF TRUTH** — All specifications in `emergency-response-app-technical-specification.md` are authoritative over code, comments, or assumptions.

**Repository Components:**
- `apps/mobile/` (MPL-2.0): Android + iOS mobile app via Tauri v2 + Svelte 5
- `backend/` (AGPL-3.0): Supabase Edge Functions, PostgreSQL, Realtime
- `infrastructure/`: Supabase configuration, Firebase FCM setup

**Platform Scope:**
- Mobile: Android and iOS via Tauri v2 only
- Region: USA only
- Language: English only

**Technology Stack:**
- Frontend: Svelte 5 with Runes
- Mobile Framework: Tauri v2 (Android + iOS)
- Database: Supabase PostgreSQL with PostGIS
- Authentication: Supabase Auth (Phone OTP)
- Session: Tauri Stronghold plugin + Biometrics
- Real-time: Supabase Realtime
- Maps: Leaflet + OpenStreetMap
- Push: Firebase Cloud Messaging (FCM)

---

## Multi-Agent Coordination

Beads supports multiple agents working simultaneously. Follow these patterns:

### Claiming Work
- Use `bd ready --assignee <agent-name>` to find work specific to your agent
- Claim tasks with `bd update <id> --status in_progress --assignee <agent-name>`
- This prevents multiple agents from working on the same task

### Concurrency Handling
- **Hash-based IDs**: Each issue gets unique hash ID (e.g., `crimsoncode-a1b2`) - prevents collisions
- **Last-writer-wins**: If two agents modify the same issue, git handles merge conflicts
- **Dolt server mode**: Recommended for high-concurrency scenarios (multiple active agents)

### Sync Workflow
- Run `bd sync` before and after working
- Commit changes to git immediately after completing work
- Pull latest changes before claiming new tasks

---

## Your Workflow

1. Session Start:
   - Run `bd prime` to get project context (auto-injected by Claude Code hooks)
   - Run `bd ready` to find unblocked work
   - Note: Optionally specify assignee: `bd ready --assignee <your-name>`

2. Select task: Choose highest-priority (P0 → P1 → P2) from `bd ready` output

   **IMPORTANT: Tasks marked as "in_progress" are CLAIMABLE if stale**
   - If task shows `in_progress` and hasn't been updated (>1 day), you can claim it
   - Verify task is unblocked before claiming

3. Claim task:
   - `bd update <id> --status=in_progress --assignee=<your-name>`
   - For epics: Claim AFTER creating subtasks (see step 4)

4. **IF TASK IS AN EPIC** (type=epic), you MUST break it into subtasks:

   **CRITICAL: This entire agent cycle is dedicated to planning ONLY. NO CODING.**

   **EPICS MUST ALWAYS BE BROKEN INTO SUBTASKS** — no exceptions.

   Subtasks may optionally be broken down further into sub-subtasks if task complexity deems it appropriate.

   Your complete workflow for this session:
   - Ultra-think through the epic requirements
   - Thoroughly examine ALL project documentation and specifications
   - Review related code, dependencies, and existing patterns
   - Plan the complete breakdown before creating any subtasks
   - Create concrete subtasks
   - Create each subtask with `--parent` flag: `bd create --title="..." --type=task --priority=<same as epic> --parent <epic-id>`
   - Subtasks auto-number as epic-id.1, epic-id.2, etc.
   - If subtask B depends on subtask A: `bd dep add <subtask-b> <subtask-a>`
   - Run `bd dep tree <epic-id>` to verify the hierarchy looks correct
   - Update epic status: `bd update <epic-id> --status=in_progress --assignee=<your-name>`
   - Land the plane: `bd sync && git push` (subtasks are now backed up)
   - END SESSION here - DO NOT start implementing subtasks

   **NOTE: Epic planning uses SIMPLIFIED landing plane** — just sync + push.
   No need to file follow-up issues, add comments, or run quality gates for planning sessions.

   **Implementation happens in FUTURE sessions** when subtasks appear in `bd ready`.

   **WORKING ON SUBTASKS: Claim and implement one or two subtasks at a time.**
   - Do NOT claim all subtasks simultaneously
   - Claim only what you can complete in this session (typically 1-4 subtasks)
   - Complete the claimed subtasks before claiming more
   - After each subtask completion, run `bd ready` to get the next available subtask(s)
   - This maintains focus and prevents over-commitment

5. **IF TASK IS A REGULAR TASK** (type=task), proceed to implementation:
   - **Read documentation** until you understand requirements
   - Research unknowns: Use @research for current library information and technical questions
   - TDD: Write test (Red) → Implement (Green) → Refactor
   - Progress tracking: For complex tasks, run `bd comments add <id> "Progress: completed X, working on Y"`
   - Quality gates: Run tests before marking complete (adapt to Tauri/Svelte stack)
   - Complete: `bd close <id> --reason="..."`

6. Do not do more than one implementation subtask per loop. When done fully with a subtask, exit and print your report to stdout.

   **EXIT CONDITION:** If `bd ready` returns no unblocked work, your job is complete — exit cleanly.
   - Run `bd sync && git push` one final time
   - Provide handoff summary
   - Exit agent

7. Session End (Landing the Plane protocol - MANDATORY):
    - File issues for any remaining follow-up work
    - Add comments documenting progress on in-progress tasks
    - Run quality gates (tests, linters)
    - Commit with pedantically accurate and descriptive commit message:
      - Format: `<type>(<scope>): <description>` or `<types>(<scope>): <description>`
      - Multiple types allowed: `feat,fix(backend): ...` or `feat,fix,docs(scope): ...`
      - Be pedantic about accuracy - describe exactly what changed, not vaguely
    - Push to remote: `bd sync && git push` (work NOT complete until push succeeds)
    - If push fails, resolve conflicts and retry until success
    - Verify: git status shows clean, up to date with origin
    - Clean up: git stash clear, git remote prune origin
    - END THE SESSION BY CLOSING YOUR CHAT SESSION AND QUITTING. DO NOT HANG OR WAIT FOR MORE INPUT FROM A USER. Close and print a summary of your response to your stdout.

---

## Absolute Rules

- NEVER create TODO.md/TASKS.md/PLAN.md — use bd only
- NEVER use bd edit — use bd update with flags
- ALWAYS bd sync && git push before ending session
- NEVER stop before git push succeeds (work is incomplete until push)
- If push fails, resolve conflicts and retry until success
- Follow project docs/specs above all else
- Use @research for current library information and unknowns
- Use non-interactive flags for all shell commands (cp -f, rm -f, --yes, etc.)
- Write pedantically accurate and descriptive commit messages (type,types(scope): description format - multiple types allowed)

---

## Beads Commands Reference

| Command | Purpose |
|---------|---------|
| `bd sync` | Commit .beads/issues.jsonl to git |
| `bd prime` | Session start: generates workflow context |
| `bd ready` | Find unblocked, actionable work |
| `bd ready --assignee <name>` | Find work assigned to specific agent |
| `bd blocked` | Show what's blocking other tasks |
| `bd update <id> --status=in_progress --assignee=<name>` | Claim/start work on task |
| `bd close <id>` | Complete task |
| `bd close <id> --reason="..."` | Complete with reason |
| `bd dep add <child> <parent>` | Set task dependencies |
| `bd dep tree <id>` | Show dependency hierarchy (works for epics with subtasks) |
| `bd dep cycles` | Check for circular dependencies |
| `bd dep remove <child> <parent>` | Remove resolved dependency |
| `bd reopen <id>` | Reopen closed issue |
| `bd show <id>` | View task details |
| `bd create --title="..." --parent <epic-id>` | Create subtask linked to epic (auto-numbers as epic.1, epic.2) |
| `bd list --status=open` | Show all open issues |
| `bd list --priority 0` | Show P0 items only |
| `bd comments add <id> "message"` | Add progress comment to task |

**Key workflow concept:**
- `bd prime` = HOW to work (meta-context, auto-injected by Claude Code hooks)
- `bd ready` = WHAT to work on (actual tasks)

---

## Anti-Patterns to Avoid

### Interactive Commands
- NEVER use `bd edit` — opens interactive editor (hangs agent)
- ALWAYS use `bd update` with explicit flags
- Use non-interactive shell flags: `cp -f`, `rm -f`, `scp -o BatchMode=yes`

### Session Management
- NEVER stop before git push succeeds
- NEVER leave work committed but not pushed
- NEVER say "ready to push when you are" — agent MUST perform the push
- NEVER mix epic planning (subtask creation) with implementation — one OR the other per session

### Task Management
- NEVER ignore dependency cycles (`bd dep cycles`)
- NEVER work on an epic without first breaking it into subtasks (epics MUST always have subtasks)
- Subtasks MAY be broken down further into sub-subtasks if task complexity deems it appropriate (optional)
- ALWAYS use `--parent <epic-id>` when creating subtasks (auto-links and auto-numbers)

### Task Granularity
- Break work into appropriate and clearly scoped quantities of work
- Epic planning (breaking into subtasks) is a FULL SESSION — do not also implement

---

## Blocking Questions

If stuck on something unresolvable by docs or @research:
1. Create a blocking question issue: `bd create --title="Question: [topic]" --type=question --priority=0`
2. Mark your current task as blocked by it: `bd dep add <current-task-id> <question-issue-id>`
3. Move on to other unblocked work: `bd ready`

---

## Completion

When `bd ready` returns no unblocked work, your job is complete — exit cleanly.

**Before exiting:**
- Ensure all work is pushed to remote
- Provide handoff summary for next session

Project root: /home/ldeen/Documents/CrimsonCode2026
