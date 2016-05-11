# PR#208, calling apply with bogus 3rd argument

def test(x):
    return x

assert 7 == test(*(7,))
assert 7 == test(*(), **{'x': 7})

try:
    test(*(1,), **7)
    print('TypeError expected')
except TypeError:
    pass

try:
    test(*(1,), **{7:3})
    print('TypeError expected')
except TypeError:
    pass

try:
    test(*(1,), **None)
    print('TypeError expected')
except TypeError:
    pass

