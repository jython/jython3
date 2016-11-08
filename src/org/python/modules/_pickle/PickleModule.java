/*
 * Copyright 1998 Finn Bock.
 *
 * This program contains material copyrighted by:
 * Copyright (c) 1991-1995 by Stichting Mathematisch Centrum, Amsterdam,
 * The Netherlands.
 */

/* note about impl:
  instanceof vs. CPython type(.) is .
*/

package org.python.modules._pickle;

import org.python.core.ArgParser;
import org.python.core.Exceptions;
import org.python.core.Py;
import org.python.core.PyBoolean;
import org.python.core.PyBuiltinCallable;
import org.python.core.PyBytes;
import org.python.core.PyDictionary;
import org.python.core.PyFile;
import org.python.core.PyFloat;
import org.python.core.PyFunction;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PyModule;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.PyReflectedFunction;
import org.python.core.PyStringMap;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.core.__builtin__;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;
import org.python.expose.ModuleInit;
import org.python.modules._io.PyStringIO;
import org.python.modules.sys.SysModule;
import org.python.util.Generic;

import java.util.Map;

@ExposedModule(name = "_pickle")
public class PickleModule {
    /**
     * The program version.
     */
    public static String __version__ = "1.30";

    /**
     * File format version we write.
     */
    public static final String format_version = "2.0";

    /**
     * Old format versions we can read.
     */
    public static final String[] compatible_formats =
                new String[] { "1.0", "1.1", "1.2", "1.3", "2.0" };

    /**
     * Highest protocol version supported.
     */
    public static final int HIGHEST_PROTOCOL = 2;

    public static String[] __depends__ = new String[] {
        "copyreg",
    };

    public static PyObject PickleError;
    public static PyObject PicklingError;
    public static PyObject UnpickleableError;
    public static PyObject UnpicklingError;
    public static PyObject BadPickleGet;

    final static char MARK            = '(';
    final static char STOP            = '.';
    final static char POP             = '0';
    final static char POP_MARK        = '1';
    final static char DUP             = '2';
    final static char FLOAT           = 'F';
    final static char INT             = 'I';
    final static char BININT          = 'J';
    final static char BININT1         = 'K';
    final static char LONG            = 'L';
    final static char BININT2         = 'M';
    final static char NONE            = 'N';
    final static char PERSID          = 'P';
    final static char BINPERSID       = 'Q';
    final static char REDUCE          = 'R';
    final static char STRING          = 'S';
    final static char BINSTRING       = 'T';
    final static char SHORT_BINSTRING = 'U';
    final static char UNICODE         = 'V';
    final static char BINUNICODE      = 'X';
    final static char APPEND          = 'a';
    final static char BUILD           = 'b';
    final static char GLOBAL          = 'c';
    final static char DICT            = 'd';
    final static char EMPTY_DICT      = '}';
    final static char APPENDS         = 'e';
    final static char GET             = 'g';
    final static char BINGET          = 'h';
    final static char INST            = 'i';
    final static char LONG_BINGET     = 'j';
    final static char LIST            = 'l';
    final static char EMPTY_LIST      = ']';
    final static char OBJ             = 'o';
    final static char PUT             = 'p';
    final static char BINPUT          = 'q';
    final static char LONG_BINPUT     = 'r';
    final static char SETITEM         = 's';
    final static char TUPLE           = 't';
    final static char EMPTY_TUPLE     = ')';
    final static char SETITEMS        = 'u';
    final static char BINFLOAT        = 'G';

    final static char PROTO           = 0x80;
    final static char NEWOBJ          = 0x81;
    final static char EXT1            = 0x82;
    final static char EXT2            = 0x83;
    final static char EXT4            = 0x84;
    final static char TUPLE1          = 0x85;
    final static char TUPLE2          = 0x86;
    final static char TUPLE3          = 0x87;
    final static char NEWTRUE         = 0x88;
    final static char NEWFALSE        = 0x89;
    final static char LONG1           = 0x8A;
    final static char LONG4           = 0x8B;

    static PyDictionary dispatch_table;
    static PyDictionary extension_registry;
    static PyDictionary inverted_registry;


    static PyType BuiltinCallableType = PyType.fromClass(PyBuiltinCallable.class);

    static PyType ReflectedFunctionType = PyType.fromClass(PyReflectedFunction.class);

    static PyType TypeType = PyType.fromClass(PyType.class);

    static PyType DictionaryType = PyType.fromClass(PyDictionary.class);

    static PyType StringMapType = PyType.fromClass(PyStringMap.class);

    static PyType FloatType = PyType.fromClass(PyFloat.class);

    static PyType FunctionType = PyType.fromClass(PyFunction.class);

    static PyType IntType = PyType.fromClass(PyInteger.class);

    static PyType ListType = PyType.fromClass(PyList.class);

    static PyType LongType = PyType.fromClass(PyLong.class);

    static PyType NoneType = PyType.fromClass(PyNone.class);

    static PyType StringType = PyType.fromClass(PyBytes.class);

    static PyType UnicodeType = PyType.fromClass(PyUnicode.class);

