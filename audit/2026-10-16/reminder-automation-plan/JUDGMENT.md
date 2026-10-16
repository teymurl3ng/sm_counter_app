# Independent judgment after research and plan

16 October 2026. Read RESEARCH.md, PLAN.md and UI_PLAN.md before running `C:/Users/Teymur/.codex/hooks/independent_judgment.py`; saved its output in judgment-hook.json. The hook emits a fixed reminder. It has not reviewed, tested or approved this design. The assessment below is the substantive judgment.

## Evidence versus inference

Verified: the current app has per-sale terms, current debt/status, manual drafts, private statement sharing, local preparation history, WorkManager patterns and a non-backed-up installation ID. Existing vendor documentation supports configurable reminders and customer pause controls. Android scheduling can be delayed. Current local history and unique worker names do not establish shared review state or single-sender behavior.

Inference/product choice: a customer-level worklist fits repeated small dairy/retail credit sales better than one automated reminder per sale. Sunday statement preparation should not rewrite rolling seven-day or calendar-month deadlines. A default seven-day follow-up interval, 10:00 preference and quiet hours are proposed operating defaults, not industry facts.

Still unknown: this shop's message volume/budget, a provider's accepted sender/API contract, exact carrier costs and whether a specific unattended-SMS distribution route is suitable. No conclusion about those has been invented. The precise coordinator/RPC/storage migrations remain piece B's implementation preflight, not verified current capabilities.

## Strongest alternatives and falsification

**A local-only owner checklist is simpler.** If one phone always handled follow-ups and duplicate contact were acceptable, this would be preferable to new shared storage. Here two phones already participate in the app, and the accepted plan explicitly requires retry/restore/two-phone behavior. A local toggle or unique WorkManager name cannot meet that requirement. Keep calculations local and add only shared coordination metadata; do not build a second ledger or server credit engine. Reconsider this choice if the owner selects a permanently single-phone/manual workflow instead.

**A dedicated SIM phone may be cheaper than a provider.** Private APK installation means Play policy alone does not disqualify it. It remains a real option. However, it relies on one phone/SIM/network, explicit subscription selection and handling a crash after the carrier may have accepted a message. The plan therefore prefers a central provider architecture conditionally, without claiming it is cheapest, available or authorized. A suitable local route/quote or a deliberate dedicated-phone operating model could change this recommendation.

**A server does not guarantee correct or exactly-once sending.** An offline phone can hold an unuploaded collection, and an external send can succeed while its response is lost. The plan explicitly limits freshness to recorded/synchronized facts and keeps unknown handoffs out of automatic retries. It must not claim zero duplicate real-world contact, perfect knowledge of payments, delivered messages from a composer, or guaranteed display of a notification.

**Do not overbuild a collections CRM.** The proposed feature is one list, one policy pane, a customer preference disclosure and existing statement/message review. No invoice engine, arbitrary dunning ladder, marketing templates, provider abstraction marketplace, payment links, automatic penalties or delivery block is needed. Customer-level pause/snooze and weekly/monthly/custom statement timing directly satisfy this business's stated flexibility; they are not an excuse to add unrelated controls.

## Changes retained after judgment

- Kept automatic owner attention distinct from unattended customer SMS; the sending commitment is explicit, not silently removed or relabeled as completed manual composition.
- Kept Sunday/monthly/custom statement cadence independent of sale deadlines. A monthly term alone cannot justify a month-end billing assumption.
- Required shared claims for coordinated app handoffs, not a phone-local preference presented as a cross-device guarantee. A handoff-started record cannot expire into a second automatic attempt just because a preparation lease elapsed.
- Preserved an offline browsing/draft path and ordinary offline finance; explained the network requirement for coordinated external handoff before enable.
- Made missed-run/quiet/denied-permission behavior explicit, with persistent in-app attention and at most one digest attempt per day. A crash may lose that day's alert rather than cause repeated buzzing.
- Made restore/revision/capability, role checks, time trust and financial-data freshness part of the existing coordination piece. These are requirements of the selected behavior, not extra release phases.
- Kept provider price/route/consent decisions open where evidence is absent. No paid service or external sending is activated by this plan.

## Verdict and next step

Proceed with **piece A only** after the owner reviews this plan: deterministic eligibility/cycle rules and the planned native surfaces, behind the unfinished feature boundary. Piece B needs the exact small metadata contract, role/failure cases and affected tests pinned before edits; deploy DEV first and obtain exact production approval later. Piece C is a decision gate, not a disguised claim of automated SMS implementation.

No reason was found to reopen phases 2–5 or run a whole-app audit. The existing footer-label observation stays in its existing LATER note. New findings do not create a fourth active piece. Planning and judgment are complete; implementation and the final sending-route decision are not.

## Post-plan SIM/tablet correction

The provider architecture already covered an operator without a SIM, but the initial manual fallback was incomplete. Rechecked the actual ReminderIntents implementation: SMS simply opens ACTION_SENDTO; WhatsApp failure currently suggests SMS without establishing that SMS is usable. This is an observed limitation in fallback wording/capability handling, not proof that all tablets fail. The phase 6 plan now distinguishes no telephony, no handler, an installed but unconfigured app, linked messaging, and provider dispatch.

Ran the fixed judgment hook again after this supplement (tablet-judgment-hook.json). Independent conclusion: do not reject all SMS composers on a no-SIM device because a linked app may work; equally, do not imply that opening one proves sending. Keep internet WhatsApp, existing statement sharing and continuing the shared review on the shop phone as explicit options. No new automatic phone relay or tablet layout project is warranted. Provider sending remains conditional and unimplemented. This is a correction inside existing A/B acceptance, not another phase.

**Operating fact correction:** the owner confirms the shop SIM is in a separate dumb phone and cannot be placed in the tablet. Therefore the prior in-app shop-phone continuation is conditional on an additional app-capable phone; it is not available on the confirmed shop phone. A dedicated Android SIM sender would require a hardware/SIM operating change. Central-provider sending remains a conditional architecture, with existing-number preservation unverified. Simply owning a SIM proves neither a tablet SMS link nor a gateway.
