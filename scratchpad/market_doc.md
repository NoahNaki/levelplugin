## The market screen (`/marketdemo`)

A 5x3 market painted over a custom shop-window texture, plus the in-game
editor used to align it. Lives in `dialogdemo/market/` rather than in the demo
command, because the alignment machinery is the reusable part.

### Real items cannot be gridded — this is the wall, not a workaround

Dialogs have exactly two body kinds, `item` and `plainMessage`; every body is
its own row; every row self-centres. `DialogBase.Builder` has no column,
anchor or alignment field at any level. The `equipment` screen above hit this
and had to become a centred vertical column with the labels stripped.

So *"real `ItemStack`s sitting in those 15 slots"* is not reachable at any
spacing. It is not a matter of finding the right offsets — the offsets do not
exist. Anything that needs an actual grid over fixed background art has to be
glyphs in a `plainMessage`, positioned by shift characters.

### Horizontal: a private shift font

Nexo's `Shift` glyphs live in its generated `default.json`, which this server
fails to deserialize (see "Gaps use spaces, not Shift" above), so every shift
renders as a missing-glyph mark. The `hex` node worked around it with 4px
spaces, which cannot move left and cannot land between multiples of 4 — fine
for one node, useless for a 15-cell grid.

The market ships its own font instead: `assets/minecraft/font/space.json`, a
vanilla `space` provider with power-of-two advances from -1024 to +1024 at
`..` (negative) and `..` (positive). Separate file,
separate codepoints, so Nexo's broken include cannot affect it, and every
integer offset decomposes exactly with no rounding or accumulation. Selected
per-component with `.font(Key.key("minecraft:space"))`.

This is worth reusing anywhere the `rewards` demo currently shows stray marks.

### Vertical: baked ascent variants

Ascent lives in the pack the client already downloaded, so it is not tunable
from the server — the same wall the `hex` node documents. Text lines advance
in 9px steps, far too coarse to align to a slot.

The way round it is the lesson that file already lands on: **pre-bake every
value you might want and select at runtime.** `market.yml` carries nine ascent
variants per icon size (`market_icon_16_a0` .. `_a8`), so a row's y splits into
whole lines plus a 0-8px remainder and reaches any pixel, live:

```java
int lines     = Math.floorDiv(y, 9);   // which text line
int remainder = Math.floorMod(y, 9);   // which a<n> variant
```

Icon *size* is baked the same way, which is why `/marketdemo size` accepts
only 12, 16, 20, 24 and 32 — it selects a variant rather than setting a number
the client reads.

### Draw order: icons on top of the window

The window art is sliced into 9px rows exactly like the hex node. For each
line the renderer draws that slice, shifts the pen back to x=0, then draws
whichever icons land on that line. Later glyphs paint over earlier ones on the
same line, so the goods sit on top of the window rather than beside it.

### One white texture, fifteen goods

The icon is a single white 16x16 calibration target — 1px border, corner
ticks, centre dot — tinted per item with a component colour. Fifteen
distinguishable goods with no per-item art, and a shape where a 1-2px
misalignment is obvious rather than hidden inside a blob. Swap in real icons
per `MarketGood` once the server's `icons_pack` is available here; none of the
layout changes when that happens, which is why the geometry is worth locking
first.

Tooltips use the carrier-item trick from the `hex` section, so each good keeps
its rarity frame.

### The editor

Every value is a server-side number read at render time, so each edit takes
effect on the next render — no restart, no pack rebuild. The dialog sets
`after_action NONE` with `pause(false)`, so re-rendering after every edit
leaves the cursor exactly where it was.

```text
/marketdemo                       open the screen
/marketdemo origin <x> <y>        top-left of slot 0, relative to the art
/marketdemo pitch <x> <y>         centre-to-centre slot spacing
/marketdemo window <x> <y>        move the whole window
/marketdemo size <12|16|20|24|32> pre-baked icon size
/marketdemo advance <px>          pen advance per icon
/marketdemo slot <0-14> <dx> <dy> per-slot correction
/marketdemo nudge <dir> [amount]  move origin by 1px (or n)
/marketdemo reset
/marketdemo dump
```

`dump` prints the tuned numbers as a paste-ready `MarketLayout` block plus the
resolved slot origins, to **both chat and the server console** — chat wraps
and mangles it, `latest.log` does not, and the log is what gets copied back.

**Read `advance` before reaching for per-slot nudges.** A wrong advance shows
up as icons drifting progressively further right across a row — slot 0 correct,
slot 4 worst. That is one number, not five nudges. Per-slot nudges are for
genuine one-off exceptions in the art, and `dump` only emits the non-zero ones
so they stay visible as exceptions.

### Regenerating the assets

```bash
python scratchpad/gen_market_icons.py     # icon texture + 45 glyph entries
python scratchpad/slice_shop_window.py    # window slices + their glyph entries
python scratchpad/measure_shop_window.py  # seed the grid from the PNG
```

All three are generated output — regenerate rather than hand-editing
`nexo/glyphs/market.yml` or `market_window.yml`.

