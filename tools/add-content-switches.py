"""Attach independent feature guards to CCD-owned runtime entry points and recipe data."""
from pathlib import Path
import json,re
root=Path(__file__).resolve().parents[1]
java=root/'src/main/java/dev/ccd'
# Guard complete CCD ticks (including inherited native ticks) without patching Create.
for p in (java/'content').glob('*BlockEntity.java'):
    s=p.read_text();name=p.stem
    if 'extends ' not in s:continue
    if re.search(r'void tick\s*\(\s*\)',s):
        s=re.sub(r'(void tick\s*\(\s*\)\s*\{)',r'\1\n        if (!dev.ccd.CCDFeatures.enabled(getBlockState())) return;',s,count=1)
    else:
        end=s.rfind('}')
        s=s[:end]+'''    @Override public void tick() {
        if (!dev.ccd.CCDFeatures.enabled(getBlockState())) return;
        super.tick();
    }
'''+s[end:]
    p.write_text(s)
# Every recipe explicitly checks its CCD ingredients and outputs; foreign recipes untouched.
features=set(re.findall(r'"([a-z_]+)"',(java/'CCDFeatures.java').read_text().split('}) CONTENT')[0]))
def feature(path):
    if path.startswith('flowing_'):path=path[8:]
    if path.endswith('_bucket') and path not in ['copper_bucket','brass_bucket']:path=path[:-7]
    return path
for p in (root/'src/main/resources/data/ccd/recipe').rglob('*.json'):
    d=json.loads(p.read_text(encoding='utf-8-sig'));refs=set()
    def walk(v):
        if isinstance(v,dict):
            for key,item in v.items():
                if key in ['item','id','fluid'] and isinstance(item,str) and item.startswith('ccd:'):
                    f=feature(item[4:])
                    if f in features:refs.add(f)
                walk(item)
        elif isinstance(v,list):
            for item in v:walk(item)
    walk(d)
    if refs:
        conditions=d.setdefault('neoforge:conditions',[])
        for f in sorted(refs):
            c={'type':'ccd:content_enabled','feature':f}
            if c not in conditions:conditions.append(c)
        p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
# Derive labels from actual registered display names.
for locale in ['zh_cn','en_us']:
    p=root/f'src/main/resources/assets/ccd/lang/{locale}.json';d=json.loads(p.read_text(encoding='utf-8-sig'))
    text=(java/'CCDFeatures.java').read_text();ids=re.findall(r'"([a-z_]+)"',text[text.index('for (String id'):text.index('}) CONTENT')])
    for f in ids:
        label=d.get('block.ccd.'+f,d.get('item.ccd.'+f,d.get('fluid.ccd.'+f,'Powder Snow' if locale=='en_us' else '细雪')))
        d['ccd.configuration.content.'+f]=label
        d['ccd.configuration.content.'+f+'.tooltip']='启用此内容。关闭后保留已有物品、方块和流体，停止其功能并隐藏获取配方。重新进入世界后生效。' if locale=='zh_cn' else 'Enable this content. Disabling retains saved items, blocks and fluids, stops their functions and hides acquisition recipes. Restart the world to apply.'
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
