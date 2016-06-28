package org.python.core;

/**
 * Created by isaiah on 6/26/16.
 */
public class AsyncIterator {
    private final PyObject anext;
    public AsyncIterator(PyObject obj) {
        anext = obj.__getattr__("__anext__");
    }

    public static Object getIter(PyObject obj) {
        PyObject aiter = obj.invoke("__aiter__");
        if (aiter instanceof PyCoroutine) {
            return aiter;
        }
        return new AsyncIterator(aiter);
    }

    public PyObject __anext__() {
        PyObject coro = anext.__call__();
        return Py.getAwaitableIter(coro);
    }
}
