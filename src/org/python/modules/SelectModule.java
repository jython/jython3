package org.python.modules;

import org.python.core.ClassDictInit;
import org.python.core.PyObject;
import org.python.core.PyUnicode;

/**
 * Created by isaiah on 6/19/16.
 */
public class SelectModule implements ClassDictInit {
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyUnicode("select"));

        // hide
        dict.__setitem__("classDictInit", null);
    }
}
