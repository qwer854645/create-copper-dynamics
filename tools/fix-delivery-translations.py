"""Write translations from UTF-8 source, without a shell encoding round-trip."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd/lang'
translations={
 'block.ccd.creative_delivery_fluid_tank':'输送式创造流体储罐',
 'ccd.creative_delivery.empty':'未选择流体',
 'ccd.creative_delivery.active':'无限供应 · 自动向开口输送',
 'ccd.creative_delivery.mode':'世界排液模式',
 'ccd.creative_delivery.containers_only':'仅向容器输送',
 'ccd.creative_delivery.allow_world':'允许向世界排液',
}
p=root/'zh_cn.json';data=json.loads(p.read_text(encoding='utf-8'));data.update(translations)
p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
p=root/'en_us.json';data=json.loads(p.read_text(encoding='utf-8'));data['ccd.creative_delivery.active']='Infinite supply - automatic outward delivery'
p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
