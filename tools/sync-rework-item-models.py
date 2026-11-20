"""Synchronize changed static item pieces with their live block/partial models."""
import copy,json
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd/models'
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def save(p,d):p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
# The frame item had its own stale copy of the pre-import geometry.
path=root/'item/copper_water_supply_frame.json'
old=read(path)
save(path,{'parent':'ccd:block/copper_water_supply/frame','display':old.get('display',{})})
# Grate item must use the same imported body plus the external filter plate.
path=root/'item/smart_collection_grate.json';old=read(path)
body=read(root/'block/smart_collection_grate.json')
panel=read(root/'block/smart_collection_grate_filter.json')
old['elements']=copy.deepcopy(body['elements'])+copy.deepcopy(panel['elements'])
old['textures']={**body['textures'],**panel['textures']}
save(path,old)
path=root/'item/fluid_arm.json';old=read(path);old['render_type']='minecraft:cutout'
# The inventory model positions the syringe differently. Keep its joint pose,
# but use alpha testing so its existing glass is not rendered as solid black.
save(path,old)
