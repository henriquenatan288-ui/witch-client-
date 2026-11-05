"""Karambit 32x32 (arte propria) para diamond_sword e netherite_sword. Uso: gen_karambit.py OUT_DIR PREVIEW"""
import sys
from PIL import Image, ImageDraw
S=16  # supersample
def make(blade_hi, blade_lo, edge, handle, ring):
    W=32
    im=Image.new("RGBA",(W*S,W*S),(0,0,0,0)); d=ImageDraw.Draw(im)
    P=lambda pts:[(x*S,y*S) for x,y in pts]
    # lamina curva (foice) do cabo para cima-direita, ponta curvando para baixo
    blade=[(10,21),(13,14),(18,8),(24,5),(28,6),(29,9),(27,8.2),(23,9.5),(19,13.5),(16,19),(14,23.5)]
    d.polygon(P(blade),fill=blade_lo)
    inner=[(11,20),(14,14.5),(18.5,9.6),(24,6.6),(27,6.8),(24,8.6),(19.6,11.8),(16.4,16.5),(14,21.5)]
    d.polygon(P(inner),fill=blade_hi)
    # fio (borda interna da curva)
    d.line(P([(14,23.5),(16,19),(19,13.5),(23,9.5),(27,8.2),(29,9)]),fill=edge,width=int(0.9*S))
    # cabo
    d.polygon(P([(7,24),(10,21),(14.2,23.8),(12,29),(8,29.5)]),fill=handle)
    d.line(P([(9,25),(12.5,27)]),fill=(255,255,255,40),width=int(0.8*S))
    # anel
    d.ellipse([(3.2*S,24.6*S),(9*S,30.4*S)],outline=ring,width=int(1.3*S))
    im=im.resize((W,W),Image.LANCZOS)
    # endurece alfa (visual de pixel art limpo)
    px=im.load()
    for y in range(W):
        for x in range(W):
            r,g,b,a=px[x,y]
            px[x,y]=(r,g,b,255 if a>110 else 0)
    return im
out=sys.argv[1]
dia=make((150,245,245,255),(40,170,190,255),(235,255,255,255),(38,38,44,255),(150,150,160,255))
net=make((120,100,125,255),(52,44,56,255),(210,190,215,255),(30,28,32,255),(110,100,112,255))
dia.save(out+"/diamond_sword.png"); net.save(out+"/netherite_sword.png")
pv=Image.new("RGBA",(320*2+30,330),(40,40,46,255))
pv.paste(dia.resize((320,320),Image.NEAREST),(5,5),dia.resize((320,320),Image.NEAREST))
pv.paste(net.resize((320,320),Image.NEAREST),(345,5),net.resize((320,320),Image.NEAREST))
pv.save(sys.argv[2])
