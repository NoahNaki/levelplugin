# Custom HUD v1 (Skeleton)

This milestone ships a minimal custom HUD runtime that renders actionbar glyphs and tracks mouse input.

## Quick start

1. Ensure `hud.yml` is in the plugin data folder. The plugin will copy the default file on first run.
2. Use `/hud reload` after editing `hud.yml`.
3. Toggle the HUD per player with `/hud toggle`.
4. Use `/hud debug` to print the resolved elements and their positions.

## Demo layout

The default `hud.yml` contains:

* A `hello` text element.
* A `mouse_combo` element that renders `%hud_mouse_combo%` with the left/right click glyphs.

## Placeholder notes

* `%hud_mouse_combo%` uses the new HUD input tracker and displays the rolling L/R queue.
* All other placeholders still resolve through PlaceholderAPI.
* The HUD does **not** rely on nested placeholder recursion. If you need derived values, add them directly to the HUD placeholder registry.

## Positioning model (v1)

* `x` is a pixel offset rendered with spaces in the actionbar.
* `y` is reserved for future multi-line/channel rendering (currently only `y: 0` is rendered).

## Migration note (BetterHud ➜ Custom HUD)

| BetterHud field | Custom HUD field |
| --- | --- |
| `x`, `y` | `x`, `y` |
| `layer` | `layer` |
| `text` | `text` |
| `conditions` | `conditions` (supports `underwater`, `dead`, `not_dead`) |

For image elements, map your glyph to a PUA codepoint in the resource pack and emit it via `text`.
