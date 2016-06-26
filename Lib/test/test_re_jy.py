import re
import sys
import unittest
import test.support
import unicodedata
from unicodedata import category


class ReTest(unittest.TestCase):

    def test_bug_1140_addendum(self):
        result = re.sub('', lambda match : None, 'foo')
        self.assertEqual(result, 'foo')
        self.assertTrue(isinstance(result, str))

    def test_sub_with_subclasses(self):
        class Foo(str):
            def join(self, items):
                return Foo(str.join(self, items))
        result = re.sub('bar', 'baz', Foo('bar'))
        self.assertEqual(result, 'baz')
        self.assertEqual(type(result), str)

        class Foo2(str):
            def join(self, items):
                return Foo2(str.join(self, items))
            def __getslice__(self, start, stop):
                return Foo2(str.__getslice__(self, start, stop))
        result = re.sub('bar', 'baz', Foo2('bar'))
        self.assertEqual(result, Foo2('baz'))
        self.assertTrue(isinstance(result, Foo2))

    def test_unkown_groupname(self):
        self.assertRaises(IndexError,
                          re.match(r'(?P<int>\d+)\.(\d*)', '3.14').group,
                          'misspelled')

    def test_whitespace(self):
        # Test for http://bugs.jython.org/issue2226 - verify against cpython
        ws_re = re.compile(r'\s')
        not_ws_re = re.compile(r'\S')
        cpython_ascii_whitespace = set(' \t\n\r\f\v')
        for i in range(256):
            c = chr(i)
            if c in cpython_ascii_whitespace:
                self.assertRegex(c, ws_re)
                self.assertNotRegexpMatches(c, not_ws_re)
            else:
                self.assertNotRegexpMatches(c, ws_re)
                self.assertRegex(c, not_ws_re)

    def test_unicode_whitespace(self):
        # Test for http://bugs.jython.org/issue2226
        ws_re = re.compile(r'\s', re.UNICODE)
        not_ws_re = re.compile(r'\S', re.UNICODE)
        separator_categories = set(['Zl', 'Zp', 'Zs'])
        separators = {chr(c) for c in [28, 29, 30, 31]}
        special = set([
            unicodedata.lookup('MONGOLIAN VOWEL SEPARATOR'),
            '\u0085', # NEXT LINE (NEL)
            ])
        cpython_whitespace = set(' \t\n\r\f\v') | separators | special
        for i in range(0xFFFF): # could test to sys.maxunicode, but does not appear to be necessary
            if i >= 0xD800 and i <= 0xDFFF:
                continue
            c = chr(i)
            if c in cpython_whitespace or category(c) in separator_categories:
                self.assertRegex(c, ws_re)
                self.assertNotRegexpMatches(c, not_ws_re)
            else:
                self.assertNotRegexpMatches(c, ws_re)
                self.assertRegex(c, not_ws_re)


def test_main():
    test.support.run_unittest(ReTest)

if __name__ == "__main__":
    test_main()
