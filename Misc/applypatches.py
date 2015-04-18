#!/usr/bin/env python
"""
This script applies the patches previously made with genpatches.py.
If a patch is successfully applied, it is deleted and the patched file is
added to version control.
"""

from __future__ import print_function
import os.path
import sys
import shutil
import subprocess

PATCHES_DIR = 'stdlib-patches'

if not os.path.exists(PATCHES_DIR):
    print('Run genpatches.py first.', file=sys.stderr)
    sys.exit(1)

if len(sys.argv) > 1:
    cpython_basepath = os.path.join('lib-python', sys.argv[1])
else:
    cpythonlib_versions = os.listdir('lib-python')
    if len(cpythonlib_versions) > 1:
        print('More than one CPython library detected.\n'
              'Please give the target version as the first argument', file=sys.stderr)
        sys.exit(2)
    else:
        cpython_basepath = os.path.join('lib-python', cpythonlib_versions[0])

succeeded = []
failed = 0
for dirpath, dirnames, filenames in os.walk(PATCHES_DIR):
    for filename in filenames:
        realfilename = filename[:-6]
        patchpath = os.path.abspath(os.path.join(dirpath, filename))
        dstpath = os.path.join('Lib', dirpath[len(PATCHES_DIR) + 1:], realfilename)
        dstdir = os.path.dirname(dstpath)

        print('Creating %s' % dstpath)
        if not os.path.exists(dstdir):
            os.makedirs(dstdir)

        dstpath = os.path.abspath(dstpath)
        retcode = subprocess.call(['patch' ,'-p1', '-s', '-t', '-i', patchpath, '-o', dstpath],
                                  cwd=cpython_basepath)
        if retcode != 0:
            failed += 1
            os.remove(dstpath)
        else:
            succeeded.append(dstpath)
            os.remove(patchpath)

            # Delete any corresponding rejects or backup files
            for suffix in '.orig', '.rej':
                path = patchpath + suffix
                if os.path.exists(path):
                    os.remove(path)

if succeeded:
    print('\nThe following files were successfully patched:')
    for path in sorted(succeeded):
        print(path)

if failed:
    print('\n%d patches failed to apply' % failed)
