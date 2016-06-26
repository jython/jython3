import unittest


class TestStrReturnsUnicode(unittest.TestCase):

    def test_join(self):
        self.assertEqual(str, type(''.join(['blah'])))

    def test_replace(self):
        self.assertEqual(str, type('hello'.replace('o', 'o')))

    def test_string_formatting_s(self):
        self.assertEqual(str, type('%s' % 'x'))
        self.assertEqual(str, type('%s %s' % ('x', 'y')))
        self.assertEqual(str, type('%(x)s' % {'x' : 'x'}))

    def test_string_formatting_r(self):
        self.assertEqual(str, type('%r' % 'x'))
        self.assertEqual(str, type('%r %r' % ('x', 'y')))
        self.assertEqual(str, type('%(x)r' % {'x' : 'x'}))

    def test_string_formatting_c(self):
        self.assertEqual(str, type('%c' % 'x'))
        self.assertEqual(str, type('%c %c' % ('x', 'y')))
        self.assertEqual(str, type('%(x)c' % {'x' : 'x'}))


class TestStrReturnsStr(unittest.TestCase):

    def test_join(self):
        self.assertEqual(str, type(''.join(['blah'])))

    def test_replace(self):
        self.assertEqual(str, type('hello'.replace('o', 'oo')))

    def test_string_formatting_s(self):
        self.assertEqual(str, type('%s' % 'x'))
        self.assertEqual(str, type('%s %s' % ('x', 'y')))
        self.assertEqual(str, type('%(x)s' % {'x' : 'xxx'}))

    def test_string_formatting_r(self):
        self.assertEqual(str, type('%r' % 'x'))
        self.assertEqual(str, type('%r %r' % ('x', 'y')))
        self.assertEqual(str, type('%(x)r' % {'x' : 'x'}))

    def test_string_formatting_c(self):
        self.assertEqual(str, type('%c' % 'x'))
        self.assertEqual(str, type('%c %c' % ('x', 'y')))
        self.assertEqual(str, type('%(x)c' % {'x' : 'x'}))


class TestUnicodeReturnsUnicode(unittest.TestCase):

    def test_join(self):
        self.assertEqual(str, type(''.join(['blah'])))
        self.assertEqual(str, type(''.join(['blah'])))

    def test_replace(self):
        self.assertEqual(str, type('hello'.replace('o', 'o')))
        self.assertEqual(str, type('hello'.replace('o', 'o')))
        self.assertEqual(str, type('hello'.replace('o', 'o')))
        self.assertEqual(str, type('hello'.replace('o', 'o')))

    def test_string_formatting_s(self):
        self.assertEqual(str, type('%s' % 'x'))
        self.assertEqual(str, type('%s' % 'x'))
        self.assertEqual(str, type('%s %s' % ('x', 'y')))
        self.assertEqual(str, type('%s %s' % ('x', 'y')))
        self.assertEqual(str, type('%(x)s' % {'x' : 'x'}))
        self.assertEqual(str, type('%(x)s' % {'x' : 'x'}))

    def test_string_formatting_r(self):
        self.assertEqual(str, type('%r' % 'x'))
        self.assertEqual(str, type('%r' % 'x'))
        self.assertEqual(str, type('%r %r' % ('x', 'y')))
        self.assertEqual(str, type('%r %r' % ('x', 'y')))
        self.assertEqual(str, type('%(x)r' % {'x' : 'x'}))
        self.assertEqual(str, type('%(x)r' % {'x' : 'x'}))

    def test_string_formatting_c(self):
        self.assertEqual(str, type('%c' % 'x'))
        self.assertEqual(str, type('%c' % 'x'))
        self.assertEqual(str, type('%c %c' % ('x', 'y')))
        self.assertEqual(str, type('%c %c' % ('x', 'y')))
        self.assertEqual(str, type('%(x)c' % {'x' : 'x'}))
        self.assertEqual(str, type('%(x)c' % {'x' : 'x'}))


if __name__ == '__main__':
    unittest.main()
