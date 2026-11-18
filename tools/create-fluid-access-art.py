"""Pixel-art access devices: CCD panels, stock-link inspired sockets, native Create pipe mouths."""
import json
import math
import sys
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0,str(Path.home()/'.config/opencode/skills/pixel-art/scripts'))
from pxl import Canvas

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/ccd'
OUT=ROOT/'outputs/fluid-access'
JAR=next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
COPPER=['#793b2b','#904931','#9a5038','#a75a40','#b26247','#c26b4c','#d67b5b','#e3826c']
BRASS=['#724731','#a46b47','#af794f','#c79758','#d7aa5e','#e4b763','#f7cb6c','#fbdc7d']
IRON=['#353438','#4f4f4f','#686d65','#818783','#b4b5a7']
# Original panel clusters. Edge folds and small broad highlights, no tiled noise.
PANEL=[
 '0133333333333310','1676666556665661','1655444555544541','1554434555445541',
 '1544334455555541','1544334555444541','1554455554433541','1555555444334541',
 '1555544443345541','1544444554455541','1544455555444541','1554555544433541',
 '1655554444334541','1554444334445541','1233333333333321','0011111111111100']

def save(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def texture(name,ramp,socket=False,band=False):
    c=Canvas(16,16)
    for y,row in enumerate(PANEL):
        for x,index in enumerate(row): c.set(x,y,ramp[int(index)])
    if socket:
        # Original 10x10 socket face: folded metal, recessed inner wall,
        # connected highlight clusters and a clean dark central opening.
        # Actual stock_link iron palette, including its warm silver and grey-green shadow.
        metal=['#2b2b2e','#3e3d41','#535751','#696962','#848277','#9f9b93','#bcb6ae']
        socket_rows=[
            '0235555430',
            '2566655542',
            '3542222452',
            '3521001242',
            '3420001232',
            '3420001232',
            '3521001242',
            '3542222342',
            '2454444321',
            '0122222210',
        ]
        for y,row in enumerate(socket_rows,3):
            for x,index in enumerate(row,3): c.set(x,y,metal[int(index)])
    if band:
        # Connected-like broad recessed side strap, redrawn in dark copper.
        top,bottom=(5,10) if name=='bridge_band' else (8,12)
        c.rect(1,top,14,bottom-top+1,'#493022')
        c.line_h(1,14,top,'#694531')
        c.line_h(1,14,bottom,'#32241c')
        for x,y,w in [(2,8,3),(7,9,4),(11,8,2),(4,10,2)]:
            c.line_h(x,x+w-1,y if name=='bridge_band' else y+1,'#593b28')
        # Small dark indicator recesses, not false lit connection states.
        if name=='bridge_band':
            c.rect(5,3,6,2,'#502a22'); c.rect(5,11,6,2,'#502a22')
        else: c.rect(6,5,4,3,'#502a22')
    path=ASSETS/f'textures/block/fluid_access/{name}.png'
    path.parent.mkdir(parents=True,exist_ok=True); c.save(str(path))

def cube(name,lo,hi,material):
    x,y,z=lo; X,Y,Z=hi
    uvs={'north':[16-X,16-Y,16-x,16-y],'south':[x,16-Y,X,16-y],
         'east':[16-Z,16-Y,16-z,16-y],'west':[z,16-Y,Z,16-y],
         'up':[x,z,X,Z],'down':[x,16-Z,X,16-z]}
    return {'name':name,'from':lo,'to':hi,'faces':{s:{'texture':'#'+material,'uv':uv} for s,uv in uvs.items()}}

def pipe(z,reverse,archive):
    result=[]
    for part in ['rim_connector','rim']:
        model=json.loads(archive.read(f'assets/create/models/block/fluid_pipe/{part}/south.json'))
        for e in model['elements']:
            e['name']='native_'+part
            e.pop('rotation',None)
            for key in ['from','to']:
                e[key][2]+=z-12
                if reverse: e[key][2]=16-e[key][2]
            e['from'][2],e['to'][2]=sorted([e['from'][2],e['to'][2]])
            if reverse:
                e['faces']={({'north':'south','south':'north'}.get(s,s)):f for s,f in e['faces'].items()}
            for f in e['faces'].values():
                f['texture']='#pipe'; f.pop('cullface',None)
            result.append(e)
    return result

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    for material,ramp in [('copper',COPPER),('brass',BRASS)]:
        texture(material,ramp); texture(material+'_socket',ramp,True)
    texture('copper_band',COPPER,band=True)
    texture('bridge_band',COPPER,band=True)
    # User-requested direct reuse: retain Connected's connector pixels exactly.
    # Recolor only its surrounding brass; source assets and license are retained.
    for source,target in [('inventory_access_port_front','connected_port'),('inventory_bridge_front','connected_bridge')]:
        original=Image.open(ROOT/f'tools/references/connected/{source}.png').convert('RGBA')
        converted=original.copy()
        for y in range(16):
            for x in range(16):
                if 3<=x<=12 and 3<=y<=12: continue
                r,g,b,a=original.getpixel((x,y))
                if r>g and g>b:
                    index=min(7,max(0,round((r-75)/180*7)))
                    color=COPPER[index]
                    converted.putpixel((x,y),tuple(bytes.fromhex(color[1:]))+(a,))
        converted.save(ASSETS/f'textures/block/fluid_access/{target}.png')
    models=[]
    with ZipFile(JAR) as archive:
        for name,bridge,material in [('fluid_access_port',False,'copper'),('fluid_bridge',True,'copper')]:
            elements=[cube('casing',[0,0,0],[16,16,16],'body')]
            for side in ['east','west']:
                elements[0]['faces'][side]['texture']='#band'
            # Recessed sockets at source ends, kept inside a single block.
            ends=['south','north'] if bridge else ['south']
            for end in ends:
                elements[0]['faces'][end]['texture']='#socket'
                # Native pipe mouth at the surface; the narrow collar is inset into the case.
                # Connected-like raised square connector; no additional pipe flange.
                lo,hi=([3,3,16],[13,13,18]) if end=='south' else ([3,3,-2],[13,13,0])
                seat=cube('raised_socket_'+end,lo,hi,'socket')
                seat['faces'][end]={'texture':'#socket','uv':[3,3,13,13]}
                elements.append(seat)
            textures={'body':f'ccd:block/fluid_access/{material}','socket':'ccd:block/fluid_access/'+('connected_bridge' if bridge else 'connected_port'),
                      'band':'ccd:block/fluid_access/'+('bridge_band' if bridge else 'copper_band'),
                       'particle':'#body'}
            model={'parent':'minecraft:block/block','ambientocclusion':False,'textures':textures,'elements':elements}
            save(ASSETS/f'models/block/{name}.json',model)
            save(ASSETS/f'models/item/{name}.json',{'parent':f'ccd:block/{name}','display':{'gui':{'rotation':[30,45,0],'scale':[.65,.65,.65]}}})
            models.append(model)
        render_preview(models,archive)
    atlas=Image.new('RGBA',(64,16))
    for i,name in enumerate(['copper','connected_port','connected_bridge','bridge_band']):
        atlas.alpha_composite(Image.open(ASSETS/f'textures/block/fluid_access/{name}.png'),(i*16,0))
    atlas.save(OUT/'textures-source.png'); atlas.resize((1024,256),Image.Resampling.NEAREST).save(OUT/'textures.png')
    save(OUT/'model.meta.json',{'reference':'Connected full casing, raised end sockets and broad side band; Create stock-link metal shading and native filter',
         'textures':'CCD hand-drawn 16x16 copper panels, dark copper band and grey metal socket seats',
           'upstreamResources':['Connected inventory_access_port_front and inventory_bridge_front (AGPL-3.0 plus additional terms)'],
           'filterPosition':'center of each connector face; no separate panel geometry',
          'connectedAssetsCopied':True,'preview':'actual model geometry, offline lighting; no client validation'})

def render_preview(models,archive):
    def texture(id):
        ns,p=id.split(':')
        return Image.open(ASSETS/f'textures/{p}.png').convert('RGBA') if ns=='ccd' else Image.open(BytesIO(archive.read(f'assets/{ns}/textures/{p}.png'))).convert('RGBA')
    def render(model,view):
        def dot(a,b): return sum(x*y for x,y in zip(a,b))
        camera=[v/math.sqrt(dot(view,view)) for v in view]
        r=math.hypot(camera[0],camera[2]); right=[camera[2]/r,0,-camera[0]/r]; up=[-camera[1]*camera[0]/r,r,-camera[1]*camera[2]/r]
        def project(p):
            p=[v-8 for v in p]; return (210+dot(p,right)*15,205-dot(p,up)*15)
        atlas={k:texture(v) for k,v in model['textures'].items() if not v.startswith('#')}
        tiles=[]
        for e in model['elements']:
            x,y,z=e['from']; X,Y,Z=e['to']
            vertices={'north':([[X,Y,z],[X,y,z],[x,y,z],[x,Y,z]],[0,0,-1]),'south':([[x,Y,Z],[x,y,Z],[X,y,Z],[X,Y,Z]],[0,0,1]),
                      'east':([[X,Y,Z],[X,y,Z],[X,y,z],[X,Y,z]],[1,0,0]),'west':([[x,Y,z],[x,y,z],[x,y,Z],[x,Y,Z]],[-1,0,0]),
                      'up':([[x,Y,z],[x,Y,Z],[X,Y,Z],[X,Y,z]],[0,1,0]),'down':([[x,y,Z],[x,y,z],[X,y,z],[X,y,Z]],[0,-1,0])}
            for side,f in e['faces'].items():
                points,n=vertices[side]
                if dot(camera,n)<=0: continue
                image=atlas[f['texture'][1:]]; u,v,U,V=f['uv']; cols=max(1,round(abs(U-u)*image.width/16)); rows=max(1,round(abs(V-v)*image.height/16))
                rotation=f.get('rotation',0)//90
                for row in range(rows):
                    for col in range(cols):
                        def point(a,b): return [points[0][i]+(points[3][i]-points[0][i])*a+(points[1][i]-points[0][i])*b for i in range(3)]
                        poly=[point(col/cols,row/rows),point(col/cols,(row+1)/rows),point((col+1)/cols,(row+1)/rows),point((col+1)/cols,row/rows)]
                        a,b=(col+.5)/cols,(row+.5)/rows
                        for _ in range(rotation): a,b=1-b,a
                        tx=min(image.width-1,max(0,int((u+(U-u)*a)*image.width/16))); ty=min(image.height-1,max(0,int((v+(V-v)*b)*image.height/16)))
                        color=image.getpixel((tx,ty))
                        if color[3]<128: continue
                        light={'up':1,'down':.6,'north':.88,'south':.85,'east':.73,'west':.78}[side]
                        tiles.append((sum(dot(p,camera) for p in poly)/4,[project(p) for p in poly],tuple(round(c*light) for c in color[:3])+(255,)))
        image=Image.new('RGBA',(420,410)); draw=ImageDraw.Draw(image)
        for _,poly,color in sorted(tiles,key=lambda t:t[0]): draw.polygon(poly,fill=color)
        return image
    sheet=Image.new('RGBA',(1260,960),'#e5e8e2'); draw=ImageDraw.Draw(sheet)
    font=ImageFont.truetype(r'C:\Windows\Fonts\msyh.ttc',20)
    for row,model in enumerate(models):
        draw.text((630,row*480+24),'流体扩展端口 · 铜壳与单端连接座' if row==0 else '流体桥接器 · 铜壳与双端连接座',font=font,fill='#303535',anchor='mm')
        for col,(view,label) in enumerate([([25,17,34],'连接面'),([-30,18,-32],'另一侧'),([0,0,40],'端面')]):
            sheet.alpha_composite(render(model,view),(col*420,row*480+35))
            draw.text((col*420+210,row*480+456),label,font=font,fill='#303535',anchor='mm')
    sheet.save(OUT/'preview.png')

if __name__=='__main__': main()
