#!/usr/bin/env python
#coding:utf-8

f=open('channel.txt','a')
for i in range(1000023,1000031):
    f.write(str(i))
    if i != 1000031-1000023:
        f.write(",")
f.close()