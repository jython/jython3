""" Extra grammar tests for Jython.
"""

from test import support
import sys
import unittest

class GrammarTest(unittest.TestCase):
    def test_triple_quote_len(self):
        s1 = r"""
        \""" 1.triple-quote
        \""" 2.triple-quote
        """

        s2 = r'''
        \""" 1.triple-quote
        \""" 2.triple-quote
        '''
        self.assertTrue(not '\r' in s1)
        self.assertEqual(len(s1), len(s2))

    def testStringPrefixes(self):
        self.assertEqual("spam", "spam")
        self.assertEqual(r"spam", R"spam")
        self.assertEqual(R"spam", r"spam")
        self.assertEqual(r"spam", R"spam")

    def testKeywordOperations(self):
        def foo(a=1, b=2 + 4):
            return b
        self.assertEqual(6, foo())
        self.assertEqual(6, foo(1))
        self.assertEqual(7, foo(1, 7))
        self.assertEqual(10, foo(b=10))


pep263 = """
    # verify that PEP263 encoding is only set by magic comments, not
    # by other similar looking input; seen in issue 1506
    >>> line = '"Content-Transfer-Encoding: 8bit"'
    >>> print line
    "Content-Transfer-Encoding: 8bit"
    """

__test__ = dict(pep263=pep263)


def test_main(verbose=None):
    support.run_unittest(GrammarTest)
    support.run_doctest(sys.modules[__name__], verbose)

if __name__ == '__main__':
    test_main(verbose=True)
