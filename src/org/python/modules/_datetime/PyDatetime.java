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
public class PyDatetime extends PyDate {
    public static final PyType TYPE = PyType.fromClass(PyDatetime.class);

    private LocalTime time;
    private ZoneOffset timezone;

    public PyDatetime(int year, int month, int day) {
        super(TYPE, year, month, day);
    }

    @ExposedNew
    final static PyObject datetime_new(PyNewWrapper new_, boolean init, PyType subtype,
                                       PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("date", args, keywords, "year", "month", "day");
        int year = ap.getInt(0);
        int month = ap.getInt(1);
        int day = ap.getInt(2);
        return new PyDatetime(year, month, day);
    }

    @ExposedMethod
    public final PyObject datetime_time() {
        return new PyTime(time, timezone);
    }
}
