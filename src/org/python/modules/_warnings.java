package org.python.modules;

import org.python.core.ArgParser;
import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.python.core.PyUnicode;

/**
 * Created by isaiah on 4/25/16.
 */
public class _warnings implements ClassDictInit {

    public static final PyUnicode __doc__ = new PyUnicode("_warnings provides basic warning filtering support.");

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyUnicode("_warnings"));
        dict.__setitem__("__doc__", __doc__);
        dict.__setitem__("_onceregistry", new PyDictionary());

        // Hide from Python
        dict.__setitem__("classDictInit", null);
    }

    public static PyObject warn(PyObject args[], String[] keywords) {
        ArgParser ap = new ArgParser("warn", args, keywords, "message", "category", "stacklevel");
        String message = ap.getString(0);
        PyObject category = ap.getPyObject(1, Py.UserWarning);
        int stackLevel = ap.getIndex(2, 1);
        Py.warning(category, message, stackLevel);
        return Py.None;
    }
}
