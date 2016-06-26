import unittest
from test import support


class TupleTestCase(unittest.TestCase):
    def test_big_tuple(self):
        """Verify that fairly large collection literals of primitives can be constructed."""
        # use \n to separate to avoid parser problems
        s = eval("(" + ",\n".join((str(x) for x in range(64000))) +")")
        self.assertEqual(len(s), 64000)
        self.assertEqual(sum(s), 2047968000)


def test_main():
    support.run_unittest(TupleTestCase)


if __name__ == '__main__':
    test_main()
