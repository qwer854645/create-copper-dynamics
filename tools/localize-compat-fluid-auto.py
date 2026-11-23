"""Refresh config help for addon-aware fluid categories."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
for loc in ['zh_cn','en_us']:
 p=root/f'src/main/resources/assets/ccd/lang/{loc}.json';d=json.loads(p.read_text(encoding='utf-8'))
 for category in ['nuclear_fluids','nether_industry_fluids','more_catalysts_fluids']:
  key='ccd.configuration.content.'+category+'.tooltip'
  text=d[key].removeprefix('默认关闭。').removeprefix('Off by default. ')
  d[key]=('默认 AUTO：加载对应加工模组时启用；ENABLED 强制开启，DISABLED 强制关闭。' if loc=='zh_cn' else 'Default AUTO enables this category when its processing addon is loaded. ENABLED and DISABLED override detection. ')+text
 p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
