#!/usr/bin/env python
"""
This script generates patches containing the Jython-specific deviations from
the CPython standard library. It generates a patch file in stdlib-patches/ for
every file in the Lib/ directory that has a counterpart in the CPython
standard library.
"""

from __future__ import print_function
import os.path
import subprocess
import sys
import shutil

PATCHES_DIR = 'stdlib-patches'

if not os.path.exists('lib-python'):
    print('You need to run this script from the Jython root directory.',
          file=sys.stderr)
    sys.exit(1)

if len(sys.argv) > 1:
    cpython_basepath = os.path.join('lib-python', sys.argv[1])
else:
    cpythonlib_versions = os.listdir('lib-python')
    if len(cpythonlib_versions) > 1:
        print('More than one CPython library detected. '
              'Please give the target version as the first argument',
              file=sys.stderr)
        sys.exit(2)
    else:
        cpython_basepath = os.path.join('lib-python', cpythonlib_versions[0])

lib_files = []
for dirpath, dirnames, filenames in os.walk('Lib'):
    for filename in filenames:
        jythonlib_path = os.path.join(dirpath, filename)
        cpythonlib_path = os.path.join(cpython_basepath, jythonlib_path[4:])
        if os.path.isfile(cpythonlib_path):
            lib_files.append((jythonlib_path, cpythonlib_path))

if os.path.exists(PATCHES_DIR):
    shutil.rmtree(PATCHES_DIR)

print('Generating patches')
for jythonlib_path, cpythonlib_path in lib_files:
    patch_path = os.path.join(PATCHES_DIR, jythonlib_path[4:] + '.patch')
    patch_dir = os.path.dirname(patch_path)
    if not os.path.exists(patch_dir):
        os.makedirs(patch_dir)

    process = subprocess.Popen(['diff', '-N', '-u', cpythonlib_path, jythonlib_path],
                               stdout=subprocess.PIPE)
    stdout, _ = process.communicate()
    if process.returncode == 0:
        print('No differences detected in {} -- no patch generated'.format(jythonlib_path))
    elif process.returncode == 1:
        with open(patch_path, 'wb') as f:
            f.write(stdout)
    else:
        print("Error creating patch for {}".format(jythonlib_path), file=sys.stderr)
        sys.exit(3)

print('All done. You can now update the CPython standard library and then run applypatches.py.')
