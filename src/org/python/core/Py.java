// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import com.google.common.base.CharMatcher;
import jline.console.UserInterruptException;
import jnr.constants.Constant;
import jnr.constants.platform.Errno;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;

import jnr.posix.util.Platform;
import org.python.antlr.base.mod;
import org.python.core.adapter.ClassicPyObjectAdapter;
import org.python.core.adapter.ExtensiblePyObjectAdapter;
import org.python.modules.posix.PosixModule;
import org.python.util.Generic;

/** Builtin types that are used to setup PyObject.
 *
 * Resolve circular dependency with some laziness. */
class BootstrapTypesSingleton {
    private final Set<Class<?>> BOOTSTRAP_TYPES;
    private BootstrapTypesSingleton() {
        BOOTSTRAP_TYPES = Generic.set();
        BOOTSTRAP_TYPES.add(PyObject.class);
        BOOTSTRAP_TYPES.add(PyType.class);
        BOOTSTRAP_TYPES.add(PyBuiltinCallable.class);
        BOOTSTRAP_TYPES.add(PyDataDescr.class);
    }

    private static class LazyHolder {
        private static final BootstrapTypesSingleton INSTANCE = new BootstrapTypesSingleton();
    }

    public static Set<Class<?>> getInstance() {
        return LazyHolder.INSTANCE.BOOTSTRAP_TYPES;
    }
}

public final class Py {

    static class SingletonResolver implements Serializable {

        private String which;

        SingletonResolver(String which) {
            this.which = which;
        }

        private Object readResolve() throws ObjectStreamException {
            if (which.equals("None")) {
                return Py.None;
            } else if (which.equals("Ellipsis")) {
                return Py.Ellipsis;
            } else if (which.equals("NotImplemented")) {
                return Py.NotImplemented;
            }
            throw new StreamCorruptedException("unknown singleton: " + which);
        }
    }
    /* Holds the singleton None and Ellipsis objects */
    /** The singleton None Python object **/
    public final static PyObject None = new PyNone();
    /** The singleton Ellipsis Python object - written as ... when indexing */
    public final static PyObject Ellipsis = new PyEllipsis();
    /** The singleton NotImplemented Python object. Used in rich comparison */
    public final static PyObject NotImplemented = new PyNotImplemented();
    /** A zero-length array of Strings to pass to functions that
    don't have any keyword arguments **/
    public final static String[] NoKeywords = new String[0];
    /** A zero-length array of PyObject's to pass to functions that
    expect zero-arguments **/
    public final static PyObject[] EmptyObjects = new PyObject[0];
    /** A frozenset with zero elements **/
    public final static PyFrozenSet EmptyFrozenSet = new PyFrozenSet();
    /** A tuple with zero elements **/
    public final static PyTuple EmptyTuple = new PyTuple(Py.EmptyObjects);
    /** The Python integer 0 **/
    public final static PyInteger Zero = new PyInteger(0);
    /** The Python integer 1 **/
    public final static PyInteger One = new PyInteger(1);
    /** The Python boolean False **/
    public final static PyBoolean False = new PyBoolean(false);
    /** The Python boolean True **/
    public final static PyBoolean True = new PyBoolean(true);
    /** A zero-length Python byte string **/
    public final static PyString EmptyString = new PyString("");
    /** A zero-length Python Unicode string **/
    public final static PyUnicode EmptyUnicode = new PyUnicode("");
    /** A Python string containing '\n' **/
    public final static PyString Newline = new PyString("\n");
    /** A Python unicode string containing '\n' **/
    public final static PyUnicode UnicodeNewline = new PyUnicode("\n");
    /** A Python string containing ' ' **/
    public final static PyString Space = new PyString(" ");
    /** A Python unicode string containing ' ' **/
    public final static PyUnicode UnicodeSpace = new PyUnicode(" ");
    /** Set if the type object is dynamically allocated */
    public final static long TPFLAGS_HEAPTYPE = 1L << 9;
    /** Set if the type allows subclassing */
    public final static long TPFLAGS_BASETYPE = 1L << 10;
    /** Type is abstract and cannot be instantiated */
    public final static long TPFLAGS_IS_ABSTRACT = 1L << 20;



    /** A unique object to indicate no conversion is possible
    in __tojava__ methods **/
    public final static Object NoConversion = new PySingleton("Error");
    public static PyObject OSError;
    public static PyException OSError(String message) {
        return new PyException(Py.OSError, message);
    }

    public static PyException OSError(IOException ioe) {
        return fromIOException(ioe, Py.OSError);
    }

    public static PyException OSError(Constant errno) {
        int value = errno.intValue();
        PyObject args = new PyTuple(Py.newInteger(value), PosixModule.strerror(value));
        return new PyException(Py.OSError, args);
    }
    
    public static PyException OSError(Constant errno, PyObject filename) {
        int value = errno.intValue();
        // see https://github.com/jruby/jruby/commit/947c661e46683ea82f8016dde9d3fa597cd10e56
        // for rationale to do this mapping, but in a nutshell jnr-constants is automatically
        // generated from header files, so that's not the right place to do this mapping,
        // but for Posix compatibility reasons both CPython andCRuby do this mapping;
        // except CPython chooses EEXIST instead of CRuby's ENOENT
        if (Platform.IS_WINDOWS && (value == 20047 || value == Errno.ESRCH.intValue())) {
            value = Errno.EEXIST.intValue();
        }
        // Pass to strerror because jnr-constants currently lacks Errno descriptions on
        // Windows, and strerror falls back to Linux's
        PyObject args = new PyTuple(Py.newInteger(value), PosixModule.strerror(value), filename);
        return new PyException(Py.OSError, args);
    }

    public static PyObject NotImplementedError;
    public static PyException NotImplementedError(String message) {
        return new PyException(Py.NotImplementedError, message);
    }

    public static PyObject EnvironmentError;
    public static PyException EnvironmentError(String message) {
        return new PyException(Py.EnvironmentError, message);
    }

    /* The standard Python exceptions */
    public static PyObject OverflowError;

    public static PyException OverflowError(String message) {
        return new PyException(Py.OverflowError, message);
    }
    public static PyObject RuntimeError;

    public static PyException RuntimeError(String message) {
        return new PyException(Py.RuntimeError, message);
    }
    public static PyObject KeyboardInterrupt;
    public static PyException KeyboardInterrupt(String message) {
        return new PyException(Py.KeyboardInterrupt, message);
    }
    public static PyObject FloatingPointError;

    public static PyException FloatingPointError(String message) {
        return new PyException(Py.FloatingPointError, message);
    }
    public static PyObject SyntaxError;

    public static PyException SyntaxError(String message) {
        return new PyException(Py.SyntaxError, message);
    }
    public static PyObject IndentationError;
    public static PyObject TabError;
    public static PyObject AttributeError;

    public static PyException AttributeError(String message) {
        return new PyException(Py.AttributeError, message);
    }
    public static PyObject IOError;

    public static PyException IOError(IOException ioe) {
        return fromIOException(ioe, Py.IOError);
    }

    public static PyException IOError(String message) {
        return new PyException(Py.IOError, message);
    }

    public static PyException IOError(Constant errno) {
        int value = errno.intValue();
        PyObject args = new PyTuple(Py.newInteger(value), PosixModule.strerror(value));
        return new PyException(Py.IOError, args);
    }

    public static PyException IOError(Constant errno, PyObject filename) {
        int value = errno.intValue();
        PyObject args = new PyTuple(Py.newInteger(value), PosixModule.strerror(value), filename);
        return new PyException(Py.IOError, args);
    }

    private static PyException fromIOException(IOException ioe, PyObject err) {
        String message = ioe.getMessage();
        if (message == null) {
            message = ioe.getClass().getName();
        }
        if (ioe instanceof FileNotFoundException) {
            PyTuple args = new PyTuple(Py.newInteger(Errno.ENOENT.intValue()),
                                       Py.newString("File not found - " + message));
            return new PyException(err, args);
        }
        return new PyException(err, message);
    }

    public static PyObject KeyError;

    public static PyException KeyError(String message) {
        return new PyException(Py.KeyError, message);
    }

    public static PyException KeyError(PyObject key) {
        return new PyException(Py.KeyError, key);
    }
    public static PyObject AssertionError;

    public static PyException AssertionError(String message) {
        return new PyException(Py.AssertionError, message);
    }
    public static PyObject TypeError;

    public static PyException TypeError(String message) {
        return new PyException(Py.TypeError, message);
    }
    public static PyObject ReferenceError;

    public static PyException ReferenceError(String message) {
        return new PyException(Py.ReferenceError, message);
    }
    public static PyObject SystemError;

    public static PyException SystemError(String message) {
        return new PyException(Py.SystemError, message);
    }
    public static PyObject IndexError;

    public static PyException IndexError(String message) {
        return new PyException(Py.IndexError, message);
    }
    public static PyObject ZeroDivisionError;

    public static PyException ZeroDivisionError(String message) {
        return new PyException(Py.ZeroDivisionError, message);
    }
    public static PyObject NameError;

    public static PyException NameError(String message) {
        return new PyException(Py.NameError, message);
    }
    public static PyObject UnboundLocalError;

    public static PyException UnboundLocalError(String message) {
        return new PyException(Py.UnboundLocalError, message);
    }
    public static PyObject SystemExit;

