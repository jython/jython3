package org.python.expose.generate;

import org.python.core.Py;
import org.python.core.PyModule;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedConst;
import org.python.expose.ExposedModule;

/**
 * Created by isaiah on 7/7/16.
 */
@ExposedModule(name = "hello", doc = "a module that prints some famous words")
public class SimpleModule {

    @ExposedConst(name = "times")
    public static final int TIMES = 1;

    @ExposedConst(name = "spaces")
    public static final String SPACES = "ss";

    @ExposedClassMethod
    public static PyObject xxx(PyType self) {
        return Py.None;
    }

    @ExposedClassMethod
    public static PyObject yyy(PyType self, PyObject u) {
        return u;
    }
}
