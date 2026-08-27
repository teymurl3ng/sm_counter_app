# AI Agent Rules & Project Boundaries

## 1. Untouchable Core & Business Logic
- **DO NOT MODIFY** any core logic, business logic, domain models, algorithms, service handlers, or ViewModel implementations.
- All core architectures, business rules, calculation engines, and data pipeline contracts are **STRICTLY READ-ONLY**.
- If a ViewModel or core service needs to expose data or commands to the UI, consume the existing interfaces without changing internal logic.

## 2. Strict UI-Only Focus
- All work in this repository is strictly focused on the **Presentation Layer (UI/UX)**:
  - Visual layout, responsive design, styling, and design system tokens (Tailwind CSS).
  - UI components, presentation state (e.g., local dialog open/close, animation states).
  - Binding views to existing ViewModels / state boundaries.
  - Interaction feedback, micro-interactions, animations, and accessibility.

## 3. Architectural Boundary Enforcement
- Keep business logic completely decoupled from view components.
- Views must only:
  - Render state provided by the ViewModel / state containers.
  - Dispatch user intent / action triggers to the designated ViewModel methods.
- Never embed business rules, calculation side effects, or domain logic directly inside React components or templates.
