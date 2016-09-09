package org.python.modules._datetime;

import org.python.core.ArgParser;
import org.python.core.CompareOp;
import org.python.core.PyLong;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

import java.time.Duration;
import java.time.Period;

@ExposedType(name = "datetime.timedelta")
public class PyTimeDelta extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyTimeDelta.class);

    private Duration delta;

    public PyTimeDelta(Period period) {
        delta = Duration.ofSeconds(period.getDays() * 86400);
    }

    public PyTimeDelta(Duration duration) {
        super(TYPE);
        delta = duration;
    }

    public PyTimeDelta(int days) {
        this(days, 0, 0);
    }
    public PyTimeDelta(int days, int seconds, int microseconds) {
        this(TYPE, days, seconds, microseconds);
    }

    public PyTimeDelta(PyType subtype, int days, int seconds, int microseconds) {
        super(subtype);
        delta = Duration.ofDays(days).plusSeconds(seconds).plusNanos(microseconds);
    }

    @ExposedNew
    final static PyObject timedelta_new(PyNewWrapper new_, boolean init, PyType subtype,
                                       PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("timedelta", args, keywords, "days", "seconds", "microseconds", "milliseconds", "minutes", "hours", "weeks");
        int days = ap.getInt(0, 0);
        int seconds = ap.getInt(1, 0);
        int microseconds = ap.getInt(2, 0);
        int millisecond = ap.getInt(3, 0);
        int minutes = ap.getInt(4, 0);
        int hours = ap.getInt(5, 0);
        seconds += minutes * 60 + hours * 3600;
        microseconds += millisecond * 1000;
        return new PyTimeDelta(subtype, days, seconds, microseconds);
    }

    @Override
    public PyObject richCompare(PyObject other, CompareOp op) {
        if (other instanceof PyTimeDelta) {
            return op.bool(delta.compareTo(((PyTimeDelta) other).delta));
        }
        return op.neq();
    }

    @Override
    public String toString() {
        if (getNano() != 0) {
            return String.format("%s(%d, %d, %d)", getType().fastGetName(), getDays(), getSeconds(), getNano());
        }
        if (getSeconds() != 0) {
            return String.format("%s(%d, %d)", getType().fastGetName(), getDays(), getSeconds());
        }
        return String.format("%s(%d)", getType().fastGetName(), getDays());
    }

    @Override
    public PyObject __neg__() {
        return timedelta___neg__();
    }

    @ExposedMethod
    final PyObject timedelta___neg__() {
        return new PyTimeDelta(delta.negated());
    }

    @ExposedMethod
    public boolean __bool__() {
        return timedelta___bool__();
    }

    public boolean timedelta___bool__() {
        return !delta.isZero();
    }

    @Override
    public PyObject __idiv__(PyObject other) {
        return timedelta___idiv__(other);
    }

    final PyObject timedelta___idiv__(PyObject other) {
        if (other instanceof PyTimeDelta) {
            return new PyTimeDelta(delta.dividedBy(((PyTimeDelta) other).delta.getSeconds()));
        } else if (other instanceof PyLong) {
            return new PyTimeDelta(delta.dividedBy(other.asLong()));
        }
        return null;
    }

    @Override
    public PyObject __floordiv__(PyObject other) {
        return timedelta___floordiv__(other);
    }

    final PyObject timedelta___floordiv__(PyObject other) {
        return timedelta___idiv__(other);
    }

    @Override
    public PyObject __mul__(PyObject other) {
        return timedelta___mul__(other);
    }

    @ExposedMethod
    public PyObject timedelta___mul__(PyObject other) {
        if (other instanceof PyLong) {
            return new PyTimeDelta(delta.multipliedBy(other.asLong()));
        }
        return null;
    }

    @Override
    public PyObject __rmul__(PyObject other) {
        return timedelta___rmul__(other);
    }

    @ExposedMethod
    public PyObject timedelta___rmul__(PyObject other) {
         if (other instanceof PyLong) {
            return new PyTimeDelta(delta.multipliedBy(other.asLong()));
        }
        return null;
    }

    @ExposedMethod
    public final PyObject timedelta_total_seconds() {
        return new PyLong(delta.getSeconds());
    }

    @ExposedGet(name = "days")
    public long getDays() {
        return delta.toDays();
    }

    @ExposedGet(name = "seconds")
    public long getSeconds() {
        return delta.minusDays(getDays()).getSeconds();
    }

    @ExposedGet(name = "microseconds")
    public long getNano() {
        return delta.getNano();
    }

    Duration toDuration() {
        return delta;
    }
}
