# Phase 14.8.2 — Cashier native visual directions

Status: **HIGH-FIDELITY PROPOSAL FOR OWNER CHOICE — no Compose implementation authorized**

Date: 2026-09-12

## Why this replaces the first preview

The first Cashier and Operations preview was a functional wireframe. It usefully exposed navigation, field order, safe defaults, result feedback, error recovery, and authorization boundaries, but its rough presentation was explicitly rejected as an implementation blueprint.

This checkpoint compares three deliberate visual worlds on the same real Cashier sale. The content and command remain constant so the owner can judge hierarchy, density, character, and confidence rather than being distracted by different features.

## Product and platform foundation

- `PRODUCT.md` now records the durable Android product truth, users, operating context, capabilities, constraints, and accessibility needs.
- `DESIGN.md` still describes the superseded React/WebView Phase 12 target. It remains historical authority for that frozen build until an accepted native direction replaces it.
- Material 3 governs Android structure, components, semantics, system Back, insets, type scaling, navigation adaptation, and motion behavior. Brand character must fit inside those platform expectations.
- The Cashier sale remains a single-product native command. None of the directions invents a multi-item cart, printing, barcode scanning, integrated card hardware, or online checkout.

## Current external evidence

### Material 3

The three proposals use a compact Android navigation bar, visible text-field meaning, native control states, and restrained state transitions as their platform floor.

References: [Material 3 navigation bar](https://m3.material.io/components/navigation-bar/overview), [Material 3 text fields](https://m3.material.io/components/text-fields/overview), [Material interaction states](https://m3.material.io/foundations/interaction/states/overview)

### Square POS

Square documents item tiles, direct item selection, and a quantity-first shortcut for high-volume checkout. Saaf applies the lesson proportionally: Milk/Yogurt are direct choices, the number entry is visually dominant, and no search or category machinery is added for two products.

Reference: [Square cart workflow](https://squareup.com/help/us/en/article/8238-build-your-customer-s-cart-in-the-square-retail-pos-app)

### Shopify POS

Shopify's checkout sequence keeps product/cart, payment, and completion distinct, while its staff model supports manager approval for restricted actions. Saaf uses a firm total-and-save ending, an immediate success state, and a one-action supervisor sheet without copying Shopify's commerce breadth.

References: [Shopify retail POS](https://www.shopify.com/pos/retail-pos), [Shopify POS processing](https://help.shopify.com/en/manual/sell-in-person/hardware/pos-go/processing-a-sale)

## Shared functional anatomy

Every direction shows the same normal sequence:

1. identify the Cashier and open business date;
2. select Milk or Yogurt;
3. choose quantity or PKR entry;
4. enter the one dominant number;
5. choose paid now or customer account;
6. verify the payment destination or customer;
7. see the product, quantity, payment state, and final total together;
8. record the sale once;
9. receive an immediate, full-viewport saved result.

The proposal includes Ready, Credit sale, Stock error, Approval, and Success. Saving is also wired into the primary action: the entered screen remains visible, the action becomes disabled, and success replaces it after completion.

## Direction 1 — Counter Ledger

**Recommendation.**

### World

A warm contemporary transaction surface informed by the order book at a physical counter: dark dependable numerals, quiet ivory working space, deep bottle green, and a restrained yellow completion action.

### Why it fits

- The eye moves down one obvious route from product to quantity to payment to total.
- The number entry has strong authority without resembling a calculator keypad.
- The dark checkout dock creates a clear end to the transaction and keeps the final facts together.
- Warm low-glare surfaces suit repeated use in a bright shop and feel more owned than a generic white business app.
- The visual grammar can become more data-dense for Owner reporting while remaining recognizably Saaf.

### Risk

If the ledger reference becomes literal decoration, reports and settings could feel nostalgic. The accepted system must carry its disciplined hierarchy, numerals, color, and separators into other roles without adding paper textures, stamps, or ornamental bookkeeping imagery.

## Direction 2 — Fresh Utility

### World

A cool, bright Material 3 interpretation using mineral teal, pale mint, generous tonal containers, and softer geometry.

### Why it fits

- It is immediately familiar to Android users.
- Tonal containers communicate selection with less visible border structure.
- It maps directly to standard Material components and adaptive widths.
- Its quietness is friendly for first-time staff.

### Risk

This is the safest direction and therefore the easiest to make generic. The number entry, checkout dock, and completion state would need disciplined repetition to stop the wider app looking like an unbranded Material sample.

## Direction 3 — Market Book

### World

A compact high-contrast system informed by stamped order books and market labels: warm stone, black type, brick-red authority, amber emphasis, and tighter geometry.

### Why it fits

- The sharper shapes support denser management and history screens.
- The color system has a memorable identity without illustration.
- Strong labels and separators scan well during busy work.

### Risk

Brick red is culturally easy to read as warning or destructive. It would require stricter semantic handling than the other directions so a normal primary action never feels dangerous.

## Visual-system decisions represented in all three

- One sans-serif family with highly legible numerals and compact Android labels.
- 48 dp minimum targets and at least 8 dp between independent controls.
- Visible labels; placeholders never carry field meaning.
- No nested dashboard cards, decorative metrics, horizontal navigation, or persistent explanation above the task.
- Color is paired with icon, label, position, and selection state.
- The bottom bar remains four stable Cashier destinations.
- The account identity stays visible on a shared phone.
- The save block repeats the amount, item, quantity, and payment destination before mutation.
- Success replaces the form in the current viewport and includes record number, money destination, and stock effect.
- Errors preserve the draft and put the recovery next to the cause.

## Motion proposal

The prototype uses one restrained 200 ms fade/vertical settle when its state changes and a 240 ms success-symbol settle. These timings are **design judgment**, not benchmark measurement or device proof. The eventual Compose implementation should use standard Material shared-axis/fade-through behavior where it preserves continuity, disable nonessential translation under Android's Remove animations setting, and prove interruption and performance on the physical Samsung targets.

## Review gate

The owner is choosing the reusable visual world, not approving the final Cashier journey or Compose code yet. The review should answer:

- Which direction feels trustworthy and pleasant enough for all-day use?
- Is the selected product unmistakable?
- Is the large number easy to enter and verify?
- Are payment destination and final total visible before save?
- Does the success state feel conclusive without becoming celebratory or slow?
- Can the chosen world become both simpler for workers and denser for Owner/Manager screens?

After one direction is selected, apply that world to the complete Cashier and Operations material-state journeys. Only then ask for the visual-proposal approval that unlocks the later Owner/Manager/Administrator design work. Compose remains behind the complete five-role design gate.
