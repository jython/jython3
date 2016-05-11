# PR#192, dir(func) and dir(method) returning []

def test1():
    'Test function 1'
    pass

def test2(a, b=2, c=3):
    pass

attrs = dir(test1)[:]
for attr in ['__doc__', '__name__', 'func_code', 'func_defaults',
             'func_doc', 'func_globals', 'func_name']:
    attrs.remove(attr)
assert not attrs

assert test1.__doc__ == test1.__doc__ == 'Test function 1'
assert test1.__name__ == test1.__name__ == 'test1'
assert test1.__code__
assert test1.__defaults__ is None
assert test1.__globals__ == globals()
assert test2.__defaults__ == (2, 3)

co = test2.__code__
attrs = dir(co)[:]
for attr in ['co_name', 'co_argcount', 'co_varnames', 'co_filename',
             'co_firstlineno', 'co_flags']:
    attrs.remove(attr)
##assert not attrs

flags = 0x4 | 0x8

assert co.co_name == 'test2'
assert co.co_argcount == 3
assert co.co_varnames == ('a', 'b', 'c')
assert co.co_filename
assert co.co_firstlineno
assert (co.co_flags & flags) == 0

def test3(a, *args, **kw):
    pass

assert (test3.__code__.co_flags & flags) == flags

class Foo:
    def method(self):
        """This is a method"""
        pass

attrs = dir(Foo.method)[:]
for attr in ['im_self', 'im_func', 'im_class', '__doc__', '__name__']:
    attrs.remove(attr)
assert not attrs

assert Foo.method.__self__ is None
assert Foo.method.__self__.__class__ == Foo
assert Foo.method.__func__
assert Foo.method.__func__.__name__ == Foo.method.__name__
assert Foo.method.__func__.__doc__ == Foo.method.__doc__

f = Foo()
m = f.method
assert m.__self__ == f
assert m.__self__.__class__ == Foo
assert m.__func__ == Foo.method.__func__
assert m.__name__ == Foo.method.__name__
assert m.__doc__ == Foo.method.__doc__

class Baz:
    pass

try:
    m.__self__.__class__ = Baz
    assert 0
except TypeError:
    pass

try:
    m.im_stuff = 7
    assert 0
except AttributeError:
    pass
