import os
import subprocess
import sys
import unittest
from test import support

WINDOWS = (os._name if support.is_jython else os.name) == 'nt'

class TestUsingInitializer(unittest.TestCase):

    def test_syspath_initializer(self):
        fn = support.findfile('check_for_initializer_in_syspath.py')
        env = dict(CLASSPATH='tests/data/initializer',
                   PATH=os.environ.get('PATH', ''))

        if WINDOWS:
            # TMP is needed to give property java.io.tmpdir a sensible value
            env['TMP'] = os.environ.get('TMP', '.')
            # SystemRoot is needed to remote debug the subprocess JVM
            env['SystemRoot'] = os.environ.get('SystemRoot', '')

        self.assertEqual(0, subprocess.call([sys.executable, fn], env=env))

def test_main():
    support.run_unittest(TestUsingInitializer)

if __name__ == "__main__":
    test_main()
