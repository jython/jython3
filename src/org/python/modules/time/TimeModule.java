// Copyright (c) Corporation for National Research Initiatives

package org.python.modules.time;

import org.python.core.BuiltinDocs;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyFloat;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyUnicode;
import org.python.core.__builtin__;
import org.python.core.imp;
import org.python.expose.ExposedConst;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;
import org.python.expose.ModuleInit;
import org.python.modules.PyNamespace;
import org.python.util.ChannelFD;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

@ExposedModule(name = "time", doc = BuiltinDocs.time_doc)
public class TimeModule {

    @ExposedConst
    public static final int _STRUCT_TM_ITEMS = 9;

    @ModuleInit
    public static void init(PyObject dict) {
        dict.__setitem__("struct_time", PyTimeTuple.TYPE);

        // calculate the static variables tzname, timezone, altzone, daylight
        TimeZone tz = TimeZone.getDefault();

        tzname = new PyTuple(new PyUnicode(tz.getDisplayName(false, 0)),
                             new PyUnicode(tz.getDisplayName(true, 0)));

        daylight = tz.useDaylightTime() ? 1 : 0;
        timezone = -tz.getRawOffset() / 1000;
        altzone = timezone - tz.getDSTSavings() / 1000;
        dict.__setitem__("tzname", tzname);
        dict.__setitem__("timezone", new PyLong(timezone));
        dict.__setitem__("daylight", new PyLong(daylight));
        dict.__setitem__("altzone", new PyLong(altzone));
    }

    public static final Map<String, PyNamespace> clockInfos = new HashMap<>();

    static {
        /**
            "clock" : time.clock()
            "monotonic" : time.monotonic()
            "perf_counter" : time.perf_counter()
            "process_time" : time.process_time()
            "time" : time.time()
         */

        clockInfos.put("clock", clockInfo(false, "clock()", true, 1e-6));
        clockInfos.put("monotonic", clockInfo(false, "clock_gettime(CLOCK_MONOTONIC)", true, 1e-9));
        clockInfos.put("perf_counter", clockInfo(false, "clock_gettime(CLOCK_MONOTONIC)", true, 1e-9));
        clockInfos.put("process_time", clockInfo(false, "clock_gettime(CLOCK_PROCESS_CPUTIME_ID)", true, 1e-9));
        clockInfos.put("time", clockInfo(true, "clock_gettime(CLOCK_REALTIME)", false, 1e-9));
    }

    private static PyNamespace clockInfo(boolean adjustable, String implementation, boolean monotonic, double resolution) {
        Map<String, PyObject> info = new HashMap<>();
        info.put("adjustable", Py.newBoolean(adjustable));
        info.put("implementation", new PyUnicode(implementation));
        info.put("monotonic", Py.newBoolean(monotonic));
        info.put("resolution", new PyFloat(resolution));
        return new PyNamespace(info);
    }


    @ExposedFunction
    public static final double time() {
        return System.currentTimeMillis()/1000.0;
    }

    /**
     * @return - the seconds elapsed since the first call to this function
     */
    @ExposedFunction
    public static final double clock() {
        // Check against an explicit initialization variable, clockInitialized,
        // rather than a value of initialClock since the initial call to
        // System.nanoTime can yield anything and that could lead to initialTime
        // being set twice.
        if(!clockInitialized) {
            initialClock = System.nanoTime();
            clockInitialized = true;
            return 0;
        }
        return (System.nanoTime() - initialClock) / NANOS_PER_SECOND;
    }
    private static final double NANOS_PER_SECOND = 1000000000.0;
    private static long initialClock;
    private static volatile boolean clockInitialized;

    private static void throwValueError(String msg) {
        throw new PyException(Py.ValueError, new PyUnicode(msg));
    }

