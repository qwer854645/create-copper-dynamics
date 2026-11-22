from pathlib import Path
import json
r=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd/lang'
for loc in ['zh_cn','en_us']:
    p=r/f'{loc}.json';d=json.loads(p.read_text(encoding='utf-8'));zh=loc=='zh_cn'
    d['block.ccd.omni_pump.tooltip.condition2']='输出关闭或目标无法接收时' if zh else 'When outputs are closed or cannot accept fluid'
    d['block.ccd.omni_pump.tooltip.behaviour2']='按原生管网规则停止输送，_不拥有储液缓存_。可直接连接容器，也可连接管道。' if zh else 'Stops transfer using native pipe rules, with _no owned fluid buffer_. Connect containers directly or through pipes.'
    d['ccd.ponder.omni_pumping.text_4']='流体由管网分配，万向泵不存液；没有可用输出时不继续抽液，反转旋转方向不交换接口模式。' if zh else 'The pipe network distributes fluid without storing it in the pump; unavailable outputs stop extraction, and rotation reversal does not swap port modes.'
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
