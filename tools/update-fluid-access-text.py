"""Keep the bilingual help consistent with the current fluid access behaviour."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/ccd/lang'
texts = {
    'en_us': {
        'block.ccd.fluid_access_port.tooltip.behaviour1': 'Shares the attached container through _all faces_. Access Ports and Fluid Bridges cannot extend each other.',
        'block.ccd.fluid_access_port.tooltip.condition2': 'When Powered by Redstone',
        'block.ccd.fluid_access_port.tooltip.behaviour2': 'Disconnects the container. A Comparator repeats the attached container’s signal.',
        'block.ccd.fluid_bridge.tooltip.behaviour1': 'Only _matching fluid_ can enter or leave that end. Explicit filters take priority over an empty filter on the opposite end.',
        'block.ccd.fluid_bridge.tooltip.condition2': 'When Powered by Redstone',
        'block.ccd.fluid_bridge.tooltip.behaviour2': 'Disconnects both containers. A Comparator outputs half the sum of their signals.',
        'ccd.ponder.fluid_access.text_1': 'An Access Port shares the attached container through all faces',
        'ccd.ponder.fluid_access.text_2': 'Access Ports and Fluid Bridges cannot extend each other',
        'ccd.ponder.fluid_access.text_3': 'A Fluid Bridge gives access to both end containers',
        'ccd.ponder.fluid_access.text_4': 'Explicit end filters take priority over an empty filter on the opposite end',
    },
    'zh_cn': {
        'block.ccd.fluid_access_port.tooltip.behaviour1': '所有面均可访问所连接的容器。扩展端口和流体桥接器_不能互相扩展_。',
        'block.ccd.fluid_access_port.tooltip.condition2': '接收红石信号时',
        'block.ccd.fluid_access_port.tooltip.behaviour2': '断开容器访问。比较器转发所连接容器的信号。',
        'block.ccd.fluid_bridge.tooltip.behaviour1': '该端只允许_匹配的流体_进出；明确设置的过滤条件优先于另一端的空过滤。',
        'block.ccd.fluid_bridge.tooltip.condition2': '接收红石信号时',
        'block.ccd.fluid_bridge.tooltip.behaviour2': '断开两端容器访问。比较器输出两端信号之和的一半。',
        'ccd.ponder.fluid_access.text_1': '流体扩展端口让所有面均可访问所连接的容器',
        'ccd.ponder.fluid_access.text_2': '扩展端口和流体桥接器不能互相扩展',
        'ccd.ponder.fluid_access.text_3': '流体桥接器可同时访问两端的容器',
        'ccd.ponder.fluid_access.text_4': '明确设置的端面过滤优先于另一端的空过滤',
    },
}
for language, updates in texts.items():
    path = root / (language + '.json')
    data = json.loads(path.read_text(encoding='utf-8'))
    data.update(updates)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