    private static int item(PyTuple tup, int i) {
        // knows about and asserts format on tuple items.  See
        // documentation for Python's time module for details.
        int val = tup.__getitem__(i).asInt();
        boolean valid = true;
        switch (i) {
        case 0: break;                                  // year
        case 1: valid = (0 <= val && val <= 12); break; // month 1-12 (or 0)
        case 2: valid = (0 <= val && val <= 31); break; // day 1 - 31 (or 0)
        case 3: valid = (0 <= val && val <= 23); break; // hour 0 - 23
        case 4: valid = (0 <= val && val <= 59); break; // minute 0 - 59
        case 5: valid = (0 <= val && val <= 61); break; // second 0 - 59 (plus 2 leap seconds)
        case 6: valid = (0 <= val && val <= 6);  break; // weekday 0 - 6
        case 7: valid = (0 <= val && val < 367); break; // julian day 1 - 366 (or 0)
        case 8: valid = (-1 <= val && val <= 1); break; // d.s. flag, -1,0,1
        }
        // raise a ValueError if not within range
        if (!valid) {
            String msg;
            switch (i) {
            case 1:
                msg = "month out of range (1-12)";
                break;
            case 2:
                msg = "day out of range (1-31)";
                break;
            case 3:
                msg = "hour out of range (0-23)";
                break;
            case 4:
                msg = "minute out of range (0-59)";
                break;
            case 5:
                msg = "second out of range (0-59)";
                break;
            case 6:
                msg = "day of week out of range (0-6)";
                break;
            case 7:
                msg = "day of year out of range (1-366)";
                break;
            case 8:
                msg = "daylight savings flag out of range (-1,0,1)";
                break;
            default:
                // make compiler happy
                msg = "ignore";
                break;
            }
            throwValueError(msg);
        }
        switch (i) {
        case 1:
        case 2:
        case 7:
            // 'or 0'
            if (val == 0) { // WTF
                val = 1;
            }
            break;
        }
        return val;
    }

    private static LocalDateTime _tupletocal(PyObject tuple) {
        if (tuple instanceof PyTuple) {
            PyTuple tup = (PyTuple) tuple;
            try {
                return LocalDateTime.of(item(tup, 0),
                        item(tup, 1),
                        item(tup, 2),
                        item(tup, 3),
                        item(tup, 4),
                        item(tup, 5));
            } catch (DateTimeException e) {
                throw Py.ValueError(e.getMessage());
            }
        }
        throw Py.TypeError("expected a tuple");
    }

    @ExposedFunction
    public static double mktime(PyObject tup) {
        ZonedDateTime cal = _tupletocal(tup).atZone(ZoneId.systemDefault());

        int dst = item((PyTuple) tup, 8);
        if(dst == 0 || dst == 1) {
            // TODO how can we handle this
        }
        return cal.toEpochSecond();
    }

    @ExposedFunction
    public static double monotonic() {
        return System.nanoTime() / 1000000000.0;
    }

    @ExposedFunction
    public static double perf_counter() {
        return System.nanoTime() / 1000000000.0;
    }

