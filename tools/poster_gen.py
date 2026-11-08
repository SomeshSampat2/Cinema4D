#!/usr/bin/env python3
"""Generate cinematic key-art posters (PPM) for Cinema4D.
Pure stdlib. Usage: python3 poster_gen.py <out_dir>"""
import math
import os
import struct
import sys
import zlib

W, H = 480, 720
OUT = sys.argv[1] if len(sys.argv) > 1 else "/tmp/cinema_build/posters"
os.makedirs(OUT, exist_ok=True)


def clamp(v, a=0, b=255):
    return a if v < a else b if v > b else v


def lerp(a, b, t):
    return a + (b - a) * t


def mix(c1, c2, t):
    return tuple(clamp(int(lerp(c1[i], c2[i], t))) for i in range(3))


class Canvas:
    def __init__(self, w=W, h=H):
        self.w, self.h = w, h
        self.img = bytearray(w * h * 3)
        self._rowcache = {}

    def _rowbytes(self, rgb, x0, x1):
        key = (rgb, x0, x1)
        rb = self._rowcache.get(key)
        if rb is None:
            rb = bytes(rgb) * (x1 - x0)
            self._rowcache[key] = rb
        return rb

    def hline(self, x0, x1, y, rgb):
        if y < 0 or y >= self.h or x1 <= x0:
            return
        x0 = max(0, x0); x1 = min(self.w, x1)
        i = (y * self.w + x0) * 3
        self.img[i:i + (x1 - x0) * 3] = self._rowbytes(rgb, x0, x1)

    def vgrad(self, stops):
        """stops: list of (t in [0,1], rgb). Vertical gradient."""
        for y in range(self.h):
            t = y / (self.h - 1)
            for i in range(len(stops) - 1):
                t0, c0 = stops[i]; t1, c1 = stops[i + 1]
                if t0 <= t <= t1 or i == len(stops) - 2:
                    f = 0 if t1 == t0 else (t - t0) / (t1 - t0)
                    self.hline(0, self.w, y, mix(c0, c1, clamp(f)))
                    break

    def circle(self, cx, cy, r, rgb):
        for y in range(max(0, int(cy - r)), min(self.h, int(cy + r) + 1)):
            dy = y - cy
            dx = math.sqrt(max(0.0, r * r - dy * dy))
            self.hline(int(cx - dx), int(cx + dx) + 1, y, rgb)

    def circle_soft(self, cx, cy, r, rgb, glow=0.35):
        """Filled core + 2 soft halo rings (cheap bloom)."""
        for k, (rr, a) in enumerate([(r * (1 + glow), 0.35), (r * (1 + glow * 2), 0.15)]):
            col = tuple(clamp(int(c * a)) for c in rgb)
            for y in range(max(0, int(cy - rr)), min(self.h, int(cy + rr) + 1)):
                dy = y - cy
                dx = math.sqrt(max(0.0, rr * rr - dy * dy))
                self.hline(int(cx - dx), int(cx + dx) + 1, y, col)
        self.circle(cx, cy, r, rgb)

    def rect(self, x0, y0, x1, y1, rgb):
        for y in range(max(0, y0), min(self.h, y1)):
            self.hline(x0, x1, y, rgb)

    def poly(self, pts, rgb):
        """Scanline even-odd polygon fill."""
        ys = [p[1] for p in pts]
        for y in range(max(0, int(min(ys))), min(self.h, int(max(ys)) + 1)):
            xs = []
            n = len(pts)
            for i in range(n):
                x1, y1 = pts[i]; x2, y2 = pts[(i + 1) % n]
                if (y1 <= y < y2) or (y2 <= y < y1):
                    xs.append(x1 + (y - y1) * (x2 - x1) / (y2 - y1))
            xs.sort()
            for i in range(0, len(xs) - 1, 2):
                self.hline(int(xs[i]), int(xs[i + 1]) + 1, y, rgb)

    def stars(self, n, seed=7, ymax_frac=0.6, bright=(255, 255, 255)):
        import random
        rnd = random.Random(seed)
        for _ in range(n):
            x = rnd.randint(0, self.w - 1)
            y = rnd.randint(0, int(self.h * ymax_frac))
            b = rnd.randint(90, 255)
            self.hline(x, x + 1, y, (b, b, min(255, b + 20)))

    def save_ppm(self, path):
        with open(path, "wb") as f:
            f.write(f"P6\n{self.w} {self.h}\n255\n".encode())
            f.write(self.img)


