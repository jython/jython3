package org.python.modules._datetime;

import org.python.core.ArgParser;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

import java.time.LocalTime;
import java.time.ZoneOffset;

@ExposedType(name = "_datetime.datetime")
public class PyDateTime extends PyDate {
    public static final PyType TYPE = PyType.fromClass(PyDateTime.class);

    private LocalTime time;
    private ZoneOffset timezone;

    public PyDateTime(PyType subType, int year, int month, int day) {
        super(subType, year, month, day);
    }

    @ExposedNew
    final static PyObject datetime_new(PyNewWrapper new_, boolean init, PyType subtype,
                                       PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("datetime", args, keywords, "year", "month", "day");
        int year = ap.getInt(0);
        int month = ap.getInt(1);
        int day = ap.getInt(2);
        return new PyDateTime(TYPE, year, month, day);
    }

    @ExposedMethod
    public final PyObject datetime_time() {
        return new PyTime(time, timezone);
    }
}
