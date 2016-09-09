package org.python.modules._datetime;

import org.python.core.ArgParser;
import org.python.core.CompareOp;
import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.modules.time.PyTimeTuple;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

@ExposedType(name = "datetime.time")
public class PyTime extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyTime.class);

    private LocalTime time;
    private ZoneOffset timezone;

    public PyTime(LocalTime time, ZoneOffset timezone) {
        this(TYPE, time, timezone);
    }

    public PyTime(PyType subtype, LocalTime time, ZoneOffset timezone) {
        super(subtype);
        this.time = time;
        this.timezone = timezone;
    }

    public PyTime(PyType subtype, LocalTime time) {
        this(subtype, time, ZoneOffset.UTC);
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
            return new PyTime(subtype, time);
        }
        return new PyTime(subtype, time, ZoneOffset.of(tzinfo.toString()));
    }

    @ExposedMethod
    public final String time_tzname() {
        return timezone.getId();
    }

    @ExposedMethod
    public final int time_utcoffset() {
        return timezone.compareTo(ZoneOffset.UTC);
    }

    @ExposedMethod
    public final int time_hour() {
        return time.getHour();
    }

    @ExposedMethod
    public final PyObject time_strftime(PyObject format) {
        return DatetimeModule.wrap_strftime(format, timetuple());
    }

    @Override
    public PyUnicode __str__() {
        return time___str__();
    }

    @ExposedMethod
    public PyUnicode time___str__() {
        return new PyUnicode(time.toString());
    }

    @Override
    public String toString() {
        int sec = time.getSecond();
        int microsec = time.getNano() / 1000;
        if (microsec == 0) {
            if (sec == 0) {
                return String.format("%s(%d, %d)", getType().fastGetName(), time.getHour(), time.getMinute());
            }
            return String.format("%s(%d, %d, %d)", getType().fastGetName(), time.getHour(), time.getMinute(), time.getSecond());
        } else {
            return String.format("%s(%d, %d, %d, %d)", getType().fastGetName(), time.getHour(), time.getMinute(), time.getSecond(), microsec);
        }
    }

    @Override
    public PyObject richCompare(PyObject other, CompareOp op) {
        if (other instanceof PyTime) {
            return op.bool(time.compareTo(((PyTime) other).time));
        }
        return op.neq();
    }

    private PyObject timetuple() {
        return DatetimeModule.build_struct_time(LocalDate.MIN, time, -1);
    }
}
