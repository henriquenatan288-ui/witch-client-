import math, os, sys
from PIL import Image

def H(h):
    h=h.lstrip('#'); return (int(h[0:2],16),int(h[2:4],16),int(h[4:6],16),255)
K=H('0f1114'); D=H('2a2e35'); G=H('4a515c'); S=H('7d8794'); L=H('b9c2cc'); W=H('f4f8fc')
C=H('5ad6ff'); Cd=H('2a8fb5'); Cl=H('b5ecff')
GOLD=[H('7a5a1c'),H('b08a2e'),H('dcb44a'),H('f3d97e'),H('fff3c4')]
STEEL=[D,G,S,L,W]
CYAN=[H('1b5a78'),Cd,C,Cl,W]
BEEF=[H('4a2a16'),H('7a4426'),H('a8643a'),H('d18a52'),H('f0b57e')]
LIME=[H('2d6a2a'),H('4fa843'),H('8ee86a'),H('c8ffa8'),W]
TEAL=[H('12403d'),H('1f7f74'),H('3fb59a'),H('8ee8cf'),W]

class Cv:
    def __init__(s,n=16): s.n=n; s.p={}
    def set(s,x,y,c):
        x=int(x);y=int(y)
        if 0<=x<s.n and 0<=y<s.n: s.p[(x,y)]=c
    def line(s,x0,y0,x1,y1,c):
        dx=abs(x1-x0);dy=-abs(y1-y0);sx=1 if x0<x1 else -1;sy=1 if y0<y1 else -1;e=dx+dy
        while True:
            s.set(x0,y0,c)
            if x0==x1 and y0==y1: break
            e2=2*e
            if e2>=dy: e+=dy;x0+=sx
            if e2<=dx: e+=dx;y0+=sy
    def rect(s,x0,y0,x1,y1,c):
        for y in range(y0,y1+1):
            for x in range(x0,x1+1): s.set(x,y,c)
    def shaded_circle(s,cx,cy,r,ramp,lx=-0.6,ly=-0.7):
        for y in range(s.n):
            for x in range(s.n):
                dx=x-cx;dy=y-cy
                if dx*dx+dy*dy<=r*r:
                    d=math.sqrt(dx*dx+dy*dy)/r
                    nz=math.sqrt(max(0,1-d*d))
                    l=(dx/r*lx+dy/r*ly)*-1*0.6+nz*0.7
                    i=min(len(ramp)-1,max(0,int(l*len(ramp)+0.15)))
                    s.set(x,y,ramp[i])
    def outline(s,col=K):
        add=[]
        for (x,y) in list(s.p):
            for dx,dy in((1,0),(-1,0),(0,1),(0,-1)):
                q=(x+dx,y+dy)
                if q not in s.p and 0<=q[0]<s.n and 0<=q[1]<s.n: add.append(q)
        for q in add: s.p[q]=col
    def up(s,k):
        o=Cv(s.n*k)
        for (x,y),c in s.p.items():
            for i in range(k):
                for j in range(k): o.p[(x*k+i,y*k+j)]=c
        return o
    def img(s):
        im=Image.new('RGBA',(s.n,s.n),(0,0,0,0))
        for (x,y),c in s.p.items(): im.putpixel((x,y),c)
        return im

def sword(bl=None,bs=None,gem=None):
    bl=bl or L; bs=bs or S; gem=gem or C
    c=Cv()
    for t in range(8):
        x=13-t;y=2+t
        c.set(x-1,y,bl);c.set(x,y,bs)
    c.set(14,1,W);c.set(13,2,W)
    c.line(3,8,7,12,G);c.set(3,8,S);c.set(4,9,S)
    c.set(5,10,gem)
    c.line(4,11,2,13,D);c.set(4,11,G);c.set(3,12,G)
    c.set(1,14,L)
    c.outline();return c

def dsword():
    return sword(H('b4fbf6'),H('2fc4cf'),H('b4fbf6'))

