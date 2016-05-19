"""Misc file tests.

Made for Jython.
"""

import os
import unittest
from test import support

class FileTestCase(unittest.TestCase):

    def tearDown(self):
        if os.path.exists(support.TESTFN):
            os.remove(support.TESTFN)

    def test_append(self):
        # http://bugs.jython.org/issue1466
        mode = 'ab'
        fp1 = open(support.TESTFN, mode)
        fp1.write('test1\n')
        fp2 = open(support.TESTFN, mode)
        fp2.write('test2\n')
        fp1.close()
        fp2.close()
        with open(support.TESTFN) as fp:
            self.assertEqual('test1\ntest2\n', fp.read())

    def test_appendplus(self):
        # regression with the test_append fix:
        # http://bugs.jython.org/issue1576
        with open(support.TESTFN, 'ab+') as fp:
            fp.write('test1\n')
            fp.seek(0)
            self.assertEqual(fp.read(), 'test1\n')

    def test_issue1825(self):
        testfnu = str(support.TESTFN)
        try:
            open(testfnu)
        except IOError as e:
            self.assertTrue(isinstance(e.filename, str))
            self.assertEqual(e.filename, testfnu)
        else:
            self.assertTrue(False)

    @unittest.skipUnless(hasattr(os, 'chmod'), 'chmod() support required for this test')
    def test_issue2081(self):
        f = open(support.TESTFN, 'wb')
        f.close()
        os.chmod(support.TESTFN, 200)      # write-only
        f = open(support.TESTFN, 'w')      # should succeed, raised IOError (permission denied) prior to fix
        f.close()


def test_main():
    support.run_unittest(FileTestCase)


if __name__ == '__main__':
    test_main()
