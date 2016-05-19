""" Python character mapping codec test

This uses the test codec in testcodec.py and thus also tests the
encodings package lookup scheme.

Written by Marc-Andre Lemburg (mal@lemburg.com).

(c) Copyright 2000 Guido van Rossum.

"""#"

import test.support, unittest

import codecs

# Register a search function which knows about our codec
def codec_search_function(encoding):
    if encoding == 'testcodec':
        from test import testcodec
        return tuple(testcodec.getregentry())
    return None

codecs.register(codec_search_function)

# test codec's name (see test/testcodec.py)
codecname = 'testcodec'

class CharmapCodecTest(unittest.TestCase):
    def test_constructorx(self):
        self.assertEqual(str(b'abc', codecname), 'abc')
        self.assertEqual(str(b'xdef', codecname), 'abcdef')
        self.assertEqual(str(b'defx', codecname), 'defabc')
        self.assertEqual(str(b'dxf', codecname), 'dabcf')
        self.assertEqual(str(b'dxfx', codecname), 'dabcfabc')

    def test_encodex(self):
        self.assertEqual('abc'.encode(codecname), b'abc')
        self.assertEqual('xdef'.encode(codecname), b'abcdef')
        self.assertEqual('defx'.encode(codecname), b'defabc')
        self.assertEqual('dxf'.encode(codecname), b'dabcf')
        self.assertEqual('dxfx'.encode(codecname), b'dabcfabc')

    # This test isn't working on Ubuntu on an Apple Intel powerbook,
    # Jython 2.7b1+ (default:6b4a1088566e, Feb 10 2013, 14:36:47) 
    # [OpenJDK 64-Bit Server VM (Oracle Corporation)] on java1.7.0_09
    @unittest.skipIf(test.support.is_jython,
                     "FIXME: Currently not working on jython")
    def test_constructory(self):
        self.assertEqual(str(b'ydef', codecname), 'def')
        self.assertEqual(str(b'defy', codecname), 'def')
        self.assertEqual(str(b'dyf', codecname), 'df')
        self.assertEqual(str(b'dyfy', codecname), 'df')

    def test_maptoundefined(self):
        self.assertRaises(UnicodeError, str, b'abc\001', codecname)

def test_main():
    test.support.run_unittest(CharmapCodecTest)

if __name__ == "__main__":
    test_main()
