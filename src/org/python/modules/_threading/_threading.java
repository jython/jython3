package org.python.modules._threading;

import org.python.core.PyObject;
import org.python.expose.ExposedModule;
import org.python.expose.ModuleInit;


@ExposedModule
public class _threading {

    @ModuleInit
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("Lock", Lock.TYPE);
        dict.__setitem__("RLock", RLock.TYPE);
        dict.__setitem__("_Lock", Lock.TYPE);
        dict.__setitem__("_RLock", RLock.TYPE);
        dict.__setitem__("Condition", Condition.TYPE);
    }

}
