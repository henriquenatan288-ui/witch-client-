"""Gera as 4 cartas (128x128) das particulas do Witch: supersample 4x + LANCZOS (bordas lisas)."""
import math, sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = sys.argv[1]
FONT = sys.argv[2]
S = 4
N = 128 * S
RED = (220, 38, 70)
BLK = (24, 22, 36)


def heart(d, cx, cy, r, col):
    d.ellipse((cx - r, cy - r * 0.95, cx, cy + r * 0.05), fill=col)
    d.ellipse((cx, cy - r * 0.95, cx + r, cy + r * 0.05), fill=col)
    d.polygon([(cx - r * 0.98, cy - r * 0.25), (cx + r * 0.98, cy - r * 0.25), (cx, cy + r * 1.15)], fill=col)


def diamond(d, cx, cy, r, col):
    d.polygon([(cx, cy - r * 1.15), (cx + r * 0.8, cy), (cx, cy + r * 1.15), (cx - r * 0.8, cy)], fill=col)


def club(d, cx, cy, r, col):
    rr = r * 0.52
    for ox, oy in ((0, -r * 0.5), (-r * 0.52, r * 0.28), (r * 0.52, r * 0.28)):
        d.ellipse((cx + ox - rr, cy + oy - rr, cx + ox + rr, cy + oy + rr), fill=col)
    d.polygon([(cx, cy), (cx - r * 0.2, cy + r * 1.1), (cx + r * 0.2, cy + r * 1.1)], fill=col)


def spade(d, cx, cy, r, col):
    d.ellipse((cx - r, cy - r * 0.15, cx, cy + r * 0.85), fill=col)
    d.ellipse((cx, cy - r * 0.15, cx + r, cy + r * 0.85), fill=col)
    d.polygon([(cx - r * 0.98, cy + r * 0.3), (cx + r * 0.98, cy + r * 0.3), (cx, cy - r * 1.2)], fill=col)
    d.polygon([(cx, cy + r * 0.4), (cx - r * 0.25, cy + r * 1.15), (cx + r * 0.25, cy + r * 1.15)], fill=col)


SUITS = {"hearts": (heart, RED), "diamonds": (diamond, RED), "clubs": (club, BLK), "spades": (spade, BLK)}


def card(name):
    fn, col = SUITS[name]
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    cw, ch = int(N * 0.66), int(N * 0.92)
    x0, y0 = (N - cw) // 2, (N - ch) // 2
    rad = int(N * 0.07)
    # brilho roxo suave atras
    glow = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    ImageDraw.Draw(glow).rounded_rectangle((x0, y0, x0 + cw, y0 + ch), rad, fill=(150, 80, 255, 200))
    glow = glow.filter(ImageFilter.GaussianBlur(N * 0.035))
    img.alpha_composite(glow)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((x0, y0, x0 + cw, y0 + ch), rad, fill=(36, 30, 56, 255))
    b = int(N * 0.012)
    # corpo claro com gradiente vertical leve
    body = Image.new("RGBA", (cw - 2 * b, ch - 2 * b))
    bp = body.load()
    for y in range(body.height):
        t = y / body.height
        c = (int(252 - 10 * t), int(250 - 12 * t), int(255 - 8 * t), 255)
        for x in range(body.width):
            bp[x, y] = c
    mask = Image.new("L", body.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, body.width - 1, body.height - 1), rad - b, fill=255)
    img.paste(body, (x0 + b, y0 + b), mask)
    d = ImageDraw.Draw(img)
    cx, cy = N // 2, N // 2
    fn(d, cx, cy, N * 0.15, col)
    # indice no canto (A + naipe pequeno), e espelhado embaixo
    font = ImageFont.truetype(FONT, int(N * 0.12))
    tx, ty = x0 + int(N * 0.045), y0 + int(N * 0.025)
    d.text((tx, ty), "A", font=font, fill=col)
    fn(d, tx + int(N * 0.045), ty + int(N * 0.165), N * 0.04, col)
    corner = img.crop((x0, y0, x0 + int(N * 0.16), y0 + int(N * 0.26))).rotate(180)
    img.alpha_composite(corner, (x0 + cw - corner.width, y0 + ch - corner.height))
    return img.resize((128, 128), Image.LANCZOS)


sheet = Image.new("RGBA", (128 * 4 + 30, 128 + 20), (20, 14, 36, 255))
for i, n in enumerate(SUITS):
    c = card(n)
    c.save(f"{OUT}/card_{n}.png")
    sheet.alpha_composite(c, (10 + i * 138, 10))
sheet.save(sys.argv[3])
print("ok")
