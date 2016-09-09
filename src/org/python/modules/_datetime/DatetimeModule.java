package org.python.modules._datetime;

import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.imp;
import org.python.expose.ExposedConst;
import org.python.expose.ExposedModule;
import org.python.expose.ModuleInit;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

@ExposedModule(name = "_datetime")
public class DatetimeModule {
    @ExposedConst
    public static final int MAXYEAR = 9999;

    @ExposedConst
    public static final int MINYEAR = 1;

    @ModuleInit
    public static final void init(PyObject dict) {
        dict.__setitem__("date", PyDate.TYPE);
        dict.__setitem__("datetime", PyDateTime.TYPE);
        dict.__setitem__("time", PyTime.TYPE);
        dict.__setitem__("timedelta", PyTimeDelta.TYPE);
        dict.__setitem__("timezone", PyTimezone.TYPE);
        dict.__setitem__("tzinfo", PyTZInfo.TYPE);
        PyObject d = PyTimeDelta.TYPE.fastGetDict();
        d.__setitem__("max", new PyTimeDelta(999999999, 86399, 999999));
        d.__setitem__("min", new PyTimeDelta(-999999999));
        d.__setitem__("resolution", new PyTimeDelta(0, 0, 1));
        d = PyTimezone.TYPE.fastGetDict();
        d.__setitem__("max", new PyTimezone(ZoneOffset.MAX));
        d.__setitem__("min", new PyTimezone(ZoneOffset.MIN));
        d.__setitem__("utc", new PyTimezone(ZoneOffset.UTC));
        d = PyDate.TYPE.fastGetDict();
        d.__setitem__("max", new PyDate(LocalDate.MAX));
        d.__setitem__("min", new PyDate(LocalDate.MIN));
        d.__setitem__("resolution", new PyTimeDelta(1));
    }

    /** shared implementation between date, datetime and time */
    public static final PyObject build_struct_time(LocalDate date, LocalTime time, int dstflag) {
        PyObject timeModule = imp.importName("time", true);
        int[] originArgs = { date.getYear(), date.getMonthValue(), date.getDayOfMonth(), time.getHour(),
                time.getMinute(), time.getSecond(), date.getDayOfWeek().getValue() - 1, date.getDayOfYear(), dstflag };
        PyObject[] args = new PyObject[originArgs.length];
        for(int i = 0; i < args.length; i++) {
            args[i] = new PyLong(originArgs[i]);
        }
        return timeModule.invoke("struct_time", args);
    }

    public static final PyObject wrap_strftime(PyObject format, PyObject tuple) {
        PyObject time = imp.importName("time", true);
        return time.invoke("strftime", format, tuple);
    }

//    private static final int weekday(int y, int m, int d) {
//        return (ymd_to_ord(y, m, d) + 6) % 7;
//    }
//
//    private static final int days_before_month(int year, int month) {
//        int days;
//
//        assert(month >= 1);
//        assert(month <= 12);
//        days = _days_before_month[month];
//        if (month > 2 && is_leap(year))
//            ++days;
//        return days;
//    }
//
//    private static final int days_before_year(int year) {
//        int y = year - 1;
//        return y*365 + y/4 - y/100 + y/400;
//    }
//
//    protected static final int ymd_to_ord(int y, int m, int d) {
//        return days_before_year(y) + days_before_month(y, m) + d;
//    }
//
//    /* year -> 1 if leap year, else 0. */
//    private static boolean is_leap(int year) {
//        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
//    }
//
//    private static int _days_before_month[] = {
//            0, /* unused; this vector uses 1-based indexing */
//            0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334
//    };
}
