"""Keep player-facing explanations concise and terminology consistent."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
for locale in ['zh_cn','en_us']:
    p=root/f'src/main/resources/assets/ccd/lang/{locale}.json'
    d=json.loads(p.read_text(encoding='utf-8-sig'));zh=locale=='zh_cn'
    addons={'enriching_reagent':'Create Nuclear','soul_stripping_reagent':'Create Nether Industry',
        'explosive_slurry':'Create: More Catalysts','resonant_solution':'Create: More Catalysts',
        'sculk_suspension':'Create: More Catalysts','purifying_solution':'Create: More Catalysts',
        'withering_fluid':'Create: More Catalysts','conduit_energy_fluid':'Create: More Catalysts',
        'moss_suspension':'Create: More Catalysts','tear_solution':'Create: More Catalysts'}
    for name,addon in addons.items():
        base=f'item.ccd.{name}_bucket.tooltip.'
        d[base+'condition1']=f'安装 {addon}，气流经过流体或装入雾化器时' if zh else f'When Used with {addon}'
        d[base+'behaviour1']='使用对应配方_批量加工物品_。' if zh else '_Processes items in bulk_ using the matching recipes.'
    d['item.ccd.corrupting_fluid_bucket.tooltip.condition1']='安装 Create: More Catalysts 及 AnvilCraft 或 L2 Hostility 时' if zh else 'With Create: More Catalysts and AnvilCraft or L2 Hostility'
    d['item.ccd.corrupting_fluid_bucket.tooltip.behaviour1']='气流经过流体或装入雾化器后，使用_腐化配方_加工物品。' if zh else 'Air passing through the fluid, or supplied by an Atomizer, processes items using _Corrupting Recipes_.'
    d['item.ccd.haunting_bucket.tooltip.behaviour1']='使用_缠魂配方_批量加工物品。' if zh else 'Processes items in bulk using _Haunting Recipes_.'
    d['item.ccd.quicksand_bucket.tooltip.summary']='用于_批量喷砂_的流体。' if zh else 'A fluid for _Bulk Sanding_.'
    d['block.ccd.flow_meter.tooltip.behaviour1']='查看_精确流量_。' if zh else 'Displays the _exact flow rate_.'
    d['item.ccd.moss_suspension_bucket.tooltip.behaviour2']='使生物获得_苔化_，将接触的可替换方块变为苔藓。直接接触或普通气流持续 4 秒，雾化后持续 2 秒。' if zh else 'Grants _Mossification_, turning touched replaceable blocks into moss. Lasts 4 seconds on contact or in ordinary air currents, or 2 seconds when atomized.'
    for kind in ['condition','behaviour']:d.pop(f'item.ccd.moss_suspension_bucket.tooltip.{kind}4',None)
    d['item.ccd.moss_suspension_bucket.tooltip.behaviour3']='将相邻的_可苔化方块_变为苔藓，上方有空间时扩散植被。' if zh else 'Turns adjacent _moss-replaceable blocks_ into moss and spreads vegetation when space is available above.'
    if zh:
        d['ccd.ponder.fluid_repackaging.text_2']='流体理包机将同一订单的物品与流体组合为黄铜包裹'
        d['ccd.ponder.fluid_repackaging.text_3']='将保留订单和收件地址的包裹送入理包机后方的容器'
        d['ccd.ponder.mossification.text_1']='苔藓悬浊液会将相邻的可苔化方块变为苔藓'
        d['ccd.ponder.powder_snow_fluid.text_4']='加工配方由所安装的附属提供'
    # Both instructions and their fallback text use the same material names.
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
