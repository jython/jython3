"""Int tests

Additional tests for Jython.
"""
import unittest
import types
from test import support

class IntTestCase(unittest.TestCase):

    def test_type_matches(self):
        self.assertTrue(isinstance(1, int))

    def test_int_pow(self):
        self.assertEqual(pow(10, 10, None), 10000000000)
        self.assertEqual(int.__pow__(10, 10, None), 10000000000)
        self.assertEqual((10).__pow__(10, None), 10000000000)

def test_main():
    support.run_unittest(IntTestCase)

if __name__ == '__main__':
    test_main()
