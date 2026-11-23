"""Write concise bilingual labels for content configuration sections and cosmetic extra."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
sections={'transport':('输送与管路','Transport and Pipes'),'storage':('储存与供水','Storage and Water Supply'),
          'logistics':('流体物流','Fluid Logistics'),'tools':('容器与过滤工具','Containers and Filters'),
          'processing':('气流加工与流体','Airflow Processing and Fluids')}
for index,locale in enumerate(['zh_cn','en_us']):
    p=root/f'src/main/resources/assets/ccd/lang/{locale}.json'
    data=json.loads(p.read_text(encoding='utf-8-sig'))
    for key,names in sections.items():data['ccd.configuration.contentSection.'+key]=names[index]
    data['ccd.configuration.rebirthName']=('彩蛋：中文模组名称','Easter Egg: Chinese Mod Title')[index]
    data['ccd.configuration.rebirthName.tooltip']=(
        '默认关闭。开启后，中文界面的模组创造栏名称显示为“流体的结合：重生（泵不住了）”。不改变玩法和注册名称，重启游戏后生效。',
        'Off by default. Changes the mod creative-tab title in Chinese to the alternate title. Gameplay and registry names are unchanged. Restart the game to apply.')[index]
    p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
