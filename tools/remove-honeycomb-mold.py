"""Remove retired mold descriptions from bilingual resources and old generators."""
import json
from pathlib import Path

root=Path(__file__).resolve().parents[1]
for locale in ('zh_cn','en_us'):
    path=root/f'src/main/resources/assets/ccd/lang/{locale}.json'
    data=json.loads(path.read_text(encoding='utf-8'))
    data={key:value for key,value in data.items() if not key.startswith('item.ccd.honeycomb_mold')}
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for script in ('refine-create-text.py','rewrite-tooltips.py'):
    path=root/'tools'/script
    text=path.read_text(encoding='utf-8')
    if script=='refine-create-text.py':
        text=text.replace("'copper_bucket', 'honeycomb_mold',", "'copper_bucket',")
        text=''.join(line for line in text.splitlines(keepends=True) if "'honeycomb_mold':" not in line)
    else:
        start=text.index("describe('item.ccd.honeycomb_mold'")
        end=text.index("describe('item.minecraft.powder_snow_bucket'",start)
        text=text[:start]+text[end:]
    path.write_text(text,encoding='utf-8')
for relative in ('README.md','docs/create-reuse.md','docs/powder-snow-and-honeycomb.md'):
    path=root/relative
    text=path.read_text(encoding='utf-8')
    if relative.endswith('powder-snow-and-honeycomb.md'):
        text=text.split('## 蜜脾模具')[0].rstrip()+'\n'
    else:
        text=''.join(line for line in text.splitlines(keepends=True)
                     if not (line.startswith('- ') and '蜜脾模具' in line))
    text=text.replace('细雪流体与蜜脾模具','细雪流体').replace('细雪与模具说明','细雪流体说明')
    path.write_text(text,encoding='utf-8')
