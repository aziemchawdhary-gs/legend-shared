# PRD: Upgrade legend-shared from Dropwizard 1.3 to Dropwizard 4

**Author:** Platform Engineering
**Date:** 2026-03-26
**Status:** Draft
**Stakeholders:** Legend Engine, Legend SDLC, Legend Depot teams

---

## Context & Why Now

- **legend-shared** is the foundational library providing Dropwizard bundles (authentication via pac4j, OpenTracing, server utilities) consumed by all Legend platform services (legend-engine, legend-sdlc, legend-depot).
- The project is pinned to **Dropwizard 1.3.29** (released 2021), which depends on Jetty 9 (EOL June 2022), Jersey 2.25, and the javax namespace. Source -- Jetty 9.4 is end-of-community-support per Eclipse Jetty lifecycle page.
- **Jetty 9 receives no security patches.** The recent log4j fix (#211) illustrates that the project is actively maintained but running on unsupported infrastructure.
- Dropwizard 4.0 (released 2023) requires Jakarta EE 10, Jetty 12, Jersey 3.1, and Java 11+. This is a three-major-version jump (skipping DW 2.x and 3.x) because there is no incremental stepping-stone that delivers value. Source -- Dropwizard 4.0.0 release notes.
- **Downstream projects cannot upgrade** their own Jetty/Jersey/Jakarta dependencies until legend-shared moves first. This is the critical-path blocker.

---

## Users & Jobs-to-Be-Done

| User | Job | Pain Today |
|------|-----|------------|
| **Legend service developers** (engine, SDLC, depot) | Depend on legend-shared for auth bundles and server utilities | Blocked from adopting Jetty 12, Jakarta EE 10, modern security patches |
| **Platform/infra engineers** | Run Legend services in production | Jetty 9 EOL means no CVE patches; javax servlet API limits container choices |
| **Open-source contributors** | Contribute to Legend ecosystem | Java 8 target and outdated APIs raise contributor friction; modern IDEs default to Jakarta |

---

## Business Goals & Success Metrics

### Goals
1. Unblock downstream Legend projects from upgrading to Dropwizard 4 / Jakarta EE 10.
2. Eliminate dependency on EOL Jetty 9 and unsupported javax servlet APIs.
3. Maintain backward-compatible public API surface where possible (package names will change due to jakarta migration).

### Leading Metrics
- All 10 modules compile and pass `mvn clean verify` on JDK 11 and JDK 17.
- Zero `javax.servlet`, `javax.ws.rs`, or `javax.annotation` imports remain (excluding JDK-owned `javax.security.auth.Subject`).
- At least one downstream project (legend-engine or legend-sdlc) can integrate the upgraded artifact in a feature branch.

### Lagging Metrics
- All downstream Legend projects release with Dropwizard 4 within 2 quarters of legend-shared release.
- Zero Jetty 9 / Jersey 2.x CVEs in Legend production deployments.

---

## Functional Requirements

### FR-1: Java Baseline Upgrade
Update `maven.compiler.release` from 8 to 11 (minimum required by DW 4).

**Acceptance Criteria:**
- `maven.compiler.release` property is set to `11` in the root POM.
- Build succeeds on JDK 11, 17, and 21.
- No Java 8-only source patterns remain that would fail under `--release 11`.

### FR-2: POM Dependency Overhaul
Introduce Dropwizard 4 BOM and update all managed dependency versions.

**Acceptance Criteria:**
- `dropwizard.version` = 4.0.x with DW BOM imported in `<dependencyManagement>`.
- Manually-managed Jetty, Jersey, and Jackson versions are removed (delegated to DW BOM).
- Updated versions: pac4j 3.8.3 to 5.x+, dropwizard-pac4j to DW 4-compatible release, Spring test to 6.x, Spring Boot autoconfigure to 3.x, SLF4J to 2.x, Mockito to 5.x, SnakeYAML to 2.x, Guava to 32.x+, JAXB to 4.0.x (jakarta.xml.bind-api + jaxb-runtime).
- Removed: `javax.servlet-api`, `javax.ws.rs-api`, `javax.activation`, `jaxb-api` (replaced by Jakarta equivalents).
- Jetty artifacts reference `org.eclipse.jetty.ee10` where needed.
- `mvn dependency:tree` shows no javax servlet/JAX-RS/activation transitive dependencies.

### FR-3: javax to jakarta Namespace Migration
Migrate all 26 affected source files (110 occurrences) from javax to Jakarta namespaces.

**Acceptance Criteria:**
- All `javax.servlet.*` imports become `jakarta.servlet.*`.
- All `javax.ws.rs.*` imports become `jakarta.ws.rs.*`.
- All `javax.annotation.*` imports become `jakarta.annotation.*`.
- `javax.security.auth.Subject` remains unchanged (JDK class, not Java EE).
- Grep for `javax\.(servlet|ws\.rs|annotation)` across `*.java` returns zero results.

### FR-4: Dropwizard API Migration
Adapt all bundle and configuration code to Dropwizard 4 package structure.

**Acceptance Criteria:**
- All imports updated: `io.dropwizard.Configuration` to `io.dropwizard.core.Configuration`, `io.dropwizard.setup.Bootstrap` to `io.dropwizard.core.setup.Bootstrap`, `io.dropwizard.setup.Environment` to `io.dropwizard.core.setup.Environment`, `io.dropwizard.server.SimpleServerFactory` to `io.dropwizard.core.server.SimpleServerFactory`.
- `Bundle` interface usage replaced with `ConfiguredBundle` (affects `HtmlRouterRedirectBundle`, `OpenTracingBundle`, `HostnameHeaderBundle`).
- `AssetServlet` constructor calls updated if signature changed.
- `ErrorPageErrorHandler` references updated to ee10 package.

### FR-5: Jetty 12 Internals Migration
Rewrite code that depends on Jetty 9 internal APIs removed in Jetty 12.

**Acceptance Criteria:**
- `ChainFixingFilterHandler`: Adapted to Jetty 12 `ee10.servlet.ServletHandler`. Method signature `getFilterChain(Request, String, ServletHolder)` updated to match Jetty 12 API. Filter reordering logic preserved.
- `UsernameFilter`: `HttpConnection.getCurrentConnection().getHttpChannel().getRequest()` replaced with Jetty 12 equivalent (e.g., `Request.getBaseRequest()` via `ServletApiRequest` or `Request` from `ServletContextRequest`). Authentication setting mechanism updated.
- `CrossOriginFilter` usage migrated to `org.eclipse.jetty.ee10.servlets.CrossOriginFilter` or `CrossOriginHandler`.
- Cookie SameSite and `SessionCookieConfig` API calls updated.
- All `org.eclipse.jetty.servlet.*` imports become `org.eclipse.jetty.ee10.servlet.*` where applicable.

### FR-6: pac4j Upgrade
Upgrade pac4j stack from 3.x to 5.x+ with dropwizard-pac4j DW 4 compatibility.

**Acceptance Criteria:**
- `J2EContext` replaced with `JEEContext` (or `WebContext` if pac4j 5.x).
- `SessionStore` generic type parameters removed (pac4j 5.x change).
- `SecurityFilter` import updated from `org.pac4j.j2e.filter` to `org.pac4j.jee.filter` (or equivalent).
- `DefaultSecurityLogic` calls updated (`setClientFinder`, `setProfileStorageDecision` API changes).
- `pac4j-j2e` dependency replaced with `pac4j-jee` (or `pac4j-jakartaee`).
- `LegendPac4jBundle` compiles and functions: session stores (HttpSessionStore, NullSessionStore, HazelcastSessionStore, MongoDbSessionStore) all updated.
- `LegendUserProfileStorageDecision` and `LegendClientFinder` adapted to new APIs.
- Confirmed: dropwizard-pac4j has a DW 4-compatible release, or a fork/alternative is identified.

### FR-7: OpenTracing Compatibility
Resolve dependency on archived opentracing-jaxrs2 which has no Jakarta release.

**Acceptance Criteria:**
- One of two approaches implemented:
  - **(A) Inline:** `CastUtils`, `SpanWrapper`, `ServerHeadersExtractTextMap` from opentracing-jaxrs2 0.5.0 are copied into `legend-shared-opentracing-servlet-filter` with appropriate license headers, and javax imports replaced with Jakarta.
  - **(B) OpenTelemetry migration:** OpenTracing dependency replaced with OpenTelemetry bridge/SDK. (Larger scope -- requires separate decision.)
- `OpenTracingFilter` compiles with Jakarta servlet API.
- `opentracing-jaxrs2` removed from dependency tree.

### FR-8: Test Suite Migration
All tests compile and pass under new dependency stack.

**Acceptance Criteria:**
- javax to jakarta applied in all test files.
- `Spring MockHttpServletRequest` uses Spring 6.x `jakarta.servlet` variant.
- Jersey test framework provider updated for Jersey 3.1.x.
- `J2EContext` replaced with `JEEContext` in test utilities (e.g., `SessionStoreTestUtil`).
- `DropwizardAppRule` usage evaluated; migrated to `DropwizardAppExtension` if JUnit 5 adopted, or kept with JUnit 4 vintage if not.
- `mvn clean verify` passes all modules with zero test failures.

---

## Non-Functional Requirements

### Performance
- No measurable regression in request latency through pac4j filter chain. Benchmark before/after on a representative downstream service.

### Scale
- No change to concurrency model. Jetty 12 virtual-thread support is out of scope for this upgrade but should not be precluded.

### SLOs / SLAs
- Not directly applicable (library project). Downstream services define their own SLOs. The upgrade must not degrade downstream service startup time by more than 10%.

### Privacy & Security
- Session cookie attributes (`Secure`, `HttpOnly`, `SameSite`) must remain enforced after migration (ref: commit 826ed8f).
- No new dependency with known CVEs at time of merge (verify via `mvn dependency-check:check` or equivalent).
- Eliminating Jetty 9 is itself a security improvement.

### Observability
- OpenTracing span creation, propagation, and decorator behavior must remain functional.
- If approach (A) is chosen for FR-7 (inline classes), add unit tests for inlined classes.

### Compatibility
- Published Maven artifacts must have correct `<dependencies>` in POM so downstream projects transitively pull Jakarta APIs, not javax.
- Minimum JDK: 11. Tested on: 11, 17, 21.

---

## Scope

### In Scope
- All 10 modules in legend-shared.
- Dependency version updates, namespace migration, API adaptation, test fixes.
- Verification that at least one downstream project can compile against updated artifacts.

### Out of Scope
- Downstream project upgrades (legend-engine, legend-sdlc, legend-depot) -- those are separate PRDs.
- JUnit 4 to JUnit 5 full migration (optional, can be done as follow-up).
- OpenTelemetry migration (if inline approach chosen for FR-7).
- Virtual thread adoption.
- Dropwizard 4 new features (health check improvements, etc.) -- adopt later.

---

## Rollout Plan

### Phase 0: Preparation (Week 1)
- Confirm dropwizard-pac4j DW 4-compatible release exists (Decision Point 1).
- Decide OpenTracing approach: inline vs. OpenTelemetry (Decision Point 2).
- Create `dropwizard4` feature branch.

### Phase 1: Foundation (Weeks 1-2)
- Java 11 baseline (FR-1).
- POM dependency overhaul (FR-2).
- javax to jakarta mechanical migration (FR-3).
- Gate: `mvn compile` succeeds on all modules (tests may still fail).

### Phase 2: Core API Migration (Weeks 2-4)
- Dropwizard API changes (FR-4).
- Jetty 12 internals (FR-5) -- highest-effort item.
- pac4j upgrade (FR-6) -- highest-risk item.
- OpenTracing fix (FR-7).
- Gate: `mvn compile` with tests succeeds; all source compiles.

### Phase 3: Test Stabilization (Weeks 4-5)
- Test migration (FR-8).
- Gate: `mvn clean verify` green on JDK 11 and 17.

### Phase 4: Downstream Validation (Week 5-6)
- Publish SNAPSHOT to local/staging repo.
- Verify legend-engine or legend-sdlc compiles against new artifacts.
- Gate: at least one downstream project builds successfully.

### Phase 5: Release (Week 6-7)
- PR review, merge to master.
- Release via existing Maven release plugin workflow.
- Communicate to downstream teams.

### Guardrails
- CI runs on every push to `dropwizard4` branch: `mvn clean verify` on JDK 11, 17, 21.
- Binary compatibility report (e.g., japicmp) against 0.25.7 to document all breaking changes for downstream consumers.
- Dependency vulnerability scan on every PR update.

### Kill Switch
- The `dropwizard4` branch does not merge to master until all gates pass.
- If dropwizard-pac4j has no DW 4-compatible release, the project can be forked under `org.finos.legend.shared` groupId as a temporary measure; the kill switch is reverting to the fork-free approach by not merging.
- Post-release: if a critical regression is found, revert to 0.25.x release line (still on DW 1.3) and re-tag. Downstream projects pin to the last known-good version.

---

## Risks & Mitigations

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|-----------|--------|------------|
| R1 | **dropwizard-pac4j has no DW 4-compatible release** | Medium | Critical | Investigate immediately. Fallback: fork and maintain internally, or rewrite LegendPac4jBundle to use pac4j-jakartaee directly without the dropwizard-pac4j wrapper. |
| R2 | **ChainFixingFilterHandler cannot be ported to Jetty 12** -- relies on `ServletHandler.getFilterChain()` override which may not exist | Medium | High | Evaluate if Jetty 12 `ServletHandler` still supports override. Alternative: use a Jetty `Handler` wrapper or `ServletContainerInitializer` to reorder filters at startup instead of lazily. |
| R3 | **UsernameFilter uses removed Jetty internals** (`HttpConnection.getCurrentConnection().getHttpChannel().getRequest()`) | High | Medium | In Jetty 12, use `Request.getBaseRequest(Request)` utility or `ServletContextRequest` to access the core request. Fallback: set authentication via `SecurityHandler` API. |
| R4 | **opentracing-jaxrs2 archived, no Jakarta build** | Certain | Medium | Inline the 3 required classes (< 200 LOC total). Low effort, well-contained. |
| R5 | **pac4j 5.x API changes are larger than anticipated** | Medium | High | Time-box pac4j investigation to 3 days. If API delta is unmanageable, consider pac4j 4.x as intermediate step. |
| R6 | **Downstream projects discover additional incompatibilities** during Phase 4 | Medium | Medium | Phase 4 validation is explicitly gated. Allocate 1 week buffer. |
| R7 | **MongoDB driver compatibility** -- current driver 3.12.8 may conflict with newer pac4j/Spring | Low | Medium | Upgrade to MongoDB driver 4.x if needed; MongoDbSessionStore is relatively isolated. |

---

## Open Questions

| # | Question | Owner | Deadline | Notes |
|---|----------|-------|----------|-------|
| OQ1 | Does dropwizard-pac4j have a release compatible with DW 4? Check Maven Central and GitHub. | Lead dev | Week 1 | Blocking for Phase 2. If not, evaluate pac4j-jakartaee direct integration. |
| OQ2 | Inline opentracing-jaxrs2 classes or migrate to OpenTelemetry? | Tech lead | Week 1 | Recommend inline for this PRD; OpenTelemetry as follow-up. |
| OQ3 | Should we adopt JUnit 5 as part of this upgrade or defer? | Team | Week 1 | DW 4 ships JUnit 5 extensions. Recommend defer to reduce scope. |
| OQ4 | What is the minimum pac4j version that supports Jakarta namespace? | Lead dev | Week 1 | pac4j 5.x introduced JEE support; pac4j 6.x is Jakarta-native. Need to confirm dropwizard-pac4j alignment. |
| OQ5 | Do downstream projects need a migration guide for the javax-to-jakarta change in legend-shared's public API? | PM | Week 2 | Any class extending legend-shared bundles or implementing its interfaces will need import changes. |

---

## Appendix: Module Impact Summary

| Module | Files Changed | Risk Level | Key Changes |
|--------|--------------|------------|-------------|
| legend-shared-pac4j | ~15 | HIGHEST | pac4j 5.x, Jetty 12 internals, jakarta, DW 4 bundle API |
| legend-shared-pac4j-kerberos | ~3 | MEDIUM | jakarta, pac4j context changes |
| legend-shared-pac4j-gitlab | ~2 | LOW | jakarta, pac4j client changes |
| legend-shared-pac4j-ping | ~2 | LOW | jakarta, pac4j client changes |
| legend-shared-server | ~12 | HIGH | Jetty 12 servlet API, DW 4 bundle API, ChainFixingFilterHandler |
| legend-shared-opentracing-base | ~2 | LOW | Minimal changes |
| legend-shared-opentracing-jersey | ~3 | MEDIUM | jakarta.ws.rs, Jersey 3.1 |
| legend-shared-opentracing-servlet-filter | ~8 | HIGH | Archived opentracing-jaxrs2, jakarta.servlet |
| legend-shared-opentracing-test | ~2 | LOW | Test dependency updates |
| legend-shared-test-reports | 0 | NONE | Aggregation module only |

---

## Appendix: Key Version Matrix

| Dependency | Current | Target | Notes |
|------------|---------|--------|-------|
| Dropwizard | 1.3.29 | 4.0.x | Three major version jump |
| Jetty | 9.4.44 | 12.x | Managed by DW BOM |
| Jersey | 2.25 | 3.1.x | Managed by DW BOM |
| Jackson | 2.10.1 | 2.15+ | Managed by DW BOM |
| pac4j | 3.8.3 | 5.x+ or 6.x | Depends on OQ1/OQ4 |
| dropwizard-pac4j | 3.0.0 | TBD | Depends on OQ1 |
| SLF4J | 1.7.21 | 2.x | Managed by DW BOM |
| Spring Test | 4.3.24 | 6.x | Test scope only |
| Spring Boot Autoconfigure | 2.3.3 | 3.x | |
| Mockito | 3.5.10 | 5.x | Test scope only |
| SnakeYAML | 1.33 | 2.x | |
| Guava | 30.0 | 32.x+ | |
| JAXB (jakarta.xml.bind-api) | 2.3.2 | 4.0.x | |
| Java baseline | 8 | 11+ | DW 4 requirement |
