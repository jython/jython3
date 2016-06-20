package org.python.modules._socket;

import org.python.core.ClassDictInit;
import org.python.core.PyObject;
import org.python.core.PyUnicode;

/**
 * Created by isaiah on 6/18/16.
 */
public class _socket implements ClassDictInit {
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyUnicode("_socket"));
        dict.__setitem__("socket", PySocket.TYPE);

        // hide
        dict.__setitem__("classDictInit", null);
    }


}
