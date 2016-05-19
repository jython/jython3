"""Test path handling on Windows

Made for Jython.
"""

import os
import unittest
from test import support

class NTAbspathTestCase(unittest.TestCase):

    def setUp(self):
        with open(support.TESTFN, 'w') as fp:
            fp.write('foo')

        # Move to the same drive as TESTFN
        drive, self.path = os.path.splitdrive(os.path.abspath(
                support.TESTFN))
        self.orig_cwd = os.getcwd()
        os.chdir(os.path.join(drive, os.sep))

    def tearDown(self):
        os.chdir(self.orig_cwd)
        os.remove(support.TESTFN)

    def test_abspaths(self):
        # Ensure r'\TESTFN' and '/TESTFN' are handled as absolute
        for path in self.path, self.path.replace('\\', '/'):
            with open(path) as fp:
                self.assertEqual(fp.read(), 'foo')


def test_main():
    if (os._name if support.is_jython else os.name) != 'nt':
        raise support.TestSkipped('NT specific test')
    support.run_unittest(NTAbspathTestCase)


if __name__ == '__main__':
    test_main()
