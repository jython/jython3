"""Ensure longs passed to int arguments are handled correctly

Made for Jython.
"""
import array
import io
import tempfile
from test import support
import unittest
import io

class LongIntArgsTestCase(unittest.TestCase):

    def test_array(self):
        a = array.array('c', 'jython')
        assert a.pop(0) == 'j'

    def test_file(self):
        test_file = tempfile.TemporaryFile()
        try:
            self._test_file(test_file)
        finally:
            test_file.close()

    def test_StringIO(self):
        self._test_file(io.StringIO())

    def test_cStringIO(self):
        self._test_file(io.StringIO())

    def test_str(self):
        self._test_basestring(str)

    def test_unicode(self):
        self._test_basestring(str)

    def _test_basestring(self, class_):
        s = class_('hello from jython')
        l = int(len(s))
        assert s.count('o', 0, l) == 3
        assert s.endswith('n', 0, l) == True
        assert s.expandtabs(1) == s
        assert s.find('h', 0, l) == 0
        assert s.index('h', 0, l) == 0
        assert s.rfind('h', 0, l) == 14
        assert s.rindex('h', 0, l) == 14
        assert s.split(' ', 1) == ['hello', 'from jython']
        assert s.startswith('jython', 11)

    def _test_file(self, test_file):
        test_file.write('jython')
        test_file.seek(0)
        assert test_file.read(1) == 'j'
        assert test_file.readline(2) == 'yt'
        assert test_file.readlines(3) == ['hon']

def test_main():
    support.run_unittest(LongIntArgsTestCase)

if __name__ == '__main__':
    test_main()
