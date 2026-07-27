# Database Access & SQL Review

Reference for the data layer: raw JDBC, Hibernate/JPA, MyBatis, and the SQL that flows through them — including the mapping/config XML. SQL injection is the highest-frequency serious vulnerability in enterprise Java, so this layer usually deserves the most scrutiny.

## Contents
1. SQL injection (the priority)
2. PreparedStatement usage details
3. ORM query safety (Hibernate / JPA)
4. MyBatis mapper XML
5. Connection and resource management
6. Transactions and concurrency
7. Credentials and datasource config
8. Data exposure and general hygiene

---

## 1. SQL injection (the priority)

This is the finding to hunt for. The pattern is always the same: untrusted input concatenated into a query string.

- **String-built queries.** Any `"... WHERE x = '" + value + "'"` where `value` traces back to `request.getParameter`, a form field, a path variable, an HTTP header, or anything a user controls is an injection. It doesn't matter if it *looks* like a number — the fix is parameterization, not validation. This is Critical.
- **Trace the taint.** Follow user input from entry point (controller/action/servlet) through to the query. Injection is often not visible in the DAO alone because the tainted value arrives as a method argument. If you can't see the origin of a value being concatenated, flag it as "verify this isn't user-controlled" rather than clearing it.
- **`Statement` vs `PreparedStatement`.** Use of `java.sql.Statement.execute*(sql)` with a dynamically built string is a red flag by itself. `PreparedStatement` with `?` placeholders and `setX()` binds is the fix.
- **Dynamic SQL that can't be fully parameterized** (e.g., dynamic column/table names, `ORDER BY` direction) can't use bind parameters. These must be validated against an allowlist of known-safe values — never passed through from user input. Flag any dynamic identifier that isn't allowlisted.
- **`LIKE` clauses** built by concatenation, and **`IN (...)`** lists built by string-joining values, are common injection spots — parameterize each element.
- **Stored procedure calls** (`CallableStatement`) built by concatenation have the same exposure.

## 2. PreparedStatement usage details

Using `PreparedStatement` isn't automatically safe — check it's used correctly:

- **All user values bound, not just some.** A `PreparedStatement` that still concatenates one value into the SQL string is fully injectable. The presence of `?` elsewhere is not protection.
- **Parameters not reused across a loop without resetting**, and `setX` indices matching the placeholders.
- **Batching** for bulk operations (`addBatch`/`executeBatch`) rather than N round-trips.

## 3. ORM query safety (Hibernate / JPA)

ORMs are safer by default but not immune:

- **HQL/JPQL injection.** `"from User where name = '" + name + "'"` in HQL/JPQL is injectable exactly like raw SQL. Use named or positional parameters (`setParameter`), never concatenation.
- **Native queries.** `createNativeQuery(...)` with concatenated input is raw SQL injection.
- **Criteria API** is generally safe for dynamic queries — prefer it over string-building for dynamic filters.
- **N+1 select problem.** Lazy associations accessed in a loop fire one query per row — a performance defect that cripples under real data volume. Look for entity-collection iteration without a fetch join / batch fetching. Worth flagging as High when it's on a hot path.
- **`FetchType.EAGER`** everywhere pulls huge object graphs; `LazyInitializationException` from accessing lazy fields outside a session/transaction is the mirror-image bug.
- **Entities returned straight to the web layer** (see also Spring reference) can trigger lazy loads during serialization and leak fields.
- **Missing optimistic locking** (`@Version`) where concurrent updates are possible → lost updates.

## 4. MyBatis mapper XML

MyBatis mapper XML has a specific, easy-to-miss injection footgun:

- **`${}` vs `#{}`.** `#{param}` creates a bound `PreparedStatement` parameter (safe). `${param}` does raw string substitution directly into the SQL (injectable). Any `${...}` fed by user input is a SQL-injection finding — flag every one and confirm it's either not user-controlled or strictly allowlisted (its only legitimate use is dynamic identifiers like sort columns, which still must be allowlisted).
- **`<if>`/`<foreach>` dynamic SQL** — check that values inside come through `#{}`, and that `<foreach>` for `IN` lists binds each item.
- Review the mapper XML files directly, not just the mapper interfaces — the SQL lives in the XML.

## 5. Connection and resource management

Resource leaks are the classic JDBC correctness bug and they take production down slowly:

- **Every `Connection`, `Statement`/`PreparedStatement`, and `ResultSet` must be closed**, in a `finally` or via **try-with-resources** (`try (Connection c = ...; PreparedStatement ps = ...)`). A leak on an error path exhausts the connection pool and hangs the app under load. Look specifically at exception paths — the happy path often closes fine while the `catch` leaks.
- **Connection pooling.** Confirm a pool (HikariCP, DBCP, c3p0, container datasource) is used rather than opening raw connections per request. Check pool sizing and leak-detection settings if visible.
- **`ResultSet` held open** while doing other work ties up the connection.

## 6. Transactions and concurrency

- **Transaction boundaries** — multi-statement invariants (debit + credit) must be atomic. See the Spring reference for `@Transactional` specifics; for manual JDBC, check `setAutoCommit(false)` + `commit`/`rollback` with rollback on the exception path.
- **Isolation level** appropriate to the consistency needs; be wary of read-modify-write races.
- **Long transactions** holding connections during slow/external calls.

## 7. Credentials and datasource config

- **Hardcoded DB credentials** in Java, `applicationContext.xml`, `*.properties`, `persistence.xml`, `hibernate.cfg.xml`, or MyBatis config XML — Critical if it's a real credential in source control. Recommend externalized/encrypted config (env, vault, JNDI, jasypt).
- **JNDI datasource** (`java:comp/env/jdbc/...`) is preferable to embedded credentials — note if the app embeds them instead.
- **Least privilege** — the app's DB user shouldn't be `root`/`dbo`/schema-owner if it only needs CRUD. Worth raising when visible.
- **SSL/TLS to the database** for sensitive data over untrusted networks.

## 8. Data exposure and general hygiene

- **`SELECT *`** couples code to schema and can over-fetch sensitive columns — prefer explicit column lists.
- **Passwords/secrets stored in plaintext.** A query that compares a password column against a raw value (`WHERE password = ?`) means credentials are stored unhashed — a Critical finding independent of framework. Passwords should be stored as salted hashes (BCrypt/Argon2) and verified in code, not matched in SQL.
- **Sensitive data logged** — queries or parameters containing PII/passwords written to logs.
- **Error messages** returning raw `SQLException` detail to users (schema disclosure).
- **Missing pagination** on queries that can return unbounded rows (memory + performance).
- **Unparameterized `count`/report queries** assembled from filters are a frequently-overlooked injection surface.
