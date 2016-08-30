package org.python.modules._datetime;

import org.python.core.ArgParser;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

import java.time.Duration;

@ExposedType(name = "datetime.timedelta")
public class PyTimeDelta extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyTimeDelta.class);

    private Duration delta;

    public PyTimeDelta(Duration duration) {
        delta = duration;
    }

    public PyTimeDelta(int days) {
        this(days, 0, 0);
    }

    public PyTimeDelta(int days, int seconds, int microseconds) {
        super(TYPE);
        delta = Duration.ofDays(days).plusSeconds(seconds).plusMillis(microseconds);
    }

    @ExposedNew
    final static PyObject timedelta_new(PyNewWrapper new_, boolean init, PyType subtype,
                                       PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("timedelta", args, keywords, "days", "seconds", "microseconds");
        int days = ap.getInt(0, 0);
        int seconds = ap.getInt(1, 0);
        int microseconds = ap.getInt(2, 0);
        return new PyTimeDelta(days, seconds, microseconds);
    }
}