def axe():
    c=Cv()
    c.line(2,14,11,5,G);c.line(2,15,11,6,D)
    for t in range(0,9,2): c.set(2+t,14-t,L)
    c.set(1,15,S)
    rows={1:(6,11),2:(5,12),3:(5,12),4:(6,12),5:(8,12),6:(10,12)}
    for y,(a,b) in rows.items():
        for x in range(a,b+1): c.set(x,y,S)
        for x in range(a,min(b,a+1)+1): c.set(x,y,L)
        c.set(b,y,G)
    for x in range(6,12): c.set(x,1,L)
    for y in (2,3,4): c.set(5 if y<4 else 6,y,C)
    c.set(6,1,Cl);c.set(5,2,Cl)
    c.set(8,4,G);c.set(9,5,D)
    c.outline();return c

def mace():
    c=Cv()
    c.line(2,14,8,8,G);c.line(2,15,8,9,D)
    for t in range(0,6,2): c.set(3+t,13-t,L)
    c.set(1,15,S);c.set(2,14,S)
    c.shaded_circle(10.5,5.5,4.3,STEEL)
    for a in range(0,360,45):
        t=math.radians(a)
        c.set(10.5+5.6*math.cos(t),5.5+5.6*math.sin(t),L if a<180 else G)
    c.set(10.5,0,W)
    for x in range(7,15): c.set(x,6,D)
    c.rect(10,5,11,6,C);c.set(10,5,Cl)
    c.outline();return c

def spear():
    c=Cv()
    c.line(1,14,10,5,G)
    for t in range(0,9,3): c.set(1+t,14-t,L)
    c.set(1,14,S);c.set(0,15,D)
    c.line(9,6,8,7,C)
    head={(14,1):W,(13,2):L,(14,2):L,(12,3):L,(13,3):S,(14,3):G,(11,4):S,(12,4):S,(13,4):G,(10,5):S,(11,5):G}
    for k,v in head.items(): c.set(k[0],k[1],v)
    c.set(10,5,C);c.set(9,6,C)
    return c

def helmet():
    c=Cv()
    for y,(a,b) in {3:(5,10),4:(4,11),5:(3,12),6:(3,12),7:(3,12),8:(3,12),9:(3,12),10:(3,5)}.items():
        for x in range(a,b+1): c.set(x,y,S)
    for y in (10,11):
        c.set(10,y,S);c.set(11,y,S);c.set(12,y,G)
    for y in range(3,10): c.set(3 if y>4 else 4,y,L)
    for x in range(5,10): c.set(x,3,L)
    c.rect(6,7,9,11,D)
    c.rect(6,8,9,8,C)
    c.set(3,11,G);c.set(4,11,G);c.set(5,11,G)
    for y in range(4,10): c.set(12 if y>4 else 11,y,G)
    c.outline();return c

def chest():
    c=Cv()
    c.rect(4,4,11,13,S);c.rect(1,3,4,7,S);c.rect(11,3,14,7,S)
    c.rect(1,8,3,10,G);c.rect(12,8,14,10,G)
    c.rect(6,2,9,3,D)
    c.rect(4,4,5,13,L);c.rect(1,3,2,7,L);c.rect(1,3,4,3,L)
    c.rect(10,4,11,13,G)
    c.rect(7,5,8,9,D);c.set(7,6,C);c.set(8,6,C);c.set(7,7,Cl)
    c.rect(4,12,11,13,G)
    c.outline();return c

def leggings():
    c=Cv()
    c.rect(3,2,12,5,S);c.rect(3,2,12,2,L)
    c.rect(3,6,6,13,S);c.rect(9,6,12,13,S)
    c.rect(3,6,4,13,L);c.rect(9,6,10,13,L)
    c.rect(6,6,6,13,G);c.rect(12,6,12,13,G)
    c.rect(7,3,8,4,D);c.set(7,3,C);c.set(8,3,C)
    c.rect(3,12,6,13,G);c.rect(9,12,12,13,G)
    c.outline();return c

def boots():
    c=Cv()
    for x0 in (1,9):
        c.rect(x0+1,5,x0+4,10,S);c.rect(x0,10,x0+6,12,S)
        c.rect(x0+1,5,x0+1,10,L);c.rect(x0,10,x0,12,L)
        c.rect(x0+4,5,x0+4,10,G);c.rect(x0,12,x0+6,12,G)
        c.rect(x0+1,5,x0+4,6,G);c.set(x0+2,6,C);c.set(x0+3,6,C)
    c.outline();return c

