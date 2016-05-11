import pr133.test
import imp

name = pr133.test.__name__
imp.reload(pr133.test)

if name != pr133.test.__name__:
    print('Name changed after reload')

imp.reload(pr133.test)

if name != pr133.test.__name__:
    print('Name changed after reload')
