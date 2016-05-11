# -*- coding: iso-8859-1 -*-
import unittest, test.test_support
import sys, io

class SysModuleTest(unittest.TestCase):

    def test_original_displayhook(self):
        import builtins
        savestdout = sys.stdout
        out = io.StringIO()
        sys.stdout = out

        dh = sys.__displayhook__

        self.assertRaises(TypeError, dh)
        if hasattr(__builtin__, "_"):
            del builtins._

        dh(None)
        self.assertEqual(out.getvalue(), "")
        self.assertTrue(not hasattr(__builtin__, "_"))
        dh(42)
        self.assertEqual(out.getvalue(), "42\n")
        self.assertEqual(builtins._, 42)


        if not test.test_support.is_jython:
            del sys.stdout
            self.assertRaises(RuntimeError, dh, 42)

        sys.stdout = savestdout

    def test_lost_displayhook(self):
        olddisplayhook = sys.displayhook
        del sys.displayhook
        code = compile("42", "<string>", "single")
        self.assertRaises(RuntimeError, eval, code)
        sys.displayhook = olddisplayhook

    def test_custom_displayhook(self):
        olddisplayhook = sys.displayhook
        def baddisplayhook(obj):
            raise ValueError
        sys.displayhook = baddisplayhook
        code = compile("42", "<string>", "single")
        self.assertRaises(ValueError, eval, code)
        sys.displayhook = olddisplayhook

    def test_original_excepthook(self):
        savestderr = sys.stderr
        err = io.StringIO()
        sys.stderr = err

        eh = sys.__excepthook__

        self.assertRaises(TypeError, eh)
        try:
            raise ValueError(42)
        except ValueError as exc:
            eh(*sys.exc_info())

        sys.stderr = savestderr
        self.assertTrue(err.getvalue().endswith("ValueError: 42\n"))

    # FIXME: testing the code for a lost or replaced excepthook in
    # Python/pythonrun.c::PyErr_PrintEx() is tricky.

    def test_exc_clear(self):
        self.assertRaises(TypeError, sys.exc_clear, 42)

        # Verify that exc_info is present and matches exc, then clear it, and
        # check that it worked.
        def clear_check(exc):
            typ, value, traceback = sys.exc_info()
            self.assertTrue(typ is not None)
            self.assertTrue(value is exc)
            self.assertTrue(traceback is not None)

            sys.exc_clear()

            typ, value, traceback = sys.exc_info()
            self.assertTrue(typ is None)
            self.assertTrue(value is None)
            self.assertTrue(traceback is None)

        def clear():
            try:
                raise ValueError(42)
            except ValueError as exc:
                clear_check(exc)

        # Raise an exception and check that it can be cleared
        clear()

        # Verify that a frame currently handling an exception is
        # unaffected by calling exc_clear in a nested frame.
        try:
            raise ValueError(13)
        except ValueError as exc:
            typ1, value1, traceback1 = sys.exc_info()
            clear()
            typ2, value2, traceback2 = sys.exc_info()

            self.assertTrue(typ1 is typ2)
            self.assertTrue(value1 is exc)
            self.assertTrue(value1 is value2)
            self.assertTrue(traceback1 is traceback2)

        # Check that an exception can be cleared outside of an except block
        clear_check(exc)

    def test_exit(self):
        self.assertRaises(TypeError, sys.exit, 42, 42)

        # call without argument
        try:
            sys.exit(0)
        except SystemExit as exc:
            self.assertEqual(exc.code, 0)
        except:
            self.fail("wrong exception")
        else:
            self.fail("no exception")

        # call with tuple argument with one entry
        # entry will be unpacked
        try:
            sys.exit(42)
        except SystemExit as exc:
            self.assertEqual(exc.code, 42)
        except:
            self.fail("wrong exception")
        else:
            self.fail("no exception")

        # call with integer argument
        try:
            sys.exit((42,))
        except SystemExit as exc:
            self.assertEqual(exc.code, 42)
        except:
            self.fail("wrong exception")
        else:
            self.fail("no exception")

        # call with string argument
        try:
            sys.exit("exit")
        except SystemExit as exc:
            self.assertEqual(exc.code, "exit")
        except:
            self.fail("wrong exception")
        else:
            self.fail("no exception")

        # call with tuple argument with two entries
        try:
            sys.exit((17, 23))
        except SystemExit as exc:
            self.assertEqual(exc.code, (17, 23))
        except:
            self.fail("wrong exception")
        else:
            self.fail("no exception")

    def test_getdefaultencoding(self):
        if test.test_support.have_unicode:
            self.assertRaises(TypeError, sys.getdefaultencoding, 42)
            # can't check more than the type, as the user might have changed it
            self.assertTrue(isinstance(sys.getdefaultencoding(), str))

    # testing sys.settrace() is done in test_trace.py
    # testing sys.setprofile() is done in test_profile.py

    def test_setcheckinterval(self):
        self.assertRaises(TypeError, sys.setcheckinterval)
        orig = sys.getcheckinterval()
        for n in 0, 100, 120, orig: # orig last to restore starting state
            sys.setcheckinterval(n)
            self.assertEqual(sys.getcheckinterval(), n)

    def test_recursionlimit(self):
        self.assertRaises(TypeError, sys.getrecursionlimit, 42)
        oldlimit = sys.getrecursionlimit()
        self.assertRaises(TypeError, sys.setrecursionlimit)
        self.assertRaises(ValueError, sys.setrecursionlimit, -42)
        sys.setrecursionlimit(10000)
        self.assertEqual(sys.getrecursionlimit(), 10000)
        sys.setrecursionlimit(oldlimit)

    def test_getwindowsversion(self):
        if hasattr(sys, "getwindowsversion"):
            v = sys.getwindowsversion()
            self.assertTrue(isinstance(v, tuple))
            self.assertEqual(len(v), 5)
            self.assertTrue(isinstance(v[0], int))
            self.assertTrue(isinstance(v[1], int))
            self.assertTrue(isinstance(v[2], int))
            self.assertTrue(isinstance(v[3], int))
            self.assertTrue(isinstance(v[4], str))

    def test_dlopenflags(self):
        if hasattr(sys, "setdlopenflags"):
            self.assertTrue(hasattr(sys, "getdlopenflags"))
            self.assertRaises(TypeError, sys.getdlopenflags, 42)
            oldflags = sys.getdlopenflags()
            self.assertRaises(TypeError, sys.setdlopenflags)
            sys.setdlopenflags(oldflags+1)
            self.assertEqual(sys.getdlopenflags(), oldflags+1)
            sys.setdlopenflags(oldflags)

    def test_refcount(self):
        self.assertRaises(TypeError, sys.getrefcount)
        c = sys.getrefcount(None)
        n = None
        self.assertEqual(sys.getrefcount(None), c+1)
        del n
        self.assertEqual(sys.getrefcount(None), c)
        if hasattr(sys, "gettotalrefcount"):
            self.assertTrue(isinstance(sys.gettotalrefcount(), int))

    def test_getframe(self):
        self.assertRaises(TypeError, sys._getframe, 42, 42)
        self.assertRaises(ValueError, sys._getframe, 2000000000)
        self.assertTrue(
            SysModuleTest.test_getframe.__func__.__code__ \
            is sys._getframe().f_code
        )

    def test_attributes(self):
        if not test.test_support.is_jython:
            self.assertTrue(isinstance(sys.api_version, int))
        self.assertTrue(isinstance(sys.argv, list))
        self.assertTrue(sys.byteorder in ("little", "big"))
        self.assertTrue(isinstance(sys.builtin_module_names, tuple))
        self.assertTrue(isinstance(sys.copyright, str))
        self.assertTrue(isinstance(sys.exec_prefix, str))
        self.assertTrue(isinstance(sys.executable, str))
        self.assertTrue(isinstance(sys.hexversion, int))
        self.assertTrue(isinstance(sys.maxsize, int))
        self.assertTrue(isinstance(sys.maxunicode, int))
        self.assertTrue(isinstance(sys.platform, str))
        self.assertTrue(isinstance(sys.prefix, str))
        self.assertTrue(isinstance(sys.version, str))
        vi = sys.version_info
        self.assertTrue(isinstance(vi, tuple))
        self.assertEqual(len(vi), 5)
        self.assertTrue(isinstance(vi[0], int))
        self.assertTrue(isinstance(vi[1], int))
        self.assertTrue(isinstance(vi[2], int))
        self.assertTrue(vi[3] in ("alpha", "beta", "candidate", "final"))
        self.assertTrue(isinstance(vi[4], int))

    def test_ioencoding(self):  # from v2.7 test
        import subprocess, os
        env = dict(os.environ)

        # Test character: cent sign, encoded as 0x4A (ASCII J) in CP424,
        # not representable in ASCII.

        env["PYTHONIOENCODING"] = "cp424"
        p = subprocess.Popen([sys.executable, "-c", 'print unichr(0xa2)'],
                             stdout = subprocess.PIPE, env=env)
        out = p.stdout.read().strip()
        self.assertEqual(out, chr(0xa2).encode("cp424"))

        env["PYTHONIOENCODING"] = "ascii:replace"
        p = subprocess.Popen([sys.executable, "-c", 'print unichr(0xa2)'],
                             stdout = subprocess.PIPE, env=env)
        out = p.stdout.read().strip()
        self.assertEqual(out, '?')


def test_main():
    if test.test_support.is_jython:
        del SysModuleTest.test_lost_displayhook
        del SysModuleTest.test_refcount
        del SysModuleTest.test_setcheckinterval
    test.test_support.run_unittest(SysModuleTest)

if __name__ == "__main__":
    test_main()
