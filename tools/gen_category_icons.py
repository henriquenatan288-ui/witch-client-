"""Gera os icones de categoria (48x48, branco com alpha, suavizados) em assets/witch/textures/gui/cat_*.png."""
import math
from PIL import Image, ImageDraw

S = 512          # supersample
OUT = 48
W = 44           # espessura do traco (em px de S)
D = 'src/main/resources/assets/witch/textures/gui/'


def canvas():
    im = Image.new('L', (S, S), 0)
    return im, ImageDraw.Draw(im)


def line(d, pts, w=W):
    d.line(pts, fill=255, width=w, joint='curve')
    for x, y in (pts[0], pts[-1]):
        d.ellipse((x - w / 2, y - w / 2, x + w / 2, y + w / 2), fill=255)


def save(name, im):
    out = im.resize((OUT, OUT), Image.LANCZOS)
    rgba = Image.new('RGBA', (OUT, OUT), (255, 255, 255, 0))
    rgba.putalpha(out)
    rgba.save(D + 'cat_' + name + '.png')


# movement: duas setas ">>"
im, d = canvas()
for ox in (-70, 90):
    line(d, [(150 + ox, 110), (290 + ox, 256), (150 + ox, 402)])
save('movement', im)

# combat: espadas cruzadas
im, d = canvas()
line(d, [(110, 110), (400, 400)], 40)
line(d, [(402, 110), (112, 400)], 40)
line(d, [(95, 190), (190, 95)], 40)   # guarda
line(d, [(322, 95), (417, 190)], 40)
save('combat', im)

# visuals: olho
im, d = canvas()
pts = []
for i in range(0, 61):
    t = i / 60
    x = 60 + 392 * t
    y = 256 - 150 * math.sin(math.pi * t)
    pts.append((x, y))
line(d, pts, 40)
pts = [(60 + 392 * (i / 60), 256 + 150 * math.sin(math.pi * i / 60)) for i in range(61)]
line(d, pts, 40)
d.ellipse((256 - 62, 256 - 62, 256 + 62, 256 + 62), fill=255)
save('visuals', im)

# world: globo
im, d = canvas()
d.ellipse((60, 60, 452, 452), outline=255, width=38)
d.ellipse((160, 60, 352, 452), outline=255, width=32)
line(d, [(60, 256), (452, 256)], 32)
save('world', im)

# utility: engrenagem
im, d = canvas()
cx = cy = 256
for k in range(8):
    a = k * math.pi / 4
    ca, sa = math.cos(a), math.sin(a)
    pts = []
    for (u, v) in ((150, -42), (225, -42), (225, 42), (150, 42)):
        pts.append((cx + u * ca - v * sa, cy + u * sa + v * ca))
    d.polygon(pts, fill=255)
d.ellipse((cx - 175, cy - 175, cx + 175, cy + 175), fill=255)
d.ellipse((cx - 78, cy - 78, cx + 78, cy + 78), fill=0)
save('utility', im)

# hud: janela com barra e bloco
im, d = canvas()
d.rounded_rectangle((50, 80, 462, 432), radius=60, outline=255, width=38)
line(d, [(60, 190), (452, 190)], 34)
d.rounded_rectangle((110, 250, 250, 370), radius=20, fill=255)
save('hud', im)

# effects: brilho
im, d = canvas()
def star(cx, cy, r, k=0.22):
    pts = []
    for i in range(8):
        a = i * math.pi / 4 - math.pi / 2
        rr = r if i % 2 == 0 else r * k
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    d.polygon(pts, fill=255)
star(210, 290, 190)
star(390, 120, 90)
save('effects', im)
print('ok')
