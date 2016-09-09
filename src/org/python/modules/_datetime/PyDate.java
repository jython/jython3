package org.python.modules._datetime;

import org.python.core.ArgParser;
import org.python.core.CompareOp;
import org.python.core.Py;
import org.python.core.PyLong;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedModule;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.modules.time.TimeModule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;

@ExposedType(name = "datetime.date")
public class PyDate extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyDate.class);

    protected LocalDate date;

    public PyDate(PyType subType, int year, int month, int day) {
        super(subType);
        date = LocalDate.of(year, month, day);
    }

    public PyDate(PyType type, LocalDate date) {
        super(type);
        this.date = date;
    }

    public PyDate(PyType type) {
        super(type);
    }

    public PyDate(LocalDate date) {
        this(TYPE, date);
    }

    @ExposedNew
    final static PyObject date_new(PyNewWrapper new_, boolean init, PyType subtype,
                                         PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("date", args, keywords, "year", "month", "day");
        int year = ap.getInt(0);
        int month = ap.getInt(1);
        int day = ap.getInt(2);
        return new PyDate(subtype, year, month, day);
    }

    @ExposedMethod
    public final PyObject date_strftime(PyObject format) {
        return DatetimeModule.wrap_strftime(format, date_timetuple());
    }

    @ExposedMethod
    public final PyObject date_timetuple() {
        return DatetimeModule.build_struct_time(date, LocalTime.MIN, -1);
    }

    @ExposedMethod
    public final PyObject date_toordinal() {
        return new PyLong(date.toEpochDay());
    }

    @ExposedMethod
    public final PyObject date_weekday() {
        // A week starts from Monday in Python
        return new PyLong((date.getDayOfWeek().getValue() - 1) % 7);
    }

    @ExposedMethod
    public final PyObject date_isocalendar() {
        return new PyTuple(new PyLong(date.getYear()),
                new PyLong(date.getLong(ChronoField.ALIGNED_WEEK_OF_YEAR)), date_weekday());
    }

    @ExposedMethod
    public final PyObject date_isoformat() {
        return new PyUnicode(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    @ExposedMethod
    public final PyObject date_isoweekday() {
        return new PyLong(date.getDayOfWeek().getValue());
    }

    @ExposedMethod
    public final PyObject date_replace(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("replace", args, keywords, "year", "month", "day");
        int year = ap.getInt(0, date.getYear());
        int month = ap.getInt(1, date.getMonthValue());
        int day = ap.getInt(2, date.getDayOfMonth());
        return new PyDate(getType(), year, month, day);
    }

    @ExposedMethod
    public final PyObject date_ctime() {
        return new PyUnicode(date.format(TimeModule.DEFAULT_FORMAT_PY));
    }

    @ExposedClassMethod
    public static final PyObject fromordinal(PyType type, long ordinal) {
        Instant instant = Instant.ofEpochMilli(ordinal * 86400 * 1000);
        return new PyDate(type, LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate());
    }

    @ExposedClassMethod
    public static final PyObject today(PyType type) {
        return new PyDate(type, LocalDate.now());
    }

    @ExposedClassMethod
    public static final PyObject fromtimestamp(PyType type, PyObject ts) {
        return new PyDate(type, LocalDateTime.ofEpochSecond(ts.asLong(), 0, ZoneOffset.UTC).toLocalDate());
    }

    @Override
    public PyObject __sub__(PyObject other) {
        return date___sub__(other);
    }

    final PyObject date___sub__(PyObject other) {
        if (other instanceof PyDate) {
            return new PyTimeDelta(date.until(((PyDate) other).date));
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s(%d, %d, %d)", getType().fastGetName(),
                date.getYear() - 1969, date.getMonthValue(), date.getDayOfMonth());
    }

    @Override
    public PyObject richCompare(PyObject other, CompareOp op) {
        if (other instanceof PyDate) {
            return op.bool(date.compareTo(((PyDate) other).date));
        }
        return Py.NotImplemented;
    }

    @ExposedGet
    public int year() {
        return date.getYear();
    }

    @ExposedGet
    public int month() {
        return date.getMonthValue();
    }

    @ExposedGet
    public int day() {
        return date.getDayOfMonth();
    }
}
