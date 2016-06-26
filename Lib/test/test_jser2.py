from test import support
import unittest
from java.io import ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream
from org.python.util import PythonObjectInputStream

def serialize(o, special=False):
    b = ByteArrayOutputStream()
    objs = ObjectOutputStream(b)
    objs.writeObject(o)
    if not special:
        OIS = ObjectInputStream
    else:
        OIS = PythonObjectInputStream
    objs = OIS(ByteArrayInputStream(b.toByteArray()))
    return objs.readObject()

from jser2_classes import A, AJ, N, NL, NT

class TestJavaSerialisation(unittest.TestCase):

    def serialize_and_check(self, obj, special=False):
        obj1 = serialize(obj, special)
        self.assertEqual(obj, obj1)

    def test_list(self):
        self.serialize_and_check([1, "a", 3.0])

    def test_dict(self):
        self.serialize_and_check({'a': 3.0})

    def test_tuple(self):
        self.serialize_and_check((1, 'a'))

    def test_oldstyle(self):
        self.serialize_and_check(A('x'))

    def test_jsubcl(self):
        self.serialize_and_check(AJ('x'), special=True)

    def test_singletons(self):
        for v in (None, Ellipsis):
            v1 = serialize(v)
            self.assertTrue(v is v1)
            v1 = serialize((v,))[0]
            self.assertTrue(v is v1)

    def test_NotImplemented(self):
        # XXX serialize(NotImplemented) is None because of __tojava__
        v1 = serialize((NotImplemented,))[0]
        self.assertTrue(v1 is NotImplemented)

    def test_type(self):
        list1 = serialize(list)
        self.assertTrue(list1 is list)
        list1 = serialize((list,))[0]
        self.assertTrue(list1 is list)

    def test_user_type(self):
        N1 = serialize(N)
        self.assertTrue(N1 is N)
        N1 = serialize((N,))[0]
        self.assertTrue(N1 is N)

    def test_newstyle(self):
        self.serialize_and_check(N('x'))

    def test_newstyle_list(self):
        self.serialize_and_check(NL('x', 1, 2, 3))

    def test_newstyle_tuple(self):
        self.serialize_and_check(NT('x', 1, 2, 3))

def test_main():
    support.run_unittest(TestJavaSerialisation)

if __name__ == "__main__":
    test_main()
