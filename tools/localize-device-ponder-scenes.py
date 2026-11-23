"""Localize newly separated device storyboards using the scene text order."""
from pathlib import Path
import json,re
root=Path(__file__).resolve().parents[1]
translations={
'turning_pump':('转向输送流体',['在相邻管口之间输送流体，传动轴与管口位于同一平面']),
'vertical_turning_pump':('连接垂直传动轴',['贯通传动轴垂直于两个相邻管口所在的平面']),
'redstone_pump':('用红石反转流向',['接入旋转力以泵送流体','红石信号反转流向，不改变驱动方向']),
'bidirectional_pump':('用旋转方向控制流向',['驱动方向决定流体方向','反转旋转力，流体流向随之反转']),
'variable_pump':('控制泵送流量',['通过露出的齿轮面接入旋转力','红石强度控制流量和距离，表盘显示信号强度','默认配置下，无信号时停止泵送，齿轮仍可转动']),
'portable_pump':('操作便携式流体泵',['右键点击手动泵送，潜行右键点击反向操作','也可连接传动轴持续驱动']),
'creative_supply':('无限供应流体',['手持装液容器右键点击，选择无限供应的流体','扳手调整出液口，侧面控件切换容器供液与向世界排液']),
'water_supply':('向相邻容器供水',['潮涌箱自动向六面相邻容器供水']),
'water_basin':('取用无限水源',['铜水槽可供手持容器和管道取水，不会自动填充相邻容器']),
}
s=(root/'src/main/java/dev/ccd/client/ponder/CCDPonderScenes.java').read_text()
for locale in ['zh_cn','en_us']:
 p=root/f'src/main/resources/assets/ccd/lang/{locale}.json';d=json.loads(p.read_text(encoding='utf-8'))
 for scene,(title,zh) in translations.items():
  match=re.search(r'var s=begin\(builder,"'+scene+r'","([^"]+)"\);(.*?)(?=\n    public static|\n    /\*\*|\Z)',s,re.S)
  assert match,scene
  messages=re.findall(r'text\(s,util,.*?,"([^"]+)"\)',match[2])
  assert len(messages)==len(zh),(scene,messages)
  d[f'ccd.ponder.{scene}.header']=title if locale=='zh_cn' else match[1]
  for i,msg in enumerate(messages,1):d[f'ccd.ponder.{scene}.text_{i}']=zh[i-1] if locale=='zh_cn' else msg
 p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
