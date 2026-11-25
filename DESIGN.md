# Reflection | Design and roadmap

Current baseline: 0.1.2. The project is deliberately small. Development should respond to problems discovered through actual use rather than becoming a substitute for the practices the app supports.

## 1. Purpose

Useful advice often remains in conversations, static documents or checklists and is rarely revisited. Reflection turns it into recurring practices and short questionnaires that can help people observe changes in everyday behaviour. It is a general personal practice tool, not a clinical assessment or a substitute for professional care. Psychology is the first use case, not a dependency of the architecture.

**Minimum friction between remembering something and actually doing it.**

## 2. Product and content design principles

* Keep the interface predictable, simple and immediately useful. Opening the app should require little decision making.
* Review lists contain recurring behaviours, not permanently completed tasks. Aim for five to seven distinct, memorable principles per category. Consolidate overlapping ideas. Seven is a design guideline, not a hard technical limit.
* Prefer four clearly distinguishable verbal choices for graded assessments. Store integers 1 through 4 for reporting, with higher values consistently indicating better outcomes. Adapt neutral wording to each question; avoid arbitrary distinctions and judgemental success/failure language. Permit skipping when a question does not apply. Use Boolean or categorical answers where more appropriate.
* Measure recognisable experiences, observable behaviours and practical consequences rather than unreliable retrospective estimates of duration, frequency or emotional intensity. For example, assess whether repetitive thinking delayed an intended activity instead of asking how many minutes were spent thinking.
* Keep daily reports around two minutes. Weekly and monthly reports may be longer but should concentrate on practical lessons and decisions. Do not add questions merely because a new insight emerges.
* Prefer meaningful changes to scores, streaks, achievements or engagement. Missed reports should not create guilt or an overwhelming backlog. Ratings are ordinal signals for review, not clinical measurements or precise interval data.
* Keep category content independent of application code. Add or revise a category via JSON unless the underlying app capability actually needs to change.
* Keep personal data local and under the user's control. No unnecessary collection or network transmission.
* The purpose is to live differently, not to turn self reflection into another analytical project.

## 3. Current functionality and decisions

| Area | Decision |
| --- | --- |
| Home | Active categories, daily/weekly/monthly due lists, import, reminder settings and guide |
| Category | Recurring practices, any number of configured reports within schema limits, and history |
| Reporting | JSON-generated questions, required answer validation, immutable submitted snapshots; no persisted drafts |
| Scheduling | Daily at 21:00; weekly Sunday at 21:00; monthly last Sunday at 21:00; device local time |
| Missed periods | Latest missed period only, optionally completed; no backlog or reminders solely for overdue reports |
| Notifications | One combined notification for outstanding reports due that day; re-evaluated on relevant app/system events |
| Archive | Hide category and stop reminders; reimport to restore and start new reporting periods without deleting history |
| Privacy | No Internet permission, accounts, server, advertising, AI integration, cloud sync or Android automatic backup |

Reports open at midnight on their due date. No reporting obligations are created for dates before activation. Submissions remain associated with their original due date. Completing early removes the period from pending reminders. Android notification permissions, exact alarm access, battery restrictions and force stops affect delivery; inexact fallback alarms can be late.

## 4. Technical architecture

| Component | Implementation |
| --- | --- |
| App | Native Java, framework views; no third party runtime dependencies |
| Application ID | `com.personal.reflection` |
| Java | 17 |
| Build | Gradle 8.9, Android Gradle Plugin 8.7.3 |
| Android SDK | Compile/target 35, minimum 26 |
| Persistence | Private SharedPreferences, key `state`, a JSON document |
| Calendar and alarms | `java.time`, Android AlarmManager |
| Import | Android system file picker |

Java classes under `app/src/main/java/com/personal/reflection/`:

| File | Responsibility |
| --- | --- |
| `MainActivity.java` | Screens, navigation, dynamic questionnaire rendering, interactions, import and guide |
| `Store.java` | Validation, persistence, snapshots, reporting availability, history, import and archive |
| `Schedule.java` | Calendar and cadence calculations |
| `Reminders.java` | Notification channel, permission checks, scheduling and delivery |
| `ReminderReceiver.java` | Alarm and system event handling |

`AndroidManifest.xml` declares permissions, receiver, activity and backup policy. `app/src/main/assets/guide.txt` is the user-facing guide and JSON contract. `examples/` contains reusable definitions only. `tests/` contains dependency-free calendar and example checks.

The store contains `categories`, `submissions` and `starts`. A report submission is identified by category ID, report ID and due date, and records original report/question metadata, answers and submission timestamp. Start dates prevent retroactive obligations. Use this small storage model until evidence warrants a migration. Any migration must preserve historical snapshots and start dates.