    static PyType TupleType = PyType.fromClass(PyTuple.class);

    static PyType FileType = PyType.fromClass(PyFile.class);

    static PyType BoolType = PyType.fromClass(PyBoolean.class);

    static final int BATCHSIZE = 1024;

    /**
     * Initialization when module is imported.
     */
    @ModuleInit
    public static void classDictInit(PyObject dict) {
        PyModule copyreg = (PyModule)importModule("copyreg");

        dispatch_table = (PyDictionary) copyreg.__getattr__("dispatch_table");
        extension_registry = (PyDictionary) copyreg.__getattr__("_extension_registry");
        inverted_registry = (PyDictionary) copyreg.__getattr__("_inverted_registry");

        PickleError = Py.makeClass("PickleError", Py.Exception, _PickleError());
        PicklingError = Py.makeClass("PicklingError", PickleError, exceptionNamespace());
        UnpickleableError = Py.makeClass("UnpickleableError", PicklingError, _UnpickleableError());
        UnpicklingError = Py.makeClass("UnpicklingError", PickleError, exceptionNamespace());
        BadPickleGet = Py.makeClass("BadPickleGet", UnpicklingError, exceptionNamespace());
    }

    public static PyObject exceptionNamespace() {
        PyObject dict = new PyStringMap();
        dict.__setitem__("__module__", new PyUnicode("PickleModule"));
        return dict;
    }

    public static PyObject _PickleError() {
        PyObject dict = exceptionNamespace();
        dict.__setitem__("__str__", getJavaFunc("__str__", "_PickleError__str__"));
        return dict;
    }

    public static PyUnicode _PickleError__str__(PyObject self, PyObject[] args, String[] kwargs) {
        PyObject selfArgs = self.__getattr__("args");
        if (selfArgs.__len__() > 0 && selfArgs.__getitem__(0).__len__()  > 0) {
            return selfArgs.__getitem__(0).__str__();
        } else {
            return new PyUnicode("(what)");
        }
    }

    public static PyObject _UnpickleableError() {
        PyObject dict = exceptionNamespace();
        dict.__setitem__("__str__", getJavaFunc("__str__", "_UnpickleableError__str__"));
        return dict;
    }

    public static PyObject _UnpickleableError__str__(PyObject self, PyObject[] args,
                                                     String[] kwargs) {
        PyObject selfArgs = self.__getattr__("args");
        PyObject a = selfArgs.__len__() > 0 ? selfArgs.__getitem__(0) : new PyBytes("(what)");
        return new PyUnicode("Cannot pickle %s objects").__mod__(a);
    }

    /**
     * @param file      a file-like object, can be a cStringIO.StringIO,
     *                  a PyFile or any python object which implements a
     *                  <i>write</i> method. The data will be written as text.
     * @return a new PyPickler instance.
     */
    public static PyPickler Pickler(PyObject file) {
        return new PyPickler(file, 0);
    }


    /**
     * @param file      a file-like object, can be a cStringIO.StringIO,
     *                  a PyFile or any python object which implements a
     *                  <i>write</i> method.
     * @param protocol  pickle protocol version (0 - text, 1 - pre-2.3 binary, 2 - 2.3)
     * @return         a new PyPickler instance.
     */
    public static PyPickler Pickler(PyObject file, int protocol) {
        return new PyPickler(file, protocol);
    }


    /**
     * Returns a unpickler instance.
     * @param file      a file-like object, can be a cStringIO.StringIO,
     *                  a PyFile or any python object which implements a
     *                  <i>read</i> and <i>readline</i> method.
     * @return         a new Unpickler instance.
     */
    public static PyUnpickler Unpickler(PyObject file) {
        return new PyUnpickler(file);
    }


    /**
     * Shorthand function which pickles the object on the file.
     * @param object    a data object which should be pickled.
     * @param file      a file-like object, can be a cStringIO.StringIO,
     *                  a PyFile or any python object which implements a
     *                  <i>write</i> method.
     * @param protocol  pickle protocol version (0 - text, 1 - pre-2.3 binary, 2 - 2.3)
     */
    @ExposedFunction
    public static void dump(PyObject object, PyObject file, int protocol) {
        new PyPickler(file, protocol).dump(object);
    }

    /**
     * Shorthand function which pickles and returns the string representation.
     * @return         a string representing the pickled object.
     */
    @ExposedFunction
    public static PyBytes dumps(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("dumps", args, kws, "object", "protocol", "*", "fix_imports");
        PyObject object = ap.getPyObject(0);
        int protocol = ap.getInt(1, 0);
        boolean fixImports = ap.getPyObject(3, Py.True).__bool__();
        PyStringIO file = new PyStringIO();
        dump(object, file, protocol);
        return file.getvalue();
    }

    /**
     * Shorthand function which unpickles a object from the file and returns
     * the new object.
     * @param file      a file-like object, can be a cStringIO.StringIO,
     *                  a PyFile or any python object which implements a
     *                  <i>read</i> and <i>readline</i> method.
     * @return         a new object.
     */
    @ExposedFunction
    public static Object load(PyObject file) {
        try {
            return new PyUnpickler(file).load();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            // invalid data, bad stack
            throw Py.IndexError(e.getMessage());
        } catch (StringIndexOutOfBoundsException e) {
            // short data
            throw Py.EOFError(e.getMessage());
        }
    }


