"""Formas extras para as particulas (128x128, lisas, desenho proprio): nota de dinheiro, chapeu de bruxa, pato de cartola.
Uso: gen_shapes.py OUT_DIR FONT_PATH PREVIEW_PNG"""
import sys, math
from PIL import Image, ImageDraw, ImageFont, ImageFilter
OUT, FONT, PREV = sys.argv[1:4]
S = 4; N = 128 * S
def P(v): return int(round(v * S))
def canvas(): 
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0)); return im, ImageDraw.Draw(im)
def finish(im, glow=(255, 255, 255)):
    # contorno/brilho suave
    a = im.split()[3]
    g = a.filter(ImageFilter.GaussianBlur(P(2.2)))
    base = Image.new("RGBA", (N, N), glow + (0,))
    base.putalpha(g.point(lambda v: int(v * 0.55)))
    base.alpha_composite(im)
    return base.resize((128, 128), Image.LANCZOS)
def rrect(d, box, r, fill, outline=None, w=0):
    d.rounded_rectangle([P(box[0]), P(box[1]), P(box[2]), P(box[3])], radius=P(r), fill=fill, outline=outline, width=P(w))

def money():
    im, d = canvas()
    rrect(d, (8, 30, 120, 98), 7, (30, 100, 55, 255))
    rrect(d, (11, 33, 117, 95), 5, (118, 196, 128, 255))
    rrect(d, (16, 38, 112, 90), 3, None, (40, 120, 70, 255), 1.6)
    d.ellipse([P(48), P(44), P(80), P(84)], fill=(160, 220, 165, 255), outline=(40, 120, 70, 255), width=P(2))
    f = ImageFont.truetype(FONT, P(30)); w = d.textlength("$", font=f)
    d.text((P(64) - w / 2, P(64)), "$", font=f, fill=(30, 100, 55, 255), anchor="lm")
    f2 = ImageFont.truetype(FONT, P(13))
    for (x, y) in ((26, 46), (102, 46), (26, 82), (102, 82)):
        w2 = d.textlength("100", font=f2); d.text((P(x) - w2 / 2, P(y)), "100", font=f2, fill=(30, 100, 55, 255), anchor="lm")
    for y in (58, 64, 70):
        d.line([P(20), P(y), P(44), P(y)], fill=(70, 150, 90, 255), width=P(1.2))
        d.line([P(84), P(y), P(108), P(y)], fill=(70, 150, 90, 255), width=P(1.2))
    return finish(im, (120, 255, 150))

def bez(p0, c1, c2, p3, n=36):
    out = []
    for k in range(n + 1):
        t = k / n; u = 1 - t
        out.append((u**3 * p0[0] + 3 * u * u * t * c1[0] + 3 * u * t * t * c2[0] + t**3 * p3[0],
                    u**3 * p0[1] + 3 * u * u * t * c1[1] + 3 * u * t * t * c2[1] + t**3 * p3[1]))
    return out

def witch():
    im, d = canvas()
    # aba larga
    d.ellipse([P(6), P(88), P(122), P(114)], fill=(40, 16, 70, 255))
    d.ellipse([P(6), P(84), P(122), P(108)], fill=(88, 44, 144, 255))
    # cone curvo e pontudo (curva em S, ponta fina pra direita)
    left = bez((34, 92), (50, 60), (36, 2), (118, 22))
    right = bez((118, 22), (78, 22), (92, 60), (94, 92))
    d.polygon([(P(x), P(y)) for x, y in left + right], fill=(100, 54, 164, 255))
    # lado iluminado
    hl = bez((34, 92), (50, 60), (36, 2), (118, 22))
    inner = bez((52, 92), (62, 62), (56, 22), (110, 24))
    d.polygon([(P(x), P(y)) for x, y in hl[:30] + inner[:30][::-1]], fill=(128, 80, 196, 255))
    # sombra do lado direito
    sh = bez((118, 22), (84, 30), (96, 66), (94, 92))
    sh2 = bez((110, 26), (88, 40), (88, 66), (84, 92))
    d.polygon([(P(x), P(y)) for x, y in sh + sh2[::-1]], fill=(72, 36, 120, 255))
    d.ellipse([P(30), P(80), P(100), P(104)], fill=(88, 44, 144, 255))
    # faixa e fivela
    d.polygon([(P(33), P(80)), (P(96), P(80)), (P(98), P(92)), (P(31), P(92))], fill=(26, 12, 50, 255))
    d.rectangle([P(56), P(77), P(74), P(95)], fill=(244, 196, 60, 255))
    d.rectangle([P(61), P(82), P(69), P(90)], fill=(26, 12, 50, 255))
    d.ellipse([P(44), P(62), P(51), P(69)], fill=(255, 255, 255, 55))
    return finish(im, (190, 120, 255))

def duck():
    im, d = canvas()
    # corpo
    d.ellipse([P(22), P(60), P(104), P(112)], fill=(255, 214, 54, 255))
    d.polygon([(P(96), P(78)), (P(118), P(70)), (P(106), P(92))], fill=(255, 214, 54, 255))
    d.ellipse([P(40), P(78), P(82), P(104)], fill=(240, 186, 30, 255))
    # cabeca
    d.ellipse([P(24), P(36), P(66), P(78)], fill=(255, 224, 80, 255))
    # bico
    d.ellipse([P(8), P(56), P(40), P(70)], fill=(255, 140, 30, 255))
    d.ellipse([P(10), P(60), P(38), P(70)], fill=(236, 112, 18, 255))
    # olho
    d.ellipse([P(44), P(50), P(52), P(60)], fill=(20, 20, 24, 255)); d.ellipse([P(47), P(51), P(50), P(54)], fill=(255, 255, 255, 255))
    # cartola
    d.rectangle([P(28), P(14), P(60), P(38)], fill=(24, 24, 30, 255))
    d.ellipse([P(28), P(10), P(60), P(20)], fill=(40, 40, 50, 255))
    d.ellipse([P(18), P(32), P(70), P(44)], fill=(24, 24, 30, 255))
    d.rectangle([P(28), P(28), P(60), P(35)], fill=(196, 30, 52, 255))
    d.ellipse([P(34), P(12), P(46), P(15)], fill=(255, 255, 255, 50))
    return finish(im, (255, 230, 120))

res = {"shape_money": money(), "shape_witchhat": witch(), "shape_duck": duck()}
for k, v in res.items(): v.save(f"{OUT}/{k}.png")
pv = Image.new("RGBA", (128 * 3 * 2 + 40, 270), (40, 40, 48, 255)); x = 10
for v in res.values():
    b = v.resize((256, 256), Image.LANCZOS); pv.paste(b, (x, 6), b); x += 262
pv.save(PREV)
