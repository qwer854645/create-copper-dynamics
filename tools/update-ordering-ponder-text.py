"""Keep the ordering storyboard's bilingual steps aligned with the staged demonstration."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
steps=[
('手持流体工厂仪表右键点击存量转信器，选择物流网络','Use a Fluid Factory Gauge on a Stock Link to select its logistics network'),
('将仪表装在收货端流体打包机上，维持后方容器的流体存量','Place the Gauge on a receiving Fluid Packager to maintain stock in its rear container'),
('手持装液容器选择流体，并以 mB 设置目标数量','Select a fluid with a filled container and set the target amount in mB'),
('补货以铜包裹送达，将选定流体补入容器','Deliveries arrive as Copper Packages and replenish the selected fluid'),
('目标设为零时，只监控存量，不请求补货','Set the target to zero to monitor stock without requesting deliveries')]
for index,locale in enumerate(['zh_cn','en_us']):
 p=root/f'src/main/resources/assets/ccd/lang/{locale}.json'
 data=json.loads(p.read_text(encoding='utf-8-sig'))
 for number,step in enumerate(steps,1):data[f'ccd.ponder.fluid_ordering.text_{number}']=step[index]
 p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
