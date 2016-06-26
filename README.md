Jython: Python for the Java Platform

Welcome to Jython 3.5!

This repo is in the very early stages of development of a release of
Jython 3.5. Planned goals are language and runtime compatibility with
CPython 3.5, along with continued substantial support of the Python
ecosystem.

Please see ACKNOWLEDGMENTS for details about Jython's copyright,
license, contributors, and mailing lists; and NEWS for detailed
release notes, including bugs fixed, backwards breaking changes, and
new features.


TODO:

1. Subclass PyString to BaseBytes class, so that it can share most behaviour with
bytearray, rename to PyBytes

2. Replace most occurrance of PyString with PyUnicode, which are used as string
before.
