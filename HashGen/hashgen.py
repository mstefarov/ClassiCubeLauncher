#! /usr/bin/env python
import os
import hashlib

files = [
    'launcher.jar',
    'client.jar',
    'lwjgl.jar.pack.lzma',
    'lwjgl_util.jar.pack.lzma',
    'jinput.jar.pack.lzma',
    'windows_natives.jar.lzma',
    'macosx_natives.jar.lzma',
    'linux_natives.jar.lzma',
    'solaris_natives.jar.lzma'
]

def hashfile(afile, hasher, blocksize=65536):
    buf = afile.read(blocksize)
    while len(buf) > 0:
        hasher.update(buf)
        buf = afile.read(blocksize)
    return hasher.hexdigest()

for file in files:
    hash = hashfile(open(file,'rb'),hashlib.sha1())
    print file,os.path.getsize(file),hash
