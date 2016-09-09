package org.python.modules._datetime;

import org.python.core.ArgParser;
import org.python.core.PyLong;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.modules.time.TimeModule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

@ExposedType(name = "datetime.datetime")
public class PyDateTime extends PyDate {
    public static final PyType TYPE = PyType.fromClass(PyDateTime.class);

    private LocalTime time;
    private ZoneOffset timezone;

    public PyDateTime(PyType type) {
        super(type);
        LocalDateTime now = LocalDateTime.now();
        date = now.toLocalDate();
        time = now.toLocalTime();
    }

    public PyDateTime(PyType subType, int year, int month, int day) {
        super(subType, year, month, day);
    }

    @ExposedNew
    final static PyObject datetime_new(PyNewWrapper new_, boolean init, PyType subtype,
                                       PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("datetime", args, keywords, "year", "month", "day", "hour", "minute", "second");
        int year = ap.getInt(0);
        int month = ap.getInt(1);
        int day = ap.getInt(2);
        return new PyDateTime(TYPE, year, month, day);
    }

    @ExposedMethod
    public final PyObject datetime_time() {
        return new PyTime(time, timezone);
    }

    @ExposedMethod
    public final PyObject datetime_ctime() {
        return new PyUnicode(LocalDateTime.of(date, time).format(TimeModule.DEFAULT_FORMAT_PY));
    }

    @ExposedMethod
    public final PyObject datetime_hour() {
        return new PyLong(time.getHour());
    }

    @ExposedClassMethod
    public static final PyObject datetime_now(PyType type) {
        return new PyDateTime(type);
    }
}