    /**
     * Shorthand function which unpickles a object from the string and
     * returns the new object.
     * @param str       a strings which must contain a pickled object
     *                  representation.
     * @return         a new object.
     */
    @ExposedFunction
    public static Object loads(PyObject str) {
        PyStringIO file = new PyStringIO(str.toString());
        return load(file);
    }



    // Factory for creating PyIOFile representation.

    private static Map<PyObject,PyObject> classmap = Generic.map();

    final static PyObject whichmodule(PyObject cls,
                                              PyObject clsname)
    {
        PyObject name = classmap.get(cls);
        if (name != null)
            return name;

        name = new PyBytes("__main__");

        PyObject modules = SysModule.getObject("modules");
        PyObject keylist = modules.invoke("keys");

        int len = keylist.__len__();
        for (int i = 0; i < len; i++) {
            PyObject key = keylist.__finditem__(i);
            PyObject value = modules.__finditem__(key);

            if (!key.equals("__main__") &&
                    value.__findattr__(clsname.toString().intern()) == cls) {
                name = key;
                break;
            }
        }

        classmap.put(cls, name);
        return name;
    }


    /*
     * A very specialized and simplified version of PyStringMap. It can
     * only use integers as keys and stores both an integer and an object
     * as value. It is very private! And should only be used thread-confined.
     */
    static public class PickleMemo {
        //Table of primes to cycle through
        private final int[] primes = {
            13, 61, 251, 1021, 4093,
            5987, 9551, 15683, 19609, 31397,
            65521, 131071, 262139, 524287, 1048573, 2097143,
            4194301, 8388593, 16777213, 33554393, 67108859,
            134217689, 268435399, 536870909, 1073741789,};

        private transient int[] keys;
        private transient int[] position;
        private transient Object[] values;

        private int size;
        private transient int filled;
        private transient int prime;

        public PickleMemo(int capacity) {
            prime = 0;
            keys = null;
            values = null;
            resize(capacity);
        }

        public PickleMemo() {
            this(4);
        }

        public int size() {
            return size;
        }

        private int findIndex(int key, Object value) {
            int[] table = keys;
            int maxindex = table.length;
            int index = (key & 0x7fffffff) % maxindex;

            // Fairly aribtrary choice for stepsize...
            int stepsize = maxindex / 5;

            // Cycle through possible positions for the key;
            //int collisions = 0;
            while (true) {
                int tkey = table[index];
                if (tkey == key && value == values[index]) {
                    return index;
                }
                if (values[index] == null) return -1;
                index = (index+stepsize) % maxindex;
            }
        }

        public int findPosition(int key, Object value) {
            int idx = findIndex(key, value);
            if (idx < 0) return -1;
            return position[idx];
        }


        public Object findValue(int key, Object value) {
            int idx = findIndex(key, value);
            if (idx < 0) return null;
            return values[idx];
        }


        private final void insertkey(int key, int pos, Object value) {
            int[] table = keys;
            int maxindex = table.length;
            int index = (key & 0x7fffffff) % maxindex;

            // Fairly aribtrary choice for stepsize...
            int stepsize = maxindex / 5;

            // Cycle through possible positions for the key;
            while (true) {
                int tkey = table[index];
                if (values[index] == null) {
                    table[index] = key;
                    position[index] = pos;
                    values[index] = value;
                    filled++;
                    size++;
                    break;
                } else if (tkey == key && values[index] == value) {
                    position[index] = pos;
                    break;
                }
                index = (index+stepsize) % maxindex;
            }
        }


        private final void resize(int capacity) {
            int p = prime;
            for(; p<primes.length; p++) {
                if (primes[p] >= capacity) break;
            }
            if (primes[p] < capacity) {
                throw Py.ValueError("can't make hashtable of size: " +
                                    capacity);
            }
            capacity = primes[p];
            prime = p;

            int[] oldKeys = keys;
            int[] oldPositions = position;
            Object[] oldValues = values;

            keys = new int[capacity];
            position = new int[capacity];
            values = new Object[capacity];
            size = 0;
            filled = 0;

            if (oldValues != null) {
                int n = oldValues.length;

                for(int i=0; i<n; i++) {
                    Object value = oldValues[i];
                    if (value == null) continue;
                    insertkey(oldKeys[i], oldPositions[i], value);
                }
            }
        }

        public void put(int key, int pos, Object value) {
            if (2*filled > keys.length) resize(keys.length+1);
            insertkey(key, pos, value);
        }
    }


    static PyObject importModule(String name) {
        PyObject fromlist = new PyTuple(Py.newString("__doc__"));
        return __builtin__.__import__(name, Py.None, Py.None, fromlist);
    }

    private static PyObject getJavaFunc(String name, String methodName) {
        return Exceptions.bindStaticJavaMethod(name, PickleModule.class, methodName);
    }
}
