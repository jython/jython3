The following are simply not the strength of Java
============

1. POSIX compatibility is wrong, simply ditch all of that

2. IO should all based on java.nio, all native IO should be removed

3. process fork/exec should be removed, simply use thread

4. other stuff that involves JNI/JNA/JNR should not be supported

Objective
=======

A language that has the same syntax, but a very variant standard library, to
utilize the power of JVM

# known bugs

A TryExcept node nested in a With stmt have their scope mixed. (looks like it
works, cannot reproduce)
e.g.

```python
with Foo() as f:
  try:
    # do something with f
  except: # <= the with scope ends here 
    raise xxx # this exception escapes the with handler
```

# TODOs

1. Currently the native module / types methods are reflected, it would be better
to call the method directly (by creating a vtable, don't know why it's not done
so, in reality native types are not as efficient as python types in terms of
method dispatching), or at least keep the method handle in the proxy

(DONE)2. `_call_with_frames_removed` in `importlib._bootstrap` should be respected,
   use the same trick as CPython to remove the frames from stacktrace

3. remove unused import related code from `org.python.core.imp`
