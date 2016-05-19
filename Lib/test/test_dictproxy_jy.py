"""Test the readonly dict wrapper dictproxy

Made for Jython.
"""
import sys
import unittest
from test import support

class DictproxyTestCase(unittest.TestCase):

    def test_dictproxy(self):
        proxy = type.__dict__
        first_key = next(iter(proxy))

        self.assertTrue(isinstance(first_key, str))
        self.assertTrue(first_key in proxy)
        self.assertTrue(first_key in proxy)
        self.assertEqual(proxy[first_key], proxy.get(first_key))
        self.assertEqual(proxy.get('NOT A KEY', 'foo'), 'foo')

        proxy_len = len(proxy)
        self.assertTrue(isinstance(proxy_len, int) and proxy_len > 2)
        self.assertTrue(proxy_len == len(list(proxy.keys())) == len(list(proxy.items())) ==
                     len(list(proxy.values())) == len(list(proxy.keys())) ==
                     len(list(proxy.items())) ==
                     len(list(proxy.values())))
        self.assertTrue(isinstance(list(proxy.items())[0], tuple))
        self.assertTrue(isinstance(next(iter(proxy.items())), tuple))

        copy = proxy.copy()
        self.assertTrue(proxy is not copy)
        self.assertEqual(len(proxy), len(copy))

    def test_dictproxy_equality(self):
        self.assertEqual(type.__dict__, type.__dict__)
        self.assertEqual(type.__dict__, type.__dict__.copy())
        self.assertEqual(type.__dict__, dict(type.__dict__))
        self.assertEqual(cmp(type.__dict__, type.__dict__), 0)
        self.assertEqual(cmp(type.__dict__, type.__dict__.copy()), 0)
        self.assertEqual(cmp(type.__dict__, dict(type.__dict__)), 0)


def test_main():
    support.run_unittest(DictproxyTestCase)


if __name__ == '__main__':
    test_main()
