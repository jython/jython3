import pickle
import unittest
from io import StringIO
from test.pickletester import AbstractPickleTests, AbstractPickleModuleTests
from test import support


class ApproxFloat(unittest.TestCase):
    # FIXME for Jython: remove this class - and its use from bases in
    # subsequent test classes - when we can guarantee that floats that
    # are pickled by cPickle are exact in the same way they are on
    # CPython
    
    def test_float(self):
        from test.pickletester import protocols

        test_values = [0.0, 4.94e-324, 1e-310, 7e-308, 6.626e-34, 0.1, 0.5,
                       3.14, 263.44582062374053, 6.022e23, 1e30]
        test_values = test_values + [-x for x in test_values]
        for proto in protocols:
            for value in test_values:
                pickle = self.dumps(value, proto)
                got = self.loads(pickle)
                self.assertAlmostEqual(value, got)


class cPickleTests(ApproxFloat, AbstractPickleTests, AbstractPickleModuleTests):

    def setUp(self):
        self.dumps = pickle.dumps
        self.loads = pickle.loads

    error = pickle.BadPickleGet
    module = cPickle

    @unittest.skipIf(support.is_jython, "FIXME: not working on Jython")
    def test_callapi(self):
        pass

    @unittest.skipIf(support.is_jython, "FIXME: not working on Jython")
    def test_dynamic_class(self):
        pass


class cPicklePicklerTests(ApproxFloat, AbstractPickleTests):

    def dumps(self, arg, proto=0):
        f = StringIO()
        p = pickle.Pickler(f, proto)
        p.dump(arg)
        f.seek(0)
        return f.read()

    def loads(self, buf):
        f = StringIO(buf)
        p = pickle.Unpickler(f)
        return p.load()

    error = pickle.BadPickleGet

    @unittest.skipIf(support.is_jython, "FIXME: not working on Jython")
    def test_dynamic_class(self):
        pass


class cPickleListPicklerTests(AbstractPickleTests):

    def dumps(self, arg, proto=0):
        p = pickle.Pickler(proto)
        p.dump(arg)
        return p.getvalue()

    def loads(self, *args):
        f = StringIO(args[0])
        p = pickle.Unpickler(f)
        return p.load()

    error = pickle.BadPickleGet

class cPickleFastPicklerTests(ApproxFloat, AbstractPickleTests):

    def dumps(self, arg, proto=0):
        f = StringIO()
        p = pickle.Pickler(f, proto)
        p.fast = 1
        p.dump(arg)
        f.seek(0)
        return f.read()

    def loads(self, *args):
        f = StringIO(args[0])
        p = pickle.Unpickler(f)
        return p.load()

    error = pickle.BadPickleGet

    def test_recursive_list(self):
        self.assertRaises(ValueError,
                          AbstractPickleTests.test_recursive_list,
                          self)

    def test_recursive_inst(self):
        self.assertRaises(ValueError,
                          AbstractPickleTests.test_recursive_inst,
                          self)

    def test_recursive_dict(self):
        self.assertRaises(ValueError,
                          AbstractPickleTests.test_recursive_dict,
                          self)

    def test_recursive_multi(self):
        self.assertRaises(ValueError,
                          AbstractPickleTests.test_recursive_multi,
                          self)

    def test_nonrecursive_deep(self):
        # If it's not cyclic, it should pickle OK even if the nesting
        # depth exceeds PY_CPICKLE_FAST_LIMIT.  That happens to be
        # 50 today.  Jack Jansen reported stack overflow on Mac OS 9
        # at 64.
        a = []
        for i in range(60):
            a = [a]
        b = self.loads(self.dumps(a))
        self.assertEqual(a, b)

    @unittest.skipIf(support.is_jython, "FIXME: not working on Jython")
    def test_dynamic_class(self):
        pass


def test_main():
    tests = [
        cPickleTests,
        cPicklePicklerTests,
        cPickleListPicklerTests,
        cPickleFastPicklerTests
    ]
    if support.is_jython:
        # FIXME Jython currently doesn't support list based picklers
        tests.remove(cPickleListPicklerTests)
        # FIXME these cause NullPointerException on Jython
        del cPickleFastPicklerTests.test_recursive_list
        del cPickleFastPicklerTests.test_recursive_inst
        del cPickleFastPicklerTests.test_recursive_dict
        del cPickleFastPicklerTests.test_recursive_multi


    support.run_unittest(*tests)

if __name__ == "__main__":
    test_main()
