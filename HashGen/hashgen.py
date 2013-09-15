#! /usr/bin/env python
import os
import hashlib
import tempfile
from subprocess import call

files = [
    'launcher.jar',
    'client.jar',
    'lwjgl.jar',
    'lwjgl_util.jar',
    'jinput.jar',
    'windows_natives.jar',
    'macosx_natives.jar',
    'linux_natives.jar',
    'solaris_natives.jar'
]

def packfile(file):
    call("pack200 -E9 -g "+file+".pack "+file, shell=True)
    call("lzma -9 "+file+".pack", shell=True)
    return file+".pack.lzma"

def hashfile(afile, hasher, blocksize=65536):
    buf = afile.read(blocksize)
    while len(buf) > 0:
        hasher.update(buf)
        buf = afile.read(blocksize)
    return hasher.hexdigest()

for file in files:
    hash = hashfile(open(file,'rb'),hashlib.sha1())
    packedname = packfile(file)
    print packedname,os.path.getsize(packedname),hash

