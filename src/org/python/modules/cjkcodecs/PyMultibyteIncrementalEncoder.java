package org.python.modules.cjkcodecs;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "MultibyteIncrementalEncoder")
public class PyMultibyteIncrementalEncoder extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyMultibyteIncrementalEncoder.class);

    @ExposedMethod
    public final PyObject MultibyteIncrementalEncoder_encode(PyObject[] args, String[] keywords) {
        return Py.None;
    }
}
