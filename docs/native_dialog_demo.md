# Native Minecraft Dialog Demo

LevelPlugin already has its own NPC dialogue manager and a LuxDialogues bridge. This demo does **not** replace either system. It is a small isolated playground for Minecraft's native dialog feature added in 1.21.6 and Paper's Java Dialog API added in 1.21.7.

## Requirements

- Paper 1.21.8
- Java 21
- LevelPlugin compiled against `paper-api:1.21.8-R0.1-SNAPSHOT`

## Commands

```text
/dialogdemo notice
/dialogdemo confirm
/dialogdemo input
/dialogdemo glyphs
/dialogdemo ui
/dialogdemo rewards
/dialogdemo hex
/dialogdemo equipment [player]
```

Aliases:

```text
/nativedialog
/ndialog
```

### `notice`

Shows a basic native modal with title, body text, tooltip and close button.

### `confirm`

Shows a native two-button quest confirmation. Both buttons execute a Java callback in LevelPlugin and print the result to the player.

### `input`

Shows native input controls:

- text field
- number slider
- boolean checkbox

The submitted values are read from Paper's dialog response and printed back to the player.

### `glyphs`

Shows one black-box test glyph at 8, 12, 16, 24, 32 and 48 logical pixels. Every sample uses the same source texture so differences come only from the Nexo glyph `height` and `ascent` settings.

### `ui`

Uses selected assets from Franuka's RPG UI pack to demonstrate both interaction paths available to glyphs:

- a glyph inside a plain-message body can show a hover tooltip and run a click callback;
- a glyph can be the label of a native action button and run a dialog callback when clicked.

The demo separates three cases:

- a static body glyph runs a callback without refreshing the screen, so the cursor stays in place;
- a clean custom button image in the dialog body cycles the supplied normal, selected and pressed textures by replacing the dialog;
- a glyph-labelled native action button keeps Minecraft's gray button frame and built-in hover/focus highlight.

Native dialogs do not expose mouse-enter or mouse-leave callbacks, so the plugin cannot swap the glyph only while the pointer is hovering it. Replacing the dialog is required to change a displayed glyph.

This demo repositions the cursor when it does so, but that turned out to be avoidable rather than inherent: the movement comes from the default `CLOSE` after-action tearing the screen down between dialogs, not from the replacement itself. Setting `afterAction(DialogAfterAction.NONE)` keeps the pointer in place — see the settings section below.

