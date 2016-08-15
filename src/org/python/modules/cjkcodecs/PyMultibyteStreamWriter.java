package org.python.modules.cjkcodecs;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

/**
 * Created by isaiah on 8/15/16.
 */
@ExposedType(name = "MultibyteStreamWriter")
public class PyMultibyteStreamWriter extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyMultibyteStreamWriter.class);

    @ExposedMethod
    public final PyObject MultibyteStreamWriter_write(PyObject strobj) {
        return Py.None;
    }
}
