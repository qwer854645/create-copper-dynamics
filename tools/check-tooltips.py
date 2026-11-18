"""Validate bilingual placeholders and Create's contiguous description sections."""
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
langs={name:json.loads((ROOT/f'src/main/resources/assets/ccd/lang/{name}.json').read_text(encoding='utf-8')) for name in ['zh_cn','en_us']}
assert langs['zh_cn'].keys()==langs['en_us'].keys()
def formats(text):
    tokens=re.findall(r'%(?:(\d+)\$)?([sdif])',text)
    return sorted((int(index) if index else i+1,kind) for i,(index,kind) in enumerate(tokens))
for key,en in langs['en_us'].items():
    zh=langs['zh_cn'][key]
    assert formats(zh)==formats(en),(key,zh,en)
    for text in [zh,en]:
        assert '\ufffd' not in text,(key,text)
        if '.tooltip.' in key: assert text.count('_')%2==0,(key,text)
for locale,entries in langs.items():
    roots={key.split('.tooltip.')[0] for key in entries if '.tooltip.summary' in key}
    for root in roots:
        for first,second in [('condition','behaviour'),('control','action')]:
            indices=sorted(int(key.rsplit(first,1)[1]) for key in entries if re.fullmatch(re.escape(root)+r'\.tooltip\.'+first+r'\d+',key))
            assert indices==list(range(1,len(indices)+1)),(root,first,indices)
            for i in indices: assert f'{root}.tooltip.{second}{i}' in entries,(root,second,i)
    print(locale,len(entries),'keys;',len(roots),'description families validated')
missing=set()
for path in (ROOT/'src/main/java').rglob('*.java'):
    for key in re.findall(r'"(ccd\.[a-zA-Z0-9_.]+)"',path.read_text(encoding='utf-8')):
        if key in langs['en_us']: continue
        if any(k.startswith(key) for k in langs['en_us']): continue
        # Skip data IDs used only as programmatic prefixes, not translation requests.
        if re.search(r'(?:translatable|CCDTooltip\.\w+)\([^\n]*"'+re.escape(key)+r'"',path.read_text(encoding='utf-8')): missing.add((str(path),key))
assert not missing,missing
print('Placeholder parity, highlight pairs, section pairs and literal translation references: OK')
