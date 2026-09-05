# Phase 12.5 — Measured Presentation Performance

Status: **COMPLETE — 2026-09-05**

## Decision

No presentation optimization is justified by the measured Samsung results. Startup, React readiness, and painted navigation are comfortably within the locked budgets, and representative navigation issues no native bridge queries. Phase 12.5 therefore keeps the one-WebView architecture, production bundle structure, security/session checks, and native data-freshness behavior unchanged.

## Measurement build

The Android project now has a dedicated `benchmark` build type initialized from `release`, signed only for local testing, non-debuggable, and profileable by shell. With `-Psaaf.isolatedQa=true`, it installs as `com.saaf.milkshop.qa.benchmark`, separate from both the real app and the ordinary QA app. Normal builds continue to use the existing debug/release paths; `-Psaaf.performanceTestBuild=true` selects benchmark as the instrumented-test target only for this measurement run.

The packaged UI is still the production Vite output. A presentation-only `saaf-react-ready` Performance Mark is emitted after the first non-checking React state has committed and received a second animation frame. It contains no business data and does not change bridge traffic.

## Physical-device results

Device: Samsung SM-A065F, Android 16, arm64-v8a.

| Measurement | Samples | Median | P95 | Locked budget |
| --- | ---: | ---: | ---: | ---: |
| Android cold-process `TotalTime` | 10 | 822 ms | 871 ms | 2,000 ms |
| WebView first contentful paint | 7 | 136 ms | 204 ms | 1,500 ms |
| React ready after document start | 7 | 327.9 ms | 378.3 ms | 2,000 ms |
| Host-observed React ready | 7 | 526.3 ms | 640.6 ms | observation only |
| Painted preloaded navigation | 30 | 24.0 ms | 29.6 ms | 100 ms median / 180 ms p95 / 250 ms max |

Painted navigation maximum was 30.6 ms. The thirty measured transitions cycled through Daily work, Deliveries, and Accounts after six warmups. Bridge metrics remained at zero calls, demonstrating that these preloaded destination changes do not request duplicate native data.

Raw sample series are retained in `phase12-evidence/sm-a065f-android16-phase12-5-performance/metrics.tsv`. The seven-line evidence file SHA-256 is `A34327B3EFADEC0A476F5F60B6836082DB47AA31669534C7014E619EC7F7BAAC`.

## Why no optimization was made

- Code splitting or deferred native reads would add lifecycle, loading, and freshness complexity without solving a measured problem.
- Removing security/session checks to improve a timing number is prohibited.
- The local production bundle has no network-transfer startup cost inside the packaged WebView.
- Every measured p95 is far inside its guardrail on the project's lower-end physical Samsung baseline.

This is a deliberate no-change performance decision, not a skipped optimization pass.

## Repeatable gate

`Phase12PresentationPerformanceTest` records seven first-contentful-paint/React-ready samples and thirty double-animation-frame navigation samples. It enforces:

- first-contentful-paint p95 no greater than 1,500 ms;
- React-ready p95 no greater than 2,000 ms;
- painted navigation median no greater than 100 ms, p95 no greater than 180 ms, and maximum no greater than 250 ms;
- zero native bridge calls during the selected preloaded navigation sequence.

The test also runs on the normal debug target during the eventual complete regression, while the benchmark property provides the release-equivalent measurement path.

## Verification

- TypeScript lint and production Vite build — passed.
- Packaged UI size — CSS 85.71 kB (14.11 kB gzip); JavaScript 433.76 kB (111.18 kB gzip).
- Normal debug Android/instrumentation compilation — passed.
- Benchmark Android/instrumentation compilation — passed.
- Merged benchmark manifest — `profileable`, shell-enabled, and not debuggable.
- `Phase12PresentationPerformanceTest` — 2/2 passed on Samsung SM-A065F / Android 16.
- Android cold-process start command — 10/10 successful.
- Bridge contract — unchanged at v4, 86 endpoints, SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.
- The last complete isolated-QA regression remains Phase 11 at 247/247; Phase 12.6 owns the new complete-suite result.

## Limitations

The Android cold-process result measures the platform-reported initial display time for a fresh benchmark process. WebView paint/React-ready samples are repeated document starts inside the benchmark instrumentation process. Navigation samples cover preloaded top-level destinations and intentionally exclude actions that must fetch fresh native detail. These are controlled device measurements, not a claim about every Android model or a production field percentile.

## Plain English

The app already opens and changes its main screens quickly on the real Samsung phone. Making the code more complicated would not provide a useful improvement, so we kept the secure and reliable design and added a test that will warn us if it becomes slower later.
