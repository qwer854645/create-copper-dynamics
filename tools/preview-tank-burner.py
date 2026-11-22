"""Offline preview of actual compressed native cage OBJ and tank model."""
from pathlib import Path
import json,importlib.util
import numpy as np
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('preview',root/'tools/preview-smart-grate-indicators.py')
p=importlib.util.module_from_spec(spec);spec.loader.exec_module(p)
# Tank is rendered using its exact JSON/UVs, with the cage drawn over it in depth order.
image=p.render('ccd:block/tank_blaze_burner/tank')
obj=(root/'src/main/resources/assets/ccd/models/block/tank_blaze_burner/cage.obj').read_text()
verts=[];uv=[];polygons=[]
camera=np.array([26,24,38],float);camera/=np.linalg.norm(camera)
right=np.cross([0,1,0],camera);right/=np.linalg.norm(right);up=np.cross(camera,right)
texture=Image.open(p.asset('create:block/blaze_heater_brazier','textures')).convert('RGBA')
for line in obj.splitlines():
    a=line.split()
    if not a:continue
    if a[0]=='v':verts.append(np.array(list(map(float,a[1:4])))*16)
    if a[0]=='vt':uv.append(np.array([float(a[1])*texture.width,(1-float(a[2]))*texture.height]))
    if a[0]=='f':
        indices=[tuple(int(v) for v in token.split('/')[:2]) for token in a[1:]]
        points=np.array([verts[v-1] for v,t in indices]);coords=np.array([uv[t-1] for v,t in indices])
        normal=np.cross(points[1]-points[0],points[2]-points[0])
        if normal@camera<=0:continue
        q=np.array([[240+(v-[8,8,8])@right*19,245-(v-[8,8,8])@up*19] for v in points])
        for i in range(1,len(points)-1):
            tri=[0,i,i+1]
            try:coeff=np.linalg.solve(np.c_[q[tri],np.ones(3)],coords[tri]).T.flatten()
            except np.linalg.LinAlgError:continue
            tile=texture.transform(image.size,Image.Transform.AFFINE,coeff,Image.Resampling.NEAREST)
            mask=Image.new('L',image.size);ImageDraw.Draw(mask).polygon([tuple(v) for v in q[tri]],fill=255)
            tile.putalpha(Image.fromarray(np.minimum(np.asarray(mask),np.asarray(tile.getchannel('A')))))
            polygons.append((points[tri].mean(axis=0)@camera,tile))
for _,tile in sorted(polygons,key=lambda f:f[0]):image.alpha_composite(tile)
out=root/'outputs/tank-blaze-burner';out.mkdir(parents=True,exist_ok=True)
image.save(out/'preview.png')
