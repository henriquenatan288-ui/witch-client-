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
    src=im.copy(); sp=src.load()
    for y in range(W):
        for x in range(W):
            r,g,b,a=sp[x,y]
            if a==0: continue
            if (r,g,b)[:3] in (blade_hi[:3], blade_lo[:3]):
                t=min(1.0,max(0.0,((x+(W-y))-16)/40.0))
                base=blade_hi if (r,g,b)==blade_hi[:3] else blade_lo
                top=tuple(min(255,int(c*1.15)) for c in base[:3]); bot=tuple(int(c*0.55) for c in base[:3])
                px[x,y]=tuple(int(top[i]*(1-t)+bot[i]*t) for i in range(3))+(255,)
            edge=any(0<=x+dx<W and 0<=y+dy<W and sp[x+dx,y+dy][3]==0 for dx,dy in((1,0),(-1,0),(0,1),(0,-1)))
            if edge:
                cr,cg,cb,_=px[x,y]; px[x,y]=(int(cr*0.5),int(cg*0.5),int(cb*0.55),255)
    return im.transpose(Image.TRANSPOSE).transpose(Image.ROTATE_180)
out=sys.argv[1]
dia=make((150,245,245,255),(40,170,190,255),(235,255,255,255),(38,38,44,255),(150,150,160,255))
net=make((235,120,200,255),(90,40,120,255),(255,210,240,255),(30,28,32,255),(150,120,160,255))
dia.save(out+"/diamond_sword.png"); net.save(out+"/netherite_sword.png")
pv=Image.new("RGBA",(320*2+30,330),(40,40,46,255))
pv.paste(dia.resize((320,320),Image.NEAREST),(5,5),dia.resize((320,320),Image.NEAREST))
pv.paste(net.resize((320,320),Image.NEAREST),(345,5),net.resize((320,320),Image.NEAREST))
pv.save(sys.argv[2])
