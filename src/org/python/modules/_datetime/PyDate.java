package org.python.modules._datetime;

import org.python.core.ArgParser;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

import java.time.LocalDate;

@ExposedType(name = "_datetime.date")
public class PyDate extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyDate.class);

    protected LocalDate date;

    public PyDate(PyType subType, int year, int month, int day) {
        super(subType);
        date = LocalDate.of(year, month, day);
    }

    @ExposedNew
    final static PyObject date_new(PyNewWrapper new_, boolean init, PyType subtype,
                                         PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("date", args, keywords, "year", "month", "day");
        int year = ap.getInt(0);
        int month = ap.getInt(1);
        int day = ap.getInt(2);
        return new PyDate(TYPE, year, month, day);
    }
}
