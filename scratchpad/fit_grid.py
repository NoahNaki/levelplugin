import colorsys
from PIL import Image, ImageDraw

SRC = "src/main/resources/resourcepack/assets/minecraft/textures/market/shop_window.png"
img = Image.open(SRC).convert("RGBA")
W, H = img.size
px = img.load()

def vivid(p):
    r, g, b, a = p
    if a < 200:
        return False
    h, s, v = colorsys.rgb_to_hsv(r/255, g/255, b/255)
    return s > 0.35 and 0.20 < v < 0.85

mask = [[vivid(px[x, y]) for y in range(H)] for x in range(W)]
seen = [[False]*H for _ in range(W)]
comps = []
for sx in range(W):
    for sy in range(H):
        if not mask[sx][sy] or seen[sx][sy]:
            continue
        stack, cells = [(sx, sy)], []
        seen[sx][sy] = True
        while stack:
            x, y = stack.pop(); cells.append((x, y))
            for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
                nx, ny = x+dx, y+dy
                if 0 <= nx < W and 0 <= ny < H and not seen[nx][ny] and mask[nx][ny]:
                    seen[nx][ny] = True; stack.append((nx, ny))
        xs = [c[0] for c in cells]; ys = [c[1] for c in cells]
        x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
        w, h = x1-x0+1, y1-y0+1
        # The awning's rope ties are vivid too, but they sit above the panel
        # and are wider than they are tall; real slots are >=24px square-ish.
        # The awning's rope ties are vivid too, but they sit above the
        # panel and are wider than tall; real slots are >=22px square-ish.
        if (22 <= w <= 40 and 22 <= h <= 40 and abs(w-h) <= 6
                and len(cells) > 0.55*w*h and y0 > 80):
            comps.append((x0, y0, w, h, (x0+x1)/2, (y0+y1)/2))

comps.sort(key=lambda c: (round(c[5]/10), c[4]))
print(f"detected {len(comps)} slot fills\n")
for c in comps:
    print(f"  x={c[0]:3d} y={c[1]:3d} w={c[2]:2d} h={c[3]:2d}  centre=({c[4]:.1f},{c[5]:.1f})")

def cluster(vals, tol=8):
    vals = sorted(vals); groups = [[vals[0]]]
    for v in vals[1:]:
        if v - groups[-1][-1] <= tol: groups[-1].append(v)
        else: groups.append([v])
    return [sum(g)/len(g) for g in groups]

cx = cluster([c[4] for c in comps])
cy = cluster([c[5] for c in comps])
print(f"\ncolumn centres: {[round(v,1) for v in cx]}")
print(f"row centres:    {[round(v,1) for v in cy]}")

def pitch(v):
    d = [b-a for a, b in zip(v, v[1:])]
    return sum(d)/len(d) if d else 0

pX, pY = pitch(cx), pitch(cy)
avg_w = sum(c[2] for c in comps)/len(comps)
avg_h = sum(c[3] for c in comps)/len(comps)
print(f"\npitchX={pX:.2f}  pitchY={pY:.2f}  avg fill {avg_w:.1f}x{avg_h:.1f}")

ICON = 16
ox = round(cx[0] - ICON/2); oy = round(cy[0] - ICON/2)
print(f"\n--- seed for MarketLayout (icon {ICON}px, centred in each slot) ---")
print(f"originX = {ox}; originY = {oy};")
print(f"pitchX = {round(pX)}; pitchY = {round(pY)};")

dbg = img.copy().resize((W*2, H*2), Image.NEAREST)
d = ImageDraw.Draw(dbg)
for c in comps:
    d.rectangle([c[0]*2, c[1]*2, (c[0]+c[2])*2-1, (c[1]+c[3])*2-1], outline=(255,0,255,255))
for r in range(len(cy)):
    for col in range(len(cx)):
        x = ox + round(col*pX); y = oy + round(r*pY)
        d.rectangle([x*2, y*2, (x+ICON)*2-1, (y+ICON)*2-1], outline=(0,255,255,255))
dbg.save("scratchpad/shop_window_debug.png")
print("\nwrote scratchpad/shop_window_debug.png (magenta=fills, cyan=fitted 16px icons)")