    static void maybeSystemExit(PyException exc) {
        if (exc.match(Py.SystemExit)) {
            PyObject value = exc.value;
            if (PyException.isExceptionInstance(exc.value)) {
                value = value.__findattr__("code");
            }
            Py.getSystemState().callExitFunc();
            if (value instanceof PyInteger) {
                System.exit(((PyInteger) value).getValue());
            } else {
                if (value != Py.None) {
                    try {
                        Py.println(value);
                        System.exit(1);
                    } catch (Throwable t) {
                        // continue
                    }
                }
                System.exit(0);
            }
        }
    }
    public static PyObject StopIteration;

    public static PyException StopIteration(String message) {
        return new PyException(Py.StopIteration, message);
    }
    public static PyObject GeneratorExit;

    public static PyException GeneratorExit(String message) {
        return new PyException(Py.GeneratorExit, message);
    }
    public static PyObject ImportError;

    public static PyException ImportError(String message) {
        return new PyException(Py.ImportError, message);
    }
    public static PyObject ValueError;

    public static PyException ValueError(String message) {
        return new PyException(Py.ValueError, message);
    }
    public static PyObject UnicodeError;

    public static PyException UnicodeError(String message) {
        return new PyException(Py.UnicodeError, message);
    }
    public static PyObject UnicodeTranslateError;

    public static PyException UnicodeTranslateError(String object,
            int start,
            int end, String reason) {
        return new PyException(Py.UnicodeTranslateError, new PyTuple(new PyString(object),
                new PyInteger(start),
                new PyInteger(end),
                new PyString(reason)));
    }
    public static PyObject UnicodeDecodeError;

    public static PyException UnicodeDecodeError(String encoding,
            String object,
            int start,
            int end,
            String reason) {
        return new PyException(Py.UnicodeDecodeError, new PyTuple(new PyString(encoding),
                new PyString(object),
                new PyInteger(start),
                new PyInteger(end),
                new PyString(reason)));
    }
    public static PyObject UnicodeEncodeError;

    public static PyException UnicodeEncodeError(String encoding,
            String object,
            int start,
            int end,
            String reason) {
        return new PyException(Py.UnicodeEncodeError, new PyTuple(new PyString(encoding),
                new PyUnicode(object),
                new PyInteger(start),
                new PyInteger(end),
                new PyString(reason)));
    }
    public static PyObject EOFError;

    public static PyException EOFError(String message) {
        return new PyException(Py.EOFError, message);
    }
    public static PyObject MemoryError;

    public static void memory_error(OutOfMemoryError t) {
        if (Options.showJavaExceptions) {
            t.printStackTrace();
        }
    }

    public static PyException MemoryError(String message) {
        return new PyException(Py.MemoryError, message);
    }

    public static PyObject BufferError;
    public static PyException BufferError(String message) {
        return new PyException(Py.BufferError, message);
    }

    public static PyObject ArithmeticError;
    public static PyObject LookupError;
    public static PyObject StandardError;
    public static PyObject Exception;
    public static PyObject BaseException;

    public static PyObject Warning;

    public static void Warning(String message) {
        warning(Warning, message);
    }
    public static PyObject UserWarning;

    public static void UserWarning(String message) {
        warning(UserWarning, message);
    }
    public static PyObject DeprecationWarning;

    public static void DeprecationWarning(String message) {
        warning(DeprecationWarning, message);
    }
    public static PyObject PendingDeprecationWarning;

    public static void PendingDeprecationWarning(String message) {
        warning(PendingDeprecationWarning, message);
    }
    public static PyObject SyntaxWarning;

    public static void SyntaxWarning(String message) {
        warning(SyntaxWarning, message);
    }
    public static PyObject RuntimeWarning;

    public static void RuntimeWarning(String message) {
        warning(RuntimeWarning, message);
    }
    public static PyObject FutureWarning;

    public static void FutureWarning(String message) {
        warning(FutureWarning, message);
    }

    public static PyObject ImportWarning;
    public static void ImportWarning(String message) {
        warning(ImportWarning, message);
    }

    public static PyObject UnicodeWarning;
    public static void UnicodeWarning(String message) {
        warning(UnicodeWarning, message);
    }
    
    public static PyObject BytesWarning;
    public static void BytesWarning(String message) {
        warning(BytesWarning, message);
    }

    public static void warnPy3k(String message) {
        warnPy3k(message, 1);
    }

    public static void warnPy3k(String message, int stacklevel) {
        if (Options.py3k_warning) {
            warning(DeprecationWarning, message, stacklevel);
        }
    }

    private static PyObject warnings_mod;

    private static PyObject importWarnings() {
        if (warnings_mod != null) {
            return warnings_mod;
        }
        PyObject mod;
        try {
            mod = __builtin__.__import__("warnings");
        } catch (PyException e) {
            if (e.match(ImportError)) {
                return null;
            }
            throw e;
        }
        warnings_mod = mod;
        return mod;
    }

    private static String warn_hcategory(PyObject category) {
        PyObject name = category.__findattr__("__name__");
        if (name != null) {
            return "[" + name + "]";
        }
        return "[warning]";
    }

    public static void warning(PyObject category, String message) {
        warning(category, message, 1);
    }

    public static void warning(PyObject category, String message, int stacklevel) {
        PyObject func = null;
        PyObject mod = importWarnings();
        if (mod != null) {
            func = mod.__getattr__("warn");
        }
        if (func == null) {
            System.err.println(warn_hcategory(category) + ": " + message);
            return;
        } else {
            func.__call__(Py.newString(message), category, Py.newInteger(stacklevel));
        }
    }

    public static void warning(PyObject category, String message,
            String filename, int lineno, String module,
            PyObject registry) {
        PyObject func = null;
        PyObject mod = importWarnings();
        if (mod != null) {
            func = mod.__getattr__("warn_explicit");
        }
        if (func == null) {
            System.err.println(filename + ":" + lineno + ":" +
                    warn_hcategory(category) + ": " + message);
            return;
        } else {
            func.__call__(new PyObject[]{
                Py.newString(message), category,
                Py.newString(filename), Py.newInteger(lineno),
                (module == null) ? Py.None : Py.newString(module),
                registry
            }, Py.NoKeywords);
        }
    }
    public static PyObject JavaError;

    public static PyException JavaError(Throwable t) {
        if (t instanceof PyException) {
            return (PyException) t;
        } else if (t instanceof InvocationTargetException) {
            return JavaError(((InvocationTargetException) t).getTargetException());
        } else if (t instanceof StackOverflowError) {
            return Py.RuntimeError("maximum recursion depth exceeded (Java StackOverflowError)");
        } else if (t instanceof OutOfMemoryError) {
            memory_error((OutOfMemoryError) t);
        } else if (t instanceof UserInterruptException) {
            return Py.KeyboardInterrupt("");
        }
        PyObject exc = PyJavaType.wrapJavaObject(t);
        PyException pyex = new PyException(exc.getType(), exc);
        // Set the cause to the original throwable to preserve
        // the exception chain.
        pyex.initCause(t);
        return pyex;
    }

    private Py() {
    }

    /**
    Convert a given <code>PyObject</code> to an instance of a Java class.
    Identical to <code>o.__tojava__(c)</code> except that it will
    raise a <code>TypeError</code> if the conversion fails.
    @param o the <code>PyObject</code> to convert.
    @param c the class to convert it to.
     **/
    public static <T> T tojava(PyObject o, Class<T> c) {
        Object obj = o.__tojava__(c);
        if (obj == Py.NoConversion) {
            throw Py.TypeError("can't convert " + o.__repr__() + " to " +
                    c.getName());
        }
        return (T)obj;
    }

    // ??pending: was @deprecated but is actually used by proxie code.
    // Can get rid of it?
    public static Object tojava(PyObject o, String s) {
        Class<?> c = findClass(s);
        if (c == null) {
            throw Py.TypeError("can't convert to: " + s);
        }
        return tojava(o, c); // prev:Class.forName
    }
    /* Helper functions for PyProxy's */

    /* Convenience methods to create new constants without using "new" */
    private final static PyInteger[] integerCache = new PyInteger[1000];

    static {
        for (int j = -100; j < 900; j++) {
            integerCache[j + 100] = new PyInteger(j);
        }
    }

    public static final PyInteger newInteger(int i) {
        if (i >= -100 && i < 900) {
            return integerCache[i + 100];
        } else {
            return new PyInteger(i);
        }
    }

    public static PyObject newInteger(long i) {
        if (i < Integer.MIN_VALUE || i > Integer.MAX_VALUE) {
            return new PyLong(i);
        } else {
            return newInteger((int) i);
        }
    }

    public static PyLong newLong(String s) {
        return new PyLong(s);
    }

    public static PyLong newLong(java.math.BigInteger i) {
        return new PyLong(i);
    }

    public static PyLong newLong(int i) {
        return new PyLong(i);
    }

    public static PyLong newLong(long l) {
        return new PyLong(l);
    }

    public static PyComplex newImaginary(double v) {
        return new PyComplex(0, v);
    }

    public static PyFloat newFloat(float v) {
        return new PyFloat((double) v);
    }

    public static PyFloat newFloat(double v) {
        return new PyFloat(v);
    }

    public static PyString newString(char c) {
        return makeCharacter(c);
    }

    public static PyString newString(String s) {
        return new PyString(s);
    }

    // Use if s may contain Unicode characters,
    // but we prefer to return a PyString
    public static PyString newStringOrUnicode(String s) {
        return newStringOrUnicode(Py.EmptyString, s);
    }

