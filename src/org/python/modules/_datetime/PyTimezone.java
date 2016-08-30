package org.python.modules._datetime;

import org.python.core.ArgParser;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

import java.time.ZoneOffset;

@ExposedType(name = "datetime.timezone")
public class PyTimezone extends PyTZInfo {
    public static final PyType TYPE = PyType.fromClass(PyTimezone.class);

    private ZoneOffset offset;

    public PyTimezone() {
        this(ZoneOffset.UTC);
    }

    public PyTimezone(ZoneOffset offset) {
        super(TYPE, offset);
        this.offset = offset;
    }

    @ExposedNew
    final static PyObject timezone_new(PyNewWrapper new_, boolean init, PyType subtype,
                                       PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("timezone", args, keywords, "name", "utcoffset");
        String tzname = ap.getString(0);
        return new PyTimezone(ZoneOffset.of(tzname));
    }

    @ExposedMethod(names = "utcoffset")
    public int timezone_utcoffset() {
        return offset.compareTo(ZoneOffset.UTC);
    }
}
