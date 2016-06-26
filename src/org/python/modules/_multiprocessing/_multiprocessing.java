package org.python.modules._multiprocessing;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyUnicode;

/**
 * Created by isaiah on 6/16/16.
 */
public class _multiprocessing implements ClassDictInit {
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyUnicode("_multiprocessing"));
//                dict.__setitem__("__doc__", new PyUnicode(__doc__));
        dict.__setitem__("SemLock", PySemLock.TYPE);
        dict.__setitem__("sem_unlock", Py.NotImplemented);

        // hide from Python
        dict.__setitem__("classDictInit", null);
    }

}