The included demo textures are from **RPG UI asset pack (by Franuka) v1.7** and are used under the license included with that pack. Creator: [Franuka](https://franuka.itch.io/).

### `rewards`

Shows a Daily Rewards layout prototype with seven weekday columns and separate free/VIP-style reward rows. It uses the existing 48px black-square glyph for every reward box, highlights Wednesday in text, and places reward labels below both rows. Each placeholder has a hover tooltip and a non-refreshing click callback so the cursor remains in place.

### `hex`

Aliases: `states`, `element5`. (Command name kept from the original hexagon build; the art underneath is now `element_1`, a square button — see below.)

Shows **element 1** from the *Gray minecraft like minimalistic* UI pack in all five of its states — the pack's second export, which replaced the earlier `element_5` hexagon used here previously:

| State | Texture | Meaning here |
|---|---|---|
| idle | `ui_sq_idle` | plain square — reachable, not chosen |
| selected | `ui_sq_selected` | cyan halo — the current choice |
| locked | `ui_sq_locked` | padlock overlay — too far along the path |
| disabled | `ui_sq_disabled` | flat light-gray fill — turned off, cannot be selected |
| activated | `ui_sq_activated` | green fill — already unlocked |

#### What can be retuned at runtime, and what cannot

**Server-side numbers: yes.** The node's body width and its slice set are read when the dialog is built, so changing them takes effect on the next render — no restart, no pack rebuild:

```text
/dialogdemo hex width <1-420>     ring and click target width
/dialogdemo hex slices <s|t|o>    9, 3 or 1 slice
```

Both values are shown along the bottom of the screen while tuning.

**The glyph's own pixel height and ascent: no.** Those live in the resource pack the client has already downloaded, so changing them means regenerating the pack and having the client fetch it again. The way around it is to **pre-bake every size you might want and select between them at runtime** — which is precisely what the three slice sets and the `_small` / `_btn` variants are.

That distinction is the useful one for real GUIs: anything expressed as a dialog field (widths, colours, which glyph id, tooltip style) is live; anything baked into the font is not.

The screen has two parts: a static legend showing all five states at third scale beside their names, and one full-size node that previews whichever state the five buttons select. The buttons carry the icon as their own label, so each state is visible on its control as well as in the preview.

The glyph set therefore exists at three sizes off the same five textures: `ui_sq_*` (full-size preview), `ui_sq_*_small` (27px legend) and `ui_sq_*_btn` (18px button labels).

#### Reserving vertical room, or the glyphs get cut off

Bitmap glyphs rise from their text baseline, and a dialog body clips a message to the height of the lines it actually contains. A tall node on a single-line message is therefore **clipped to that line and drawn over whatever sits above it** — the first build of this demo sliced the tops and bottoms off every icon and painted them across the description text.

Each glyph row reserves room with leading blank lines, about `ascent / 9px` of them, the same technique the rewards mockup uses. The full-size node reserves 7 lines; the third-scale legend reserves 4.

#### Making the picture itself clickable

A body click callback **does** fire — Chess (`codes.castled.chess.ui.PaperBoardDialog`) uses exactly that for its Resign/Draw/Flip line. But a dialog hit-tests a click to **one text line**, 9px tall at the baseline, and the glyph is drawn far above the line it sits on. Its art and its hit area barely overlap, so clicking the picture does nothing except focus the body element — which is what draws the white outline around it, and reads as clicking straight through to the container.

The fix is to stop treating the picture as one glyph. The full-size node is **nine 9px slices**, one per line, each carrying the same click callback:

- `81 / 9 = 9` slices exactly, and each renders at `height: 9`, so the art reassembles at native 1:1 with no seams;
- every row of the icon is its own click target, so the whole face is clickable;
- the slices occupy nine real lines, which removes the need for reserved blank lines around this glyph entirely.

**Slice height decides how big the focus ring is.** The ring is as tall as the lines the art occupies, and slices do not have to be one line tall. Two sets are generated from the same art:

| Set | Slices | Ring height | Clickable |
|---|---|---|---|
| `_s0.._s8` | nine 9px | as tall as the art | the whole icon |
| `_t0.._t2` | three 27px | a third of the art | the middle band only |

The `_t` set tiles with **shrinking ascents — 27, 9, −9**. Line baselines advance 9px but each slice must be drawn 27px lower than the last, so ascent absorbs the 18px difference and goes negative on the third. Minecraft accepts a negative ascent; the pack already ships one (`dialog_reward_highlight` uses `-4`). Switching sets is two constants (`NODE_SLICES`, `NODE_SLICE_INFIX`) — no regeneration.

**Art that overhangs its body will be laid out into.** With the `_t` set the icon is 81px tall but occupies only 27px of layout, so it overhangs its own body box by 54px — 20px above (slice 0 has ascent 27 while a text line rises only 7px above its baseline) and 34px below. The bodies either side lay out as if the node were 27px tall and their text lands on top of the art.

The clearance goes in the **neighbouring** bodies (`NODE_OVERHANG_ABOVE_LINES` / `NODE_OVERHANG_BELOW_LINES`), never in the node's own — padding the node would grow its ring back, which is the entire point of slicing this way.

**A body message overflows its width; it is not clipped.** Confirmed with the node body at 40px against a wider art canvas: the icon renders whole and overhangs the ring on both sides. So the ring's width is a free knob, independent of what the art measures.

**Every slice must report the same width, or the stack shears.** Minecraft derives a bitmap glyph's advance from its **rightmost non-transparent pixel**, and these slices differ enormously in content: the icon's tip row holds far fewer opaque pixels than its middle rows. A dialog body centres each line on its own width, so narrow slices centre differently from wide ones and the icon comes out visibly stepped. Each slice therefore carries an `alpha = 1` pixel in its leftmost and rightmost columns — invisible to the eye, non-transparent to the width calculation — which pins all nine to the full canvas width.

`scratchpad/SliceE1.java` (the hexagon build's `Slice.java`, adapted for five states) cuts the five normalised states into `assets/minecraft/textures/ui_gray/slices/`, stamps those edge pixels, and writes `nexo/glyphs/ui_gray_slices_sq.yml` (75 entries: 5 states x (9+3+1) slices, plus a _small and a _btn variant each). That file is generated — regenerate it rather than editing it by hand.

This is the general technique for any clickable glyph art in a dialog, not just this node.

#### The focus ring

A body element is a focusable widget and the client draws a white ring around the focused one. Nothing in the Dialog API controls that: `DialogBase.Builder` exposes only `externalTitle`, `canCloseWithEscape`, `pause`, `afterAction`, `body` and `inputs`, and `DialogBody.plainMessage` takes only a component and a width. There is no border, focus or highlight field at any level, so it cannot be turned off from the server.

It is not tied to clickability either — a plain, non-clickable message body shows the same ring once it holds focus, so removing the click callback does not remove the ring.

The alternative is a button, which never rings but always draws its own frame. Neither surface gives a bare clickable image; the choice is which chrome you prefer. This demo keeps the clickable body glyph and its ring.

What *is* controllable is its size, because the ring follows the **body element's** width. The node has its own body at `NODE_BODY_WIDTH` with its status line in a separate full-width body, so the ring hugs the icon instead of boxing the dialog. Any clickable body art wants the same treatment.

#### Normalising five states — and a false lead along the way

Every pixel in every source file for this pack is either fully opaque or fully transparent — `anyAlpha` bbox and `solid` bbox (alpha > 200) are identical for all five states, confirmed with `scratchpad/Probe.java`. There is no soft or anti-aliased edge anywhere to disambiguate.

The first version of this normalisation carried over the hexagon build's technique regardless: search for the corner bracket's darkest pixel (`scratchpad/DarkestPixel.java`) and anchor on that, because the hexagon pack's `selected` state genuinely needed luminance-based isolation to tell its glow apart from its body. Applied here, where there's no soft edge for that search to disambiguate in the first place, it just picked a slightly different pixel per state — anti-aliasing noise in the *search itself*, since the states are independently generated art rather than one shape recoloured — and **introduced** a few pixels of drift between icons that should have landed identically. That surfaced as the reported "moving by a few pixels instead of just changing state."

The fix is simpler than what it replaced: **centre each icon's raw alpha bounding box** on the shared canvas, no anchor search at all (`scratchpad/NormalizeE1b.java`). A bounding-box centre is a fixed point derivable from the box alone, so it's identical for identically-sized boxes by construction — no measurement, no noise:

```
idle       content=63x63  centred at (9,9)
locked     content=63x63  centred at (9,9)
disabled   content=63x63  centred at (9,9)
activated  content=65x65  centred at (8,8)
selected   content=73x75  centred at (4,3)
```

`idle`, `locked` and `disabled` land on the exact same offset because they share the exact same content size — zero rounding error, not "close enough." `activated` (2px larger) and `selected` (its own halo included) land at different offsets, but every one of the five sits on the identical geometric centre, (40.5, 40.5) on the 81×81 canvas: `9 + 63/2 = 40.5`, `8 + 65/2 = 40.5`, `4 + 73/2 = 40.5`. The two larger icons grow symmetrically outward from that shared centre instead of drifting toward one corner, which reads as "this state is slightly bigger," not as movement.

The general lesson, sharpened by getting it wrong once: reach for anchor-on-a-structural-feature (corner joint, stroke intersection) only when a state's silhouette genuinely can't be trusted — a soft or asymmetric glow that a plain bounding box would misread. When every edge is crisp, as it is here, centring the bounding box is exact and a manual anchor search is *strictly worse* — it adds a source of noise the simpler method doesn't have.

#### When a button is the better answer

Slicing suits one large piece of art. For a *grid* of many small cells, real `ActionButton`s are simpler: that is how Chess renders 64 individually clickable board squares — an 8-column `multiAction` grid of 20px buttons, each labelled with a 16px glyph. The five state buttons here use the same idea, each labelled with its own icon.

**A button label is not clipped to the button.** This was measured with a throwaway `btnsize` probe — the same icon as a label at 16, 20, 24, 28, 32, 40 and 54px. Every size drew *whole*; the large ones simply overflowed their frame and spilled across the neighbouring rows. So the practical limit is not clipping but collision: a label much taller than the ~20px frame will overlap whatever sits above it, which is why Chess keeps to 16px in a dense 8x8 grid, and why the `_btn` variants here are 18px.

What a button can never do is *be* the art. The client always draws the button's own frame and hover highlight behind the label, so a glyph in a button reads as an icon **on** a button, not as a bare clickable image.

One more constraint from Chess: **a `multiAction` grid gives every column the width of its widest button**, so mixing a 150px control into a grid of 20px cells blows the layout apart. Chess keeps its side controls out of the grid as body text for exactly this reason.

Chess also uses `ClickCallback.UNLIMITED_USES` rather than a one-use callback: with `after_action NONE` the dialog stays open and is re-shown after each click, so a one-use button dies if it is clicked twice quickly. The buttons here do the same.

#### Custom-framed tooltips

The node's hover uses a resource-pack tooltip frame rather than the vanilla purple box, matched to each state: `artifact` (red) for locked, `common` (grey) for idle and disabled, `rare` (blue) for selected, `uncommon` (green) for activated. Those are the same styles the plugin already applies to item rarities via `ItemUtil.applyRarityTooltipStyle`, with sprites already in the pack under `textures/gui/sprites/tooltip/<id>_background.png` and `_frame.png`.

The frame comes from the **`tooltip_style` item component**, which means it is only available to *item* hovers. A plain `showText` hover renders in vanilla styling and always will. So the text is wrapped in a throwaway item — any material, since a hover tooltip never draws the icon — carrying the display name, the lore and the style component, and that item is used as the hover:

```java
ItemUtil.setKeyedComponent(carrier, DataComponentTypes.TOOLTIP_STYLE, Key.key("minecraft:rare"));
return carrier.asHoverEvent();
```

Set `TextDecoration.ITALIC` to false on the name and every lore line, or the client applies its usual italic styling to custom item names.

**This does not work for buttons.** `ActionButton.tooltip(Component)` takes a component, not a hover event, so there is nowhere to attach an item — button tooltips are stuck with vanilla styling. Styled tooltips are a body-hover feature only.

#### Gaps use spaces, not Shift

Nexo's `Shift` characters live in the generated default font, and this server currently logs `Failed to deserialize resource at: assets/minecraft/font/include/default.json` on every pack build — so every shift renders as a stray missing-glyph mark. The node spacing here uses plain spaces (4px each) instead. Worth knowing before reaching for `pixelShift` in a new screen; the `rewards` demo leans on it heavily and will show the same marks until that font include is fixed.

#### Normalising the state textures

Same problem as every asset swap in this file: the pack ships each state cropped to its own content, so the raw files are all different sizes, and using them as glyphs directly would make a node visibly grow and jump as its state changed (a Nexo glyph's advance follows its texture's aspect ratio). See "Normalising five states that don't share a palette" above for how the current `element_1` asset is composited onto one shared canvas; the technique differed from the original hexagon build because this pack's states don't share a fill colour.

The glyph entries deliberately carry **no `char:`** in either build — Nexo assigns a free codepoint itself, and the demo resolves them by id through `NexoPlugin.instance().fontManager().glyphFromID(...)`. That sidesteps hand-picking codepoints and the collision risk that comes with it.

#### The state swap is free now

Native dialogs still have no hover callback, so a node's texture can only change by replacing the dialog. The `ui` demo above warns that this repositions the cursor — it no longer does. This screen sets `after_action NONE` with `pause(false)`, so the client never tears the screen down between states and the pointer stays exactly where the player clicked. That is what makes a click-through node tree feel acceptable rather than jarring.

### `equipment`

Aliases: `gear`, `inspect`. Usage: `/dialogdemo equipment [player]` — defaults to yourself.

Renders another player's **live** helmet, chestplate, leggings, boots, main hand and off hand as real item icons — not glyphs, not synthetic carrier items, the actual `ItemStack`s read straight off `PlayerInventory` when the dialog is built. This is the technique from the KaMenu project review: an `item` dialog body can show any live `ItemStack`, so a "what is this player wearing" screen needs no icon assets of its own.

**No 3D player model exists in the Dialog API.** There are exactly two body kinds — `item` and `plainMessage` — confirmed by listing the actual classes Paper ships (`ItemDialogBody`, `PlainMessageDialogBody`, nothing else). The closest real substitute is a big 2D player-head icon, shown at `PORTRAIT_SIZE` (64px) above the equipment list. That ceiling is a platform fact, not a corner we cut.

**No horizontal alignment control exists either.** `DialogBase.Builder` has no column, anchor, or alignment field of any kind — every body element self-centers, always. The first version of this screen used `ItemDialogBody.Builder#description(...)`, which glues the icon and a text label into one block that centers **as a unit**. A short label ("Boots") and a long one ("Main Hand  (empty)") produce different-width blocks, so the icon itself visibly drifted left or right depending on how long its own label happened to be — that was the actual cause of the staggered row, not the platform centering everything.

The fix is structural, not a workaround: **the six equipment icons carry no attached text at all.** Six bodies of the identical width all center to the identical x, which is the only way to get a straight column given that nothing here can be explicitly left-anchored. The slot name still needs to live somewhere, so it moved into the item's own tooltip — a `"Slot: Helmet"` lore line added to a **clone**, never to the real live `ItemStack`. Hovering answers "what is this" without the label ever touching the body's rendered width.

**Empty slots get a fallback, not a gap**, and now carry the same slot-context lore as a real item would: an empty helmet slot shows the target's own skin head via `HeadUtil.createPlayerHead` (safe to name directly, since it's a placeholder this code constructs, not the player's real item) with `"Slot: Helmet"` in its lore; every other empty slot shows a gray pane with the same. The off hand is usually empty, so it's a reliable way to see the fallback without needing a specific loadout.

**The read is live, including on Refresh.** There is no caching anywhere in this path — `slot.read(target)` calls straight through to `getHelmet()` / `getItemInMainHand()` / etc. every time the dialog is built. The Refresh button re-resolves the target **by name**, not by reusing the captured `Player` object, and checks `isOnline()` before rendering: if the target logged off between opening the dialog and clicking Refresh, a stale `Player` reference would silently keep returning whatever it last held rather than reflecting that they're gone.

## Why this is separate from the current NPC system

The current NPC/quest dialogue code controls things such as quest state, walking-away cancellation, typewriter timing and existing LuxDialogues rendering. Native dialogs are a different client UI surface. Keeping this demo separate makes it easy to test the feature first and later decide where it should be used.

Good first production candidates are:

- quest accept/decline prompts
- merchant confirmations
- profile creation/options
- server/quick-action menus
- short forms that need text, boolean or slider input

Native dialogs are intentionally limited and should not be treated as a complete replacement for every custom inventory GUI or cinematic NPC conversation.

## Production usage so far

### Auction house

`AuctionDialogService` keeps the inventory GUI as the auction browser and moves the form-shaped interactions to native dialogs: listing, bidding, BIN purchase, search and listing cancellation. Each call takes a return `Runnable` so the browser reopens when the dialog resolves.

### Settings

`SettingsDialogService` is the main `/settings` front-end. Settings are a form rather than a browser, so unlike the auction house there is no inventory screen behind it:

- the root screen is a `multiAction` dialog with two columns: four category forms (Combat & Spells, Visual & Sound, Social & Privacy, Personal Environment), a Spell Keybinds dialog, one link to the Spells screen (still an inventory browser), and a "Classic Menu" button that opens the original chest menu;
- each category is a `confirmation` dialog whose inputs are the settings themselves;
- **Save Changes** applies the whole form at once and reports how many settings changed; **Back** discards it.

#### Why every control is a single-option button

Toggles are **not** `DialogInput.bool`. Paper's boolean builder exposes `initial`, `onTrue` and `onFalse` but **no `width`**, so the client sizes each checkbox row to its own label and centres it — a column of settings comes out ragged, with every checkbox at a different x position. `singleOption` does take a width, so every control is a fixed-width `singleOption`: toggles carry `On`/`Off` entries, and enum settings carry one entry per value. Uniform width means the controls line up, and the result reads like the vanilla Options screen, which uses exactly these cycle buttons.

#### Where glyphs and color go

An icon on every row of a long list reads as decoration, so the rule is: a glyph marks a **screen or a named concept**, never a plain on/off row.

- screen titles and root menu buttons carry their category icon (sword, sun, heart, trees, key, book, scroll);
- settings that name a concept of their own carry one — Loot Pickup Filter (diamond), Spell Input Mode (mana), Player Visibility (shield), Personal Weather (sun), Personal Time (moon);
- the on/off rows stay bare, with a white label.

Because `singleOption` widths are fixed independently of the label, a glyph costs no alignment — the button is the same size whatever the icon renders as. That is only true now that the controls are single-option; it was not true of the checkbox layout.

Color carries state rather than decorating it: `On` is green and `Off` is red, rarities use their own `ItemRarity` colors, Player Visibility runs green → yellow → gray as it gets more restrictive, weather and time tint to what they describe, and in the keybind editor an `Unbound` input is red where a bound one is green — the same read the inventory editor gave.

The Save Changes and Back buttons on a category form are plain white with no glyph. They are the dialog's own controls rather than settings, so coloring them competes with the values above for attention.

Glyph codepoints must be **checked against the server's `icons_pack.yml`**, not inferred. The icons in the file are mostly sequential, which makes guessing look safe — but `class_icon` sits at `ꐲ`, far outside the run that holds its neighbours in the config.

The inventory menu re-rendered the entire chest after every single click. The dialog form edits many settings before anything is committed, which is the main reason this screen was worth converting.

#### Keeping the mouse where the player left it

Navigating between dialogs used to throw the cursor back to the centre of the screen on every hop. The cause is not that the dialog is being replaced — it is the default `CLOSE` **after-action**, which tears the screen down before the next dialog arrives. The client only re-centres the pointer when it opens a screen from *no* screen, so passing through that empty state is what moves it.

Every settings dialog therefore sets `afterAction(DialogAfterAction.NONE)`. The screen stays up, the next dialog replaces it in place, and the pointer does not move — which matters most in the keybind editor, where Switch Class and Switch Input Mode re-show the same screen repeatedly.

`pause(false)` has to go with it. `pause` defaults to true, and Paper refuses to build a pausing dialog whose after-action never unpauses the game — *"Dialogs that pause the game must use after_action values that unpause it after user action!"* — which throws at `DialogBase.Builder#build`, so the command fails rather than the screen misbehaving. Pausing only affects singleplayer, so turning it off costs nothing here.

The other trade-off is that nothing closes on its own any more, so buttons leaving the dialog system must close it themselves:

- the root screen's exit button calls `closeDialog()` in its own callback;
- hand-offs to an inventory GUI (Classic Menu, Spells) close the dialog before opening the inventory.

Three details worth keeping in mind when converting further menus:

- `singleOption` reports the **selected option id through `getText`**, not through a dedicated getter, so option ids are the enum names here.
- Dialog callbacks do not arrive on the main thread. Every apply path schedules with `Bukkit.getScheduler().runTask` before touching Bukkit state, exactly as the auction service does.

Player Visibility is locked while the Office Errands quest is in progress. The dialog omits the input entirely in that case rather than showing a disabled control, and re-checks the lock on submit in case the quest started while the form was open.

### Spell keybinds

`SpellKeybindDialogService` replaces the keybind chest editor. Each of the four inputs is a fixed-width selector listing `Unbound` plus the four spells, labelled with the input it fires on (`RRL`, `Sneak+Right`, …) for the class and mode being edited.

It is a `multiAction` dialog rather than a confirmation because it needs four buttons: **Save**, **Switch Input Mode**, **Switch Class** (only when more than one class is unlocked) and **Cancel**. Native dialogs cannot re-render in place, so the two switch buttons read the submitted form, fold it into a pending per-player edit state, and re-show the dialog — edits made before a switch survive it, and nothing is written until Save, matching the old editor's save/cancel semantics. Validation issues (unbound inputs, a spell bound twice) are shown as a red body line rather than blocking the save, as before.

### Still an inventory GUI

**Spells** (`SpellUpgradeGUI`) stays a chest menu. It is a browser of spells with per-spell upgrade actions, which is the auction-house shape: the list belongs in an inventory, and only the actions taken from it would be worth converting.

The classic chest settings menu keeps opening the classic chest sub-screens, so the old path stays intact end to end.

#### Shared mutation layer

`SettingsActions` owns the side effects for every setting — manager sync, `/toggle` dispatch, profile save and chat feedback. Both the dialogs and the classic inventory menu apply values through it, so the two front-ends cannot drift apart. Each method is a no-op when the value already matches, which is what lets a submitted form pass along fields the player never touched.
