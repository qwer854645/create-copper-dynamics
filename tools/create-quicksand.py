"""Create quicksand data and pixel-art fluid material, with no copied sanding recipes."""
import json
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
R=ROOT/'src/main/resources'
def save(path,data):
    path.parent.mkdir(parents=True,exist_ok=True); path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
save(R/'data/ccd/recipe/mixing/quicksand.json',{'type':'create:mixing','ingredients':[{'item':'minecraft:sand'}],
    'results':[{'id':'ccd:quicksand','amount':1000}],'processing_time':100})
save(R/'assets/ccd/models/item/quicksand_bucket.json',{'parent':'minecraft:item/generated','textures':{'layer0':'ccd:item/quicksand_bucket'}})
save(R/'assets/ccd/blockstates/quicksand.json',{'variants':{'':{'model':'minecraft:block/water'}}})
for name,size in [('quicksand_still',16),('quicksand_flow',32)]:
    image=Image.new('RGBA',(size,size*4))
    palette=[(173,145,89,255),(192,163,105,255),(211,185,129,255),(222,200,151,255)]
    for frame in range(4):
        for y in range(size):
            for x in range(size):
                # Connected sand ripples advect between frames, with integer pixels only.
                index=((x//3+(y+frame)//2+(x+y)//7)%4)
                image.putpixel((x,y+frame*size),palette[index])
    p=R/f'assets/ccd/textures/block/{name}.png'; p.parent.mkdir(parents=True,exist_ok=True); image.save(p)
    save(p.with_suffix('.png.mcmeta'),{'animation':{'frametime':6}})
for locale,name in [('zh_cn','流沙'),('en_us','Quicksand')]:
    p=R/f'assets/ccd/lang/{locale}.json'; data=json.loads(p.read_text(encoding='utf-8'))
    data.update({'fluid_type.ccd.quicksand':name,'fluid.ccd.quicksand':name,'fluid.ccd.flowing_quicksand':name,
        'block.ccd.quicksand':name,'item.ccd.quicksand_bucket':name+'桶' if locale=='zh_cn' else 'Quicksand Bucket'})
    save(p,data)
