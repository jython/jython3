package org.python.core;

import org.python.core.finalization.FinalizeTrigger;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "coroutine", base = PyObject.class, isBaseType = false, doc = BuiltinDocs.coroutine_doc)
public class PyCoroutine extends PyGenerator {
    public static final PyType TYPE = PyType.fromClass(PyCoroutine.class);

    @ExposedGet
    protected PyFrame cr_frame;

    @ExposedGet
    protected PyCode cr_code;

    @ExposedGet
    protected boolean cr_running;

    public PyCoroutine(PyFrame frame, PyObject closure) {
        super(TYPE, frame, closure);
        cr_frame = gi_frame;
        cr_code = gi_code;
        cr_running = gi_running;
        FinalizeTrigger.ensureFinalizer(this);
    }

    public PyObject send(PyObject value) {
        return coroutine_send(value);
    }

    @ExposedMethod(doc = BuiltinDocs.coroutine_send_doc)
    final PyObject coroutine_send(PyObject value) {
        return generator_send(value);
    }

    public PyObject throw$(PyObject type, PyObject value, PyObject tb) {
        return coroutine_throw$(type, value, tb);
    }

    @ExposedMethod(names="throw", defaults={"null", "null"}, doc = BuiltinDocs.coroutine_throw_doc)
    final PyObject coroutine_throw$(PyObject type, PyObject value, PyObject tb) {
        return generator_throw$(type, value, tb);
    }

    @Override
    public PyObject __iter__() {
        return coroutine___iter__();
    }

    @ExposedMethod()
    final PyObject coroutine___iter__() {
        throw Py.TypeError("'coroutine' object is not iterable");
    }

    public PyObject close() {
        return coroutine_close();
    }

    @ExposedMethod(doc = BuiltinDocs.coroutine_close_doc)
    final PyObject coroutine_close() {
        return generator_close();
    }

    public PyObject __await__() {
        return coroutine___await__();
    }

    @ExposedMethod(doc = BuiltinDocs.coroutine___await___doc)
    final PyObject coroutine___await__() {
        return new PyCoroutineWrapper(this);
    }

    // Simply those attributes are not inherited
    @ExposedGet(doc = BuiltinDocs.coroutine___name___doc)
    final String __name__() {
        return getName();
    }

    @ExposedGet(doc = BuiltinDocs.coroutine___qualname___doc)
    final String __qualname__() {
        return getQualname();
    }
}
