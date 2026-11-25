import json, pathlib, re
p=pathlib.Path(__file__).resolve().parents[1]/'examples/breaking-the-feedback-loop.json'
c=json.loads(p.read_text())
assert c['schemaVersion']==1
assert re.fullmatch(r'[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}',c['id'])
assert len(c['name'])<=100 and len(c['description'])<=3000
assert 0<len(c['review'])<=100 and all(0<len(s)<=2000 for s in c['review'])
assert {r['cadence'] for r in c['reports']}=={'daily','weekly','monthly'}
assert len(set(r['id'] for r in c['reports']))==len(c['reports'])
for r in c['reports']:
    assert re.fullmatch(r'[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}',r['id'])
    assert 0<len(r['questions'])<=50
    assert len({q['id'] for q in r['questions']})==len(r['questions'])
    for q in r['questions']:
        assert re.fullmatch(r'[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}',q['id'])
        assert 0<len(q['label'])<=500
        assert isinstance(q.get('required',True),bool)
        assert q['type'] in {'boolean','text','choice','multiChoice','scale'}
        if q['type'] in {'choice','multiChoice'}:
            assert 2<=len(q['options'])<=30 and len(set(q['options']))==len(q['options']) and all(0<len(s)<=200 for s in q['options'])
        if q['type']=='scale':
            assert type(q['min'])==type(q['max'])==int and -1000<=q['min']<q['max']<=1000 and q['max']-q['min']<=100
            if 'labels' in q:
                labels=q['labels']
                assert isinstance(labels,list) and len(labels)==q['max']-q['min']+1
                assert all(isinstance(x,str) and 0<len(x.strip()) and len(x)<=200 for x in labels)
                assert len(set(labels))==len(labels)
                assert q.get('required',True) or 'Not applicable (skip)' not in labels
                assert q['min']==1 and q['max']==4
assert len(c['review'])==7
assert all(q['type']!='scale' or 'labels' in q for r in c['reports'] for q in r['questions'])
print(f"PASS: {len(c['reports'])} reports, {sum(len(r['questions']) for r in c['reports'])} questions, {len(c['review'])} practice items")
