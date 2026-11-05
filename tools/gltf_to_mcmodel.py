"""Converte o karambit (glTF exportado do Blockbench, cuboides com rotacao em X multipla de 22.5) em modelo de item JSON do Minecraft.
Uso: gltf_to_mcmodel.py MODEL.gltf TEX.png RES_DIR [SIGN]"""
import sys,json,base64,struct,math,os,shutil
src,tex,R=sys.argv[1:4]; SIGN=float(sys.argv[4]) if len(sys.argv)>4 else 1.0
g=json.load(open(src)); buf=g['buffers'][0]['uri']; data=base64.b64decode(buf.split(',',1)[1])
def acc(i):
    a=g['accessors'][i]; bv=g['bufferViews'][a['bufferView']]
    n={'VEC3':3,'VEC2':2,'SCALAR':1}[a['type']]; off=bv.get('byteOffset',0)+a.get('byteOffset',0)
    return [struct.unpack_from('<'+'f'*n,data,off+k*n*4) for k in range(a['count'])]
def M(v): return (v[2]*16,v[1]*16,-v[0]*16)   # (x,y,z)->(z,y,-x): lamina passa pro plano XY do item
cubes=[]
for nd in g['nodes']:
    if 'mesh' not in nd: continue
    pr=g['meshes'][nd['mesh']]['primitives'][0]
    P=acc(pr['attributes']['POSITION']); N=acc(pr['attributes']['NORMAL']); UV=acc(pr['attributes']['TEXCOORD_0'])
    T=nd['translation']; q=nd.get('rotation',[0,0,0,1]); ang=math.degrees(2*math.atan2(q[0],q[3]))
    mn=[min(p[i] for p in P) for i in range(3)]; mx=[max(p[i] for p in P) for i in range(3)]
    a=M([T[i]+mn[i] for i in range(3)]); b=M([T[i]+mx[i] for i in range(3)]); o=M(T)
    fr=[min(a[i],b[i]) for i in range(3)]; to=[max(a[i],b[i]) for i in range(3)]
    faces={}
    for f in range(6):
        vs=range(f*4,f*4+4); n=N[f*4]; nx,ny,nz=n[2],n[1],-n[0]
        d=max(((abs(nx),'x'),(abs(ny),'y'),(abs(nz),'z')))[1]
        name={'x':'east' if nx>0 else 'west','y':'up' if ny>0 else 'down','z':'south' if nz>0 else 'north'}[d]
        us=[UV[i][0]*16 for i in vs]; vv=[(1-UV[i][1])*16 for i in vs]
        faces[name]={"uv":[min(us),min(vv),max(us),max(vv)],"texture":"#0"}
    e={"from":fr,"to":to,"faces":faces}
    if abs(ang)>0.1: e["rotation"]={"angle":-SIGN*ang,"axis":"z","origin":o}
    cubes.append(e)
allp=[c["from"] for c in cubes]+[c["to"] for c in cubes]
cen=[(min(p[i] for p in allp)+max(p[i] for p in allp))/2 for i in range(3)]
sh=[8-cen[i] for i in range(3)]
for e in cubes:
    e["from"]=[round(e["from"][i]+sh[i],4) for i in range(3)]; e["to"]=[round(e["to"][i]+sh[i],4) for i in range(3)]
    if "rotation" in e: e["rotation"]["origin"]=[round(e["rotation"]["origin"][i]+sh[i],4) for i in range(3)]
mdl={"parent":"minecraft:item/handheld","textures":{"0":"witch:item/karambit","particle":"witch:item/karambit"},"elements":cubes,
 "display":{"thirdperson_righthand":{"rotation":[0,-90,55],"translation":[0,2,1],"scale":[0.4,0.4,0.4]},
  "firstperson_righthand":{"rotation":[0,-90,25],"translation":[1.5,3.0,1.0],"scale":[0.3,0.3,0.3]},
  "gui":{"rotation":[30,225,0],"translation":[0,0,0],"scale":[0.38,0.38,0.38]}}}
os.makedirs(R+"/assets/witch/models/item",exist_ok=True); os.makedirs(R+"/assets/witch/textures/item",exist_ok=True); os.makedirs(R+"/assets/minecraft/items",exist_ok=True)
shutil.copy(tex,R+"/assets/witch/textures/item/karambit.png")
json.dump(mdl,open(R+"/assets/witch/models/item/karambit.json","w"))
for n in ("diamond_sword","netherite_sword"):
    json.dump({"model":{"type":"minecraft:model","model":"witch:item/karambit"}},open(f"{R}/assets/minecraft/items/{n}.json","w"))
print(len(cubes),"cuboides; range",[round(min(p[i] for p in allp)+sh[i],1) for i in range(3)],[round(max(p[i] for p in allp)+sh[i],1) for i in range(3)])
