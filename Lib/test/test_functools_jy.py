from functools import partial
import unittest

from test import support


class PartialDictTest(unittest.TestCase):
    def test_assign_attribute(self):
        partial(lambda: None).somevar = 1

    def test_subclass_assign_attribute(self):
        class A(partial): pass
        A(lambda: None).somevar = 1


def test_main():
    support.run_unittest(PartialDictTest)

if __name__ == "__main__":
    test_main()