def totem():
    c=Cv()
    GD=GOLD
    # bracos: vertical 4 de largura, horizontal 4 de altura
    for y in range(2,14):
        for x in range(6,10):
            c.set(x,y,S if x in (7,8) else (L if x==6 else G))
    for x in range(2,14):
        for y in range(5,9):
            c.set(x,y,S if y in (6,7) else (L if y==5 else G))
    # pontas alargadas (cruz pattee)
    c.rect(5,1,10,2,S);c.rect(5,1,10,1,L);c.rect(10,1,10,2,G)
    c.rect(1,4,2,9,S);c.rect(1,4,2,4,L);c.rect(1,9,2,9,G)
    c.rect(13,4,14,9,S);c.rect(13,4,14,4,L);c.rect(13,9,14,9,G)
    c.rect(5,13,10,14,S);c.rect(5,13,10,13,L);c.rect(5,14,10,14,G)
    # filetes dourados nas pontas
    for x in range(5,11): c.set(x,1,GD[3])
    for x in range(5,11): c.set(x,14,GD[1])
    for y in range(4,10): c.set(1,y,GD[3]);c.set(14,y,GD[1])
    # ranhura no braco de baixo
    for y in range(10,13): c.set(7,y,D);c.set(8,y,G)
    # centro: joia ciano com engaste dourado
    c.rect(6,5,9,8,GD[2]);c.rect(6,5,9,5,GD[3]);c.rect(6,8,9,8,GD[1])
    c.rect(7,6,8,7,C);c.set(7,6,W);c.set(8,7,Cd)
    c.outline();return c

def crystal():
    c=Cv()
    for y in range(16):
        for x in range(16):
            v=abs(x-7.5)/5.2+abs(y-7.5)/6.8
            if v<=1:
                if x<8: col=CYAN[3] if y<8 else CYAN[2]
                else: col=CYAN[2] if y<8 else CYAN[1]
                if abs(x-7.5)<1: col=W
                if v>0.86: col=CYAN[1] if x>=8 else CYAN[2]
                c.set(x,y,col)
    for y in range(3,13):
        pass
    c.set(6,5,W);c.set(5,6,W)
    c.line(8,3,8,12,Cl)
    c.outline();return c

def pearl():
    c=Cv()
    c.shaded_circle(7.5,7.5,5.7,TEAL)
    for a in range(0,200,6):
        r=3.4*(a/200)+0.5; t=a/18.0
        c.set(7.5+r*math.cos(t),7.5+r*math.sin(t),TEAL[0])
    c.set(5,5,W);c.set(6,4,TEAL[4]);c.set(5,6,TEAL[3])
    c.outline();return c

def xp():
    c=Cv()
    c.rect(7,1,8,2,D);c.rect(7,3,8,4,L)
    c.shaded_circle(7.5,10,4.6,STEEL)
    for y in range(7,15):
        for x in range(2,14):
            dx=x-7.5;dy=y-10
            if dx*dx+dy*dy<=3.6*3.6 and y>=8:
                i=3 if (x<7 and y<11) else (2 if x<9 else 1)
                c.set(x,y,LIME[i])
    c.set(5,9,W);c.set(6,8,LIME[4]);c.set(9,12,LIME[3]);c.set(8,10,LIME[3])
    c.outline();return c

def gapple():
    c=Cv()
    c.shaded_circle(7.5,9,5.4,GOLD)
    c.set(7,4,GOLD[0]);c.set(8,4,GOLD[0])
    c.line(8,2,8,3,H('5a3a1a'))
    c.set(9,2,LIME[2]);c.set(10,2,LIME[1]);c.set(10,3,LIME[1]);c.set(9,3,LIME[0])
    c.set(5,7,GOLD[4]);c.set(5,8,GOLD[4]);c.set(6,6,GOLD[4])
    c.outline();return c

def beef():
    c=Cv()
    for y in range(16):
        for x in range(16):
            dx=(x-7.5);dy=(y-8)
            u=(dx+dy*0.5)/6.8;v=(dy-dx*0.5)/4.6
            if u*u+v*v<=1:
                d=u*u+v*v
                col=BEEF[3] if d<0.35 else (BEEF[2] if d<0.7 else BEEF[1])
                if x<7 and y<8 and d<0.8: col=BEEF[3] if d>0.3 else BEEF[4]
                c.set(x,y,col)
    for t in(-3,0,3):
        for k in range(-3,4):
            x=8+k+t*0.0;y=8+k*0.6+t*0.9
            if (int(x),int(y)) in c.p: c.set(x,y,BEEF[0])
    c.set(4,10,H('f4e2c0'));c.set(5,11,H('f4e2c0'));c.set(6,12,H('f4e2c0'))
    c.outline();return c

