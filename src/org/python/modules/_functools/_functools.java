/* Copyright (c) Jython Developers */
package org.python.modules._functools;

import org.python.core.Py;
import org.python.core.PyBytes;
import org.python.core.PyObject;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;
import org.python.expose.ModuleInit;

/**
 * The Python _functools module.
 */
@ExposedModule(doc = _functools.__doc__)
public class _functools {

    public static final String __doc__ = "Tools that operate on functions.";

    @ModuleInit
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("partial", PyPartial.TYPE);
    }

    public static final String __doc__reduce =
    "reduce(function, sequence[, initial]) -> value\n\n" +
    "Apply a function of two arguments cumulatively to the items of a sequence,\n" +
    "from left to right, so as to reduce the sequence to a single value.\n" +
    "For example, reduce(lambda x, y: x+y, [1, 2, 3, 4, 5]) calculates\n" +
    "((((1+2)+3)+4)+5).  If initial is present, it is placed before the items\n" +
    "of the sequence in the calculation, and serves as a default when the\n" +
    "sequence is empty.";

    @ExposedFunction(defaults = {"null"}, doc = __doc__reduce)
    public static PyObject reduce(PyObject f, PyObject l, PyObject z) {
        PyObject result = z;
        PyObject iter = Py.iter(l, "reduce() arg 2 must support iteration");

        for (PyObject item; (item = iter.__next__()) != null;) {
            if (result == null) {
                result = item;
            } else {
                result = f.__call__(result, item);
            }
        }
        if (result == null) {
            throw Py.TypeError("reduce of empty sequence with no initial value");
        }
        return result;
    }
}
