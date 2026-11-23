"""Read-only lever fit illustrations using current item models and vanilla lever geometry."""
from pathlib import Path
import importlib.util
import numpy as np
from PIL import Image, ImageDraw, ImageFont
root=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('preview',root/'tools/preview-smart-grate-indicators.py')
p=importlib.util.module_from_spec(spec);spec.loader.exec_module(p)
lever=p.model('minecraft:block/lever')
extra=[]
for e in lever['elements']:
    x,y,z=e['from'];X,Y,Z=e['to']
    vertices={'down':[(x,y,Z),(x,y,z),(X,y,z),(X,y,Z)],'up':[(x,Y,z),(x,Y,Z),(X,Y,Z),(X,Y,z)],
      'north':[(X,Y,z),(X,y,z),(x,y,z),(x,Y,z)],'south':[(x,Y,Z),(x,y,Z),(X,y,Z),(X,Y,Z)],
      'west':[(x,Y,z),(x,y,z),(x,y,Z),(x,Y,Z)],'east':[(X,Y,Z),(X,y,Z),(X,y,z),(X,Y,z)]}
    for side,face in e['faces'].items():
        points=np.array(vertices[side],float)
        rotation=e.get('rotation',{})
        if rotation.get('angle'):
            origin=np.array(rotation['origin']);points-=origin
            angle=np.deg2rad(rotation['angle']);c,s=np.cos(angle),np.sin(angle)
            yy,zz=points[:,1].copy(),points[:,2].copy()
            points[:,1]=yy*c-zz*s;points[:,2]=yy*s+zz*c;points+=origin
        # Floor lever -> east wall, normal +X; attachment plane stays at block X=16.
        points=np.array([(16+v[1],16-v[0],v[2]) for v in points])
        texture=lever['textures'][face['texture'][1:]]
        if ':' not in texture:texture='minecraft:'+texture
        image=Image.open(p.asset(texture,'textures')).convert('RGBA')
        u,v,U,V=face['uv'];uv=np.array([(u,v),(u,V),(U,V),(U,v)])*[image.width/16,image.height/16]
        extra.append((points,uv,image))
names=[('redstone_fluid_pump','红石双向动力泵：侧面轴端触边，底座周围无平面'),
       ('three_way_valve','三通阀：侧面表盘距边界约 2 像素'),
       ('redstone_fluid_valve','红石流体阀门：侧面距边界 1 像素'),
       ('variable_flow_pump','加宽调频泵：侧面机壳到边界，覆盖底座范围')]
out=root/'outputs/redstone-lever-fit';out.mkdir(parents=True,exist_ok=True)
sheet=Image.new('RGBA',(1100,len(names)*540),'#e5e8e2')
font=ImageFont.truetype(r'C:\Windows\Fonts\msyh.ttc',19)
for i,(name,label) in enumerate(names):
    sheet.paste(p.render('ccd:item/'+name),(0,i*540+35))
    sheet.paste(p.render('ccd:item/'+name,extra),(540,i*540+35))
    d=ImageDraw.Draw(sheet);d.text((20,i*540+5),label,font=font,fill='#303535')
    d.text((560,i*540+5),'原版拉杆位于真实方块边界（外观示意）',font=font,fill='#303535')
    p.render('ccd:item/'+name,extra).save(out/(name+'.png'))
sheet.save(out/'comparison.png')
print(out/'comparison.png')
