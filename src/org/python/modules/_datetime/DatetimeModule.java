package org.python.modules._datetime;

import org.python.core.PyObject;
import org.python.expose.ExposedConst;
import org.python.expose.ExposedModule;
import org.python.expose.ModuleInit;

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
        dict.__setitem__("datetime", PyDatetime.TYPE);
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
    }
}
