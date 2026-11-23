"""Labels for component-specific statistics using Create's label/value layout."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
labels={
 'ccd.goggles.target_flow':('目标流量：','Target Flow Rate:'),
 'ccd.goggles.signal':('红石信号强度：','Redstone Signal Strength:'),
 'ccd.goggles.range':('单侧输送距离：','Range per Side:'),
 'ccd.unit.flow':(' mB/t',' mB/t'),
 'ccd.unit.blocks':(' 格',' blocks'),
 'ccd.goggles.pumping':('泵送信息：','Pumping Stats:'),
 'ccd.goggles.flow':('流量：','Flow Rate:'),
}
for index,locale in enumerate(['zh_cn','en_us']):
 p=root/f'src/main/resources/assets/ccd/lang/{locale}.json'
 data=json.loads(p.read_text(encoding='utf-8-sig'))
 for key,text in labels.items():data[key]=text[index]
 p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
