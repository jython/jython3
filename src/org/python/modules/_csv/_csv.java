/* Copyright (c) Jython Developers */
package org.python.modules._csv;

import org.python.core.ArgParser;
import org.python.core.BuiltinDocs;
import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyUnicode;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.expose.ExposedConst;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;
import org.python.expose.ModuleInit;

/**
 * The Python _csv module.
 *
 * Provides the low-level underpinnings of a CSV reading/writing module.  Users should not
 * use this module directly, but import the csv.py module instead.
 */
@ExposedModule(name = "_csv", doc = BuiltinDocs.csv_doc)
public class _csv {

    // XXX: should be per PySystemState
    /** Dialect registry. */
    public static PyDictionary _dialects = new PyDictionary();

    // XXX: should be per PySystemState
    /** Max parsed field size */
    public static volatile int field_limit = 128 * 1024;

    /** _csv.Error exception. */
    public static final PyObject Error = Py.makeClass("Error", Py.Exception, exceptionNamespace());
    public static PyException Error(String message) {
        return new PyException(Error, message);
    }

    /** Module version. */
    public static String __version__ = "1.0";

    @ModuleInit
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("Dialect", PyDialect.TYPE);
        dict.__setitem__("Error", Error);
        dict.__setitem__("_dialects", _dialects);
        dict.__setitem__("__version__", new PyUnicode(__version__));

        for (QuoteStyle style : QuoteStyle.values()) {
            dict.__setitem__(style.name(), Py.newInteger(style.ordinal()));
        }
    }

    @ExposedFunction(doc = BuiltinDocs.csv_register_dialect_doc)
    public static void register_dialect(PyObject[] args, String[] keywords) {
        int argc = args.length - keywords.length;
        if (argc > 2) {
            throw Py.TypeError("register_dialect() expected at most 2 arguments, got " + argc);
        }

        ArgParser ap = parseArgs("register_dialect", args, keywords);
        PyObject name = ap.getPyObject(0);
        PyObject dialect = ap.getPyObject(1, null);

        if (!(name instanceof PyUnicode)) {
            throw Py.TypeError("dialect name must be a string");
        }

        _dialects.__setitem__(name, dialectFromKwargs(dialect, args, keywords));
    }

    @ExposedFunction(doc = BuiltinDocs.csv_unregister_dialect_doc)
    public static void unregister_dialect(PyObject name) {
        if (!_dialects.__contains__(name)) {
            throw Error("unknown dialect");
        }
        _dialects.__delitem__(name);
    }

    @ExposedFunction(doc = BuiltinDocs.csv_get_dialect_doc)
    public static PyObject get_dialect(PyObject name) {
        return get_dialect_from_registry(name);
    }

    @ExposedFunction(doc = BuiltinDocs.csv_list_dialects_doc)
    public static PyObject list_dialects() {
        return _dialects.keys_as_list();
    }

    @ExposedFunction(doc = BuiltinDocs.csv_reader_doc)
    public static PyObject reader(PyObject[] args, String[] keywords) {
        ArgParser ap = parseArgs("reader", args, keywords);
        PyObject iterator = Py.iter(ap.getPyObject(0), "argument 1 must be an iterator");
        PyObject dialect = ap.getPyObject(1, null);
        return new PyReader(iterator, dialectFromKwargs(dialect, args, keywords));
    }

    @ExposedFunction(doc = BuiltinDocs.csv_writer_doc)
    public static PyObject writer(PyObject[] args, String[] keywords) {
        ArgParser ap = parseArgs("writer", args, keywords);
        PyObject outputFile = ap.getPyObject(0);
        PyObject dialect = ap.getPyObject(1, null);

        PyObject writeline = outputFile.__findattr__("write");
        if (writeline == null || !writeline.isCallable()) {
            throw Py.TypeError("argument 1 must have a \"write\" method");
        }
        return new PyWriter(writeline, dialectFromKwargs(dialect, args, keywords));
    }

    @ExposedFunction(defaults = {"null"}, doc = BuiltinDocs.csv_field_size_limit_doc)
    public static PyLong field_size_limit(PyObject new_limit) {
        if (new_limit == null) {
            return Py.newLong(field_limit);
        }
        if (!(new_limit instanceof PyLong)) {
            throw Py.TypeError("limit must be an integer");
        }
        int old_limit = field_limit;
        field_limit = new_limit.asInt();
        return Py.newInteger(old_limit);
    }

    static PyObject get_dialect_from_registry(PyObject name) {
        PyObject dialect = _dialects.__finditem__(name);
        if (dialect == null) {
            throw Error("unknown dialect");
        }
        return dialect;
    }

    /**
     * Return an ArgParser that ignores keyword args.
     */
    private static ArgParser parseArgs(String funcName, PyObject[] args, String[] keywords) {
        // XXX: _weakref.ReferenceType has the same code
        if (keywords.length > 0) {
            int argc = args.length - keywords.length;
            PyObject[] justArgs = new PyObject[argc];
            System.arraycopy(args, 0, justArgs, 0, argc);
            args = justArgs;
        }
        return new ArgParser(funcName, args, Py.NoKeywords, Py.NoKeywords);
    }

    /**
     * Return a Dialect instance created or updated from keyword arguments.
     */
    private static PyDialect dialectFromKwargs(PyObject dialect, PyObject[] args,
                                               String[] keywords) {
        PyObject[] dialectArgs;
        int argc = args.length - keywords.length;

        // was a dialect keyword specified?
        boolean dialectKeyword = false;
        for (String keyword : keywords) {
            if (keyword.equals("dialect")) {
                dialectKeyword = true;
            }
        }

        if (dialect == null || dialectKeyword) {
            // dialect wasn't passed as a positional arg
            dialectArgs = new PyObject[keywords.length];
            System.arraycopy(args, argc, dialectArgs, 0, keywords.length);
        } else {
            // have dialect -- pass it to dialect_new as a positional arg
            dialectArgs = new PyObject[1 + keywords.length];
            dialectArgs[0] = dialect;
            System.arraycopy(args, argc, dialectArgs, 1, keywords.length);
        }
        return (PyDialect)PyDialect.TYPE.__call__(dialectArgs, keywords);
    }

    private static PyObject exceptionNamespace() {
        PyObject dict = new PyStringMap();
        dict.__setitem__("__module__", new PyUnicode("_csv"));
        return dict;
    }
}
