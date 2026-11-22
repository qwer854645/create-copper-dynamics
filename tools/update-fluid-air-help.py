import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd/lang'
for locale in ['zh_cn','en_us']:
    path=root/(locale+'.json');data=json.loads(path.read_text(encoding='utf-8'))
    data['item.ccd.explosive_slurry_bucket.tooltip.behaviour2']='施加_中毒_，燃烧中的生物触发不破坏方块的_小爆炸_。' if locale=='zh_cn' else 'Applies _Poison_. Burning entities trigger a _small explosion_ that leaves blocks intact.'
    data['item.ccd.purifying_solution_bucket.tooltip.behaviour2']='移除_有害效果_并灭火，每 5 秒缓慢恢复_半颗心_。' if locale=='zh_cn' else 'Removes _harmful effects_, extinguishes fire and restores _half a heart_ every 5 seconds.'
    for id in ['slime','haunting','quicksand','enriching_reagent','soul_stripping_reagent','explosive_slurry','resonant_solution','sculk_suspension','purifying_solution']:
        data[f'item.ccd.{id}_bucket.tooltip.condition2']='接触流体或受其气流影响时' if locale=='zh_cn' else 'When touching the fluid or exposed to its air current'
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
