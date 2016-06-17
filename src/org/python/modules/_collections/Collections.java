package org.python.modules._collections;

import org.python.core.ClassDictInit;
import org.python.core.PyObject;
import org.python.core.PyUnicode;

/**
 * Collections - This module adds the ability to use high performance data 
 *               structures.
 *               - deque:  ordered collection accessible from endpoints only
 *               - defaultdict:  dict subclass with a default value factory
 */
public class Collections implements ClassDictInit {

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("name", new PyUnicode("_collections"));
        dict.__setitem__("deque", PyDeque.TYPE);  
        dict.__setitem__("defaultdict", PyDefaultDict.TYPE);

        // hide from Python
        dict.__setitem__("classDictInit", null);
    }
}
