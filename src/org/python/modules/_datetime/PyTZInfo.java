package org.python.modules._datetime;

import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "datetime.tzinfo")
public class PyTZInfo extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyTZInfo.class);

    public PyTZInfo() {
        super(TYPE);
    }

    public PyTZInfo(PyType subType) {
        super(subType);
    }

    @ExposedNew
    final static PyObject tzinfo_new(PyNewWrapper new_, boolean init, PyType subtype,
                                     PyObject[] args, String[] keywords) {
        return new PyTZInfo();
    }
}
