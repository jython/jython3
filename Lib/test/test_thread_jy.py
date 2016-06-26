import _thread
import synchronize
import unittest
import test.support
from java.lang import Runnable, Thread
from java.util.concurrent import CountDownLatch
import collections

class AllocateLockTest(unittest.TestCase):

    def test_lock_type(self):
        """thread.LockType should exist"""
        t = _thread.LockType
        self.assertEqual(t, type(_thread.allocate_lock()),
            "thread.LockType has wrong value")

class SynchronizeTest(unittest.TestCase):

    def test_make_synchronized(self):
        doneSignal = CountDownLatch(10)
        class SynchedRunnable(Runnable):
            i = 0
            def run(self):
                self.i += 1
                doneSignal.countDown()
            run = synchronize.make_synchronized(run)
        runner = SynchedRunnable()
        for _ in range(10):
            Thread(runner).start()
        doneSignal.await()
        self.assertEqual(10, runner.i)

    def test_synchronized_callable(self):
        self.assertTrue(isinstance(synchronize.make_synchronized(lambda: None), collections.Callable))


def test_main():
    test.support.run_unittest(AllocateLockTest, SynchronizeTest)

if __name__ == "__main__":
    test_main()
