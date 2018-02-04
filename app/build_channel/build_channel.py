#!/usr/bin/env python
#coding:utf-8
import zipfile
import shutil
import os
import sys
import datetime
base_dir = './'

start = datetime.datetime.now().microsecond
baseProductDirList = os.listdir('./')
for product in baseProductDirList:
    if product[0:4] == "wawa":
        apk_name = product

apk_path = "./" + apk_name
print("apk_path : " + apk_path)

apkBaseNameList = apk_name.split('-')
apkBaseName = apkBaseNameList[0] + "_" + apkBaseNameList[1] + "_" + apkBaseNameList[2] + "_"
apkBaseName = apkBaseName[0: -5]
print("apkBaseName : " + apkBaseName)

#sys.exit("done!!!")

empty_file = base_dir + 'xunwang_build'
f = open(empty_file, 'w')
f.close()

channel_file = base_dir + 'channel.txt'
f = open(channel_file)
lines = f.readlines()
linesList = lines[0].split(",")
f.close()

output_dir = "./" + 'channel_output'
print("output_dir" + output_dir)

totle = len(linesList)
i = 0

print("totle is %d "%(totle))

if not os.path.exists(output_dir):
    os.mkdir(output_dir)
for line in linesList:
    i = i + 1
    target_channel = line.strip()
    target_apk = output_dir + '/' + apkBaseName + '_' + target_channel + '.apk'
    print("current is %d, totle is %d, current apk name %s" % (i, totle, target_apk));
    shutil.copy(apk_path, target_apk)
    zipped = zipfile.ZipFile(target_apk, 'a', zipfile.ZIP_DEFLATED)
    empty_channel_file = 'META-INF/channel_' + target_channel
    zipped.write(empty_file, empty_channel_file)
    zipped.close()
end = datetime.datetime.now().microsecond
#print ("totle time: %f s" % ((end - start)/1000))