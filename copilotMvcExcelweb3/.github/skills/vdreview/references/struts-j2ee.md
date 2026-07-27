# Struts & J2EE Review

Reference for the web tier: Struts actions and their XML wiring, ActionForms, servlets, JSPs, and `web.xml`. Struts has one of the worst CVE histories of any Java framework, so treat this layer as security-critical.

## Contents
1. Struts version and why it matters
2. Struts action mappings (`struts-config.xml` / `struts.xml`)
3. ActionForms and input validation
4. Known Struts RCE / OGNL exposure
5. JSP and view-layer issues
6. Servlets and `web.xml`
7. General J2EE concerns

---

## 1. Struts version and why it matters

Establish whether this is **Struts 1** (`org.apache.struts.action.Action`, `struts-config.xml`, `ActionForm`) or **Struts 2** (`com.opensymphony.xwork2.ActionSupport`, `struts.xml`, OGNL value stack). They are different frameworks with different risks:

- **Struts 1** reached end-of-life in 2013 and receives no security patches. Its presence is itself a finding — flag it and recommend a migration path, because unpatched framework code is a standing liability.
- **Struts 2** is where the famous OGNL remote-code-execution CVEs live (see §4). Version and patch level matter enormously.

## 2. Struts action mappings (`struts-config.xml` / `struts.xml`)

Action mappings are the routing and security backbone of a Struts app. Review the config file directly.

Check for:

- **Wildcard mappings that are too permissive.** In `struts.xml`, `<action name="*" method="{1}">` or Dynamic Method Invocation (`action!method`) lets a caller invoke arbitrary methods on the action. DMI (`struts.enable.DynamicMethodInvocation=true`) has been the root of multiple RCEs — flag it and recommend disabling it.
- **Missing or catch-all forwards.** Every logical outcome should map to a defined `<forward>` / `<result>`. An action that returns an unmapped result, or forwards based on unvalidated user input, can enable open redirects or unexpected navigation.
- **Actions reachable without authentication.** Cross-reference each `<action>` path against the security constraints in `web.xml` (or Struts interceptors). An action that mutates state or exposes data but sits outside any `<security-constraint>` is a missing-authorization finding.
- **Form bean wiring.** In Struts 1, confirm each `<action>` references the right `<form-bean>` and `scope` (request vs session). Session-scoped forms holding per-request data cause cross-request data bleed and are a common source of subtle bugs.
- **Exception handling.** Look for global `<exception>` / `<global-exceptions>` handlers. Actions that let raw exceptions propagate leak stack traces to users (information disclosure) and give a poor UX.
- **Interceptor stack (Struts 2).** Confirm custom actions still run through the `defaultStack` (or an equivalent that includes `params`, `validation`, and `workflow`). Actions that bypass the default interceptor stack often bypass validation and token checks too.
- **CSRF tokens.** Look for `<s:token>` / the `token` interceptor on state-changing actions. Struts does not add CSRF protection automatically; its absence on POST actions that modify data is a real finding.

## 3. ActionForms and input validation

- **Validation presence.** Is input validated via `validation.xml` / the `validate()` method / annotations? Unvalidated form fields flow into business logic and queries.
- **Type coercion / mass assignment.** Struts populates form/action fields from request parameters automatically. If the action exposes sensitive setters (e.g., `setAdmin`, `setPrice`), an attacker can set them via extra parameters (parameter tampering / mass assignment). Recommend `excludeParams` / restricting bound properties.
- **Server-side, not just client-side.** JavaScript validation is not security. Confirm the same rules exist server-side.

## 4. Known Struts RCE / OGNL exposure

Struts 2's use of OGNL against the value stack has produced repeated critical RCEs. You don't need to memorize every CVE, but recognize the shapes:

- **OGNL injection** — any place where user input reaches an OGNL evaluation (`%{...}` in tags fed by request data, forced double-evaluation, `#context` manipulation). This is the mechanism behind the widely-exploited Struts RCE advisories (e.g., the S2-045 Content-Type / Jakarta multipart parser issue and S2-057 namespace/redirect issue). If the code or version predates the relevant patches, flag it as Critical.
- **Multipart / file upload parsers** — historically a rich source of RCE. Check the configured parser and version.
- **`DynamicMethodInvocation`** enabled (see §2).

For any Struts 2 app, "what version, and is it patched?" is a required question. An unpatched known-vulnerable version is the single most serious thing you can find here.

## 5. JSP and view-layer issues

- **XSS.** The big one. Raw output of user data into HTML is a stored/reflected XSS vector. In JSP that means `<%= userValue %>` scriptlets or `<c:out ... escapeXml="false">`. `<c:out>` escapes by default — flag anywhere that default is turned off or bypassed. Struts tags like `<s:property>` escape by default too (`escapeHtml="false"` disables it).
- **Scriptlets.** `<% ... %>` Java in JSPs mixes logic into the view, is hard to secure and test, and often hides the XSS above. Recommend JSTL/EL and tag libraries.
- **Direct request access in views** (`request.getParameter` inside the JSP) usually means unvalidated data is being echoed — trace where it goes.
- **Path/JSP inclusion** driven by user input (`<jsp:include page="${param.x}">`) is a file-inclusion / path-traversal risk.

## 6. Servlets and `web.xml`

`web.xml` is where a lot of J2EE security is declared. Review it:

- **`<security-constraint>` coverage** — which URL patterns are protected, by which roles, and which are wide open. Look for state-changing endpoints outside any constraint.
- **`<transport-guarantee>CONFIDENTIAL</transport-guarantee>`** — is transport security enforced for auth'd areas, or can credentials travel over plain HTTP?
- **Error pages** — `<error-page>` should map exceptions and 500s to a safe page. Without it, containers show stack traces.
- **Session config** — `<session-config>`/`<cookie-config>`: is `HttpOnly` set? `Secure`? A sane `<session-timeout>`?
- **Servlet mappings** — any debug/admin servlet mapped and reachable? Any `invoker` servlet enabled (a classic hole)?
- **Filters** — auth/CSRF/encoding filters in the right order and covering the right patterns.

## 7. General J2EE concerns

- **Thread safety.** Servlets and Struts 1 actions are singletons — one instance serves all requests. Instance fields holding request state are a concurrency bug that surfaces as random cross-user data corruption under load. This is a frequent, serious, easily-missed finding.
- **Resource management.** JDBC connections, streams, JNDI lookups closed in `finally` / try-with-resources.
- **Insecure deserialization.** `ObjectInputStream` on untrusted data, or vulnerable libraries on the classpath, is an RCE vector.
- **Sensitive data in session** without need, or session fixation (no session regeneration on login).
- **Hardcoded secrets / JNDI datasource credentials** in config or code.
