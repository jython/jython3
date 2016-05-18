package org.python.core;

/**
 * Created by isaiah on 5/18/16.
 */
public interface AsyncContextManager {
    PyObject __aenter__(ThreadState ts);
    boolean __aexit__(ThreadState ts, PyException exception);
}
