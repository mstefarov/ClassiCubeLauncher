#! /usr/bin/env python
import os
import hashlib
import zipfile
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

def packfile(filename):
    call("pack200 -E9 -g "+filename+".pack "+filename, shell=True)
    call("lzma -9 "+filename+".pack", shell=True)
    return filename+".pack.lzma"

def hashfile(jarname, hasher, blocksize=65536):
    zf = zipfile.ZipFile(jarname, 'r')
    buf = zf.read('META-INF/MANIFEST.MF', blocksize)
    hasher.update(buf)
    zf.close()
    return hasher.hexdigest()

for f in files:
    hash = hashfile(f, hashlib.sha1())
    packedname = packfile(f)
    print packedname, os.path.getsize(packedname), hash
