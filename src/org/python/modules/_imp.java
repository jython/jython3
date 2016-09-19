
package org.python.modules;

import org.python.Version;
import org.python.core.BufferProtocol;
import org.python.core.BytecodeLoader;
import org.python.core.ClassDictInit;
import org.python.core.PyBUF;
import org.python.core.PyBuffer;
import org.python.core.PyByteArray;
import org.python.core.PyBytes;
import org.python.core.PyCode;
import org.python.core.PyStringMap;
import org.python.core.PyTableCode;
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
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantLock;

@ExposedModule
public class _imp {
    public static final int PY_SOURCE = 1;
    public static final int PY_COMPILED = 2;
    public static final int C_EXTENSION = 3;
    public static final int PKG_DIRECTORY = 5;
    public static final int C_BUILTIN = 6;
    public static final int PY_FROZEN = 7;
    public static final int IMP_HOOK = 9;

    @ExposedFunction
    public static PyObject create_builtin(PyObject spec) {
        PyObject name = spec.__getattr__("name");
        String modName = PyObject.asName(name);
        for (String newmodule : Setup.newbuiltinModules) {
            if (modName.equals(newmodule.split(":")[0])) {
                return new PyModule(modName, new PyStringMap());
            }
        }
        return imp.loadBuiltin(modName);
    }

    @ExposedFunction
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

    @ExposedFunction
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

    @ExposedFunction
    public static PyModule new_module(String name) {
        return new PyModule(name, null);
    }

    @ExposedFunction
    public static boolean is_builtin(String name) {
        switch(name) {
            case "builtins":
            case "sys":
                return true;
            default:
                return PySystemState.getBuiltin(name) != null;
        }
    }

    @ExposedFunction
    public static PyObject _fix_co_filename(PyObject code, PyObject path) {
        if (code instanceof PyTableCode) {
            ((PyTableCode) code).co_filename = path.toString();
        }
        return Py.None;
    }

    @ExposedFunction
    public static PyObject get_frozen_object(String name) {
        return null;
    }

    @ExposedFunction
    public static int init_frozen(String name) {
        return -1;
    }

    @ExposedFunction
    public static boolean is_frozen_package(String name) {
        return false;
    }

    @ExposedFunction
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
    @ExposedFunction
    public static void acquire_lock() {
        Py.getSystemState().getImportLock().lock();
    }

    /**
     * Release the interpreter's import lock.
     *
     */
    @ExposedFunction
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
    @ExposedFunction
    public static boolean lock_held() {
        return Py.getSystemState().getImportLock().isHeldByCurrentThread();
    }

    /**
     * Returns the list of file suffixes used to identify extension modules.
     */
    @ExposedFunction
    public static final PyObject extension_suffixes() {
        return new PyList(); // there is no extension type for jython yet.
    }

    /**
     * Compile python source code to java bytecode
     * @param data python source
     * @param path source file path
     * @return bytes object
     */
    @ExposedFunction
    public static final PyObject _compile_source(PyObject name, PyObject data, PyObject path) {
        byte[] source;
        if (data instanceof PyBytes) {
            source = ((PyBytes) data).toBytes();
        } else if (data instanceof PyUnicode) {
            source = ((PyUnicode) data).getString().getBytes();
        } else {
            throw Py.TypeError("bytes object expected for data");
        }
        byte[] bytes = imp.compileSource(name.toString(), new ByteArrayInputStream(source), path.toString());
        return new PyBytes(bytes);
    }

    /**
     * Compile java bytecode to PyCode object
     * @param name module name
     * @param data java bytecode
     * @param filename original file name
     * @return Python code object
     */
    @ExposedFunction
    public static final PyObject _compile_bytecode(PyObject name, PyObject data, PyObject filename) {
        if (data instanceof PyBytes) {
            byte[] bytes = ((PyBytes) data).toBytes();
            return BytecodeLoader.makeCode(name.toString() + Version.PY_CACHE_TAG, bytes, filename.toString());
        }
        throw Py.TypeError(String.format("bytes expected, found %s", data.getType().getName()));
    }
}
