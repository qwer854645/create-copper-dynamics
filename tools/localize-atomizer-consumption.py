from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
for locale in ['zh_cn','en_us']:
    p=root/f'src/main/resources/assets/ccd/lang/{locale}.json';d=json.loads(p.read_text(encoding='utf-8'))
    if locale=='zh_cn':
        d['ccd.configuration.atomizer.consumptionMultiplier']='流体消耗倍率'
        d['ccd.configuration.atomizer.consumptionMultiplier.tooltip']='作用于所有雾化流体，包括自定义配方。0 不消耗，但仍需装入对应流体；1 使用基础或配方耗量。正数耗量向上取整。重新进入世界后生效。'
        d['block.ccd.fluid_atomizer.tooltip.summary']='将储存的流体雾化，为气流提供_批量加工_效果。'
        d['ccd.ponder.fluid_media.text_1']='流体雾化器使用储存的流体提供加工效果，消耗量可在配置中调整'
        d['ccd.configuration.atomizer.normalConsumption']='加工介质基础耗量'
        d['ccd.configuration.atomizer.potionConsumption']='药液基础耗量'
    else:
        d['ccd.configuration.atomizer.consumptionMultiplier']='Fluid Consumption Multiplier'
        d['ccd.configuration.atomizer.consumptionMultiplier.tooltip']='Scales all atomizer costs, including custom recipes. 0 consumes nothing but still requires matching fluid; 1 uses the base or recipe cost. Positive amounts round up. Reopen the world to apply.'
        d['block.ccd.fluid_atomizer.tooltip.summary']='Atomizes stored fluids to provide _Bulk Processing_ effects to air currents.'
        d['ccd.ponder.fluid_media.text_1']='Fluid Atomizers use stored fluid for processing; consumption can be configured'
        d['ccd.configuration.atomizer.normalConsumption']='Processing Medium Base Consumption'
        d['ccd.configuration.atomizer.potionConsumption']='Potion Base Consumption'
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
