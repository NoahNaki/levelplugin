"""
Measure the 15 slot rectangles in the market shop-window texture.

The slots are a regular 5x3 grid, so this fits a *parametric* grid
(origin, pitch, size) rather than 15 independent rectangles -- that is
what the in-game editor exposes, so it is what we want to seed.

Detection is deliberately not colour-based: the slot fills run from red
through orange, and orange is too close to the panel's wood brown to
threshold reliably. Instead it keys on the near-black outline every slot
carries, which is the one feature all 15 share regardless of fill.

Writes scratchpad/shop_window_debug.png with the fitted grid drawn on top
so the fit can be eyeballed before any of it reaches the game.
"""
import sys, json
from PIL import Image, ImageDraw

SRC = sys.argv[1] if len(sys.argv) > 1 else \
    "src/main/resources/resourcepack/assets/minecraft/textures/market/shop_window.png"

img = Image.open(SRC).convert("RGBA")
W, H = img.size
px = img.load()
print(f"canvas {W}x{H}")

# --- outline mask: dark and opaque ------------------------------------
def is_outline(p):
    r, g, b, a = p
    return a > 128 and (r + g + b) / 3 < 70

mask = [[is_outline(px[x, y]) for y in range(H)] for x in range(W)]

col = [sum(1 for y in range(H) if mask[x][y]) for x in range(W)]
row = [sum(1 for x in range(W) if mask[x][y]) for y in range(H)]

def peaks(profile, min_frac=0.35):
    """Runs of columns/rows whose outline count clears a fraction of the max."""
    hi = max(profile) * min_frac
    out, run = [], None
    for i, v in enumerate(profile):
        if v >= hi:
            run = i if run is None else run
        elif run is not None:
            out.append((run, i - 1)); run = None
    if run is not None:
        out.append((run, len(profile) - 1))
    return out

cp, rp = peaks(col), peaks(row)
print(f"\ncolumn peak bands ({len(cp)}): {cp}")
print(f"row peak bands ({len(rp)}): {rp}")

# --- connected components, the cross-check on the projection fit ------
seen = [[False] * H for _ in range(W)]
comps = []
for sx in range(W):
    for sy in range(H):
        if mask[sx][sy] or seen[sx][sy] or px[sx, sy][3] < 128:
            continue
        stack, cells = [(sx, sy)], []
        seen[sx][sy] = True
        while stack:
            x, y = stack.pop()
            cells.append((x, y))
            for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
                nx, ny = x+dx, y+dy
                if 0 <= nx < W and 0 <= ny < H and not seen[nx][ny] \
                   and not mask[nx][ny] and px[nx, ny][3] >= 128:
                    seen[nx][ny] = True
                    stack.append((nx, ny))
        xs = [c[0] for c in cells]; ys = [c[1] for c in cells]
        x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
        w, h = x1-x0+1, y1-y0+1
        # slot-shaped: roughly square, plausible size, mostly filled
        if 12 <= w <= 60 and 12 <= h <= 60 and abs(w-h) <= 6 \
           and len(cells) > 0.5 * w * h:
            comps.append((x0, y0, w, h))

comps.sort(key=lambda c: (round(c[1]/8), c[0]))
print(f"\nslot-shaped components ({len(comps)}):")
for c in comps:
    print(f"  x={c[0]:3d} y={c[1]:3d} w={c[2]:2d} h={c[3]:2d}")

result = None
if len(comps) == 15:
    xs = sorted({c[0] for c in comps})
    ys = sorted({c[1] for c in comps})
    def pitch(v):
        if len(v) < 2: return 0
        d = [b-a for a, b in zip(v, v[1:])]
        return round(sum(d)/len(d))
    # cluster near-identical coordinates before deriving pitch
    def cluster(v, tol=4):
        out = [[v[0]]]
        for n in v[1:]:
            (out[-1] if n - out[-1][-1] <= tol else out.append([n]) or out[-1]).append(n)
        return [round(sum(g)/len(g)) for g in out]
    cx, cy = cluster(xs), cluster(ys)
    result = {
        "originX": cx[0], "originY": cy[0],
        "pitchX": pitch(cx), "pitchY": pitch(cy),
        "slotW": round(sum(c[2] for c in comps)/15),
        "slotH": round(sum(c[3] for c in comps)/15),
        "cols": len(cx), "rows": len(cy),
    }
    print("\nfitted grid:")
    print(json.dumps(result, indent=2))
else:
    print("\n!! expected 15 slot components -- fit by hand from the bands above")

dbg = img.copy().resize((W*2, H*2), Image.NEAREST)
d = ImageDraw.Draw(dbg)
for (x0, y0, w, h) in comps:
    d.rectangle([x0*2, y0*2, (x0+w)*2-1, (y0+h)*2-1], outline=(255, 0, 255, 255))
dbg.save("scratchpad/shop_window_debug.png")
print("\nwrote scratchpad/shop_window_debug.png (2x, magenta = detected slots)")
