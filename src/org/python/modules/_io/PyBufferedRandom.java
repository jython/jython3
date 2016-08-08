package org.python.modules._io;

import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedType;

@ExposedType(name = "_io.BufferedRandom")
public class PyBufferedRandom extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyBufferedRandom.class);

    public PyBufferedRandom() {
        super(TYPE);
    }
}