    @ExposedFunction
    public static double process_time() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        double total = 0;
        for (long id : threadBean.getAllThreadIds()) {
            total += threadBean.getThreadCpuTime(id);
        }
        return total / 1000000000.0;
    }

    @ExposedFunction
    public static PyObject get_clock_info(String name) {
        if (clockInfos.containsKey(name)) {
            return clockInfos.get(name);
        }
        throw Py.ValueError("unknown clock");
    }
    
    protected static PyTimeTuple _timefields(double secs, TimeZone tz) {
        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.clear();
        secs = secs * 1000;
        if (secs < Long.MIN_VALUE || secs > Long.MAX_VALUE) {
            throw Py.ValueError("timestamp out of range for platform time_t");
        }
        cal.setTime(new Date((long)secs));
        int isdst = tz.inDaylightTime(cal.getTime()) ? 1 : 0;
        return toTimeTuple(cal, isdst);
    }

    private static PyTimeTuple toTimeTuple(Calendar cal, int isdst) {
        int dow = cal.get(Calendar.DAY_OF_WEEK) - 2;
        if (dow < 0) {
            dow += 7;
        }
        PyTimeTuple tup = new PyTimeTuple(new PyLong(cal.get(Calendar.YEAR)),
                new PyLong(cal.get(Calendar.MONTH) + 1),
                new PyLong(cal.get(Calendar.DAY_OF_MONTH)),
                new PyLong(cal.get(Calendar.HOUR)
                + 12 * cal.get(Calendar.AM_PM)),
                new PyLong(cal.get(Calendar.MINUTE)),
                new PyLong(cal.get(Calendar.SECOND)),
                new PyLong(dow),
                new PyLong(cal.get(Calendar.DAY_OF_YEAR)),
                new PyLong(isdst));
        tup.tm_zone = new PyUnicode(cal.getTimeZone().getID());
        return tup;
    }


    /**
     * Convert a time argument that may be an optional float or None value to a
     * double. Throws a TypeError on failure.
     *
     * @param arg a PyObject number of None
     * @return a double value
     */
    public static double parseTimeDoubleArg(PyObject arg) {
        if (arg == null || arg == Py.None) {
            return time();
        }
        Object result = arg.__tojava__(Double.class);
        if (result == Py.NoConversion) {
            throw Py.TypeError("a float is required");
        }
        return (Double)result;
    }

    @ExposedFunction(defaults = {"null"})
    public static PyTuple localtime(PyObject secs) {
        return _timefields(parseTimeDoubleArg(secs), TimeZone.getDefault());
    }

    @ExposedFunction(defaults = {"null"})
    public static PyTuple gmtime(PyObject arg) {
        return _timefields(parseTimeDoubleArg(arg), TimeZone.getTimeZone("GMT"));
    }

    @ExposedFunction(defaults = {"null"})
    public static PyUnicode ctime(PyObject secs) {
        return asctime(localtime(secs));
    }

    // Python's time module specifies use of current locale
    protected static Locale currentLocale = Locale.getDefault();

    @ExposedFunction(defaults = {"null"})
    public static PyUnicode asctime(PyObject obj) {
        PyTuple tup;
        if (obj == null) {
            obj = localtime(obj);
        }
        if (obj instanceof PyTuple) {
            tup = (PyTuple)obj;
        } else {
            tup = PyTuple.fromIterable(obj);
        }
        int len = tup.__len__();
        if (len != 9) {
            throw Py.TypeError(
                String.format("argument must be sequence of length 9, not %d", len));
        }
        LocalDateTime dateTime = _tupletocal(tup);
        return new PyUnicode(dateTime.format(DEFAULT_FORMAT_PY));
    }

    @ExposedFunction
    public static void sleep(double secs) {
        if (secs < 0) {
            throw Py.ValueError("sleep length must be non-negative");
        } else if (secs == 0) {
            // Conform to undocumented, or at least very underdocumented, but quite
            // reasonable behavior in CPython. See Alex Martelli's answer,
            // http://stackoverflow.com/a/790246/423006
            java.lang.Thread.yield();
        } else {
            try {
                java.lang.Thread.sleep((long)(secs * 1000));
            } catch (InterruptedException e) {
                throw new PyException(Py.KeyboardInterrupt, "interrupted sleep");
            }
        }
    }

    // set by classDictInit()
    public static int timezone;
    public static int altzone = -1;
    public static int daylight;
    public static PyTuple tzname = null;

    @ExposedFunction(defaults = {"null"})
    public static PyUnicode strftime(String format, PyObject tup) {
        if (tup == null) {
            tup = localtime(tup);
        }

        PyTuple timeTuple = (PyTuple) tup;
        if (timeTuple.__len__() == 1) {
            timeTuple = (PyTuple) timeTuple.get(0);
        }
        StringBuilder s = new StringBuilder();
        int lastc = 0;
        LocalDateTime cal = _tupletocal(timeTuple);
        ZoneId zone = null;
        if (timeTuple instanceof PyTimeTuple) {
            PyObject tm_zone = ((PyTimeTuple) timeTuple).tm_zone;
            if (tm_zone != null) {
                zone = ZoneId.of(tm_zone.toString());
            }
        }
        int padding = 0;
        while (lastc < format.length()) {
            int i = format.indexOf("%", lastc);
            if (i < 0) {
                // the end of the format string
                s.append(format.substring(lastc));
                break;
            }
            if (i == format.length() - 1) {
                // there's a bare % at the end of the string.  Python lets
                // this go by just sticking a % at the end of the result
                // string
                s.append("%");
                break;
            }
            s.append(format.substring(lastc, i));
            i++;
            Locale locale = Locale.US;

            String txt = "";
            char ch = format.charAt(i);

            while (Character.isDigit(ch)) {
                padding = padding * 10 + Integer.parseInt(ch + "");
                ch = format.charAt(++i);
            }
            switch (ch) {
                case 'a':
                    // abbrev weekday
                    txt = cal.format(DateTimeFormatter.ofPattern("E"));
                    break;
                case 'A':
                    // full weekday
                    txt = cal.getDayOfWeek().getDisplayName(TextStyle.FULL, locale);
                    break;
                case 'b':
                    // abbrev month
                    txt = cal.getMonth().getDisplayName(TextStyle.SHORT, locale);
                    break;
                case 'B':
                    // full month
                    txt = cal.getMonth().getDisplayName(TextStyle.FULL, locale);
                    break;
                case 'c':
                    txt = cal.format(DEFAULT_FORMAT_PY);
                    break;
                case 'd':
                    // day of month (01-31)
                    txt = cal.format(DateTimeFormatter.ofPattern("dd", locale));
                    break;
                case 'f':
                    txt = cal.format(DateTimeFormatter.ofPattern("S", locale));
                    break;
                case 'H':
                    // hour (00-23)
                    txt = cal.format(DateTimeFormatter.ofPattern("HH", locale));
                    break;
                case 'I':
                    // hour (01-12)
                    txt = cal.format(DateTimeFormatter.ofPattern("hh", locale));
                    break;
                case 'j':
                    // day of year (001-366)
                    txt = cal.format(DateTimeFormatter.ofPattern("DDD", locale));
                    break;
                case 'm':
                    // month (01-12)
                    txt = cal.format(DateTimeFormatter.ofPattern("MM", locale));
                    break;
                case 'M':
                    // minute (00-59)
                    txt = cal.format(DateTimeFormatter.ofPattern("mm", locale));
                    break;
                case 'p':
                    // AM/PM
                    txt = cal.format(DateTimeFormatter.ofPattern("a", locale));
                    break;
                case 'S':
                    // seconds (00-61)
                    txt = cal.format(DateTimeFormatter.ofPattern("ss", locale));
                    break;
                case 'U':
                    // week of year (sunday is first day) (00-53).  all days in
                    // new year preceding first sunday are considered to be in
                    // week 0
                    txt = cal.format(DateTimeFormatter.ofPattern("w", locale));
                    break;
                case 'w':
                    // weekday as decimal (0=Sunday-6)
                    // tuple format has monday=0
                    txt = String.valueOf((item(timeTuple, 6) + 1) % 7);
//                txt = cal.format(DateTimeFormatter.ofPattern("e", locale)));
                    break;
                case 'W':
                    // week of year (monday is first day) (00-53).  all days in
                    // new year preceding first sunday are considered to be in
                    // week 0
                    txt = cal.format(DateTimeFormatter.ofPattern("w", locale));
                    break;
                case 'x':
                    txt = cal.format(DateTimeFormatter.ofPattern("MM/dd/yy", locale));
                    break;
                case 'X':
                    txt = cal.format(DateTimeFormatter.ofPattern("HH:mm:ss", locale));
                    break;
                case 'Y':
                    // year w/ century
                    txt = cal.format(DateTimeFormatter.ofPattern("u", locale));
                    break;
                case 'y':
                    // year w/o century (00-99)
                    txt = cal.format(DateTimeFormatter.ofPattern("u", locale)).substring(2);
                    break;
                case 'z':
                    if (zone == null) {
                        txt = "";
                    } else {
                        txt = ZonedDateTime.of(cal, zone).format(DateTimeFormatter.ofPattern("ZZ", locale));
                    }
                case 'Z':
                    // timezone name
                    if (zone == null) {
                        txt = "";
                    } else {
                        txt = ZonedDateTime.of(cal, zone).format(DateTimeFormatter.ofPattern("zz", locale));
                    }
                    break;
                case '%':
                    // %
                    s.append("%");
                    break;
                default:
                    // otherwise if this is a control number
                    s.append("%" + ch);
                    break;
            }
            if (txt != "") {
                boolean isDigit = txt.matches("[0-9].*");
                for (int k = txt.length(); k < padding; k++) {
                    if (isDigit) {
                        s.append("0");
                    } else {
                        s.append(" ");
                    }
                }
                padding = 0;
                s.append(txt);
            }
            lastc = ++i;
        }
        return new PyUnicode(s);
    }

    /**
     * Calls _strptime.strptime(), for cases that our SimpleDateFormat backed
     * strptime can't handle.
     */
    private static PyObject pystrptime(PyObject data_string, PyObject format) {
        return imp.importName("_strptime", true)
                .invoke("_strptime_time", data_string, format);
    }

    @ExposedFunction(defaults = {"null"})
    public static PyObject strptime(PyObject dataString, PyObject format) {
        return pystrptime(dataString, format);
    }

    public static final DateTimeFormatter DEFAULT_FORMAT_PY = DateTimeFormatter.ofPattern("E MMM ppd HH:mm:ss y");
}