def bucket():
    c=Cv()
    for y in range(6,15):
        a=3.2+(y-6)*0.18; b=12.2-(y-6)*0.18
        for x in range(int(round(a)),int(round(b))+1):
            c.set(x,y,S)
        c.set(int(round(a)),y,L);c.set(int(round(a))+1,y,L if y%2 else S);c.set(int(round(b)),y,G)
    for y in (8,12):
        for x in range(3,13):
            if (x,y) in c.p: c.set(x,y,G)
    c.rect(2,6,13,6,L)
    for x in range(3,13):
        c.set(x,4,C);c.set(x,5,CYAN[1] if x>8 else C)
    for x in range(3,7): c.set(x,4,Cl)
    c.set(5,4,W)
    c.rect(2,3,13,3,L);c.set(2,3,W)
    c.line(3,3,4,1,G);c.line(4,1,11,1,G);c.line(11,1,12,3,G)
    c.set(7,1,L);c.set(8,1,L)
    c.outline();return c

def wind():
    c=Cv()
    for a in range(0,360,4):
        t=math.radians(a)
        if 20<a<150 or 200<a<330: c.set(7.5+5.6*math.cos(t),7.5+5.6*math.sin(t),L)
    for a in range(0,360,5):
        t=math.radians(a)
        if 70<a<250: c.set(7.5+3.4*math.cos(t),7.5+3.4*math.sin(t),W)
    c.rect(7,7,8,8,C)
    c.set(13,3,Cl);c.set(2,12,Cl);c.set(14,4,C);c.set(1,11,C)
    c.outline();return c

def cobweb():
    c=Cv()
    ws=H('e6edf3'); wl=(230,237,243,200)
    for dx,dy in((1,0),(0,1),(1,1),(1,-1)):
        for k in range(-8,9):
            c.set(7.5+k*dx*0.999+ (0.5 if dx else 0),7.5+k*dy*0.999,wl)
    for r in (3,6):
        for a in range(0,360,8):
            t=math.radians(a)
            c.set(8+r*math.cos(t),8+r*math.sin(t),(230,237,243,170))
    return c

ITEMS={'netherite_sword':sword,'diamond_sword':dsword,'netherite_axe':axe,'mace':mace,'netherite_helmet':helmet,'netherite_chestplate':chest,
 'netherite_leggings':leggings,'netherite_boots':boots,'totem_of_undying':totem,'end_crystal':crystal,'ender_pearl':pearl,
 'experience_bottle':xp,'golden_apple':gapple,'cooked_beef':beef,'water_bucket':bucket,'wind_charge':wind}

def build(outdir_item,outdir_block):
    os.makedirs(outdir_item,exist_ok=True);os.makedirs(outdir_block,exist_ok=True)
    for n,f in ITEMS.items(): f().img().save(f'{outdir_item}/{n}.png')
    s=spear();s16=Cv();s16.p=dict(s.p);s16.outline();s16.img().save(f'{outdir_item}/netherite_spear.png')
    s32=s.up(2);s32.outline();s32.img().save(f'{outdir_item}/netherite_spear_in_hand.png')
    cobweb().img().save(f'{outdir_block}/cobweb.png')

if __name__=='__main__':
    build(sys.argv[1],sys.argv[2])
    names=list(ITEMS)+['netherite_spear']
    m=Image.new('RGBA',(8*80,3*80),(34,38,44,255))
    for i,n in enumerate(names):
        im=Image.open(f'{sys.argv[1]}/{n}.png').convert('RGBA').resize((72,72),Image.NEAREST)
        m.alpha_composite(im,((i%8)*80+4,(i//8)*80+4))
    im=Image.open(f'{sys.argv[2]}/cobweb.png').convert('RGBA').resize((72,72),Image.NEAREST); m.alpha_composite(im,(2*80+4,2*80+4))
    im=Image.open(f'{sys.argv[1]}/netherite_spear_in_hand.png').convert('RGBA').resize((72,72),Image.NEAREST); m.alpha_composite(im,(3*80+4,2*80+4))
    m.save(sys.argv[3])
