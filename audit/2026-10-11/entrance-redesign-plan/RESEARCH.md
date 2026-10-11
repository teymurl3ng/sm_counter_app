# Design research — bounded study

Reviewed 11 October 2026. Primary product/platform documentation establishes supported patterns; it does not establish that our exact composition is industry-mandated or already usable on Android.

|Reference|Verified pattern|Adaptation here|
|---|---|---|
|[Shopify POS lock-screen customization](https://help.shopify.com/en/manual/sell-in-person/shopify-pos/customize-pos/lock-screen)|Shop identity can be expressed with a logo and background at the access boundary.|Use the configured shop name and existing small mark on the entrance canvas. A separate operational header is unnecessary there. No new logo/background settings.|
|[Shopify POS staff access](https://help.shopify.com/en/manual/sell-in-person/shopify-pos/manage-your-shopify-pos-pin)|Staff access identifies the individual; permissions are managed separately.|Keep name, role and selected identity obvious. Preserve our enrolled PIN/phone-security/passphrase choices; do not copy Shopify's authentication policy.|
|[Square shared POS passcodes](https://squareup.com/help/us/en/article/8357-require-passcodes-at-point-of-sale)|Personal staff access supports attribution; shared team access has attribution limits.|Retain the explicit person picker and all existing authorization. Decoration must not obscure which person is signing in.|
|[Impeccable layout](https://impeccable.style/docs/layout/) and [typeset](https://impeccable.style/docs/typeset/)|Arrange around the task, group related content, remove unnecessary boxes and establish consistent text roles; check long text and scaling.|Remove repeated entrance metadata and the duplicate account-row framing. Keep Manrope; use one title role and one supporting role. Impeccable is a researched reference, not an installed/invoked skill.|
|[Android Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)|Interactive targets should be at least 48dp; expanding small targets can cause overlap.|Keep Back and account rows comfortably tappable. Separate the status pill's visible proportions from its non-overlapping 48dp-minimum touch area.|

## Existing Stitch study

The connected Stitch project `17568665013202933334` and its screens were checked read-only. Local final A/B Welcome images were inspected alongside current installed-app captures. Both earlier Welcome treatments used a neutral canvas without an operational green band. A used a compact mark; B used a larger illustration. Neither was previously adopted wholesale (`design/stitch/REVIEW.md`). They are proposals, not broadly deployed software or proof of usable native layouts.

Carry forward the compact identity, neutral canvas and clear primary action. Avoid the large dairy illustration, hardcoded Saaf Milk title and oversized assurance cards: they consume space and conflict with the configurable shop identity. No new generation, project mutation or automatic adoption was performed.

## Recommended direction

One calm entrance family: warm ivory background, existing deep-green accents, restrained yellow primary action, established Manrope. Welcome presents the shop; access presents the task and the person. Green remains a focused accent here and the useful identity band after authentication.

Alternative considered: remove the band only on Welcome/picker while preserving the green band on passphrase and recovery. Rejected as the recommended direction because it would produce a pronounced chrome change on the ordinary picker-to-sign-in path and duplicate the shared frame. The broader neutral access frame is still a small presentation scope, not an auth-feature redesign.

Our exact dp/sp, copy and motion choices in PLAN.md are proposals to test, not values claimed by these sources.
