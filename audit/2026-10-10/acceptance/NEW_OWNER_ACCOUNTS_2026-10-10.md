# New acceptance owners — 2026-10-10

This supersedes the earlier acceptance setup after the owner requested fresh accounts because the old local throwaway passphrase was forgotten and the existing server account did not work in DEV.

## Ready now

- **A06:** open **Saaf Milk (preview)**. The active acceptance package is now **com.saaf.milkshop.qa**, version 0.4.0-qa, built from the same repaired production source with isolated QA identity. A fresh owner named **Acceptance Owner**, login **accept.owner**, was created through the actual first-run UI. The app is left authenticated on Owner Overview. Debug sample data and role fixtures are intentional local preview data.
- The previous **com.saaf.milkshop.preview** workspace, including the forgotten owner and its history, remains installed but disabled for user 0. The instrumentation benchmark is also disabled. No old account password was reset and no old workspace was cleared. This leaves one visible acceptance preview icon.
- **Server:** **accept.owner** is a new active owner in **saafmilk-dev**, with a separate business **Saaf Milk Acceptance 2026-10-10** and **Acceptance Shop**, timezone Asia/Karachi. It has one active owner, a confirmed Auth identity, a shop assignment and a non-secret creation audit entry. No existing server account was changed.
- **Viewer:** http://127.0.0.1:3100/login is running locally against DEV. Real login with the new server credential, authenticated health (ping plus my_profile), and overview pass. The default browser was launched at the login page; its browser session is not claimed signed in. Enter the new DEV credentials there.

The credentials and local recovery code are saved for the owner at [account details](<C:/Users/Teymur/AppData/Local/Temp/saaf-accept-owner-20261010.json>). They are not stored in repository reports or committed source. Local passphrase and server credential are separate; server credentials follow the existing 15-digit contract.

## Evidence and limits

DEV Auth identity creation used the project's existing configured Auth Admin integration because the connected MCP exposes no Auth creation operation. Public business/shop/staff/assignment/audit provisioning and verification used Supabase MCP. Auth tables were not manually fabricated. Only the newly requested DEV identity/business/shop were created; PROD remains unchanged.

The first Viewer login succeeded but protected HTTP requests lost its secure cookies. The local production-build launch now uses the existing documented COOKIE_SECURE=false option on localhost only, with explicit DEV configuration. Authenticated checks then pass; no Viewer source or production hosting setting changed.

In-app browser launch failed twice because its kernel assets path was unavailable. No plugin repair was attempted; the default browser was launched instead. Android form automation initially left the form when Back was sent without checking keyboard state; no owner was created by that attempt. A subsequent helper stopped on Windows decoding of device output before submitting any changes. UTF-8 handling and live form/keyboard checks completed owner bootstrap successfully. These are setup/infrastructure failures, not product findings; their final success does not erase the attempts.

The recovery code was copied to the owner's temporary credential file before confirming its save. Public XML evidence is redacted, and temporary shared-device recovery captures were removed.

No new broad audit, benchmark, existing-account recovery, staff-disable/sign-out workflow suite, phone server enrollment, history transfer or production deployment was included. Those workflow checks remain separate from the verified login/health/overview and local owner-bootstrap checks. Earlier audit findings and dated evidence are preserved.
