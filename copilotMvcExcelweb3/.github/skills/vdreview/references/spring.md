# Spring Framework Review

Reference for Spring-wired code: dependency injection, bean lifecycle/scopes, Spring MVC, Spring Security, transactions, and the configuration (XML `applicationContext.xml`, annotations, or Java `@Configuration`) that drives all of it.

## Contents
1. Dependency injection
2. Bean scopes and thread safety
3. Configuration: XML vs annotation vs Java config
4. Spring MVC / web
5. Spring Security
6. Transactions
7. Externalized config and secrets
8. Known Spring exposure (SpEL, Spring4Shell class)

---

## 1. Dependency injection

DI style is the focus area here, and it has real consequences beyond taste.

- **Prefer constructor injection over field injection.** Field injection (`@Autowired` on a private field) hides dependencies, makes the class impossible to instantiate without reflection (so it's hard to unit-test), and permits circular dependencies to be constructed silently. Constructor injection makes dependencies explicit, supports `final` fields (immutability / thread safety), and fails fast on cycles. Flag heavy field injection as a maintainability/design finding and recommend constructor injection.
- **Circular dependencies.** If bean A needs B and B needs A via constructors, Spring can't build them and throws at startup. Teams often "fix" this by switching one side to field/setter injection, which hides a genuine design problem. Call out the cycle, not just the injection style.
- **Injecting concrete classes instead of interfaces** reduces testability and couples beans to implementations.
- **`@Autowired` on a type with multiple candidate beans** without `@Qualifier`/`@Primary` is an ambiguity that fails at startup or, worse, wires the wrong bean.
- **Field/setter injection of required collaborators as non-final mutable fields** — combined with a singleton scope, this can be a thread-safety issue if anything mutates them post-construction.
- **`@Value` injection** — check for sane defaults and that required properties aren't silently empty.

## 2. Bean scopes and thread safety

- **Default scope is singleton** — one instance shared across all threads. **Singleton beans must be stateless.** A singleton `@Service`/`@Controller` with mutable instance fields that change per request is a concurrency bug that corrupts data under load. This is one of the most common and most serious Spring findings — look for it specifically.
- **`prototype`/request/session scope** — confirm the scope matches the intended lifecycle. A prototype bean injected once into a singleton is only created once (scoped-proxy needed), a classic gotcha.
- **Non-thread-safe helpers held as singleton fields** — `SimpleDateFormat`, non-thread-safe SDK clients, etc. — are the same bug in disguise.

## 3. Configuration: XML vs annotation vs Java config

Spring config lives in `applicationContext.xml` (and `*-context.xml`, `dispatcher-servlet.xml`), in annotations (`@Component` + `@ComponentScan`), or in `@Configuration` classes. Review whichever is present.

For **XML config** (`applicationContext.xml` etc.):
- **Bean definitions** — correct class, scope, and wiring. `<bean>` with `scope` unset defaults to singleton.
- **`<context:component-scan base-package="...">`** — scope of scanning; overly broad scans pick up unintended beans.
- **`<property>` / `<constructor-arg>` refs** resolve to real beans; watch for stale definitions referencing renamed classes.
- **Datasource beans** — see §7 for credentials; also check pool settings.
- **`autowire="byName"/"byType"` on `<bean>`** is fragile and surprising; prefer explicit wiring or annotation-based DI.
- **Dead config** — bean definitions for classes that no longer exist or are never used. Config rot is a real maintainability finding.

For **annotation / Java config**:
- `@ComponentScan` breadth, `@Profile` usage (are prod beans and test/mock beans cleanly separated?), `@Bean` methods that accidentally create multiple instances, and `@Import` chains.

## 4. Spring MVC / web

- **Request mappings** — `@RequestMapping`/`@GetMapping`/`@PostMapping`: state-changing operations should not be reachable via GET (CSRF-friendly, cache-leaking, logged in URLs). Flag GET mappings that mutate data.
- **Mass assignment / data binding.** `@ModelAttribute` binding a whole entity straight from request params lets attackers set fields they shouldn't (e.g., `role`, `id`). Recommend DTOs or `@InitBinder` `setAllowedFields`/`setDisallowedFields`.
- **Missing input validation** — `@Valid`/`@Validated` with Bean Validation constraints on request bodies/params.
- **Path variable / open redirect** — user-controlled values flowing into redirects (`redirect:` + user input) or file paths.
- **`@ResponseBody` returning entities directly** can leak fields (passwords, internal flags) via serialization — check what the object graph exposes.
- **CORS** — overly permissive `@CrossOrigin("*")` on authenticated endpoints.
- **Exception handling** — `@ControllerAdvice`/`@ExceptionHandler` should prevent stack traces reaching clients.

## 5. Spring Security

If Spring Security is present, it's the authz/authn backbone — review it closely:

- **Endpoint authorization rules** — `authorizeHttpRequests`/`authorizeRequests` (or `<intercept-url>` in XML). Look for `permitAll()` on sensitive paths, order-dependent rules where a broad `permitAll` precedes a narrower `authenticated()` (first match wins), and any endpoint not covered.
- **CSRF** — is it disabled (`.csrf().disable()`)? For browser-facing session apps that's usually wrong. It's often legitimately disabled for stateless token APIs — judge by context, but always flag it so the developer confirms it was intentional.
- **Password storage** — `NoOpPasswordEncoder` (plaintext!), or weak/absent hashing. Recommend `BCrypt`/`Argon2`.
- **Method security** — `@PreAuthorize`/`@Secured` on service methods where appropriate.
- **`permitAll`/`hasRole` string typos** — role name mismatches silently deny or grant.

## 6. Transactions

- **`@Transactional` placement.** On a `private` method or a self-invoked method within the same class, the proxy is bypassed and the annotation does nothing — a silent correctness bug. Flag self-invocation.
- **`readOnly`** on read paths (perf + intent), and correct **propagation/isolation** where it matters.
- **Checked exceptions don't roll back by default** — `@Transactional` rolls back on unchecked exceptions only unless `rollbackFor` is set. A method that catches an exception and continues may commit partial work.
- **Transaction spanning too much** (external calls inside a tx hold DB connections) or too little (multi-statement invariants not atomic).

## 7. Externalized config and secrets

- **Hardcoded secrets** — passwords, API keys, datasource credentials in `applicationContext.xml`, `application.properties`/`.yml`, or Java. These belong in a vault/env/`jasypt`-encrypted values. This is a Critical finding when it's a production credential in version control.
- **`PropertyPlaceholderConfigurer`/`@PropertySource`** — confirm secrets come from externalized, non-committed sources.
- **Actuator / debug endpoints** (`/actuator/env`, `/actuator/heapdump`) exposed without auth leak config and secrets.
- **Verbose error/`spring.profiles`** left in a prod-like profile.

## 8. Known Spring exposure (SpEL, Spring4Shell class)

- **SpEL injection.** User input flowing into a `SpelExpressionParser` (or `@Value("#{...}")` / `@PreAuthorize` expressions built from user data) allows expression-language injection → RCE. Trace any dynamic expression evaluation fed by request data.
- **Data-binding RCE (Spring4Shell, CVE-2022-22965).** Affected Spring MVC versions allowed manipulation of nested bean properties (e.g., `class.module.classLoader...`) via request params to achieve RCE. If the app is on an affected Spring version and binds request params to POJOs, flag the version and recommend patching / an `@InitBinder` denylist.
- **SnakeYAML / insecure deserialization** on the classpath handling untrusted input.
- **Old Spring versions generally** — like any framework, unpatched versions accumulate CVEs; note the version and whether it's current.
