package org.python.core;

/**
 * Created by isaiah on 5/18/16.
 */
public interface AsyncContextManager {
    // Both method returns an awaitable
    PyObject __aenter__(ThreadState ts);
    PyObject __aexit__(ThreadState ts, PyException exception);

    // the exception that passed to __aexit__
    PyException exception();
}
