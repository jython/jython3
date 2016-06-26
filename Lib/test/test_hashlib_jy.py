# encoding: utf-8
"""Misc hashlib tests

Made for Jython.
"""
import hashlib
import unittest
from array import array
from test import support

class HashlibTestCase(unittest.TestCase):

    def test_unicode(self):
        self.assertEqual(hashlib.md5('foo').hexdigest(),
                         'acbd18db4cc2f85cedef654fccc4a4d8')
        self.assertRaises(UnicodeEncodeError, hashlib.md5, 'Gráin amháiñ')

    def test_array(self):
        self.assertEqual(hashlib.sha1(array('c', 'hovercraft')).hexdigest(),
                         '496df4d8de2c71973d7e917c4fbe57e6ad46d738')
        intarray = array('i', list(range(5)))
        self.assertEqual(hashlib.sha1(intarray).hexdigest(),
                         hashlib.sha1(intarray.tostring()).hexdigest())


def test_main():
    support.run_unittest(HashlibTestCase)


if __name__ == '__main__':
    test_main()
