# After this pass

- Capsule/menu styling overhaul: user reports misalignment, poor fit and prefers the older appearance. Do not change capsule styling during this pass.
- Landscape design is outside the phone product scope, which is portrait. Do not carry the prior landscape layout suggestion forward as a required repair.
- Tablet/desktop layouts and exhaustive physical per-state motion/TalkBack review require separate scope.

The preflight video is retained as earlier evidence. The finalized checkpoint route video has an exact test-start trigger and a complete MP4 container. Both require compatible MP4 playback for visual review; no decoder tooling expansion in this pass.

Decoder clarification: the installed Playwright ffmpeg supports WebM, not MP4 input. Its decode error does not establish corruption. Container structure is recorded in motion-container-check.json; retain the finalized route video for native playback/Owner review rather than spending this pass on decoder tooling.

Evidence limits noted for later: Collect ready/IME captures were made while the business day was closed by an earlier test; they prove visibility, not an open-day write. The 150% standalone ComponentActivity fixture has dark status icons on the green header; production NativePreviewActivity captures use light icons. Neither observation was expanded into new work in this pass.
