"""Misc cPickle tests.

Made for Jython.
"""
import builtins
import sys
import pickle
import pickle
import unittest
from io import StringIO
from test import support

class MyClass(object):
    pass


class CPickleTestCase(unittest.TestCase):

    def test_zero_long(self):
        self.assertEqual(pickle.loads(pickle.dumps(0, 2)), 0)
        self.assertEqual(pickle.dumps(0, 2), pickle.dumps(0, 2))

    def test_cyclic_memoize(self):
        # http://bugs.python.org/issue998998 - cPickle shouldn't fail
        # this, though pickle.py still does
        m = MyClass()
        m2 = MyClass()

        s = set([m])
        m.foo = set([m2])
        m2.foo = s

        s2 = pickle.loads(pickle.dumps(s))
        self.assertEqual(len(s2), 1)
        m3 = next(iter(s2))
        self.assertEqual(len(m3.foo), 1)
        m4 = next(iter(m3.foo))
        self.assertEqual(m4.foo, s2)

    def test_find_global(self):

        class A(object):
            def __init__(self, x, y):
                self.x = x
                self.y = y

            def __eq__(self, other):
                if isinstance(other, A) and self.x == other.x and self.y == other.y:
                    return True
                return False

        class B(object):
            def __init__(self, x, y):
                self.x = x
                self.y = y

        def restrictive_find_global(module, clsname):
            if clsname == 'A':
                return A
            else:
                raise pickle.UnpicklingError("Cannot load class", module, clsname)

        a = A("python", "C")
        a_pickled = pickle.dumps(a, 2)
        a_unpickler = pickle.Unpickler(StringIO(a_pickled))
        a_unpickler.find_global = restrictive_find_global
        self.assertEqual(a_unpickler.load(), a)

        b_pickled = pickle.dumps(B("jython", "java"), 2)
        b_unpickler = pickle.Unpickler(StringIO(b_pickled))
        b_unpickler.find_global = restrictive_find_global
        self.assertRaises(pickle.UnpicklingError, b_unpickler.load)


    def testWithUserDefinedImport(self):
        """test cPickle calling a user defined import function."""
        # This tests the fix for http://bugs.jython.org/issue1665
        # setup
        original_import = builtins.__import__
        def import_hook(name, _globals=None, locals=None, fromlist=None, level= -1):
            return original_import(name, _globals, locals, fromlist, level)
    
        # test
        builtins.__import__ = import_hook
        try:
            if "no_such_module" in sys.modules:
                del sys.modules["no_such_module"]  # force cPickle to call __import__
            self.assertRaises(ImportError, pickle.loads, pickle.GLOBAL + "no_such_module\n" + "no_such_class\n")
        finally:
            builtins.__import__ = original_import



def test_main():
    support.run_unittest(CPickleTestCase)


if __name__ == '__main__':
    test_main()
