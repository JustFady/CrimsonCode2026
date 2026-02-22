# Beads Agent Instructions - CrimsonCode2026

*Last updated: February 2026*

You are an autonomous development agent using beads for task tracking on the **CrimsonCode2026 Emergency Response App**.

## Project Overview

**DOCS ARE SOURCE OF TRUTH** — All specifications in `emergency-response-app-technical-specification.md` are authoritative over code, comments, or assumptions.

**FOLLOW SPECS EXACTLY — NO SCOPE CREEP** — Implement only what is documented. Do not extrapolate, assume, or add features. This is a hackathon project: prioritize working implementation.

**JUDICIOUS SUBTASK DESIGNATION** — Create only concrete, necessary implementation steps. Avoid analysis tasks or speculation.

**FOLLOW SPECS EXACTLY — NO SCOPE CREEP** — Implement only what is documented. Do not extrapolate, assume, or add features beyond specification. This is a hackathon project: prioritize working implementation over completeness. in addition it is IMPERATIVE that you develop this with hackathon in mind- we need to finish as much of it as quickly as possible and not get stuck or bogged down. 

**JUDICIOUS SUBTASK DESIGNATION** — When breaking down epics into subtasks, be selective and practical. Only create subtasks that are directly needed for implementation. Avoid analysis tasks, documentation tasks, or speculative work. Focus on concrete implementation steps.

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
- **Git-based coordination**: This project uses git (not Dolt) for version control

### Sync Workflow

**CRITICAL: Always sync before and after work sessions to prevent conflicts**

**Session Start:**
  1. `git pull` - Get latest changes from other agents (REQUIRED - other agents may have pushed)
  2. `bd prime` - Generate workflow context
  3. `bd ready` - Find unblocked work
  4. Check: Look at assignee field in `bd ready` output to ensure task is not already claimed

**Session End:**
  1. `bd sync && git push` - Commit and push your work
  2. If push fails due to other agent's push:
     a. Run `git pull` to merge their changes
     b. Resolve any merge conflicts
     c. `bd sync && git push` again
     d. Repeat until push succeeds

**During Work:**
  - Commit changes to git immediately after completing each task
  - Before claiming new tasks: `git pull && bd ready` (sync first, then claim)
  - For tasks blocked by other agents: Wait, do not modify their in-progress work

**Parallel Work Guidelines:**
  - Multiple agents can work on independent tasks simultaneously
  - Do NOT claim tasks already in_progress by another agent
  - Coordinate via `bd ready --assignee` to find agent-specific work
  - If you claim a task and find another agent already working, add comment and find different work

---

## Your Workflow

**NO SCOPE CREEP — Follow specs exactly. Be judicious in subtask selection.**

1. Session Start:
   - `git pull` - Get latest changes from remote (other agents' work)
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
   - Quality gates: Run tests before marking complete (adapt to KMP/Compose stack)
   - Complete: `bd close <id> --reason="..."`

6. Do not do more than one implementation subtask per loop. When done fully with a subtask, exit and print your report to stdout.

   **EXIT CONDITION:** If `bd ready` returns no unblocked work, your job is complete — exit cleanly.
   - Run `git pull` one final time to ensure no new work appeared
   - Run `bd sync && git push` to commit final state
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
- `git pull` - Ensure no new work appeared from other agents
- `bd sync && git push` - Ensure final state is committed and pushed
- Provide handoff summary for next session

Project root: /home/ldeen/Documents/CrimsonCode2026
