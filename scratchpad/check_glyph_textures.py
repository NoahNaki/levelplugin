"""
Fail on any glyph texture Minecraft's font atlas cannot hold.

Font glyphs are stitched into a 256x256 atlas. A bitmap larger than that in
either dimension is dropped silently -- no server error, no client error, just
a missing-character box where the art should be. This is the check that turns
that into a build failure.

Run against the deployed Nexo pack, since that is what the client receives.
"""
import glob, os, sys, yaml
from PIL import Image

NEXO = r"C:\Users\noahs\Desktop\Minecraft Servers\1.21.8 XPrison Migration\server\plugins\Nexo"
TEX = os.path.join(NEXO, "pack", "assets", "minecraft", "textures")
ATLAS_MAX = 256

oversize, missing, checked = {}, [], 0
for f in glob.glob(os.path.join(NEXO, "glyphs", "**", "*.yml"), recursive=True):
    for key, value in (yaml.safe_load(open(f, encoding="utf-8")) or {}).items():
        if not isinstance(value, dict) or not value.get("texture"):
            continue
        path = os.path.join(TEX, *str(value["texture"]).split("/")) + ".png"
        if not os.path.exists(path):
            missing.append((key, value["texture"], os.path.basename(f)))
            continue
        checked += 1
        w, h = Image.open(path).size
        if w > ATLAS_MAX or h > ATLAS_MAX:
            # Key by texture: one bad file usually backs many glyph entries.
            oversize[value["texture"]] = (w, h, os.path.basename(f))

print(f"checked {checked} glyph textures ({len(missing)} entries point at missing files)")
if oversize:
    print(f"\nOVERSIZE (> {ATLAS_MAX}px, will not render):")
    for tex, (w, h, src) in sorted(oversize.items()):
        print(f"   {tex:<40} {w}x{h}   [{src}]")
    sys.exit(1)
print("no texture exceeds the atlas limit")