    // Use when returning a PyString or PyUnicode is based on what "kind" is,
    // but always fallback to PyUnicode if s contains Unicode characters.
    public static PyString newStringOrUnicode(PyObject kind, String s) {
        if (kind instanceof PyUnicode) {
            return Py.newUnicode(s);
        }
        if (CharMatcher.ASCII.matchesAllOf(s)) {
            return Py.newString(s);
        } else {
            return Py.newUnicode(s);
        }
    }

    public static PyString newStringUTF8(String s) {
        if (CharMatcher.ASCII.matchesAllOf(s)) {
            // ascii of course is a subset of UTF-8
            return Py.newString(s);
        } else {
            return Py.newString(codecs.PyUnicode_EncodeUTF8(s, null));
        }
    }

    public static PyStringMap newStringMap() {
        // enable lazy bootstrapping (see issue #1671)
        if (!PyType.hasBuilder(PyStringMap.class)) {
            BootstrapTypesSingleton.getInstance().add(PyStringMap.class);
        }
        return new PyStringMap();
    }

    public static PyUnicode newUnicode(char c) {
        return (PyUnicode) makeCharacter(c, true);
    }

    static PyObject newUnicode(int codepoint) {
        return makeCharacter(codepoint, true);
    }

    public static PyUnicode newUnicode(String s) {
        return new PyUnicode(s);
    }

    public static PyUnicode newUnicode(String s, boolean isBasic) {
        return new PyUnicode(s, isBasic);
    }

    public static PyBoolean newBoolean(boolean t) {
        return t ? Py.True : Py.False;
    }

    public static PyObject newDate(Date date) {
        if (date == null) {
            return Py.None;
        }
        PyObject datetimeModule = __builtin__.__import__("datetime");
        PyObject dateClass = datetimeModule.__getattr__("date");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        return dateClass.__call__(newInteger(cal.get(Calendar.YEAR)),
                                  newInteger(cal.get(Calendar.MONTH) + 1),
                                  newInteger(cal.get(Calendar.DAY_OF_MONTH)));

    }

    public static PyObject newTime(Time time) {
        if (time == null) {
            return Py.None;
        }
        PyObject datetimeModule = __builtin__.__import__("datetime");
        PyObject timeClass = datetimeModule.__getattr__("time");
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        return timeClass.__call__(newInteger(cal.get(Calendar.HOUR_OF_DAY)),
                                  newInteger(cal.get(Calendar.MINUTE)),
                                  newInteger(cal.get(Calendar.SECOND)),
                                  newInteger(cal.get(Calendar.MILLISECOND) *
                                             1000));
    }

    public static PyObject newDatetime(Timestamp timestamp) {
        if (timestamp == null) {
            return Py.None;
        }
        PyObject datetimeModule = __builtin__.__import__("datetime");
        PyObject datetimeClass = datetimeModule.__getattr__("datetime");
        Calendar cal = Calendar.getInstance();
        cal.setTime(timestamp);
        return datetimeClass.__call__(new PyObject[] {
                                      newInteger(cal.get(Calendar.YEAR)),
                                      newInteger(cal.get(Calendar.MONTH) + 1),
                                      newInteger(cal.get(Calendar.DAY_OF_MONTH)),
                                      newInteger(cal.get(Calendar.HOUR_OF_DAY)),
                                      newInteger(cal.get(Calendar.MINUTE)),
                                      newInteger(cal.get(Calendar.SECOND)),
                                      newInteger(timestamp.getNanos() / 1000)});
    }

    public static PyObject newDecimal(String decimal) {
        if (decimal == null) {
            return Py.None;
        }
        PyObject decimalModule = __builtin__.__import__("decimal");
        PyObject decimalClass = decimalModule.__getattr__("Decimal");
        return decimalClass.__call__(newString(decimal));
    }

    public static PyCode newCode(int argcount, String varnames[],
            String filename, String name,
            boolean args, boolean keywords,
            PyFunctionTable funcs, int func_id,
            String[] cellvars, String[] freevars,
            int npurecell, int kwonlyargcount, int moreflags) {
        return new PyTableCode(argcount, varnames,
                filename, name, 0, args, keywords, funcs,
                func_id, cellvars, freevars, npurecell,
                kwonlyargcount, moreflags);
    }

    public static PyCode newCode(int argcount, String varnames[],
            String filename, String name,
            int firstlineno,
            boolean args, boolean keywords,
            PyFunctionTable funcs, int func_id,
            String[] cellvars, String[] freevars,
            int npurecell, int kwonlyargcount, int moreflags) {
        return new PyTableCode(argcount, varnames,
                filename, name, firstlineno, args, keywords,
                funcs, func_id, cellvars, freevars, npurecell,
                kwonlyargcount, moreflags);
    }

    // --
    public static PyCode newCode(int argcount, String varnames[],
            String filename, String name,
            boolean args, boolean keywords,
            PyFunctionTable funcs, int func_id) {
        return new PyTableCode(argcount, varnames,
                filename, name, 0, args, keywords, funcs,
                func_id);
    }

    public static PyCode newCode(int argcount, String varnames[],
            String filename, String name,
            int firstlineno,
            boolean args, boolean keywords,
            PyFunctionTable funcs, int func_id) {
        return new PyTableCode(argcount, varnames,
                filename, name, firstlineno, args, keywords,
                funcs, func_id);
    }

    public static PyCode newJavaCode(Class<?> cls, String name) {
        return new JavaCode(newJavaFunc(cls, name));
    }

    public static PyObject newJavaFunc(Class<?> cls, String name) {
        try {
            Method m = cls.getMethod(name, new Class<?>[]{PyObject[].class, String[].class});
            return new JavaFunc(m);
        } catch (NoSuchMethodException e) {
            throw Py.JavaError(e);
        }
    }

    private static PyObject initExc(String name, PyObject exceptions,
            PyObject dict) {
        PyObject tmp = exceptions.__getattr__(name);
        dict.__setitem__(name, tmp);
        return tmp;
    }

    static void initClassExceptions(PyObject dict) {
        PyObject exc = imp.load("exceptions");

        BaseException = initExc("BaseException", exc, dict);
        Exception = initExc("Exception", exc, dict);
        SystemExit = initExc("SystemExit", exc, dict);
        StopIteration = initExc("StopIteration", exc, dict);
        GeneratorExit = initExc("GeneratorExit", exc, dict);
        StandardError = initExc("StandardError", exc, dict);
        KeyboardInterrupt = initExc("KeyboardInterrupt", exc, dict);
        ImportError = initExc("ImportError", exc, dict);
        EnvironmentError = initExc("EnvironmentError", exc, dict);
        IOError = initExc("IOError", exc, dict);
        OSError = initExc("OSError", exc, dict);
        EOFError = initExc("EOFError", exc, dict);
        RuntimeError = initExc("RuntimeError", exc, dict);
        NotImplementedError = initExc("NotImplementedError", exc, dict);
        NameError = initExc("NameError", exc, dict);
        UnboundLocalError = initExc("UnboundLocalError", exc, dict);
        AttributeError = initExc("AttributeError", exc, dict);

        SyntaxError = initExc("SyntaxError", exc, dict);
        IndentationError = initExc("IndentationError", exc, dict);
        TabError = initExc("TabError", exc, dict);
        TypeError = initExc("TypeError", exc, dict);
        AssertionError = initExc("AssertionError", exc, dict);
        LookupError = initExc("LookupError", exc, dict);
        IndexError = initExc("IndexError", exc, dict);
        KeyError = initExc("KeyError", exc, dict);
        ArithmeticError = initExc("ArithmeticError", exc, dict);
        OverflowError = initExc("OverflowError", exc, dict);
        ZeroDivisionError = initExc("ZeroDivisionError", exc, dict);
        FloatingPointError = initExc("FloatingPointError", exc, dict);
        ValueError = initExc("ValueError", exc, dict);
        UnicodeError = initExc("UnicodeError", exc, dict);
        UnicodeEncodeError = initExc("UnicodeEncodeError", exc, dict);
        UnicodeDecodeError = initExc("UnicodeDecodeError", exc, dict);
        UnicodeTranslateError = initExc("UnicodeTranslateError", exc, dict);
        ReferenceError = initExc("ReferenceError", exc, dict);
        SystemError = initExc("SystemError", exc, dict);
        MemoryError = initExc("MemoryError", exc, dict);
        BufferError = initExc("BufferError", exc, dict);
        Warning = initExc("Warning", exc, dict);
        UserWarning = initExc("UserWarning", exc, dict);
        DeprecationWarning = initExc("DeprecationWarning", exc, dict);
        PendingDeprecationWarning = initExc("PendingDeprecationWarning", exc, dict);
        SyntaxWarning = initExc("SyntaxWarning", exc, dict);
        RuntimeWarning = initExc("RuntimeWarning", exc, dict);
        FutureWarning = initExc("FutureWarning", exc, dict);
        ImportWarning = initExc("ImportWarning", exc, dict);
        UnicodeWarning = initExc("UnicodeWarning", exc, dict);
        BytesWarning = initExc("BytesWarning", exc, dict);

        // Pre-initialize the PyJavaClass for OutOfMemoryError so when we need
        // it it creating the pieces for it won't cause an additional out of
        // memory error.  Fix for bug #1654484
        PyType.fromClass(OutOfMemoryError.class);
    }
    public static volatile PySystemState defaultSystemState;
    // This is a hack to get initializations to work in proper order
    public static synchronized boolean initPython() {
        PySystemState.initialize();
        return true;
    }

