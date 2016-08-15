package org.python.modules.cjkcodecs;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "MultibyteStreamReader")
public class PyMultibyteStreamReader extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyMultibyteStreamReader.class);

    @ExposedMethod
    public final PyObject MultibyteStreamReader_read(PyObject sizeobj) {
        return Py.None;
    }
}
