"""Recolore a textura da entidade do Wind Charge e as particulas 'gust' para o ciano do Witch.
Uso: python3 -I recolor_wind.py <pasta_com_assets_extraidos_do_jar> <pasta_resources_destino>"""
import os, sys
from PIL import Image
import numpy as np

src, dst = sys.argv[1], sys.argv[2]
RAMP = np.array([[27, 90, 120], [42, 143, 181], [90, 214, 255], [181, 236, 255], [244, 248, 252]], dtype=np.float32)

def recolor(path, out):
    im = Image.open(path).convert('RGBA')
    a = np.asarray(im).astype(np.float32)
    lum = (0.299 * a[..., 0] + 0.587 * a[..., 1] + 0.114 * a[..., 2]) / 255.0
    pos = np.clip(lum, 0, 1) * (len(RAMP) - 1)
    i0 = np.floor(pos).astype(int); i1 = np.minimum(i0 + 1, len(RAMP) - 1); f = (pos - i0)[..., None]
    rgb = RAMP[i0] * (1 - f) + RAMP[i1] * f
    a[..., :3] = rgb
    os.makedirs(os.path.dirname(out), exist_ok=True)
    Image.fromarray(a.astype('uint8'), 'RGBA').save(out)

recolor(f'{src}/assets/minecraft/textures/entity/projectiles/wind_charge.png',
        f'{dst}/assets/minecraft/textures/entity/projectiles/wind_charge.png')
for n in range(12):
    p = f'{src}/assets/minecraft/textures/particle/gust_{n}.png'
    if os.path.exists(p):
        recolor(p, f'{dst}/assets/minecraft/textures/particle/gust_{n}.png')
