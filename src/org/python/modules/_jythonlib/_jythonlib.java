/* Copyright (c) Jython Developers */
package org.python.modules._jythonlib;

import org.python.core.ClassDictInit;
import org.python.core.PyBytes;
import org.python.core.PyObject;


public class _jythonlib implements ClassDictInit {

    public static final PyBytes __doc__ = new PyBytes("jythonlib module");

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyBytes("_jythonlib"));
        dict.__setitem__("__doc__", __doc__);
        dict.__setitem__("__module__", new PyBytes("_jythonlib"));
        dict.__setitem__("dict_builder", dict_builder.TYPE);
        dict.__setitem__("set_builder", set_builder.TYPE);


        // Hide from Python
        dict.__setitem__("classDictInit", null);
    }

}
