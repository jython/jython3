"""Basic coerce tests cases

Made for Jython.
"""
import unittest
from test import support

class CoerceTestCase(unittest.TestCase):

    def test_int_coerce__(self):
        self.assertEqual(int.__coerce__(1, None), NotImplemented)
        self.assertRaises(TypeError, int.__coerce__, None, 1)

    def test_long_coerce__(self):
        self.assertEqual(int.__coerce__(1, None), NotImplemented)
        self.assertRaises(TypeError, int.__coerce__, None, 1)

    def test_float_coerce__(self):
        self.assertRaises(TypeError, float.__coerce__, None, 1)
        self.assertEqual(float.__coerce__(10.23, None), NotImplemented)


def test_main():
    support.run_unittest(CoerceTestCase)

if __name__ == "__main__":
    test_main()
