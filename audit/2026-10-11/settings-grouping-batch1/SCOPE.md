# Batch 1 execution boundary

Only shared information section/heading/fact primitives and Shop settings overview/category detail are changed. Existing identity/rates/opening/create forms, role wrappers, business state and callbacks, top/bottom chrome and portrait lock are retained. Category rows keep their current surface; remove their redundant exterior divider. No other batch is started.

Capture the current A06 preview settings screen before production edits, then inspect affected layouts using focused component/settings tests and A06/S24 portrait evidence. Tests run only in com.saaf.milkshop.qa.tests; interactive preview updates use com.saaf.milkshop.qa. Ordinary com.saaf.milkshop metadata is recorded for both devices and must remain unchanged. Existing preview data is preserved.

The user's scoped-test instruction overrides NATIVE_SCREEN_ACCEPTANCE.md's broad qaFast/checkpoint and landscape requirements. Select the settings contract class, new meaningful layout/reflow checks, and Owner/Admin settings navigation checks. Do not run the full suite. Screenshot review remains UNREVIEWED until user acceptance; agent inspection does not self-approve sidecars.

Before-source hashes capture the pre-existing entrance/Quick PIN working changes. This batch must not rewrite them. New unrelated findings go to settings-grouping-plan/LATER.md.
