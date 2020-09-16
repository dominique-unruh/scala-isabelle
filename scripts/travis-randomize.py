#!/usr/bin/python3

import random

before = []
jobs = []
after = []
output = []

with open(".travis.yml","rt") as f:
    mode = 'before'
    for line in f.readlines():
        line = line.rstrip()
        #print(line)
        if mode=='before':
            before.append(line)
            if line.startswith('  include:'):
                mode = 'jobs'
        elif mode=="jobs":
            if line.startswith('    - '):
                jobs.append(line)
            elif line.startswith('#    - '):
                jobs.append(line.lstrip("#"))
            else:
                after.append(line)
                mode = 'after'
        elif mode=='after':
            after.append(line)
        else:
            error()


for line in before: output.append(line)

jobnr = random.randrange(0, len(jobs))
print(f"Picking job {jobnr+1}")
for i in range(len(jobs)):
    if i==jobnr:
        output.append(jobs[i])
    else:
        output.append("#"+jobs[i])
            
for line in after: output.append(line)



with open(".travis.yml","wt") as f:
    for l in output:
        f.write(l+"\n")
