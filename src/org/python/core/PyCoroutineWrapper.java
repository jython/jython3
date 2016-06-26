package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "coroutine_wrapper", doc = BuiltinDocs.coroutine_wrapper_doc)
public class PyCoroutineWrapper extends PyObject {
    private PyCoroutine cw_coroutine;
    public static final PyType TYPE = PyType.fromClass(PyCoroutineWrapper.class);

    public PyCoroutineWrapper(PyCoroutine coroutine) {
        super(TYPE);
        cw_coroutine = coroutine;
    }

    public PyObject send(PyObject value) {
        return coroutine_wrapper_send(value);
    }

    @ExposedMethod(doc = BuiltinDocs.coroutine_wrapper_send_doc)
    final PyObject coroutine_wrapper_send(PyObject value) {
        return cw_coroutine.send(value);
    }

    public PyObject close() {
        return coroutine_wrapper_close();
    }

    @ExposedMethod(doc = BuiltinDocs.coroutine_wrapper_close_doc)
    final PyObject coroutine_wrapper_close() {
        return cw_coroutine.close();
    }

    public PyObject throw$(PyObject type, PyObject value, PyObject tb) {
        return coroutine_wrapper_throw$(type, value, tb);
    }

    @ExposedMethod(names = "throw", doc = BuiltinDocs.coroutine_wrapper_throw_doc)
    final PyObject coroutine_wrapper_throw$(PyObject type, PyObject value, PyObject tb) {
        return cw_coroutine.throw$(type, value, tb);
    }

    public PyObject __iter__() {
        return coroutine_wrapper___iter__();
    }

    @ExposedMethod(doc = BuiltinDocs.coroutine_wrapper___iter___doc)
    final PyObject coroutine_wrapper___iter__() {
        return this;
    }

    public PyObject __next__() {
        return coroutine_wrapper___next__();
    }

    @ExposedMethod(doc = BuiltinDocs.coroutine_wrapper___next___doc)
    final PyObject coroutine_wrapper___next__() {
        return cw_coroutine.send(Py.None);
    }
}