# ------------------------------------------------------------------ posters
def poster_neon_divide():
    """Sci-fi thriller: teal dusk, neon grid, glowing portal."""
    c = Canvas()
    c.vgrad([(0, (6, 14, 26)), (0.55, (10, 42, 60)), (0.72, (16, 78, 92)), (1, (4, 10, 18))])
    # stars
    c.stars(90, seed=3, ymax_frac=0.5)
    horizon = int(H * 0.70)
    # sun/portal glow behind skyline
    c.circle_soft(W // 2, horizon - 40, 120, (64, 224, 220), glow=0.5)
    c.circle(W // 2, horizon - 40, 96, (140, 245, 240))
    c.circle(W // 2, horizon - 40, 78, (220, 255, 252))
    # skyline silhouette
    rnd_sky = [(0, horizon)]
    import random
    rnd = random.Random(11)
    x = 0
    while x < W:
        bw = rnd.randint(18, 46)
        bh = rnd.randint(20, 130)
        rnd_sky.append((x, horizon - bh))
        rnd_sky.append((x + bw, horizon - bh))
        x += bw + rnd.randint(0, 8)
    pts = [(0, H)] + [(px, py) for px, py in rnd_sky] + [(W, H)]
    # flatten pairs into polygon
    poly = [(rnd_sky[i][0], rnd_sky[i][1]) for i in range(0, len(rnd_sky), 2)]
    poly = [(0, H)] + poly + [(W, H)]
    c.poly(poly, (3, 8, 14))
    # neon grid on ground
    gy = horizon
    for i in range(9):
        t = i / 8
        yy = int(gy + (H - gy) * (t ** 1.7))
        c.hline(0, W, yy, (0, 120, 130) if i % 2 else (0, 90, 110))
    for i in range(-8, 9):
        # converging verticals
        x_top = W // 2 + i * 14
        x_bot = W // 2 + i * 60
        steps = 40
        for s in range(steps):
            t = s / steps
            xx = int(lerp(x_top, x_bot, t * t))
            yy = int(lerp(gy, H, t))
            c.hline(xx, xx + 1, yy, (0, 140, 150))
    # magenta accent line
    for y in range(horizon - 60, horizon + 4):
        c.hline(0, W, y, (0, 0, 0))
    c.hline(0, W, horizon - 1, (255, 45, 140))
    return c


def poster_cartographer():
    """Adventure: amber dusk, giant sun, layered mountains."""
    c = Canvas()
    c.vgrad([(0, (30, 18, 8)), (0.45, (150, 74, 28)), (0.62, (232, 138, 52)), (0.66, (255, 190, 90)), (1, (24, 12, 6))])
    c.circle_soft(W // 2, int(H * 0.60), 110, (255, 205, 110), glow=0.55)
    c.circle(W // 2, int(H * 0.60), 88, (255, 222, 150))
    # mountain layers (far -> near)
    layers = [
        ((0.66, (96, 52, 30)), [(-40, 470), (120, 380), (260, 460), (380, 350), (520, 450)]),
        ((0.78, (58, 30, 18)), [(-40, 540), (100, 430), (240, 520), (360, 420), (500, 520)]),
        ((0.90, (30, 15, 8)),  [(-40, 620), (140, 500), (300, 590), (430, 500), (540, 610)]),
    ]
    for (yt, col), pts in layers:
        base = int(H * yt)
        poly = [(x, base + (y - 470) * 0.4) for x, y in pts]
        poly = [(pts[0][0] - 40, H)] + poly + [(pts[-1][0] + 40, H)]
        c.poly(poly, col)
    # tiny explorer silhouette on nearest peak
    c.rect(W // 2 + 148 - 3, 508, W // 2 + 148 + 3, 522, (10, 6, 4))
    c.circle(W // 2 + 148, 504, 4, (10, 6, 4))
    return c


def poster_static():
    """Horror: near-black, red glitch, white noise slash."""
    c = Canvas()
    c.vgrad([(0, (4, 2, 2)), (0.6, (16, 4, 4)), (1, (2, 1, 1))])
    # vertical glitch bars
    import random
    rnd = random.Random(21)
    for _ in range(26):
        x = rnd.randint(0, W - 30)
        y = rnd.randint(int(H * 0.15), int(H * 0.85))
        wbar = rnd.randint(2, 26)
        hbar = rnd.randint(2, 14)
        shade = rnd.choice([(60, 8, 8), (120, 12, 12), (30, 4, 4), (200, 30, 30)])
        c.rect(x, y, x + wbar, y + hbar, shade)
    # red vignette pulse circle off-centre
    c.circle_soft(W // 2 + 40, int(H * 0.42), 150, (90, 6, 8), glow=0.6)
    # white slash
    for i in range(-H, H, 3):
        c.hline(W // 2 - 60 + i // 6, W // 2 + 60 + i // 6, int(H * 0.55) + i // 10, (210, 210, 205))
    # scanlines
    for y in range(0, H, 4):
        c.hline(0, W, y, (0, 0, 0))
    # grain-ish dots
    for _ in range(900):
        x = rnd.randint(0, W - 1); y = rnd.randint(0, H - 1)
        v = rnd.randint(60, 160)
        c.hline(x, x + 1, y, (v, v // 3, v // 3))
    return c


def poster_harbor():
    """Romance drama: dusk navy->pink, water shimmer, two lights."""
    c = Canvas()
    c.vgrad([(0, (10, 12, 40)), (0.5, (44, 40, 96)), (0.68, (150, 70, 130)), (0.72, (240, 140, 170)), (1, (8, 10, 30))])
    c.stars(70, seed=9, ymax_frac=0.45)
    hy = int(H * 0.70)
    # moon + lamp glow
    c.circle_soft(W // 2 - 90, int(H * 0.30), 46, (255, 214, 220), glow=0.5)
    c.circle(W // 2 - 90, int(H * 0.30), 34, (255, 236, 238))
    c.circle_soft(W // 2 + 110, int(H * 0.52), 26, (255, 190, 140), glow=0.5)
    c.circle(W // 2 + 110, int(H * 0.52), 17, (255, 220, 170))
    # water: dark below horizon + shimmer streaks under lights
    c.rect(0, hy, W, H, (10, 12, 34))
    rnd_sh = __import__("random").Random(5)
    for i in range(60):
        y = hy + rnd_sh.randint(4, H - hy - 4)
        xw = rnd_sh.randint(10, 90)
        xc = W // 2 - 90 if rnd_sh.random() < 0.5 else W // 2 + 110
        c.hline(xc - xw // 2, xc + xw // 2, y, (90, 80, 140) if i % 3 else (140, 110, 160))
    # horizon line
    c.hline(0, W, hy, (220, 150, 170))
    return c


def poster_iron_vow():
    """Action: steel grey, ember glow, hazard diagonal."""
    c = Canvas()
    c.vgrad([(0, (18, 20, 26)), (0.55, (52, 52, 60)), (0.8, (30, 26, 24)), (1, (10, 8, 8))])
    # ember explosion low centre
    c.circle_soft(W // 2, int(H * 0.72), 130, (255, 130, 30), glow=0.7)
    c.circle(W // 2, int(H * 0.72), 92, (255, 190, 80))
    c.circle(W // 2, int(H * 0.72), 55, (255, 240, 190))
    # smoke bands
    for i, (yy, hh, col) in enumerate([(int(H * 0.5), 40, (24, 22, 24)), (int(H * 0.58), 60, (16, 14, 14))]):
        c.rect(0, yy, W, yy + hh, col)
    # hazard chevrons bottom
    for x in range(-H, W + H, 64):
        for s in range(32):
            y0 = H - 90 + s
            c.hline(x + s, x + s + 16, y0, (200, 160, 30))
            c.hline(x + s + 16, x + s + 32, y0, (12, 12, 12))
    return c


def poster_paper_moons():
    """Family animation: pastel paper-cut, crescent moons, clouds."""
    c = Canvas()
    c.vgrad([(0, (150, 140, 220)), (0.6, (220, 170, 200)), (1, (250, 210, 170))])
    c.stars(50, seed=13, ymax_frac=0.7, bright=(255, 250, 230))
    # big crescent moon (circle minus offset circle)
    mx, my, mr = W // 2, int(H * 0.34), 90
    # draw moon via per-row spans with bite
    for y in range(my - mr, my + mr + 1):
        dy = y - my
        dx = math.sqrt(max(0.0, mr * mr - dy * dy))
        xl, xr = int(mx - dx), int(mx + dx) + 1
        if 0 <= y < H:
            c.hline(xl, xr, y, (255, 244, 200))
    # bite shadow (background-ish purple)
    bx, by, br = mx + 46, my - 26, mr - 8
    for y in range(by - br, by + br + 1):
        dy = y - by
        dx = math.sqrt(max(0.0, br * br - dy * dy))
        if 0 <= y < H:
            c.hline(int(bx - dx), int(bx + dx) + 1, y, (120, 105, 200))
    # small moon
    c.circle_soft(W // 2 + 130, int(H * 0.22), 34, (255, 220, 160), glow=0.4)
    c.circle(W // 2 + 130, int(H * 0.22), 26, (255, 236, 190))
    # paper clouds (rounded blobs)
    for cx, cy, s in [(90, 140, 1.0), (390, 110, 0.8), (300, 190, 0.65)]:
        c.circle_soft(cx, cy, int(46 * s), (255, 250, 245), glow=0.5)
        c.circle(cx, cy, int(38 * s), (255, 252, 248))
        c.circle(cx - int(20 * s), cy + int(8 * s), int(26 * s), (255, 252, 248))
        c.circle(cx + int(22 * s), cy + int(6 * s), int(24 * s), (255, 252, 248))
    # rolling hills bottom
    c.poly([(0, H), (0, int(H * 0.86)), (W // 3, int(H * 0.78)), (2 * W // 3, int(H * 0.88)), (W, int(H * 0.80)), (W, H)], (110, 160, 110))
    c.poly([(0, H), (0, int(H * 0.93)), (W // 2, int(H * 0.86)), (W, int(H * 0.94)), (W, H)], (80, 130, 90))
    return c


POSTERS = {
    "neon_divide": poster_neon_divide,
    "last_cartographer": poster_cartographer,
    "static": poster_static,
    "harbor_lights": poster_harbor,
    "iron_vow": poster_iron_vow,
    "paper_moons": poster_paper_moons,
}

for name, fn in POSTERS.items():
    c = fn()
    path = os.path.join(OUT, f"{name}.ppm")
    c.save_ppm(path)
    print("wrote", path)
