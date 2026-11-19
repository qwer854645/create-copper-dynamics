"""Recolour the existing bucket silhouette using Create brass colours; keep alpha."""
import json
from pathlib import Path
from PIL import Image

root=Path(__file__).resolve().parents[1]
source=Image.open(root/'src/main/resources/assets/ccd/textures/item/copper_bucket.png').convert('RGBA')
palette=[(73,47,27),(114,71,49),(164,107,71),(191,142,85),(215,170,94),(228,183,99),(247,203,108),(251,220,125)]
tones=sorted({p[:3] for p in source.getdata() if p[3] and p[0]>p[1]>p[2]},key=lambda p:sum(p))
mapping={p:palette[round(i*(len(palette)-1)/max(1,len(tones)-1))] for i,p in enumerate(tones)}
image=source.copy()
for y in range(image.height):
    for x in range(image.width):
        pixel=source.getpixel((x,y))
        if pixel[:3] in mapping: image.putpixel((x,y),mapping[pixel[:3]]+(pixel[3],))
image.save(root/'src/main/resources/assets/ccd/textures/item/brass_bucket.png')
for locale,name,filled,summary in [
    ('zh_cn','黄铜桶','装有%s的黄铜桶','可携带_八桶_同种流体的大容量黄铜桶。'),
    ('en_us','Brass Bucket','Brass Bucket of %s','A large bucket that holds _Eight Buckets_ of a single fluid.')]:
    path=root/f'src/main/resources/assets/ccd/lang/{locale}.json'
    data=json.loads(path.read_text(encoding='utf-8'))
    for key,value in list(data.items()):
        if key.startswith('item.ccd.copper_bucket.tooltip.'):
            data[key.replace('copper_bucket','brass_bucket')]=value
    data['item.ccd.brass_bucket']=name
    data['item.ccd.brass_bucket.filled']=filled
    data['item.ccd.brass_bucket.tooltip.summary']=summary
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
