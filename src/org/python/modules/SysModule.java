package org.python.modules;

import org.python.core.BuiltinDocs;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyFrame;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.core.PyTuple;
import org.python.expose.ExposedConst;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;

import java.util.concurrent.Callable;

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

    @ExposedFunction(defaults = "-1")
    public static PyObject _getframe(int depth) {
        PyFrame f = Py.getFrame();

        while (depth > 0 && f != null) {
            f = f.f_back;
            --depth;
        }
        if (f == null) {
            throw Py.ValueError("call stack is not deep enough");
        }
        return f;
    }

    @ExposedFunction
    public static PyObject registerCloser(final PyObject closer) {
        Callable<Void> resourceCloser = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                closer.__call__();
                return null;
            }
        };
        Py.getSystemState().registerCloser(resourceCloser);
        return Py.None;
    }

    @ExposedFunction
    public static PyObject unregisterCloser(Callable<Void> resourceCloser) {
        return Py.newBoolean(Py.getSystemState().unregisterCloser(resourceCloser));
    }

    // Java API
    public static void setObject(String name, PyObject value) {
        PyObject sysdict = Py.getSystemState().sysdict;
        sysdict.__setitem__(name, value);
    }

}
