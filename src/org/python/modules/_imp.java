
package org.python.modules;

import org.python.Version;
import org.python.core.ClassDictInit;
import org.python.core.PyCode;
import org.python.core.PyStringMap;
import org.python.core.__builtin__;
import org.python.core.Py;
import org.python.core.PyFile;
import org.python.core.PyList;
import org.python.core.PyModule;
import org.python.core.PyObject;
import org.python.core.PyUnicode;
import org.python.core.PySystemState;
import org.python.core.PyTuple;
import org.python.core.imp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantLock;

/*
 * A bogus implementation of the CPython builtin module "imp".
 * Only the functions required by IDLE and PMW are implemented.
 * Luckily these function are also the only function that IMO can
 * be implemented under Jython.
 */

public class _imp {
    public static PyUnicode __doc__ = new PyUnicode(
        "This module provides the components needed to build your own\n"+
        "__import__ function.  Undocumented functions are obsolete.\n"
    );

    public static final int PY_SOURCE = 1;
    public static final int PY_COMPILED = 2;
    public static final int C_EXTENSION = 3;
    public static final int PKG_DIRECTORY = 5;
    public static final int C_BUILTIN = 6;
    public static final int PY_FROZEN = 7;
    public static final int IMP_HOOK = 9;

    public static PyObject create_builtin(PyObject spec) {
        PyObject name = spec.__getattr__("name");
        String modName = name.toString().intern();
        for (String newmodule : Setup.newbuiltinModules) {
            if (modName.equals(newmodule.split(":")[0])) {
                String classname = className(newmodule);
                Class c = Py.findClassEx(classname, "builtin module");
                PyObject dict = null;
                if (ClassDictInit.class.isAssignableFrom(c)) {
                    try {
                        Method classDictInit = c.getMethod("classDictInit");
                        dict = (PyObject) classDictInit.invoke(null);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    dict = new PyStringMap();
                }
                return new PyModule(modName, dict);
            }
        }
        if (modName.equals("sys")) {
            return Py.java2py(Py.getSystemState());
        }
        if (modName.equals("__builtin__") || modName.equals("builtins")) {
            return new PyModule("builtins", Py.getSystemState().builtins);
        }
        return imp.loadBuiltin(modName);
    }

    public static int exec_builtin(PyObject mod) {
        String name = mod.__findattr__("__name__").toString();
        String classname = null;
        for (String newmodule : Setup.newbuiltinModules) {
            if (name.equals(newmodule.split(":")[0])) {
                classname = className(newmodule) + "$PyExposer";
                break;
            }
        }
        if (classname == null) {
            return 0;
        }
        Class c = Py.findClassEx(classname, "builtin modules");
        if (c != null) {
            try {
                Method clinit = c.getMethod("clinic", PyModule.class);
                clinit.invoke(null, (PyModule) mod);
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }
        return 0;
    }

    private static String className(String name) {
        String classname;
        String modname;

        int colon = name.indexOf(':');
        if (colon != -1) {
            // name:fqclassname
            modname = name.substring(0, colon).trim();
            classname = name.substring(colon + 1, name.length()).trim();
            if (classname.equals("null")) {
                // name:null, i.e. remove it
                classname = null;
            }
        } else {
            modname = name.trim();
            classname = "org.python.modules." + modname;
        }
        return classname;
    }

    public static PyObject get_magic() {
	return new PyUnicode("\u0003\u00f3\r\n");
    }
    
    public static PyObject get_suffixes() {
        return new PyList(new PyObject[] {new PyTuple(new PyUnicode(".py"),
                                                      new PyUnicode("r"),
                                                      Py.newLong(PY_SOURCE)),
                                          new PyTuple(new PyUnicode(Version.PY_CACHE_TAG + ".class"),
                                                      new PyUnicode("rb"),
                                                      Py.newLong(PY_COMPILED)),});
    }

    public static PyModule new_module(String name) {
        return new PyModule(name, null);
    }

    public static boolean is_builtin(String name) {
        switch(name) {
            case "builtins":
            case "sys":
                return true;
            default:
                return PySystemState.getBuiltin(name) != null;
        }
    }

    public static PyObject _fix_co_filename(PyCode code, PyObject path) {
        return Py.None;
    }

    public static PyObject get_frozen_object(String name) {
        return null;
    }

    public static int init_frozen(String name) {
        return -1;
    }

    public static boolean is_frozen_package(String name) {
        return false;
    }

    public static boolean is_frozen(String name) {
        return false;
    }

    /**
     * Acquires the interpreter's import lock for the current thread.
     *
     * This lock should be used by import hooks to ensure
     * thread-safety when importing modules.
     *
     */
    public static void acquire_lock() {
        Py.getSystemState().getImportLock().lock();
    }

    /**
     * Release the interpreter's import lock.
     *
     */
    public static void release_lock() {
        try{
            ReentrantLock importLock = Py.getSystemState().getImportLock();
            // XXX (isaiah) remove this once we sort it out
            if (importLock.isLocked())
                importLock.unlock();
        } catch(IllegalMonitorStateException e){
            throw Py.RuntimeError("not holding the import lock");
        }
    }

    /**
     * Return true if the import lock is currently held, else false.
     *
     * @return true if the import lock is currently held, else false.
     */
    public static boolean lock_held() {
        return Py.getSystemState().getImportLock().isHeldByCurrentThread();
    }

    /**
     * Returns the list of file suffixes used to identify extension modules.
     */
    public static PyObject extension_suffixes() {
        return new PyList();
    }
}
