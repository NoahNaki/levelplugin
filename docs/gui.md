# GUI Widget Refactor Guide

This document explains the shared widget framework and how Storage, Auction House, and Salvage were refactored to use it. Use this as a template for refactoring additional GUIs.

## 1) Shared widget framework

**Core types** (under `me.nakilex.levelplugin.utils.gui.widgets`):
- **GuiContext**: provides `(Player, Inventory)` context for rendering and clicks. Use the top inventory when routing clicks so widgets operate on the GUI, not the player inventory. (`GuiContext`) 
- **GuiLayout**: a thin wrapper for setting items in the inventory. (`GuiLayout`)
- **GuiWidget**: interface for contributing items to a layout and handling clicks. (`GuiWidget`)
- **SlotWidget**: base class for slot-based widgets with a single render + click handler. (`SlotWidget`)
- **ActionWidget**: a reusable shared widget that takes a renderer and click handler, eliminating one-off inner classes. (`ActionWidget`)
- **NexoButtonWidget**: a slot widget that renders a Nexo icon button with optional lore builder + click handler for common control buttons. (`NexoButtonWidget`)

These are intentionally small and composable, so GUIs can define widgets without duplicating inventory setup logic.

### Widget reuse guide
- **ActionWidget**: use when the item rendering is dynamic (depends on player state, paging, filters) or needs a custom ItemStack builder.
- **NexoButtonWidget**: use for standard GUI controls with Nexo icons (info, confirm, cancel, deposit/withdraw, navigation) so the lore and hidden attributes are consistent.
- **SlotWidget**: use when you need a stateful widget class with shared helper methods or multiple slots handled by one widget.

## 2) Refactor pattern used

1. **Define widget list** in the GUI class and create a `buildWidgets()` method.
2. **Render widgets** in the `open(...)` method after you build or select the inventory to display.
3. **Route clicks** through `handleWidgetClick(...)` early in the GUI click handler and return if consumed.
4. **Protect widget slots** using widget `handlesSlot(...)` in drag and click handling where needed.

This pattern makes menus consistent and ensures future widget extraction is painless.

## 3) Storage GUI refactor

Storage uses shared `ActionWidget` instances for:
- prev/next page navigation
- sort and filter toggles
- info slot

It also centralizes protected slots by checking widget `handlesSlot` instead of repeating slot checks in multiple branches.

**Key methods to replicate:**
- `buildWidgets()` creates the widget list and wires click behavior.
- `renderWidgets(...)` writes widget items into the GUI.
- `handleWidgetClick(...)` intercepts clicks.
- `isProtectedSlot(...)` blocks moving widget items.

**Implementation reference:**
- `StorageGUI.buildWidgets`, `StorageGUI.renderWidgets`, `StorageGUI.handleWidgetClick`, `StorageGUI.isProtectedSlot`. 

## 4) Auction House refactor

Auction House uses shared widgets for:
- refresh, sell, my listings, search
- level filter, rarity filter, sort
- prev/next page
- info slot

The widget handlers call small methods (`handleSellClick`, `handleSearchClick`, etc.) to keep logic readable.

**Implementation reference:**
- `AuctionHouseGUI.buildWidgets`, `AuctionHouseGUI.renderWidgets`, `AuctionHouseGUI.handleWidgetClick`, and the helper handlers.

## 5) Salvage GUI refactor

Salvage uses widgets for:
- info, toggle, confirm, cancel
- deposit/return all
- rarity deposit buttons

It also centralizes the input-slot rules in `SalvageGUI.isInputSlot`, which the listener uses for validation.

**Implementation reference:**
- `SalvageGUI.buildWidgets`, `SalvageGUI.renderWidgets`, `SalvageGUI.isInputSlot`, `SalvageListener` input checks.

## 6) Refactor examples added

- **Arena Queue**: uses `ActionWidget` for the queue buttons and shared rendering/click routing.
- **Catacombs Entry**: uses `NexoButtonWidget` for the entry button with a lore builder.
- **Dungeon List**: uses `ActionWidget` for filter toggles and shared click routing.
- **Enchant**: uses `NexoButtonWidget` for info and an `ActionWidget` for the dynamic enchant action button.
- **Codex Main**: uses `ActionWidget` for head-based category buttons and a `NexoButtonWidget` back control.
- **Guild Settings**: uses `ActionWidget` for permission toggles and back navigation with shared click routing.
- **Server Selector**: uses `ActionWidget` for server choices with consistent click handling.
- **Codex Lists**: use `ActionWidget` for pagination and back controls with shared click routing.

## 7) UX styling guidance

Use existing utilities for consistent look-and-feel:
- `GuiUtil` for Nexo items, filler panes, stat formatting, and borders.
- `TooltipUtil` for standard click instructions and bullet lists.
- `ChatMessageUtil` for messaging consistent with the rest of the plugin.

Avoid hardcoding formatting when these utilities already exist.

## 8) Suggested checklist for refactoring another GUI

1. Identify menu slots that are fixed controls (pagination, filters, info, confirm/back, etc.).
2. Create widgets for those slots in `buildWidgets()` (prefer `ActionWidget` for simple slots).
3. Replace direct `inv.setItem(slot, ...)` for controls with `renderWidgets(...)`.
4. Route click handling through `handleWidgetClick(...)` and keep specialized logic in helper methods.
5. Protect widget slots from drag/move using `handlesSlot` checks.

## 9) Example snippets (pseudo)

```java
private List<GuiWidget> buildWidgets() {
  List<GuiWidget> widgets = new ArrayList<>();
  widgets.add(new ActionWidget(PREV_SLOT, ctx -> prevItem(),
      (click, ctx) -> goPrev(ctx.player())));
  widgets.add(new ActionWidget(NEXT_SLOT, ctx -> nextItem(),
      (click, ctx) -> goNext(ctx.player())));
  widgets.add(new ActionWidget(INFO_SLOT, ctx -> infoItem(), null));
  return widgets;
}
```

---

## File references
- `utils/gui/widgets/GuiContext.java`
- `utils/gui/widgets/GuiLayout.java`
- `utils/gui/widgets/GuiWidget.java`
- `utils/gui/widgets/SlotWidget.java`
- `utils/gui/widgets/ActionWidget.java`
- `utils/gui/widgets/NexoButtonWidget.java`
- `storage/gui/StorageGUI.java`
- `auctionhouse/AuctionHouseGUI.java`
- `salvage/gui/SalvageGUI.java`
- `salvage/listeners/SalvageListener.java`
- `arena/gui/ArenaQueueGUI.java`
- `catacombs/CatacombsGUI.java`
