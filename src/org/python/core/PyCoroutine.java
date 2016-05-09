package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "coroutine", doc = BuiltinDocs.coroutine_doc)
public class PyCoroutine extends PyGenerator {
    public PyCoroutine(PyFrame frame, PyObject closure) {
        super(frame, closure);
    }

    @ExposedMethod(doc = BuiltinDocs.coroutine_send_doc)
    final PyObject coroutine_send(PyObject value) {
        return generator_send(value);
    }

    @ExposedMethod(names="throw", defaults={"null", "null"}, doc = BuiltinDocs.coroutine_throw_doc)
    final PyObject coroutine_throw$(PyObject type, PyObject value, PyObject tb) {
        return generator_throw$(type, value, tb);
    }

    @ExposedMethod(doc = BuiltinDocs.coroutine_close_doc)
    final PyObject coroutine_close() {
        return generator_close();
    }
}
