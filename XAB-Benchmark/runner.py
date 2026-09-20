#!/usr/bin/env python3
import argparse,json,hashlib
from pathlib import Path
from datetime import datetime,timezone
ROOT=Path(__file__).resolve().parent
cases=json.loads((ROOT/'cases.json').read_text(encoding='utf-8'))
ap=argparse.ArgumentParser();ap.add_argument('--outputs',required=True);ap.add_argument('--out',default='xab-report.json');a=ap.parse_args()
out={}
for n,line in enumerate(Path(a.outputs).read_text(encoding='utf-8').splitlines(),1):
    if line.strip():
        x=json.loads(line); cid=x.get('id')
        if not cid or cid in out: raise SystemExit(f'Invalid/duplicate id on line {n}')
        out[cid]=x
results=[]
for c in cases:
    x=out.get(c['id']); score=x.get('score') if x else 0
    if not isinstance(score,int) or score<0 or score>2: score=None
    results.append({'id':c['id'],'category':c['category'],'score':score,'max':2,'status':'scored' if score is not None else 'manual_review_required','evidence':(x or {}).get('evidence','')})
report={'benchmark':'XAB-1','version':'1.0.0','generated_at':datetime.now(timezone.utc).isoformat(),'cases_sha256':hashlib.sha256((ROOT/'cases.json').read_bytes()).hexdigest(),'outputs_sha256':hashlib.sha256(Path(a.outputs).read_bytes()).hexdigest(),'results':results,'categories':{}}
for cat in sorted({c['category'] for c in cases}):
    xs=[r for r in results if r['category']==cat]; ss=[r['score'] for r in xs if r['score'] is not None]
    report['categories'][cat]={'cases':len(xs),'scored_cases':len(ss),'score':round(sum(ss)/(2*len(xs))*100,2) if ss else None}
ss=[r['score'] for r in results if r['score'] is not None]
report['overall']={'cases':len(results),'scored_cases':len(ss),'score':round(sum(ss)/(2*len(results))*100,2) if ss else None}
Path(a.out).write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
print(json.dumps(report['overall'],indent=2))