## 5. Configuration and compatibility

The schema remains version 1. A UTF-8 JSON file defines one category with a stable ID, review list and reports. See [the in-app guide](app/src/main/assets/guide.txt) for the authoritative fields, supported types, examples and validation limits. `Store.validate()` implements the import validation. Keep guide, validator and tests consistent.

Labelled integer scales, introduced in 0.1.1, have a `labels` array ordered from min to max. The UI displays labels in descending value order but saves integers. Optional labelled scales allow a skip; unlabelled numeric scales remain supported. History uses the original question labels. Although the preferred design is 1 through 4, the existing general schema accepts other valid ranges for compatibility.

A new category ID creates a category; an existing ID updates the definition. Keep category, report and question IDs stable where the meaning remains the same. Use a new report ID for a cadence change and new question IDs for changed meanings or rating ranges. Do not combine old and new incompatible scores. Removing reports stops future reminders but retains history. Validate a definition before altering state. Importing a category is not restoring answers.

The supplied generic example, `examples/breaking-the-feedback-loop.json`, contains seven practices and daily (5 questions), weekly (5), and monthly (4) reports. It focuses on early disengagement, facts versus interpretations, uncertainty tolerance, attention control, testing assumptions in reality and delaying nonurgent emotional responses. It measures interference rather than requiring retrospective time estimates. Content can evolve based on real use without app changes.

## 6. Data ownership and limitations

All reflection answers remain in private local app storage. This is not additional application-level encryption. Notifications use private visibility and a generic public preview. Uninstalling or clearing data deletes history; there is no export or backup/restore in the current baseline. Incompatible signing keys can prevent in-place updates. Never uninstall simply to solve an update or build problem. Preserve the application ID and signing identity. Keep signing credentials and personal data outside Git.

## 7. Roadmap, in priority order

### Priority 0: Use the existing app

Use the current app for an initial period of at least two weeks. At an ordinary weekly or monthly review, assess whether reminders arrived, questions were useful and the practices were revisited. Notice whether behaviour changes or the app is becoming another chore. No separate app evaluation questionnaire is necessary. Fix blocking defects affecting normal use, data or reminder reliability; defer everything else unless there is a concrete need.

### Priority 1: Local backup and restore

Use an explicitly versioned backup format separate from category definitions. Include categories, submissions with original question snapshots, and reporting start dates. Use the Android file picker. Validate completely before modifying live data. Define replace versus merge semantics, prefer all-or-nothing restore and require confirmation for destructive replacement. Never silently overwrite history. Warn that backup files contain sensitive data; consider optional encryption if there is a concrete need. No automatic upload or cloud backup.

### Priority 2: Reliability and actual usability issues

Verify real-device notification delivery at 21:00 with the app closed; combined Sunday reminders; no reminder after early completion; rescheduling after reboot and time/timezone changes; import, update, archive and restore without losing history; invalid JSON leaving state untouched; and optional catch-up without backlog. Check labelled scale rendering, skip, integer persistence and historical rendering of old numeric and labelled answers. Add focused regression tests for confirmed defects.

If lost answers become a real problem, persist drafts by reporting period and define how they interact with changed definitions. If navigation to pending reports creates friction, investigate direct routing before adding more reminders.

### Priority 3: Selective report export

Export chosen categories and date ranges as human-readable Markdown and structured JSON. Make sharing deliberate and keep the default offline. This supports periodic review without manual transcription or automatic AI integration.

### Priority 4: Add categories through JSON

Create new categories only for a genuine recurring need. Keep practices memorable and questions concrete. Do not introduce application code for content-specific changes.

### Deferred

Trends/charts, flexible schedules, internal category editor, app lock, database migration, other platforms, accounts, cloud sync and automated interpretation are not commitments. Reassess only when usage or a concrete privacy risk justifies them. Avoid gamification. Public releases and CI can be considered when needed; define signing and update continuity before distribution.

## 8. Development workflow and verification

Read the current source, guide and design document before proposing changes. Make one focused change at a time; avoid unrelated refactoring. Preserve existing data, snapshots, JSON compatibility and scheduling unless an explicit migration is agreed. Update documentation and tests when behaviour changes. Report actual validation and untested areas. Keep commits focused. Do not treat software development as evidence of behavioural progress.

The initial app was built and opened on a device, but comprehensive device testing and notification reliability have not been established. The 0.1.1 source introduced labelled scales and the shorter example. Version 0.1.2 is a packaging and documentation update with no additional application logic changes. The calendar and example checks are portable; full Android compilation and device validation still need an SDK-equipped environment.
