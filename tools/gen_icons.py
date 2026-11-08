#!/usr/bin/env python3
"""Generate the Cinema4D launcher icon set: adaptive layers, monochrome, legacy PNGs."""
import os
import subprocess

import tempfile

RES = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res"))


def rrect(x, y, w, h, r):
    return (
        f"M{x + r},{y} h{w - 2 * r} a{r},{r} 0 0 1 {r},{r} v{h - 2 * r} "
        f"a{r},{r} 0 0 1 -{r},{r} h-{w - 2 * r} a{r},{r} 0 0 1 -{r},-{r} "
        f"v-{h - 2 * r} a{r},{r} 0 0 1 {r},-{r} z"
    )


def ellipse(cx, cy, rx, ry):
    return (
        f"M{cx - rx},{cy} a{rx},{ry} 0 1 0 {2 * rx},0 a{rx},{ry} 0 1 0 -{2 * rx},0 z"
    )


HEADER = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
'''

# cinema ticket: rounded body with a half-circle notch on each side
TICKET = (
    "M42,26 H66 A8,8 0 0 1 74,34 V46 A7,7 0 0 0 74,60 V74 "
    "A8,8 0 0 1 66,82 H42 A8,8 0 0 1 34,74 V60 A7,7 0 0 0 34,46 "
    "V34 A8,8 0 0 1 42,26 Z"
)


def perforation(x=52.5, w=3.0, r=1.5):
    """Dashed tear line drawn as rounded segments (VectorDrawable has no dash)."""
    parts = []
    for y in (32.0, 41.0, 50.0, 59.0, 68.0):
        parts.append(rrect(x, y, w, 6.0, r))
    return " ".join(parts)


# ---------------------------------------------------------------- background
background = HEADER + '''    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="0"
                android:startY="0"
                android:endX="108"
                android:endY="108"
                android:startColor="#FF4A5E"
                android:centerColor="#E0243C"
                android:endColor="#A81227" />
        </aapt:attr>
    </path>
</vector>
'''

# ---------------------------------------------------------------- foreground
foreground = HEADER + '''    <group
        android:pivotX="54"
        android:pivotY="54"
        android:rotation="-14"
        android:scaleX="0.80"
        android:scaleY="0.80">
        <path
            android:fillColor="#FFFFFFFF"
            android:pathData="''' + TICKET + '''" />
        <path
            android:fillColor="#E0243C"
            android:pathData="''' + perforation() + '''" />
    </group>
</vector>
'''

# ---------------------------------------------------------------- monochrome
mono = HEADER + '''    <group
        android:pivotX="54"
        android:pivotY="54"
        android:rotation="-14"
        android:scaleX="0.80"
        android:scaleY="0.80">
        <path
            android:fillColor="#FFFFFFFF"
            android:pathData="''' + TICKET + '''" />
    </group>
</vector>
'''

# ---------------------------------------------------------------- adaptive xml
adaptive = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
'''

os.makedirs(f"{RES}/drawable", exist_ok=True)
os.makedirs(f"{RES}/mipmap-anydpi-v26", exist_ok=True)
open(f"{RES}/drawable/ic_launcher_background.xml", "w").write(background)
open(f"{RES}/drawable/ic_launcher_foreground.xml", "w").write(foreground)
open(f"{RES}/drawable/ic_launcher_monochrome.xml", "w").write(mono)
open(f"{RES}/mipmap-anydpi-v26/ic_launcher.xml", "w").write(adaptive)
open(f"{RES}/mipmap-anydpi-v26/ic_launcher_round.xml", "w").write(adaptive)
print("adaptive icon XML written")

# ---------------------------------------------------------------- legacy renders
# Full-bleed square artwork and a circular-masked version, both rendered to PNG
# at every density and dropped into the mipmap folders.
square_svg = '''<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 108 108">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#FF4A5E"/>
      <stop offset="55%" stop-color="#E0243C"/>
      <stop offset="100%" stop-color="#A81227"/>
    </linearGradient>
    {clip}
  </defs>
  <g {clipattr}>
  <rect width="108" height="108" fill="url(#bg)"/>
  <g transform="rotate(-14 54 54) scale(0.80) translate(13.5 13.5)">
    <path fill="#FFFFFF" d="M42 26 H66 A8 8 0 0 1 74 34 V46 A7 7 0 0 0 74 60 V74 A8 8 0 0 1 66 82 H42 A8 8 0 0 1 34 74 V60 A7 7 0 0 0 34 46 V34 A8 8 0 0 1 42 26 Z"/>
    <g fill="#E0243C">
      <rect x="52.5" y="32" width="3" height="6" rx="1.5"/>
      <rect x="52.5" y="41" width="3" height="6" rx="1.5"/>
      <rect x="52.5" y="50" width="3" height="6" rx="1.5"/>
      <rect x="52.5" y="59" width="3" height="6" rx="1.5"/>
      <rect x="52.5" y="68" width="3" height="6" rx="1.5"/>
    </g>
  </g>
  </g>
</svg>
'''

square = square_svg.replace("{clip}", "").replace("{clipattr}", "")
round_svg = square_svg.replace(
    "{clip}",
    '<clipPath id="c"><circle cx="54" cy="54" r="54"/></clipPath>',
).replace("{clipattr}", 'clip-path="url(#c)"')

work = os.path.join(tempfile.gettempdir(), "cinema4d_icon_render")
os.makedirs(work, exist_ok=True)
open(f"{work}/square.svg", "w").write(square)
open(f"{work}/round.svg", "w").write(round_svg)

for name in ("square", "round"):
    subprocess.run(
        ["qlmanage", "-t", "-s", "1024", "-o", work, f"{work}/{name}.svg"],
        capture_output=True,
    )

DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for density, size in DENSITIES.items():
    out_dir = f"{RES}/mipmap-{density}"
    os.makedirs(out_dir, exist_ok=True)
    for kind, target in (("square", "ic_launcher"), ("round", "ic_launcher_round")):
        src_png = f"{work}/{kind}.svg.png"
        dst = f"{out_dir}/{target}.png"
        subprocess.run(["sips", "-z", str(size), str(size), src_png, "--out", dst], capture_output=True)
    # drop the old template webp files
    for stale in ("ic_launcher.webp", "ic_launcher_round.webp"):
        p = f"{out_dir}/{stale}"
        if os.path.exists(p):
            os.remove(p)
print("legacy densities written")
