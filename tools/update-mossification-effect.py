"""Use vanilla flowering azalea artwork for the status icon; update effect help."""
from pathlib import Path
from zipfile import ZipFile
import json
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
with ZipFile(next((root/'build/moddev/artifacts').glob('*minecraft-resources.jar'))) as z:
    icon=assets/'textures/mob_effect/mossification.png'
    icon.parent.mkdir(parents=True,exist_ok=True)
    icon.write_bytes(z.read('assets/minecraft/textures/block/flowering_azalea_side.png'))
for locale,name,description in [
 ('zh_cn','苔化','给予_苔化_：持续期间接触原版苔藓可替换方块时，将其化为苔藓，并按骨粉催生苔藓的规则扩散与生长植被。雾化后持续时间更短。'),
 ('en_us','Mossification','Grants _Mossification_: touching vanilla moss-replaceable blocks turns them into moss and triggers vanilla bonemeal spread and vegetation. Atomized exposure lasts less time.')]:
    p=assets/f'lang/{locale}.json';d=json.loads(p.read_text(encoding='utf-8'))
    d['effect.ccd.mossification']=name
    d['item.ccd.moss_suspension_bucket.tooltip.behaviour2']=description
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
