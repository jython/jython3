import unittest
import test.support

class SubclassInstanceTest(unittest.TestCase):

    def test_subclass_int(self):
        try:
            class foo(12): pass
        except TypeError:
            pass
        else:
            self.fail("expected TypeError for subclassing an int instance")

def test_main():
    test.support.run_unittest(SubclassInstanceTest)

if __name__ == "__main__":
    test_main()