    private static boolean syspathJavaLoaderRestricted = false;

    /**
     * Common code for findClass and findClassEx
     * @param name Name of the Java class to load and initialize
     * @param reason Reason for loading it, used for debugging. No debug output
     *               is generated if it is null
     * @return the loaded class
     * @throws ClassNotFoundException if the class wasn't found by the class loader
     */
    private static Class<?> findClassInternal(String name, String reason) throws ClassNotFoundException {
        ClassLoader classLoader = Py.getSystemState().getClassLoader();
        if (classLoader != null) {
            if (reason != null) {
                writeDebug("import", "trying " + name + " as " + reason +
                          " in sys.classLoader");
            }
            return loadAndInitClass(name, classLoader);
        } 
        if (!syspathJavaLoaderRestricted) {
            try {
                classLoader = imp.getSyspathJavaLoader();
                if (classLoader != null && reason != null) {
                    writeDebug("import", "trying " + name + " as " + reason +
                            " in SysPathJavaLoader");
                }                
            } catch (SecurityException e) {
                syspathJavaLoaderRestricted = true;
            }
        }        
        if (syspathJavaLoaderRestricted) {
            classLoader = imp.getParentClassLoader();
            if (classLoader != null && reason != null) {
                writeDebug("import", "trying " + name + " as " + reason +
                        " in Jython's parent class loader");     
            }
        } 
        if (classLoader != null) {
            try {
                return loadAndInitClass(name, classLoader);
            } catch (ClassNotFoundException cnfe) {
                // let the default classloader try
                // XXX: by trying another classloader that may not be on a
                //      parent/child relationship with the Jython's parent 
                //      classsloader we are risking some nasty class loading
                //      problems (such as having two incompatible copies for 
                //      the same class that is itself a dependency of two 
                //      classes loaded from these two different class loaders) 
            }
        }
        if (reason != null) {
            writeDebug("import", "trying " + name + " as " + reason +
                       " in context class loader, for backwards compatibility");
        }
        return loadAndInitClass(name, Thread.currentThread().getContextClassLoader());
    }
    
    /**
     * Tries to find a Java class.
     * @param name Name of the Java class.
     * @return The class, or null if it wasn't found
     */
    public static Class<?> findClass(String name) {
        try {
            return findClassInternal(name, null);
        } catch (ClassNotFoundException e) {
            //             e.printStackTrace();
            return null;
        } catch (IllegalArgumentException e) {
            //             e.printStackTrace();
            return null;
        } catch (NoClassDefFoundError e) {
            //             e.printStackTrace();
            return null;
        }
    }

    /**
     * Tries to find a Java class. 
     * 
     * Unless {@link #findClass(String)}, it raises a JavaError 
     * if the class was found but there were problems loading it.
     * @param name Name of the Java class.
     * @param reason Reason for finding the class. Used for debugging messages.
     * @return The class, or null if it wasn't found
     * @throws JavaError wrapping LinkageErrors/IllegalArgumentExceptions 
     * occurred when the class is found but can't be loaded.
     */
    public static Class<?> findClassEx(String name, String reason) {
        try {            
            return findClassInternal(name, reason);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (IllegalArgumentException e) {
            throw JavaError(e);
        } catch (LinkageError e) {
            throw JavaError(e);
        }
    }

    // An alias to express intent (since boolean flags aren't exactly obvious).
    // We *need* to initialize classes on findClass/findClassEx, so that import 
    // statements can trigger static initializers
    private static Class<?> loadAndInitClass(String name, ClassLoader loader) throws ClassNotFoundException {
        return Class.forName(name, true, loader);
    } 
 
    
    public static void initProxy(PyProxy proxy, String module, String pyclass, Object[] args)
    {
        if (proxy._getPyInstance() != null)
            return;
        PyObject instance = (PyObject)(ThreadContext.initializingProxy.get()[0]);
        ThreadState ts = Py.getThreadState();
        if (instance != null) {
            if (JyAttribute.hasAttr(instance, JyAttribute.JAVA_PROXY_ATTR)) {
                throw Py.TypeError("Proxy instance reused");
            }
            JyAttribute.setAttr(instance, JyAttribute.JAVA_PROXY_ATTR, proxy);
            proxy._setPyInstance(instance);
            proxy._setPySystemState(ts.systemState);
            return;
        }

        // Ensure site-packages are available before attempting to import module.
        // This step enables supporting modern Python apps when using proxies
        // directly from Java (eg through clamp).
        importSiteIfSelected();

        PyObject mod = imp.importName(module.intern(), false);
        PyType pyc = (PyType)mod.__getattr__(pyclass.intern());


        PyObject[] pargs;
        if (args == null || args.length == 0) {
            pargs = Py.EmptyObjects;
        } else {
            pargs = Py.javas2pys(args);
        }
        instance = pyc.__call__(pargs);
        JyAttribute.setAttr(instance, JyAttribute.JAVA_PROXY_ATTR, proxy);
        proxy._setPyInstance(instance);
        proxy._setPySystemState(ts.systemState);
    }

    /**
     * Initializes a default PythonInterpreter and runs the code from
     * {@link PyRunnable#getMain} as __main__
     *
     * Called by the code generated in {@link org.python.compiler.Module#addMain()}
     */
    public static void runMain(PyRunnable main, String[] args) throws Exception {
        runMain(new PyRunnableBootstrap(main), args);
    }

    /**
     * Initializes a default PythonInterpreter and runs the code loaded from the
     * {@link CodeBootstrap} as __main__ Called by the code generated in
     * {@link org.python.compiler.Module#addMain()}
     */
    public static void runMain(CodeBootstrap main, String[] args)
            throws Exception {
        PySystemState.initialize(null, null, args, main.getClass().getClassLoader());
        try {
            imp.createFromCode("__main__", CodeLoader.loadCode(main));
        } catch (PyException e) {
            Py.getSystemState().callExitFunc();
            if (e.match(Py.SystemExit)) {
                return;
            }
            throw e;
        }
        Py.getSystemState().callExitFunc();
    }
    //XXX: this needs review to make sure we are cutting out all of the Java
    //     exceptions.
    private static String getStackTrace(Throwable javaError) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        javaError.printStackTrace(new PrintStream(buf));

        String str = buf.toString();
        int index = -1;
        if (index == -1) {
            index = str.indexOf(
                    "at org.python.core.PyReflectedConstructor.__call__");
        }
        if (index == -1) {
            index = str.indexOf("at org.python.core.PyReflectedFunction.__call__");
        }
        if (index == -1) {
            index = str.indexOf(
                    "at org/python/core/PyReflectedConstructor.__call__");
        }
        if (index == -1) {
            index = str.indexOf("at org/python/core/PyReflectedFunction.__call__");
        }

        if (index != -1) {
            index = str.lastIndexOf("\n", index);
        }

        int index0 = str.indexOf("\n");

        if (index >= index0) {
            str = str.substring(index0 + 1, index + 1);
        }

        return str;
    }

    /* Display a PyException and stack trace */
    public static void printException(Throwable t) {
        printException(t, null, null);
    }

    public static void printException(Throwable t, PyFrame f) {
        printException(t, f, null);
    }

    public static synchronized void printException(Throwable t, PyFrame f,
            PyObject file) {
        StdoutWrapper stderr = Py.stderr;

        if (file != null) {
            stderr = new FixedFileWrapper(file);
        }

        if (Options.showJavaExceptions) {
            stderr.println("Java Traceback:");
            java.io.CharArrayWriter buf = new java.io.CharArrayWriter();
            if (t instanceof PyException) {
                ((PyException)t).super__printStackTrace(new java.io.PrintWriter(buf));
            } else {
                t.printStackTrace(new java.io.PrintWriter(buf));
            }
            stderr.print(buf.toString());
        }

        PyException exc = Py.JavaError(t);

        maybeSystemExit(exc);

        setException(exc, f);

        ThreadState ts = getThreadState();

        ts.systemState.last_value = exc.value;
        ts.systemState.last_type = exc.type;
        ts.systemState.last_traceback = exc.traceback;

        PyObject exceptHook = ts.systemState.__findattr__("excepthook");
        if (exceptHook != null) {
            try {
                exceptHook.__call__(exc.type, exc.value, exc.traceback);
            } catch (PyException exc2) {
                exc2.normalize();
                flushLine();
                stderr.println("Error in sys.excepthook:");
                displayException(exc2.type, exc2.value, exc2.traceback, file);
                stderr.println();
                stderr.println("Original exception was:");
                displayException(exc.type, exc.value, exc.traceback, file);
            }
        } else {
            stderr.println("sys.excepthook is missing");
            displayException(exc.type, exc.value, exc.traceback, file);
        }

        ts.exception = null;
    }

