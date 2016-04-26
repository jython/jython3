package org.python.modules;

import org.python.core.ClassDictInit;
import org.python.core.PyObject;
import org.python.core.PyUnicode;

/**
 * Created by isaiah on 4/25/16.
 */
public class _warnings implements ClassDictInit {

    public static final PyUnicode __doc__ = new PyUnicode("Tools that operate on functions.");

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyUnicode("_functools"));
        dict.__setitem__("__doc__", __doc__);

        // Hide from Python
        dict.__setitem__("classDictInit", null);
    }

}
