package org.python.modules._datetime;

import org.python.core.ArgParser;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

import java.time.ZoneId;
import java.time.ZoneOffset;

@ExposedType(name = "_datetime.tzinfo")
public class PyTZInfo extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyTZInfo.class);

    private ZoneId zoneId;

    public PyTZInfo(String zone) {
        super(TYPE);
        zoneId = ZoneId.of(zone);
    }

    public PyTZInfo(PyType subType, ZoneId zone) {
        super(subType);
        zoneId = zone;
    }

    @ExposedNew
    final static PyObject tzinfo_new(PyNewWrapper new_, boolean init, PyType subtype,
                                     PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("tzinfo", args, keywords, "name", "utcoffset");
        String tzname = ap.getString(0);
        return new PyTimezone(ZoneOffset.of(tzname));
    }
}
