// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * A super class for all python code implementations.
 */
public abstract class PyCode extends PyObject
{
    public String co_name;

    abstract public PyObject call(ThreadState state, PyFrame frame, PyObject closure);

    public PyObject call(PyFrame frame) {
        return call(Py.getThreadState(), frame);
    }

    public PyObject call(ThreadState state, PyFrame frame) {
        return call(state, frame, null);
    }

    abstract public PyObject call(ThreadState state,
                                  PyObject args[], String keywords[],
                                  PyObject globals, PyObject[] defaults,
                                  PyDictionary kw_defaults, PyObject closure);

    abstract public PyObject call(ThreadState state,
                                  PyObject self, PyObject args[],
                                  String keywords[],
                                  PyObject globals, PyObject[] defaults,
                                  PyDictionary kw_defaults, PyObject closure);

    abstract public PyObject call(ThreadState state,
                                  PyObject globals, PyObject[] defaults,
                                  PyDictionary kw_defaults, PyObject closure);

    abstract public PyObject call(ThreadState state,
                                  PyObject arg1, PyObject globals,
                                  PyObject[] defaults,
                                  PyDictionary kw_defaults, PyObject closure);

    abstract public PyObject call(ThreadState state,
                                  PyObject arg1, PyObject arg2,
                                  PyObject globals, PyObject[] defaults,
                                  PyDictionary kw_defaults, PyObject closure);

    abstract public PyObject call(ThreadState state,
                                  PyObject arg1, PyObject arg2, PyObject arg3,
                                  PyObject globals, PyObject[] defaults,
                                  PyDictionary kw_defaults, PyObject closure);

    abstract public PyObject call(ThreadState state,
                                  PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4,
                                  PyObject globals, PyObject[] defaults,
                                  PyDictionary kw_defaults, PyObject closure);

}
