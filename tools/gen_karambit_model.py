"""Gera modelos 3D (cuboides) do karambit + textura de paleta. Uso: gen_karambit_model.py RES_DIR"""
import sys, json, math, os
from PIL import Image
R=sys.argv[1]
def palette(path, hi, lo, handle, ring):
    im=Image.new("RGBA",(16,16),(0,0,0,0))
    for i,c in enumerate([hi,lo,handle,ring]):
        for x in range(i*4,i*4+4):
            for y in range(0,4): im.putpixel((x,y),c+(255,))
    im.save(path)
def U(i): return [i*4+1,1,i*4+3,3]
def face(i): return {"uv":U(i),"texture":"#0"}
def cub(frm,to,i,rot=None,faces=None):
    e={"from":frm,"to":to,"faces":{k:face(i) for k in ["north","south","east","west","up","down"]}}
    if rot: e["rotation"]={"angle":rot[1],"axis":"z","origin":rot[0]}
    return e
Z0,Z1=7.3,8.7
def build():
    els=[]
    # (direcao, comprimento) da lamina: parte vertical e depois horizontal (limite de 45 graus por cuboide)
    path=[(90,2.4),(67.5,2.4),(45,2.4),(22.5,2.2),(0,2.0),(-22.5,1.8),(-45,1.4)]
    x,y=4.2,6.4
    t=1.5
    for k,(d,L) in enumerate(path):
        r=math.radians(d); mx=x+math.cos(r)*L/2; my=y+math.sin(r)*L/2
        thick=t*(1-0.1*k)
        if d>=45:
            frm=[mx-thick/2,my-L/2,Z0]; to=[mx+thick/2,my+L/2,Z1]; phi=d-90
        else:
            frm=[mx-L/2,my-thick/2,Z0]; to=[mx+L/2,my+thick/2,Z1]; phi=d
        els.append(cub(frm,to,1 if k%2 else 0,rot=([mx,my,8],phi) if abs(phi)>0.01 else None))
        # fio (lado interno) mais claro
        x+=math.cos(r)*L; y+=math.sin(r)*L
    # cabo e anel
    els.append(cub([3.2,2.0,Z0],[5.2,6.6,Z1],2))
    for (a,b,c,d_) in [(1.2,0.2,5.6,1.2),(1.2,4.0,5.6,5.0),(1.2,0.2,2.2,5.0),(4.6,0.2,5.6,5.0)]:
        pass
    els.append(cub([1.0,0.0,Z0+0.2],[5.8,1.0,Z1-0.2],3))
    els.append(cub([1.0,0.0,Z0+0.2],[2.0,4.6,Z1-0.2],3))
    els.append(cub([4.8,0.0,Z0+0.2],[5.8,2.0,Z1-0.2],3))
    els.append(cub([1.0,3.6,Z0+0.2],[3.2,4.6,Z1-0.2],3))
    return els
def model(tex):
    return {"parent":"minecraft:item/handheld","textures":{"0":tex,"particle":tex},"elements":build(),
      "display":{"thirdperson_righthand":{"rotation":[0,-90,55],"translation":[0,2,1],"scale":[0.9,0.9,0.9]},
                 "firstperson_righthand":{"rotation":[0,-90,25],"translation":[1.5,4.6,1.0],"scale":[0.42,0.42,0.42]},
                 "gui":{"rotation":[30,225,0],"translation":[0,0,0],"scale":[0.9,0.9,0.9]}}}
os.makedirs(R+"/assets/witch/models/item",exist_ok=True); os.makedirs(R+"/assets/witch/textures/item",exist_ok=True)
os.makedirs(R+"/assets/minecraft/items",exist_ok=True)
for name,pal in [("diamond",((150,245,245),(40,170,190),(38,38,44),(150,150,160))),("netherite",((170,150,175),(70,60,76),(34,32,38),(120,110,124)))]:
    palette(f"{R}/assets/witch/textures/item/karambit_{name}.png",*pal)
    json.dump(model(f"witch:item/karambit_{name}"),open(f"{R}/assets/witch/models/item/karambit_{name}.json","w"),indent=1)
    json.dump({"model":{"type":"minecraft:model","model":f"witch:item/karambit_{name}"}},open(f"{R}/assets/minecraft/items/{name}_sword.json","w"))
