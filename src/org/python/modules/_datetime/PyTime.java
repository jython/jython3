package org.python.modules._datetime;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

import java.time.LocalTime;
import java.time.ZoneOffset;

@ExposedType(name = "_datetime.time")
public class PyTime extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyTime.class);

    private LocalTime time;
    private ZoneOffset timezone;

    public PyTime(LocalTime time, ZoneOffset timezone) {
        this.time = time;
        this.timezone = timezone;
    }

    public PyTime(LocalTime time) {
        this(time, ZoneOffset.UTC);
    }

    @ExposedNew
    final static PyObject time_new(PyNewWrapper new_, boolean init, PyType subtype,
                                       PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("time", args, keywords, "hour", "minute", "second", "microsecond", "tzinfo");
        int hour = ap.getInt(0, 0);
        int minute = ap.getInt(1, 0);
        int second = ap.getInt(2, 0);
        int microsecond = ap.getInt(3, 0);
        PyObject tzinfo = ap.getPyObject(4, Py.None);
        LocalTime time = LocalTime.of(hour, minute, second, microsecond * 1000);
        if (tzinfo == Py.None) {
            return new PyTime(time);
        }
        return new PyTime(time, ZoneOffset.of(tzinfo.toString()));
    }

    @ExposedMethod
    public final String time_tzname() {
        return timezone.getId();
    }

    @ExposedMethod
    public final int time_utcoffset() {
        return timezone.compareTo(ZoneOffset.UTC);
    }
}
