package org.python.modules;

import org.python.core.BuiltinDocs;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.core.PyTuple;
import org.python.expose.ExposedConst;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;

@ExposedModule(name = "sys")
public class SysModule {
    @ExposedFunction(names = "exc_info", doc = BuiltinDocs.sys_exc_info_doc)
    public static PyObject sys_exc_info() {
        PyException exc = Py.getThreadState().exceptions.peek();
        if (exc == null) {
            return new PyTuple(Py.None, Py.None, Py.None);
        }
        PyObject tb = exc.traceback;
        PyObject value = exc.value;
        return new PyTuple(exc.type, value == null ? Py.None : value, tb == null ? Py.None : tb);
    }

    @ExposedFunction
    public static PyObject getfilesystemencoding() {
        return Py.getSystemState().getfilesystemencoding();
    }

    public static void setObject(String name, PyObject value) {
        PyObject sysdict = Py.getSystemState().sysdict;
        sysdict.__setitem__(name, value);
    }
}
