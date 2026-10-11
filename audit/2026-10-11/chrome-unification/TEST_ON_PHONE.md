# Check the stable app bars

Use the updated isolated QA app on the A06. Preview accounts use 1234567890qwert; the existing Acceptance Owner keeps its own passphrase. No account/data reset is needed.

1. Cashier: switch between Sell, Collect, Activity and More, then open Expense, Count cash, Count & close and deliveries. The green band and navigation bar must stay the same height and the body must start/end at the same boundaries when the keyboard is closed.
2. Owner, Admin, Manager and Operations: repeat Work/More/Records and their connected submenus. Shop name, account name, role/date and day status should stay in their reserved positions. Longer page titles/names may ellipsize inside the bar; they must not push the entire page down.
3. Sign out, then go through Welcome, Choose account, Sign in and any already-configured Quick PIN/sign-in method. Access instructions scroll in the body. Back sits in the same leading slot; recovery/setup/error steps use the same header geometry. Existing account names remain in the identity cards.
4. Repeat one role menu and one entry in dark mode and with 150%/200% text. Enlarging system text may enlarge all bars through one shared rule; it must not give different screens different sizes or clip a metadata line. Body text keeps your accessibility setting.
5. Open the keyboard in Sell/Collect/Expense/Count cash. The viewport necessarily shrinks and work navigation hides while typing; fields and required actions must remain reachable. Closing the keyboard restores the same bars.
6. Open/close Receive milk and deliveries repeatedly; pull refresh and open an Activity record. Refresh must not insert a strip that pushes content. Review the retained route recording for any perceived movement/flashing.

The source map covers every native screen family, and automated tests check shared geometry and the functional matrix. It is representative visual coverage, not an exhaustive manual review of every screen state. Report findings as role / screen / action / text setting / expected / actual. Owner visual approval remains pending.
