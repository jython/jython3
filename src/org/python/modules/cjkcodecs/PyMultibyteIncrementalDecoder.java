package org.python.modules.cjkcodecs;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "MultibyteIncrementalDecoder")
public class PyMultibyteIncrementalDecoder extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyMultibyteIncrementalDecoder.class);

    @ExposedMethod
    public final PyObject MultibyteIncrementalDecoder_decode(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("decode", args, keywords, "input", "final");
        PyObject input = ap.getPyObject(0);
        boolean final_ = ap.getPyObject(1, Py.False).__bool__();
        return Py.None;
    }
}