    public static void displayException(PyObject type, PyObject value, PyObject tb,
                                        PyObject file) {
        StdoutWrapper stderr = Py.stderr;
        if (file != null) {
            stderr = new FixedFileWrapper(file);
        }
        flushLine();

        if (tb instanceof PyTraceback) {
            stderr.print(((PyTraceback) tb).dumpStack());
        }
        if (__builtin__.isinstance(value, Py.SyntaxError)) {
            PyObject filename = value.__findattr__("filename");
            PyObject text = value.__findattr__("text");
            PyObject lineno = value.__findattr__("lineno");
            stderr.print("  File \"");
            stderr.print(filename == Py.None || filename == null ?
                         "<string>" : filename.toString());
            stderr.print("\", line ");
            stderr.print(lineno == null ? Py.newString("0") : lineno);
            stderr.print("\n");
            if (text != Py.None && text != null && text.__len__() != 0) {
                printSyntaxErrorText(stderr, value.__findattr__("offset").asInt(),
                                     text.toString());
            }
            value = value.__findattr__("msg");
            if (value == null) {
                value = Py.None;
            }
        }

        if (value.getJavaProxy() != null) {
            Object javaError = value.__tojava__(Throwable.class);

            if (javaError != null && javaError != Py.NoConversion) {
                stderr.println(getStackTrace((Throwable) javaError));
            }
        }
        try {
            stderr.println(formatException(type, value));
        } catch (Exception ex) {
            stderr.println(formatException(type, Py.None));
        }
    }

    /**
     * Print the two lines showing where a SyntaxError was caused.
     *
     * @param out StdoutWrapper to print to
     * @param offset the offset into text
     * @param text a source code String line
     */
    private static void printSyntaxErrorText(StdoutWrapper out, int offset, String text) {
        if (offset >= 0) {
            if (offset > 0 && offset == text.length()) {
                offset--;
            }

            // Eat lines if the offset is on a subsequent line
            while (true) {
                int nl = text.indexOf("\n");
                if (nl == -1 || nl >= offset) {
                    break;
                }
                offset -= nl + 1;
                text = text.substring(nl + 1, text.length());
            }

            // lstrip
            int i = 0;
            for (; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c != ' ' && c != '\t') {
                    break;
                }
                offset--;
            }
            text = text.substring(i, text.length());
        }

