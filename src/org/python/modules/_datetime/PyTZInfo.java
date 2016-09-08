package org.python.modules._datetime;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyLong;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

import java.time.ZoneId;
import java.time.ZoneOffset;

@ExposedType(name = "datetime.tzinfo")
public class PyTZInfo extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyTZInfo.class);

    private ZoneId zoneId;

    public PyTZInfo() {
        super(TYPE);
    }

    public PyTZInfo(PyType subType, String zone) {
        this(subType, ZoneId.of(zone));
    }

    public PyTZInfo(PyType subType, ZoneId zone) {
        super(subType);
        zoneId = zone;
    }

    @ExposedNew
    final static PyObject tzinfo_new(PyNewWrapper new_, boolean init, PyType subtype,
                                     PyObject[] args, String[] keywords) {
        return new PyTZInfo();
    }
}
