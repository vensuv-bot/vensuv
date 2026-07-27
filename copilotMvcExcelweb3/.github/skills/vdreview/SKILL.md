---
name: vdreview
description: Perform structured code review of Java / J2EE enterprise applications built on Struts, Spring, and SQL/database access layers, including their XML configuration files. Use this whenever the user asks you to review, audit, or find issues in Java, J2EE, JSP, servlet, Struts (struts-config.xml / struts.xml), Spring (applicationContext.xml, bean/DI config, Spring MVC, Spring Security), or SQL/JDBC/Hibernate/JPA/MyBatis code — even if they just paste a file and say "look at this" or "is this okay?". Focus areas include Struts action mappings, Spring dependency injection, and secure SQL queries. Trigger it for reviews of individual files, diffs/pull requests, whole modules, or the XML configuration that wires these frameworks together.
---

# Java Enterprise Code Review

This skill guides a systematic review of enterprise Java applications — the Struts/J2EE web tier, the Spring wiring, and the database access layer — plus the XML configuration files that hold it all together. The goal is a review that a senior engineer would recognize as thorough: it catches real security, correctness, and maintainability problems, explains *why* each one matters, and hands the developer something they can act on rather than a vague list of nits.

## How to run a review

Work through these steps in order. Don't skip the scoping step — reviewing without knowing what you're looking at produces generic advice.

### 1. Scope the code

First, figure out what you're actually reviewing so you can pull in the right expertise:

- **Identify the frameworks in play.** Look at imports, file names, and XML. `struts-config.xml` / `struts.xml` and `org.apache.struts.*` mean Struts. `applicationContext.xml`, `@Autowired`, `@Component`, `@Configuration` mean Spring. `Statement`, `PreparedStatement`, `EntityManager`, `SessionFactory`, or `*Mapper.xml` mean database access. A single file often touches more than one.
- **Note the framework version if you can find it** (e.g., Struts 1 vs Struts 2, Spring 4 vs 5/6, `javax.*` vs `jakarta.*`). Version drives which known vulnerabilities and idioms apply.
- **Read the whole scope before commenting.** For a diff or PR, read the surrounding code too — a lot of enterprise bugs live in the interaction between a changed line and the config that wires it.

### 2. Read the relevant reference(s)

Based on what you found, read the matching reference file(s) before writing findings. Each contains a detailed checklist, the specific XML files to inspect, and the anti-patterns that matter for that layer:

- `references/struts-j2ee.md` — Struts action mappings, ActionForms, JSP/servlet issues, `struts-config.xml` / `struts.xml` / `web.xml`, known OGNL/RCE exposure.
- `references/spring.md` — dependency injection style and pitfalls, bean scopes, Spring MVC, Spring Security, transactions, `applicationContext.xml` and annotation/Java config, externalized secrets, SpEL and Spring4Shell-class issues.
- `references/sql-database.md` — SQL injection, `PreparedStatement` usage, ORM (Hibernate/JPA/MyBatis) query safety, connection/resource leaks, transaction correctness, credentials in config.

If the code spans several layers, read several references. Don't review a Spring-wired DAO that builds SQL by hand using only the Spring reference — the SQL issues are usually the more serious ones.

### 3. Review against the checklists

For each area, go through the reference checklist against the actual code. As you find issues, capture them with enough specificity that the developer can find and fix them: file, location, what's wrong, and why it matters. Prioritize substance over volume — three real security findings are worth more than twenty style nitpicks.

Weight your attention toward the things that actually hurt in these stacks:
- **Security first.** SQL injection, OGNL/SpEL injection, XSS in JSP, missing authorization checks, secrets in config, and insecure deserialization are the findings that matter most. These frameworks have a long history of serious CVEs, and legacy enterprise code is where they live.
- **Then correctness.** Resource leaks, broken transaction boundaries, thread-safety bugs (especially singleton Struts actions and Spring singletons holding mutable state), and misconfigured mappings.
- **Then maintainability.** Only after the above — DI style, dead config, duplicated logic.

### 4. Write the report

Use the format below. Always include it in full even if some sections are short.

## Report structure

Use this exact structure:

```
# Code Review: [what was reviewed]

## Summary
[2-4 sentences: overall assessment, the most important thing to fix, and general code health. Be honest — if it's solid, say so; if it's alarming, say so.]

## Findings

### 🔴 Critical / Security
[Issues that are exploitable or cause data loss/corruption. For each finding use the finding format below.]

### 🟠 High
[Correctness bugs, resource leaks, broken transactions, serious design flaws.]

### 🟡 Medium
[Maintainability, minor correctness, config hygiene, deprecated APIs.]

### 🔵 Low / Nitpicks
[Style, naming, small improvements. Keep this short.]

## What's done well
[Genuinely call out good patterns. This is not filler — it tells the developer what to keep doing, and a review that only criticizes gets ignored.]
```

### Finding format

Each finding should be a short block, not a one-liner:

```
**[Short title]** — `File.java:42` (or `struts-config.xml`, `<action path="/login">`)
What's wrong, concretely. Then why it matters (the exploit, the failure mode, the maintenance cost).
Suggested fix, ideally with a corrected snippet when it's short.
```

Ground severity in impact, not in how easy the fix is. A one-character SQL-injection fix is still Critical. Keep code snippets short — show the fix, don't rewrite the whole file. If you're uncertain whether something is a real bug (e.g., you can't see how a value is used), say so rather than asserting — flag it as "worth checking" rather than a confirmed defect.

## Principles

- **Explain the why.** "Use `PreparedStatement`" is weak. "This concatenates `request.getParameter("id")` straight into the query, so `id=1 OR 1=1` dumps the table — use a `PreparedStatement` with a bound parameter" is a review that teaches. Every serious finding should make the developer understand the risk, not just the rule.
- **Be specific to the code in front of you.** Quote the actual variable, path, or bean name. Generic advice that could apply to any codebase reads as filler.
- **XML config is code.** In these stacks, a huge share of security and correctness lives in `struts-config.xml`, `web.xml`, `applicationContext.xml`, and MyBatis/Hibernate mapping XML — not just `.java`. Review it with the same rigor. When code is provided without its config, note which config files you'd want to see to complete the review.
- **Don't invent problems.** If the code is clean, say so. Padding a review with manufactured concerns erodes trust and buries the findings that matter.