        out.print("    ");
        out.print(text);
        if (text.length() == 0 || !text.endsWith("\n")) {
            out.print("\n");
        }
        if (offset == -1) {
            return;
        }
        out.print("    ");
        for (offset--; offset > 0; offset--) {
            out.print(" ");
        }
        out.print("^\n");
    }

    public static String formatException(PyObject type, PyObject value) {
        return formatException(type, value, false);
    }

    public static String formatException(PyObject type, PyObject value, boolean useRepr) {
        StringBuilder buf = new StringBuilder();

        if (PyException.isExceptionClass(type)) {
            String className = PyException.exceptionClassName(type);
            int lastDot = className.lastIndexOf('.');
            if (lastDot != -1) {
                className = className.substring(lastDot + 1);
            }
            PyObject moduleName = type.__findattr__("__module__");
            if (moduleName == null) {
                buf.append("<unknown>");
            } else {
                String moduleStr = moduleName.toString();
                if (!moduleStr.equals("exceptions")) {
                    buf.append(moduleStr);
                    buf.append(".");
                }
            }
            buf.append(className);
        } else {
            buf.append(useRepr ? type.__repr__() : type.__str__());
        }
        if (value != null && value != Py.None) {
            // only print colon if the str() of the object is not the empty string
            PyObject s = useRepr ? value.__repr__() : value.__str__();
            if (!(s instanceof PyString) || s.__len__() != 0) {
                buf.append(": ");
            }
            buf.append(s);
        }
        return buf.toString();
    }

    public static void writeUnraisable(Throwable unraisable, PyObject obj) {
        PyException pye = JavaError(unraisable);
        stderr.println(String.format("Exception %s in %s ignored",
                                     formatException(pye.type, pye.value, true), obj));
    }


    /* Equivalent to Python's assert statement */
    public static void assert_(PyObject test, PyObject message) {
        if (!test.__bool__()) {
            throw new PyException(Py.AssertionError, message);
        }
    }

    public static void assert_(PyObject test) {
        assert_(test, Py.None);
    }

    /* Helpers to implement finally clauses */
    public static void addTraceback(Throwable t, PyFrame frame) {
        Py.JavaError(t).tracebackHere(frame, true);
    }

    /* Helpers to implement except clauses */
    public static PyException setException(Throwable t, PyFrame frame) {
        PyException pye = Py.JavaError(t);
        pye.normalize();
        pye.tracebackHere(frame);
        getThreadState().exception = pye;
        return pye;
    }


    /**
     * @deprecated As of Jython 2.5, use {@link PyException#match} instead.
     */
    @Deprecated
    public static boolean matchException(PyException pye, PyObject exc) {
        return pye.match(exc);
    }


    // XXX: the following 4 are backwards compat. for the
    // oldcompiler. newcompiler should just call doRaise instead
    public static PyException makeException(PyObject type, PyObject value,
                                            PyObject traceback) {
        return PyException.doRaise(type, value, traceback);
    }

    public static PyException makeException(PyObject type, PyObject value) {
        return makeException(type, value, null);
    }

    public static PyException makeException(PyObject type) {
        return makeException(type, null);
    }

    public static PyException makeException() {
        return makeException(null);
    }

    public static PyObject runCode(PyCode code, PyObject locals, PyObject globals) {
        PyFrame f;
        ThreadState ts = getThreadState();
        if (locals == null || locals == Py.None) {
            if (globals != null && globals != Py.None) {
                locals = globals;
            } else {
                locals = ts.frame.getLocals();
            }
        }

        if (globals == null || globals == Py.None) {
            globals = ts.frame.f_globals;
        } else if (globals.__finditem__("__builtins__") == null) {
            // Apply side effect of copying into globals,
            // per documentation of eval and observed behavior of exec
            try {
                globals.__setitem__("__builtins__", Py.getSystemState().modules.__finditem__("__builtin__").__getattr__("__dict__"));
            } catch (PyException e) {
                // Quietly ignore if cannot set __builtins__ - Jython previously allowed a much wider range of
                // mappable objects for the globals mapping than CPython, do not want to break existing code
                // as we try to get better CPython compliance
                if (!e.match(AttributeError)) {
                    throw e;
                }
            }
        }

        PyBaseCode baseCode = null;
        if (code instanceof PyBaseCode) {
            baseCode = (PyBaseCode) code;
        }

        f = new PyFrame(baseCode, locals, globals, Py.getSystemState().getBuiltins());
        return code.call(ts, f);
    }

    public static void exec(PyObject o, PyObject globals, PyObject locals) {
        PyCode code;
        int flags = 0;
        if (o instanceof PyTuple) {
            PyTuple tuple = (PyTuple) o;
            int len = tuple.__len__();
            if ((globals == null || globals.equals(None))
                    && (locals == null || locals.equals(None))
                    && (len >= 2 && len <= 3)) {
                o = tuple.__getitem__(0);
                globals = tuple.__getitem__(1);
                if (len == 3) {
                    locals = tuple.__getitem__(2);
                }
            }
        }
        if (o instanceof PyCode) {
            code = (PyCode) o;
            if (locals == null && o instanceof PyBaseCode && ((PyBaseCode) o).hasFreevars()) {
                throw Py.TypeError("code object passed to exec may not contain free variables");
            }
        } else {
            String contents = null;
            if (o instanceof PyString) {
                if (o instanceof PyUnicode) {
                    flags |= CompilerFlags.PyCF_SOURCE_IS_UTF8;
                }
                contents = o.toString();
            } else if (o instanceof PyFile) {
                PyFile fp = (PyFile) o;
                if (fp.getClosed()) {
                    return;
                }
                contents = fp.read().toString();
            } else {
                throw Py.TypeError(
                        "exec: argument 1 must be string, code or file object");
            }
            code = Py.compile_flags(contents, "<string>", CompileMode.exec,
                                    getCompilerFlags(flags, false));
        }
        Py.runCode(code, locals, globals);
    }

    private final static ThreadStateMapping threadStateMapping = new ThreadStateMapping();

    public static final ThreadState getThreadState() {
        return getThreadState(null);
    }

    public static final ThreadState getThreadState(PySystemState newSystemState) {
        return threadStateMapping.getThreadState(newSystemState);
    }

    public static final PySystemState setSystemState(PySystemState newSystemState) {
        ThreadState ts = getThreadState(newSystemState);
        PySystemState oldSystemState = ts.systemState;
        if (oldSystemState != newSystemState) {
            //XXX: should we make this a real warning?
            //System.err.println("Warning: changing systemState "+
            //                   "for same thread!");
            ts.systemState = newSystemState;
        }
        return oldSystemState;
    }

    public static final PySystemState getSystemState() {
        return getThreadState().systemState;
    //defaultSystemState;
    }

    /* Get and set the current frame */
    public static PyFrame getFrame() {
        ThreadState ts = getThreadState();
        if (ts == null) {
            return null;
        }
        return ts.frame;
    }

    public static void setFrame(PyFrame f) {
        getThreadState().frame = f;
    }

    /**
     * The handler for interactive consoles, set by {@link #installConsole(Console)} and accessed by
     * {@link #getConsole()}.
     */
    private static Console console;

    /**
     * Get the Jython Console (used for <code>input()</code>, <code>raw_input()</code>, etc.) as
     * constructed and set by {@link PySystemState} initialization.
     *
     * @return the Jython Console
     */
    public static Console getConsole() {
        if (console == null) {
            // We really shouldn't ask for a console before PySystemState initialization but ...
            try {
                // ... something foolproof that we can supersede.
                installConsole(new PlainConsole("ascii"));
            } catch (Exception e) {
                // This really, really shouldn't happen
                throw Py.RuntimeError("Could not create fall-back PlainConsole: " + e);
            }
        }
        return console;
    }

    /**
     * Install the provided Console, first uninstalling any current one. The Jython Console is used
     * for <code>raw_input()</code> etc., and may provide line-editing and history recall at the
     * prompt. A Console may replace <code>System.in</code> with its line-editing input method.
     *
     * @param console The new Console object
     * @throws UnsupportedOperationException if some prior Console refuses to uninstall
     * @throws IOException if {@link Console#install()} raises it
     */
    public static void installConsole(Console console) throws UnsupportedOperationException,
            IOException {
        if (Py.console != null) {
            // Some Console class already installed: may be able to uninstall
            Py.console.uninstall();
            Py.console = null;
        }

        // Install the specified Console
        console.install();
        Py.console = console;

        // Cause sys (if it exists) to export the console handler that was installed
        if (Py.defaultSystemState != null) {
            Py.defaultSystemState.__setattr__("_jy_console", Py.java2py(console));
        }
    }

    /**
     * Check (using the {@link POSIX} library and <code>jnr-posix</code> library) whether we are in
     * an interactive environment. Amongst other things, this affects the type of console that may
     * be legitimately installed during system initialisation. Note that the result may vary
     * according to whether a <code>jnr-posix</code> native library is found along
     * <code>java.library.path</code>, or the pure Java fall-back is used.
     *
     * @return true if (we think) we are in an interactive environment
     */
    public static boolean isInteractive() {
        // python.launcher.tty is authoratative; see http://bugs.jython.org/issue2325
        String isTTY = System.getProperty("python.launcher.tty");
        if (isTTY != null && isTTY.equals("true")) {
            return true;
        }
        if (isTTY != null && isTTY.equals("false")) {
            return false;
        }
        // Decide if System.in is interactive
        try {
            POSIX posix = POSIXFactory.getPOSIX();
            FileDescriptor in = FileDescriptor.in;
            return posix.isatty(in);
        } catch (SecurityException ex) {
            return false;
        }
    }

    public static boolean importSiteIfSelected() {
        if (Options.importSite) {
            try {
                // Ensure site-packages are available
                imp.load("site");
                return true;
            } catch (PyException pye) {
                if (pye.match(Py.ImportError)) {
                    PySystemState sys = Py.getSystemState();
                    throw Py.ImportError(String.format(""
                                    + "Cannot import site module and its dependencies: %s\n"
                                    + "Determine if the following attributes are correct:\n"
                                    + "  * sys.path: %s\n"
                                    + "    This attribute might be including the wrong directories, such as from CPython\n"
                                    + "  * sys.prefix: %s\n"
                                    + "    This attribute is set by the system property python.home, although it can\n"
                                    + "    be often automatically determined by the location of the Jython jar file\n\n"
                                    + "You can use the -S option or python.import.site=false to not import the site module",
                            pye.value.__getattr__("args").__getitem__(0),
                            sys.path,
                            sys.prefix));
                } else {
                    throw pye;
                }
            }
        }
        return false;
    }

    /* A collection of functions for implementing the print statement */
    public static StdoutWrapper stderr = new StderrWrapper();
    static StdoutWrapper stdout = new StdoutWrapper();

    //public static StdinWrapper stdin;
    public static void print(PyObject file, PyObject o) {
        if (file == None) {
            print(o);
        } else {
            new FixedFileWrapper(file).print(o);
        }
    }

    public static void printComma(PyObject file, PyObject o) {
        if (file == None) {
            printComma(o);
        } else {
            new FixedFileWrapper(file).printComma(o);
        }
    }

    public static void println(PyObject file, PyObject o) {
        if (file == None) {
            println(o);
        } else {
            new FixedFileWrapper(file).println(o);
        }
    }

    public static void printlnv(PyObject file) {
        if (file == None) {
            println();
        } else {
            new FixedFileWrapper(file).println();
        }
    }

    public static void print(PyObject o) {
        stdout.print(o);
    }

    public static void printComma(PyObject o) {
        stdout.printComma(o);
    }

    public static void println(PyObject o) {
        stdout.println(o);
    }

    public static void println() {
        stdout.println();
    }

    public static void flushLine() {
        stdout.flushLine();
    }

    /*
     * A collection of convenience functions for converting PyObjects to Java primitives
     */
    public static boolean py2boolean(PyObject o) {
        return o.__bool__();
    }

    public static byte py2byte(PyObject o) {
        if (o instanceof PyInteger) {
            return (byte) ((PyInteger) o).getValue();
        }
        Object i = o.__tojava__(Byte.TYPE);
        if (i == null || i == Py.NoConversion) {
            throw Py.TypeError("integer required");
        }
        return ((Byte) i).byteValue();
    }

    public static short py2short(PyObject o) {
        if (o instanceof PyInteger) {
            return (short) ((PyInteger) o).getValue();
        }
        Object i = o.__tojava__(Short.TYPE);
        if (i == null || i == Py.NoConversion) {
            throw Py.TypeError("integer required");
        }
        return ((Short) i).shortValue();
    }

    public static int py2int(PyObject o) {
        return py2int(o, "integer required");
    }

    public static int py2int(PyObject o, String msg) {
        if (o instanceof PyInteger) {
            return ((PyInteger) o).getValue();
        }
        Object obj = o.__tojava__(Integer.TYPE);
        if (obj == Py.NoConversion) {
            throw Py.TypeError(msg);
        }
        return ((Integer) obj).intValue();
    }

    public static long py2long(PyObject o) {
        if (o instanceof PyInteger) {
            return ((PyInteger) o).getValue();
        }
        Object i = o.__tojava__(Long.TYPE);
        if (i == null || i == Py.NoConversion) {
            throw Py.TypeError("integer required");
        }
        return ((Long) i).longValue();
    }

    public static float py2float(PyObject o) {
        if (o instanceof PyFloat) {
            return (float) ((PyFloat) o).getValue();
        }
        if (o instanceof PyInteger) {
            return ((PyInteger) o).getValue();
        }
        Object i = o.__tojava__(Float.TYPE);
        if (i == null || i == Py.NoConversion) {
            throw Py.TypeError("float required");
        }
        return ((Float) i).floatValue();
    }

    public static double py2double(PyObject o) {
        if (o instanceof PyFloat) {
            return ((PyFloat) o).getValue();
        }
        if (o instanceof PyInteger) {
            return ((PyInteger) o).getValue();
        }
        Object i = o.__tojava__(Double.TYPE);
        if (i == null || i == Py.NoConversion) {
            throw Py.TypeError("float required");
        }
        return ((Double) i).doubleValue();
    }

    public static char py2char(PyObject o) {
        return py2char(o, "char required");
    }

    public static char py2char(PyObject o, String msg) {
        if (o instanceof PyString) {
            PyString s = (PyString) o;
            if (s.__len__() != 1) {
                throw Py.TypeError(msg);
            }
            return s.toString().charAt(0);
        }
        if (o instanceof PyInteger) {
            return (char) ((PyInteger) o).getValue();
        }
        Object i = o.__tojava__(Character.TYPE);
        if (i == null || i == Py.NoConversion) {
            throw Py.TypeError(msg);
        }
        return ((Character) i).charValue();
    }

    public static void py2void(PyObject o) {
        if (o != Py.None) {
            throw Py.TypeError("None required for void return");
        }
    }

    private final static PyString[] letters = new PyString[256];

    static {
        for (char j = 0; j < 256; j++) {
            letters[j] = new PyString(j);
        }
    }

    public static final PyString makeCharacter(Character o) {
        return makeCharacter(o.charValue());
    }

    public static final PyString makeCharacter(char c) {
        if (c <= 255) {
            return letters[c];
        } else {
            // This will throw IllegalArgumentException since non-byte value
            return new PyString(c);
        }
    }

    static final PyString makeCharacter(int codepoint, boolean toUnicode) {
        if (toUnicode) {
            return new PyUnicode(codepoint);
        } else if (codepoint < 0 || codepoint > 255) {
            // This will throw IllegalArgumentException since non-byte value
            return new PyString('\uffff');
        }
        return letters[codepoint];
    }

    /**
     * Uses the PyObjectAdapter passed to {@link PySystemState#initialize} to turn o into a PyObject.
     *
     * @see ClassicPyObjectAdapter - default PyObjectAdapter type
     */
    public static PyObject java2py(Object o) {
        return getAdapter().adapt(o);
    }

    /**
     * Uses the PyObjectAdapter passed to {@link PySystemState#initialize} to turn
     * <code>objects</code> into an array of PyObjects.
     *
     * @see ClassicPyObjectAdapter - default PyObjectAdapter type
     */
    public static PyObject[] javas2pys(Object... objects) {
        PyObject[] objs = new PyObject[objects.length];
        for (int i = 0; i < objs.length; i++) {
            objs[i] = java2py(objects[i]);
        }
        return objs;
    }

    /**
     * @return the ExtensiblePyObjectAdapter used by java2py.
     */
    public static ExtensiblePyObjectAdapter getAdapter() {
        if (adapter == null) {
            adapter = new ClassicPyObjectAdapter();
        }
        return adapter;
    }

    /**
     * Set the ExtensiblePyObjectAdapter used by java2py.
     *
     * @param adapter The new ExtensiblePyObjectAdapter
     */
    protected static void setAdapter(ExtensiblePyObjectAdapter adapter) {
        Py.adapter = adapter;
    }
    /**
     * Handles wrapping Java objects in PyObject to expose them to jython.
     */
    private static ExtensiblePyObjectAdapter adapter;

    // XXX: The following two makeClass overrides are *only* for the
    // old compiler, they should be removed when the newcompiler hits
    public static PyObject makeClass(String name, PyObject[] bases, PyCode code) {
        return makeClass(name, bases, code, null);
    }

    public static PyObject makeClass(String name, PyObject[] bases, PyCode code,
                                     PyObject[] closure_cells) {
        ThreadState state = getThreadState();
        PyObject dict = code.call(state, Py.EmptyObjects, Py.NoKeywords,
                state.frame.f_globals, Py.EmptyObjects, new PyDictionary(), new PyTuple(closure_cells));
        return makeClass(name, bases, dict, metaclass);
    }
    public static PyObject makeClass(String name, PyObject base, PyObject dict) {
        return makeClass(name, base, dict, null);
    }

    public static PyObject makeClass(String name, PyObject base, PyObject dict) {
        PyObject[] bases = base == null ? EmptyObjects : new PyObject[] {base};
        return makeClass(name, bases, dict);
    }

    /**
     * Create a new Python class.
     *
     * @param name the String name of the class
     * @param bases an array of PyObject base classes
     * @param dict the class's namespace, containing the class body
     * definition
     * @return a new Python Class PyObject
     */
    public static PyObject makeClass(String name, PyObject[] bases, PyObject dict) {
        PyObject metaclass = dict.__finditem__("__metaclass__");

        if (metaclass == null) {
            if (bases.length != 0) {
                PyObject base = bases[0];
                metaclass = base.__findattr__("__class__");
                if (metaclass == null) {
                    metaclass = base.getType();
                }
            } else {
                PyObject globals = getFrame().f_globals;
                if (globals != null) {
                    metaclass = globals.__finditem__("__metaclass__");
                }
                if (metaclass == null) {
                    metaclass = PyClass.TYPE;
                }
            }
        }

        try {
            return metaclass.__call__(new PyString(name), new PyTuple(bases), dict);
        } catch (PyException pye) {
            if (!pye.match(TypeError)) {
                throw pye;
            }
            pye.value = Py.newString(String.format("Error when calling the metaclass bases\n    "
                                                   + "%s", pye.value.__str__().toString()));
            throw pye;
        }
    }
    private static int nameindex = 0;

    public static synchronized String getName() {
        String name = "org.python.pycode._pyx" + nameindex;
        nameindex += 1;
        return name;
    }

    public static CompilerFlags getCompilerFlags() {
        return CompilerFlags.getCompilerFlags();
    }

    public static CompilerFlags getCompilerFlags(int flags, boolean dont_inherit) {
        final PyFrame frame;
        if (dont_inherit) {
            frame = null;
        } else {
            frame = Py.getFrame();
        }
        return CompilerFlags.getCompilerFlags(flags, frame);
    }

    public static CompilerFlags getCompilerFlags(CompilerFlags flags, boolean dont_inherit) {
        final PyFrame frame;
        if (dont_inherit) {
            frame = null;
        } else {
            frame = Py.getFrame();
        }
        return CompilerFlags.getCompilerFlags(flags, frame);
    }

    // w/o compiler-flags
    public static PyCode compile(InputStream istream, String filename, CompileMode kind) {
        return compile_flags(istream, filename, kind, new CompilerFlags());
    }

    /**
     * Entry point for compiling modules.
     *
     * @param node Module node, coming from the parsing process
     * @param name Internal name for the compiled code. Typically generated by
     *        calling {@link #getName()}.
     * @param filename Source file name
     * @param linenumbers True to track source line numbers on the generated
     *        code
     * @param printResults True to call the sys.displayhook on the result of
     *                     the code
     * @param cflags Compiler flags
     * @return Code object for the compiled module
     */
    public static PyCode compile_flags(mod node, String name, String filename,
                                         boolean linenumbers, boolean printResults,
                                         CompilerFlags cflags) {
        return CompilerFacade.compile(node, name, filename, linenumbers, printResults, cflags);
    }

    public static PyCode compile_flags(mod node, String filename,
                                         CompileMode kind, CompilerFlags cflags) {
        return Py.compile_flags(node, getName(), filename, true,
                                kind == CompileMode.single, cflags);
    }

    /**
     * Compiles python source code coming from a file or another external stream
     */
    public static PyCode compile_flags(InputStream istream, String filename,
                                         CompileMode kind, CompilerFlags cflags) {
        mod node = ParserFacade.parse(istream, kind, filename, cflags);
        return Py.compile_flags(node, filename, kind, cflags);
    }

    /**
     * Compiles python source code coming from String (raw bytes) data.
     *
     * If the String is properly decoded (from PyUnicode) the PyCF_SOURCE_IS_UTF8 flag
     * should be specified.
     */
    public static PyCode compile_flags(String data, String filename,
                                         CompileMode kind, CompilerFlags cflags) {
        if (data.contains("\0")) {
            throw Py.TypeError("compile() expected string without null bytes");
        }
        if (cflags != null && cflags.dont_imply_dedent) {
            data += "\n";
        } else {
            data += "\n\n";
        }
        mod node = ParserFacade.parse(data, kind, filename, cflags);
        return Py.compile_flags(node, filename, kind, cflags);
    }

    public static PyObject compile_command_flags(String string, String filename,
            CompileMode kind, CompilerFlags cflags, boolean stdprompt) {
        mod node = ParserFacade.partialParse(string + "\n", kind, filename,
                                                 cflags, stdprompt);
        if (node == null) {
            return Py.None;
        }

        return Py.compile_flags(node, Py.getName(), filename, true, true, cflags);
    }

    public static PyObject[] unpackIterator(PyObject obj, int argcount, int argcountAfter) {
        if (obj instanceof PyTuple && obj.__len__() == argcount && argcountAfter == -1) {
            // optimization
            return ((PyTuple)obj).getArray();
        }

        PyObject[] ret = new PyObject[argcount + argcountAfter + 1];
        PyObject iter = obj.__iter__();
        int i = 0;
        for (; i < argcount; i++) {
            PyObject tmp = iter.__iternext__();
            if (tmp == null) {
                if (argcountAfter == -1) {
                    throw Py.ValueError(String.format("not enough values to unpack (expected %d, got %d)",
                                        argcount, i));
                } else {
                    throw Py.ValueError(String.format("not enough values to unpack (expected at least %d, got %d)",
                                        argcount + argcountAfter, i));
                }
            }
            ret[i] = tmp;
        }

        if (argcountAfter == -1) {
            // We better have exhausted the iterator now.
            if (iter.__iternext__() != null) {
                throw Py.ValueError(String.format("too many values to unpack (expected %d)", argcount));
            }
        } else {
            PyList after = new PyList();
            for (PyObject o = null; (o = iter.__iternext__()) != null;) {
                after.append(o);
            }
            if (after.size() < argcountAfter) {
                throw Py.ValueError(String.format("not enough values to unpack (expected at least %d, got %d)",
                                                  argcount + argcountAfter, argcount + after.size()));
            }
            ret[i] = after;
            i++;
            for (int j = after.size() - argcountAfter;j < after.size();j++, i++) {
                ret[i] = after.pop(j);
            }
        }
        return ret;
    }

    public static PyObject iter(PyObject seq, String message) {
        try {
            return seq.__iter__();
        } catch (PyException exc) {
            if (exc.match(Py.TypeError)) {
                throw Py.TypeError(message);
            }
            throw exc;
        }
    }
    private static IdImpl idimpl = new IdImpl();

    public static long id(PyObject o) {
        return idimpl.id(o);
    }

    public static String idstr(PyObject o) {
        return idimpl.idstr(o);
    }

    public static long java_obj_id(Object o) {
        return idimpl.java_obj_id(o);
    }

    public static void printResult(PyObject ret) {
        Py.getThreadState().systemState.invoke("displayhook", ret);
    }
    public static final int ERROR = -1;
    public static final int WARNING = 0;
    public static final int MESSAGE = 1;
    public static final int COMMENT = 2;
    public static final int DEBUG = 3;

    public static void maybeWrite(String type, String msg, int level) {
        if (level <= Options.verbose) {
            System.err.println(type + ": " + msg);
        }
    }

    public static void writeError(String type, String msg) {
        maybeWrite(type, msg, ERROR);
    }

    public static void writeWarning(String type, String msg) {
        maybeWrite(type, msg, WARNING);
    }

    public static void writeMessage(String type, String msg) {
        maybeWrite(type, msg, MESSAGE);
    }

    public static void writeComment(String type, String msg) {
        maybeWrite(type, msg, COMMENT);
    }

    public static void writeDebug(String type, String msg) {
        maybeWrite(type, msg, DEBUG);
    }

    public static void saveClassFile(String name, ByteArrayOutputStream bytestream) {
        String dirname = Options.proxyDebugDirectory;
        if (dirname == null) {
            return;
        }

        byte[] bytes = bytestream.toByteArray();
        File dir = new File(dirname);
        File file = makeFilename(name, dir);
        new File(file.getParent()).mkdirs();
        try {
            FileOutputStream o = new FileOutputStream(file);
            o.write(bytes);
            o.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static File makeFilename(String name, File dir) {
        int index = name.indexOf(".");
        if (index == -1) {
            return new File(dir, name + ".class");
        }

        return makeFilename(name.substring(index + 1, name.length()),
                new File(dir, name.substring(0, index)));
    }

    public static boolean isInstance(PyObject inst, PyObject cls) {
        // Quick test for an exact match
        if (inst.getType() == cls) {
            return true;
        }

        if (cls instanceof PyTuple) {
            for (PyObject item : cls.asIterable()) {
                if (isInstance(inst, item)) {
                    return true;
                }
            }
            return false;
        }

        PyObject checkerResult;
        if ((checkerResult = dispatchToChecker(inst, cls, "__instancecheck__")) != null) {
            return checkerResult.__bool__();
        }

        return recursiveIsInstance(inst, cls);
    }

    static boolean recursiveIsInstance(PyObject inst, PyObject cls) {
        if (cls instanceof PyClass && inst instanceof PyInstance) {
            PyClass inClass = ((PyInstance) inst).fastGetClass();
            return inClass.isSubClass((PyClass) cls);
        }
        if (cls instanceof PyType) {
            PyType type = (PyType)cls;

            //Special case PyStringMap to compare as an instance type dict.
            if (inst instanceof PyStringMap &&
                type.equals(PyDictionary.TYPE)) {
                    return true;
            }

            PyType instType = inst.getType();

            // equiv. to PyObject_TypeCheck
            if (instType == type || instType.isSubType(type)) {
                return true;
            }

            PyObject instCls = inst.__findattr__("__class__");
            if (instCls != null && instCls != instType && instCls instanceof PyType) {
                return ((PyType) instCls).isSubType(type);
            }
            return false;
        }
        
        checkClass(cls, "isinstance() arg 2 must be a class, type, or tuple of classes and types");
        PyObject instCls = inst.__findattr__("__class__");
        if (instCls == null) {
            return false;
        }
        return abstractIsSubClass(instCls, cls);
    }

    public static boolean isSubClass(PyObject derived, PyObject cls) {
        if (cls instanceof PyTuple) {
            for (PyObject item : cls.asIterable()) {
                if (isSubClass(derived, item)) {
                    return true;
                }
            }
            return false;
        }

        PyObject checkerResult;
        if ((checkerResult = dispatchToChecker(derived, cls, "__subclasscheck__")) != null) {
            return checkerResult.__bool__();
        }

        return recursiveIsSubClass(derived, cls);
    }

    static boolean recursiveIsSubClass(PyObject derived, PyObject cls) {
        if (derived instanceof PyType && cls instanceof PyType) {
            if (derived == cls) {
                return true;
            }
            PyType type = (PyType)cls;
            PyType subtype = (PyType)derived;

            // Special case PyStringMap to compare as a subclass of
            // PyDictionary. Note that we don't need to check for stringmap
            // subclasses, since stringmap can't be subclassed. PyStringMap's
            // TYPE is computed lazily, so we have to use PyType.fromClass :(
            if (type == PyDictionary.TYPE &&
                subtype == PyType.fromClass(PyStringMap.class)) {
                return true;
            }

            return subtype.isSubType(type);
        }
        if (derived instanceof PyClass && cls instanceof PyClass) {
            return ((PyClass) derived).isSubClass((PyClass) cls);
        }

        checkClass(derived, "issubclass() arg 1 must be a class");
        checkClass(cls, "issubclass() arg 2 must be a class or tuple of classes");
        return abstractIsSubClass(derived, cls);
    }

    private static boolean abstractIsSubClass(PyObject derived, PyObject cls) {
        while (true) {
            if (derived == cls) {
                return true;
            }

            PyTuple bases = abstractGetBases(derived);
            if (bases == null) {
                return false;
            }

            int basesSize = bases.size();
            if (basesSize == 0) {
                return false;
            }
            if (basesSize == 1) {
                // Avoid recursivity in the single inheritance case
                derived = bases.pyget(0);
                continue;
            }

            for (PyObject base : bases.asIterable()) {
                if (abstractIsSubClass(base, cls)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Attempt to dispatch an isinstance/issubclass call to cls's associated
     * __instancecheck__/__subclasscheck__.
     *
     * @param checkerArg the argument to call the checker with
     * @param cls a Python class
     * @param checkerName the checker name
     * @return null if cls provides no checker, otherwise the result of calling the
     * checker
     */
    private static PyObject dispatchToChecker(PyObject checkerArg, PyObject cls,
                                              String checkerName) {
        //Ignore old style classes.
        if (cls instanceof PyClass) {
            return null;
        }

        PyObject checker = cls.__findattr__(checkerName);
        if (checker == null) {
            return null;
        }

        return checker.__call__(checkerArg);
    }

    /**
     * Return the __bases__ of cls. Returns null if no valid __bases__ are found.
     */
    private static PyTuple abstractGetBases(PyObject cls) {
        PyObject bases = cls.__findattr__("__bases__");
        return bases instanceof PyTuple ? (PyTuple) bases : null;
    }

    /**
     * Throw a TypeError with the specified message if cls does not appear to be a Python
     * class.
     */
    private static void checkClass(PyObject cls, String message) {
        if (abstractGetBases(cls) == null) {
            throw Py.TypeError(message);
        }
    }

    static PyObject[] make_array(PyObject iterable) {
        // Special-case the common tuple and list cases, for efficiency
        if (iterable instanceof PySequenceList) {
            return ((PySequenceList) iterable).getArray();
        }

        // Guess result size and allocate space. The typical make_array arg supports
        // __len__, with one exception being generators, so avoid the overhead of an
        // exception from __len__ in their case
        int n = 10;
        if (!(iterable instanceof PyGenerator)) {
            try {
                n = iterable.__len__();
            } catch (PyException pye) {
                // ok
            }
        }

        List<PyObject> objs = new ArrayList<PyObject>(n);
        for (PyObject item : iterable.asIterable()) {
            objs.add(item);
        }
        return objs.toArray(Py.EmptyObjects);
    }
}

class FixedFileWrapper extends StdoutWrapper {

    private PyObject file;

    public FixedFileWrapper(PyObject file) {
        name = "fixed file";
        this.file = file;

        if (file.getJavaProxy() != null) {
            Object tojava = file.__tojava__(OutputStream.class);
            if (tojava != null && tojava != Py.NoConversion) {
                this.file = new PyFile((OutputStream) tojava);
            }
        }
    }

    @Override
    protected PyObject myFile() {
        return file;
    }
}

/**
 * A code object wrapper for a python function.
 */
class JavaCode extends PyCode implements Traverseproc {

    private PyObject func;

    public JavaCode(PyObject func) {
        this.func = func;
        if (func instanceof PyReflectedFunction) {
            this.co_name = ((PyReflectedFunction) func).__name__;
        }
    }

    @Override
    public PyObject call(ThreadState state, PyFrame frame, PyObject closure) {
        //XXX: what the heck is this?  Looks like debug code, but it's
        //     been here a long time...
        System.out.println("call #1");
        return Py.None;
    }

    @Override
    public PyObject call(ThreadState state, PyObject args[], String keywords[],
                         PyObject globals, PyObject[] defaults, PyDictionary kw_defaults,
                         PyObject closure) {
        return func.__call__(args, keywords);
    }

    @Override
    public PyObject call(ThreadState state, PyObject self, PyObject args[], String keywords[],
                         PyObject globals, PyObject[] defaults, PyDictionary kw_defaults,
                         PyObject closure) {
        return func.__call__(self, args, keywords);
    }

    @Override
    public PyObject call(ThreadState state, PyObject globals, PyObject[] defaults,
                         PyDictionary kw_defaults, PyObject closure) {
        return func.__call__();
    }

    @Override
    public PyObject call(ThreadState state, PyObject arg1, PyObject globals,
                         PyObject[] defaults, PyDictionary kw_defaults, PyObject closure) {
        return func.__call__(arg1);
    }

    @Override
    public PyObject call(ThreadState state, PyObject arg1, PyObject arg2, PyObject globals,
                         PyObject[] defaults, PyDictionary kw_defaults, PyObject closure) {
        return func.__call__(arg1, arg2);
    }

    @Override
    public PyObject call(ThreadState state, PyObject arg1, PyObject arg2, PyObject arg3,
                         PyObject globals, PyObject[] defaults, PyDictionary kw_defaults,
                         PyObject closure) {
        return func.__call__(arg1, arg2, arg3);
    }

    @Override
    public PyObject call(ThreadState state, PyObject arg1, PyObject arg2,
                         PyObject arg3, PyObject arg4, PyObject globals,
                         PyObject[] defaults, PyDictionary kw_defaults, PyObject closure) {
        return func.__call__(arg1, arg2, arg3, arg4);
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        return func != null ? visit.visit(func, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && ob == func;
    }
}

/**
 * A function object wrapper for a java method which comply with the
 * PyArgsKeywordsCall standard.
 */
@Untraversable
class JavaFunc extends PyObject {

    Method method;

    public JavaFunc(Method method) {
        this.method = method;
    }

    @Override
    public PyObject __call__(PyObject[] args, String[] kws) {
        Object[] margs = new Object[]{args, kws};
        try {
            return Py.java2py(method.invoke(null, margs));
        } catch (Throwable t) {
            throw Py.JavaError(t);
        }
    }

    @Override
    public PyObject _doget(PyObject container) {
        return _doget(container, null);
    }

    @Override
    public PyObject _doget(PyObject container, PyObject wherefound) {
        if (container == null) {
            return this;
        }
        return new PyMethod(this, container, wherefound);
    }

    public boolean _doset(PyObject container) {
        throw Py.TypeError("java function not settable: " + method.getName());
    }
}
