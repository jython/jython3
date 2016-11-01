// Copyright 2001 Finn Bock
package org.python.core;

import org.python.modules.zipimport.ZipImportModule;

import java.io.File;
import java.lang.reflect.Method;

/**
 * The builtin Exceptions module. The entire module should be imported from
 * python. None of the methods defined here should be called from java.
 */
@Untraversable
public class Exceptions {

    /**
     * <i>Internal use only. Do not call this method explicit.</i>
     */
    public static void init(PyObject dict) {
        dict.__setitem__("BaseException", PyBaseException.TYPE);

        buildClass(dict, "KeyboardInterrupt", "BaseException", "Program interrupted by user.");

        buildClass(dict, "SystemExit", "BaseException", SystemExit(),
                "Request to exit from the interpreter.");

        buildClass(dict, "Exception", "BaseException",
                "Common base class for all non-exit Exceptions.");

        buildClass(dict, "JavaException", "BaseException",
                "Base class for all standard Java Exceptions");

        buildClass(dict, "StandardError", "Exception",
                "Base class for all standard Python Exceptions that do not represent\n"
                        + "interpreter exiting.");

        buildClass(dict, "SyntaxError", "StandardError", SyntaxError(), "Invalid syntax.");

        buildClass(dict, "IndentationError", "SyntaxError", "Improper indentation.");

        buildClass(dict, "TabError", "IndentationError", "Improper mixture of spaces and tabs.");

        buildClass(dict, "OSError", "Exception", OSError(),
                "Base class for I/O related errors.");

        buildClass(dict, "FileExistsError", "OSError", "File already exists.");
        buildClass(dict, "FileNotFoundError", "OSError", "File not found.");
        buildClass(dict, "IsADirectoryError", "OSError", "Operation doesn't work on directories");
        buildClass(dict, "NotADirectoryError", "OSError", "Operation only works on directories");
        buildClass(dict, "PermissionError", "OSError", "Not enough permissions.");
        buildClass(dict, "BlockingIOError", "OSError", "I/O operation would block.");
        buildClass(dict, "InterruptedError", "OSError", "Interrupted by signal.");
        buildClass(dict, "ChildProcessError", "OSError", "Child process error.");
        buildClass(dict, "ProcessLookupError", "OSError", "Process not found.");
        buildClass(dict, "TimeoutError", "OSError", "Timeout expired.");

        buildClass(dict, "ConnectionError", "OSError", "Connection Error");
        buildClass(dict, "BrokenPipeError", "ConnectionError", "Broken pipe.");
        buildClass(dict, "ConnectionResetError", "ConnectionError", "Connection reset");
        buildClass(dict, "ConnectionAbortedError", "ConnectionError", "Connection aborted.");
        buildClass(dict, "ConnectionRefusedError", "ConnectionError", "Connection refused.");

        buildClass(dict, "RuntimeError", "StandardError", "Unspecified run-time error.");

        buildClass(dict, "RecursionError", "RuntimeError", "Recursion limit exceeded.");

        buildClass(dict, "NotImplementedError", "RuntimeError",
                "Method or function hasn't been implemented yet.");

        buildClass(dict, "SystemError", "StandardError",
                "Internal error in the Python interpreter.\n\n"
                        + "Please report this to the Python maintainer, "
                        + "along with the traceback,\n"
                        + "the Python version, and the hardware/OS "
                        + "platform and version.");

        buildClass(dict, "ReferenceError", "StandardError",
                "Weak ref proxy used after referent went away.");

        buildClass(dict, "EOFError", "StandardError", "Read beyond end of file.");

        buildClass(dict, "ImportError", "StandardError", ImportError(),
                "Import can't find module, or can't find name in module.");

        buildClass(dict, "TypeError", "StandardError", "Inappropriate argument type.");

        buildClass(dict, "ValueError", "StandardError",
                "Inappropriate argument value (of correct type).");

        buildClass(dict, "UnicodeError", "ValueError", UnicodeError(), "Unicode related error.");

        buildClass(dict, "UnicodeEncodeError", "UnicodeError", UnicodeEncodeError(),
                "Unicode encoding error.");

        buildClass(dict, "UnicodeDecodeError", "UnicodeError", UnicodeDecodeError(),
                "Unicode decoding error.");

        buildClass(dict, "UnicodeTranslateError", "UnicodeError", UnicodeTranslateError(),
                "Unicode translation error.");

        buildClass(dict, "AssertionError", "StandardError", "Assertion failed.");

        buildClass(dict, "ArithmeticError", "StandardError", "Base class for arithmetic errors.");

        buildClass(dict, "OverflowError", "ArithmeticError",
                "Result too large to be represented.");

        buildClass(dict, "FloatingPointError", "ArithmeticError",
                "Floating point operation failed.");

        buildClass(dict, "ZeroDivisionError", "ArithmeticError",
                "Second argument to a division or modulo operation "
                        + "was zero.");

        buildClass(dict, "LookupError", "StandardError", "Base class for lookup errors.");

        buildClass(dict, "IndexError", "LookupError", "Sequence index out of range.");

        buildClass(dict, "KeyError", "LookupError", KeyError(), "Mapping key not found.");

        buildClass(dict, "AttributeError", "StandardError", "Attribute not found.");

        buildClass(dict, "NameError", "StandardError", "Name not found globally.");

        buildClass(dict, "UnboundLocalError", "NameError",
                "Local name referenced but not bound to a value.");

        buildClass(dict, "MemoryError", "StandardError", "Out of memory.");

        buildClass(dict, "BufferError", "StandardError", "Buffer error.");

        buildClass(dict, "StopIteration", "Exception", StopIteration(),
                "Signal the end from iterator.__next__().");

        buildClass(dict, "StopAsyncIteration", "Exception", StopIteration(),
                "Signal the end from iterator.__anext__().");

        buildClass(dict, "GeneratorExit", "BaseException", "Request that a generator exit.");

        buildClass(dict, "Warning", "Exception", "Base class for warning categories.");

        buildClass(dict, "UserWarning", "Warning",
                "Base class for warnings generated by user code.");

        buildClass(dict, "DeprecationWarning", "Warning",
                "Base class for warnings about deprecated features.");

        buildClass(dict, "PendingDeprecationWarning", "Warning",
                "Base class for warnings about features which will be deprecated\n"
                        + "in the future.");

        buildClass(dict, "SyntaxWarning", "Warning",
                "Base class for warnings about dubious syntax.");

        buildClass(dict, "ResourceWarning", "Warning",
                "Base class for warnings about resource usage.");

        buildClass(dict, "RuntimeWarning", "Warning",
                "Base class for warnings about dubious runtime behavior.");

        buildClass(dict, "FutureWarning", "Warning",
                "Base class for warnings about constructs that will change semantically\n"
                        + "in the future.");

        buildClass(dict, "ImportWarning", "Warning",
                "Base class for warnings about probable mistakes in module imports");

        buildClass(dict, "UnicodeWarning", "Warning",
                "Base class for warnings about Unicode related problems, mostly\n"
                        + "related to conversion problems.");

        buildClass(dict, "BytesWarning", "Warning",
                "Base class for warnings about bytes and buffer related problems, mostly\n"
                        + "related to conversion from str or comparing to str.");

        // Initialize ZipImportError here, where it's safe to; it's
        // needed immediately
        ZipImportModule.initClassExceptions(dict);
    }

    public static PyObject ImportError() {
        PyObject __dict__ = new PyStringMap();
        defineSlots(__dict__, "args", "msg", "name", "path");
        __dict__.__setitem__("__init__", bindStaticJavaMethod("__init__", "ImportError__init__"));
        __dict__.__setitem__("__str__", bindStaticJavaMethod("__str__", "ImportError__str__"));
        return __dict__;
    }

    public static void ImportError__init__(PyObject self, PyObject[] args, String[] kwargs) {
        PyBaseException.TYPE.invoke("__init__", self, args, Py.NoKeywords);
        ArgParser ap = new ArgParser("__init__", args, kwargs,
                new String[] {"msg", "name", "path"});
        initSlots(self);
        self.__setattr__("msg", ap.getPyObject(0, Py.None));
        self.__setattr__("name", ap.getPyObject(1, Py.None));
        self.__setattr__("path", ap.getPyObject(2, Py.None));
    }

    public static PyUnicode ImportError__str__(PyObject self, PyObject[] arg, String[] kwargs) {
        PyObject msg = self.__getattr__("msg");
        PyUnicode str = msg.__str__();

        PyObject name = self.__findattr__("name");
        PyObject path = self.__findattr__("path");
        boolean haveName = name instanceof PyUnicode;
        boolean havePath = path instanceof PyLong;
        if (!haveName && !havePath) {
            return str;
        }

        String result;
        if (haveName && havePath) {
            result = String.format("%s (%s, path %s)", str, basename(name.toString()),
                    path.toString());
        } else if (haveName) {
            result = String.format("%s (%s)", str, basename(name.toString()));
        } else {
            result = String.format("%s (path %s)", str, path.toString());
        }

        return Py.newUnicode(result);
    }

    public static PyObject StopIteration(){
        PyObject __dict__ = new PyStringMap();
        defineSlots(__dict__, "value");
        __dict__.__setitem__("__init__", bindStaticJavaMethod("__init__", "StopIteration__init__"));
        __dict__.__setitem__("__str__", bindStaticJavaMethod("__str__", "StopIteration__str__"));
//        __dict__.__setitem__("__repr__", bindStaticJavaMethod("__repr__", "StopIteration__repr__"));
        return __dict__;
    }

    public static void StopIteration__init__(PyObject self, PyObject[] args, String[] kwargs) {
        PyBaseException.TYPE.invoke("__init__", self, args, kwargs);
        initSlots(self);
        if (args.length > 0) {
            self.__setattr__("value", args[0]);
        } else {
            self.__setattr__("value", Py.None);
        }
    }

//    public static PyUnicode StopIteration__repr__(PyObject self, PyObject[] args, String[] kwargs) {
//        PyObject value = ((PyBaseException) self).args.__finditem__(0);
//        if (value != null && value != Py.None) {
//            return new PyUnicode(String.format("StopIteration(%s)", value.__str__()));
//        }
//        return new PyUnicode("StopIteration()");
//    }

    public static PyUnicode StopIteration__str__(PyObject self, PyObject[] args, String[] kwargs) {
        PyObject value = ((PyBaseException) self).args.__finditem__(0);
        if (value != null) {
            return value.__str__();
        }
        return Py.EmptyUnicode;
    }

    public static PyObject SyntaxError() {
        PyObject __dict__ = new PyStringMap();
        defineSlots(__dict__, "msg", "filename", "lineno", "offset", "text",
                    "print_file_and_line");
        __dict__.__setitem__("__init__", bindStaticJavaMethod("__init__", "SyntaxError__init__"));
        __dict__.__setitem__("__str__", bindStaticJavaMethod("__str__", "SyntaxError__str__"));
        return __dict__;
    }

    public static void SyntaxError__init__(PyObject self, PyObject[] args, String[] kwargs) {
        PyBaseException.TYPE.invoke("__init__", self, args, kwargs);
        initSlots(self);

        if (args.length >= 1) {
            self.__setattr__("msg", args[0]);
        }
        if (args.length == 2) {
            PyObject[] info = Py.make_array(args[1]);
            if (info.length != 4) {
                throw Py.IndexError("tuple index out of range");
            }

            self.__setattr__("filename", info[0]);
            self.__setattr__("lineno", info[1]);
            self.__setattr__("offset", info[2]);
            self.__setattr__("text", info[3]);
        }
    }

    public static PyUnicode SyntaxError__str__(PyObject self, PyObject[] arg, String[] kwargs) {
        PyObject msg = self.__getattr__("msg");
        PyUnicode str = msg.__str__();

        PyObject filename = self.__findattr__("filename");
        PyObject lineno = self.__findattr__("lineno");
        boolean haveFilename = filename instanceof PyUnicode;
        boolean haveLieno = lineno instanceof PyLong;
        if (!haveFilename && !haveLieno) {
            return str;
        }

        String result;
        if (haveFilename && haveLieno) {
            result = String.format("%s (%s, line %d)", str, basename(filename.toString()),
                                   lineno.asInt());
        } else if (haveFilename) {
            result = String.format("%s (%s)", str, basename(filename.toString()));
        } else {
            result = String.format("%s (line %d)", str, lineno.asInt());
        }

        return Py.newUnicode(result);
    }

    public static PyObject OSError() {
        PyObject dict = new PyStringMap();
        defineSlots(dict, "errno", "strerror", "filename", "filename2");
        dict.__setitem__("__init__", bindStaticJavaMethod("__init__", "OSError__init__"));
        dict.__setitem__("__str__", bindStaticJavaMethod("__str__", "OSError__str__"));
        dict.__setitem__("__reduce__", bindStaticJavaMethod("__reduce__", "OSError__reduce__"));
        return dict;
    }

    public static void OSError__init__(PyObject self, PyObject[] args, String[] kwargs) {
        PyBaseException.TYPE.invoke("__init__", self, args, Py.NoKeywords);
        initSlots(self);

        if (args.length <= 1) {
            return;
        }
        PyObject errno = args[0];
        PyObject strerror = args[1];
        self.__setattr__("errno", errno);
        self.__setattr__("strerror", strerror);
        if (args.length > 2) {
            PyObject arg = args[2];
            if (Py.isInstance(arg, PyLong.TYPE)) {
                self.__setattr__("characters_written", arg);
            } else {
                self.__setattr__("filename", args[2]);
            }
        }
        // ignore args[3] winerror
        if (args.length > 4) {
            self.__setattr__("filename2", args[4]);
        }
        self.__setattr__("args", new PyTuple(errno, strerror));
    }

    public static PyUnicode OSError__str__(PyObject self, PyObject[] args,
                                                   String[] kwargs) {
        PyObject errno = self.__findattr__("errno");
        PyObject strerror = self.__findattr__("strerror");
        PyObject filename = self.__findattr__("filename");
        String result;
        if (filename.__bool__()) {
            result = String.format("[Errno %s] %s: %s", errno, strerror, filename.__repr__());
        } else if (errno.__bool__() && strerror.__bool__()) {
            result = String.format("[Errno %s] %s", errno, strerror);
        } else {
            return (PyUnicode) PyBaseException.TYPE.invoke("__str__", self, args, kwargs);
        }
        return Py.newUnicode(result);
    }

    public static PyObject OSError__reduce__(PyObject self, PyObject[] args,
                                                      String[] kwargs) {
        PyBaseException selfBase = (PyBaseException)self;
        PyObject reduceArgs = selfBase.args;
        PyObject filename = self.__findattr__("filename");
        PyObject filename2 = self.__findattr__("filename2");

        // self->args is only the first two real arguments if there was a file name given
        // to OSError
        if (selfBase.args.__len__() == 2 && filename != null) {
            reduceArgs = new PyTuple(selfBase.args.__finditem__(0),
                                     selfBase.args.__finditem__(1),
                                     filename, Py.None, filename2);
        }

        if (selfBase.__dict__ != null) {
            return new PyTuple(selfBase.getType(), reduceArgs, selfBase.__dict__);
        } else {
            return new PyTuple(selfBase.getType(), reduceArgs);
        }
    }

    public static PyObject SystemExit() {
        PyObject dict = new PyStringMap();
        defineSlots(dict, "code");
        dict.__setitem__("__init__", bindStaticJavaMethod("__init__", "SystemExit__init__"));
        return dict;
    }

    public static void SystemExit__init__(PyObject self, PyObject[] args, String[] kwargs) {
        PyBaseException.TYPE.invoke("__init__", self, args, kwargs);
        initSlots(self);

        if (args.length == 1) {
            self.__setattr__("code", args[0]);
        } else if (args.length > 1) {
            self.__setattr__("code", new PyTuple(args));
        }
    }

    public static PyObject KeyError() {
        PyObject dict = new PyStringMap();
//        dict.__setitem__("__str__", bindStaticJavaMethod("__str__", "KeyError__str__"));
        return dict;
    }

    public static PyObject KeyError__str__(PyObject self, PyObject[] args, String[] kwargs) {
        PyBaseException selfBase = (PyBaseException)self;
        // If args is a tuple of exactly one item, apply repr to args[0].
        // This is done so that e.g. the exception raised by {}[''] prints
        // KeyError: ''
        // rather than the confusing
        // KeyError
        // alone.  The downside is that if KeyError is raised with an explanatory
        // string, that string will be displayed in quotes.  Too bad.
//        if (selfBase.args.__len__() == 1) {
        return selfBase.args.__getitem__(0).__repr__();
//        }
//        return PyBaseException.TYPE.invoke("__str__", self, args, kwargs);
    }

    public static PyObject UnicodeError() {
        PyObject dict = new PyStringMap();
        defineSlots(dict, "encoding", "object", "start", "end", "reason");
        // NOTE: UnicodeError doesn't actually use its own constructor
        return dict;
    }

    public static void UnicodeError__init__(PyObject self, PyObject[] args, String[] kwargs,
                                            PyType objectType) {
        ArgParser ap = new ArgParser("__init__", args, kwargs,
                                     new String[] {"encoding", "object", "start", "end",
                                                   "reason" },
                                     5);
        self.__setattr__("encoding", ap.getPyObjectByType(0, PyUnicode.TYPE));
        self.__setattr__("object", ap.getPyObjectByType(1, objectType));
        self.__setattr__("start", ap.getPyObjectByType(2, PyLong.TYPE));
        self.__setattr__("end", ap.getPyObjectByType(3, PyLong.TYPE));
        self.__setattr__("reason", ap.getPyObjectByType(4, PyUnicode.TYPE));
    }

    public static PyObject UnicodeDecodeError() {
        PyObject dict = new PyStringMap();
        dict.__setitem__("__init__", bindStaticJavaMethod("__init__",
                                                          "UnicodeDecodeError__init__"));
        dict.__setitem__("__str__", bindStaticJavaMethod("__str__", "UnicodeDecodeError__str__"));
        return dict;
    }

    public static void UnicodeDecodeError__init__(PyObject self, PyObject[] args,
                                                  String[] kwargs) {
        PyBaseException.TYPE.invoke("__init__", self, args, kwargs);
        UnicodeError__init__(self, args, kwargs, PyObject.TYPE);
        PyObject object = self.__getattr__("object");
        if (!Py.isInstance(object, PyBytes.TYPE)) {
            if (!(object instanceof BufferProtocol)) {
                throw Py.TypeError(String.format("argument 2 must be a buffer, not %s",
                        object.getType().fastGetName()));
            }
            PyBuffer buf = ((BufferProtocol) object).getBuffer(PyBUF.FULL_RO);
            char[] chars = new char[buf.getLen()];
            for (int i = 0; i < chars.length; i++) {
                chars[i] = (char) buf.intAt(i);
            }
            self.__setattr__("object", new PyBytes(new String(chars)));
        }
    }

    public static PyUnicode UnicodeDecodeError__str__(PyObject self, PyObject[] args,
                                                     String[] kwargs) {
        int start = self.__getattr__("start").asInt();
        int end = self.__getattr__("end").asInt();
        // Get reason and encoding as strings, which they might not be if they've been
        // modified after we were contructed
        PyObject reason = self.__getattr__("reason").__str__();
        PyObject encoding = self.__getattr__("encoding").__str__();
        PyObject object = getString(self.__getattr__("object"), "object");

        String result;
        if (start < object.__len__() && end == (start + 1)) {
            int badByte = (object.toString().charAt(start)) & 0xff;
            result = String.format("'%.400s' codec can't decode byte 0x%x in position %d: %.400s",
                                   encoding, badByte, start, reason);
        } else {
            result = String.format("'%.400s' codec can't decode bytes in position %d-%d: %.400s",
                                   encoding, start, end - 1, reason);
        }
        return Py.newUnicode(result);
    }

    public static PyObject UnicodeEncodeError() {
        PyObject dict = new PyStringMap();
        dict.__setitem__("__init__", bindStaticJavaMethod("__init__",
                                                          "UnicodeEncodeError__init__"));
        dict.__setitem__("__str__", bindStaticJavaMethod("__str__", "UnicodeEncodeError__str__"));
        return dict;
    }

    public static void UnicodeEncodeError__init__(PyObject self, PyObject[] args, String[] kwargs) {
        PyBaseException.TYPE.invoke("__init__", self, args, kwargs);
        UnicodeError__init__(self, args, kwargs, PyUnicode.TYPE);
    }

    public static PyUnicode UnicodeEncodeError__str__(PyObject self, PyObject[] args,
                                                     String[] kwargs) {
        int start = self.__getattr__("start").asInt();
        int end = self.__getattr__("end").asInt();
        // Get reason and encoding as strings, which they might not be if they've been
        // modified after we were contructed
        PyObject reason = self.__getattr__("reason").__str__();
        PyObject encoding = self.__getattr__("encoding").__str__();
        PyObject object = getUnicode(self.__getattr__("object"), "object");

        String result;
        if (start < object.__len__() && end == (start + 1)) {
            int badchar = object.toString().codePointAt(start);
            String badcharStr;
            if (badchar <= 0xff) {
                badcharStr = String.format("x%02x", badchar);
            } else if (badchar <= 0xffff) {
                badcharStr = String.format("u%04x", badchar);
            } else {
                badcharStr = String.format("U%08x", badchar);
            }
            result = String.format("'%.400s' codec can't encode character '\\%s' in position %d: "
                                   + "%.400s", encoding, badcharStr, start, reason);
        } else {
            result = String.format("'%.400s' codec can't encode characters in position %d-%d: "
                                   + "%.400s", encoding, start, end - 1, reason);
        }
        return Py.newUnicode(result);
    }

    public static PyObject UnicodeTranslateError() {
        PyObject dict = new PyStringMap();
        dict.__setitem__("__init__", bindStaticJavaMethod("__init__",
                                                          "UnicodeTranslateError__init__"));
        dict.__setitem__("__str__", bindStaticJavaMethod("__str__",
                                                         "UnicodeTranslateError__str__"));
        return dict;
    }

    public static void UnicodeTranslateError__init__(PyObject self, PyObject[] args,
                                                     String[] kwargs) {
        PyBaseException.TYPE.invoke("__init__", self, args, kwargs);
        ArgParser ap = new ArgParser("__init__", args, kwargs,
                                     new String[] {"object", "start", "end", "reason"},
                                     4);
        self.__setattr__("object", ap.getPyObjectByType(0, PyUnicode.TYPE));
        self.__setattr__("start", ap.getPyObjectByType(1, PyLong.TYPE));
        self.__setattr__("end", ap.getPyObjectByType(2, PyLong.TYPE));
        self.__setattr__("reason", ap.getPyObjectByType(3, PyUnicode.TYPE));
    }

    public static PyUnicode UnicodeTranslateError__str__(PyObject self, PyObject[] args,
                                                        String[] kwargs) {
        int start = self.__getattr__("start").asInt();
        int end = self.__getattr__("end").asInt();
        // Get reason as a string, which it might not be if it's been modified after we
        // were contructed
        PyObject reason = self.__getattr__("reason").__str__();
        PyObject object = getUnicode(self.__getattr__("object"), "object");

        String result;
        if (start < object.__len__() && end == (start + 1)) {
            int badchar = object.toString().codePointAt(start);
            String badCharStr;
            if (badchar <= 0xff) {
                badCharStr = String.format("x%02x", badchar);
            } else if (badchar <= 0xffff) {
                badCharStr = String.format("u%04x", badchar);
            } else {
                badCharStr = String.format("U%08x", badchar);
            }
            result = String.format("can't translate character '\\%s' in position %d: %.400s",
                                   badCharStr, start, reason);
        } else {
            result = String.format("can't translate characters in position %d-%d: %.400s",
                                   start, end - 1, reason);
        }
        return Py.newUnicode(result);
    }

    /**
     * Determine the start position for UnicodeErrors.
     *
     * @param self a UnicodeError value
     * @param unicode whether the UnicodeError object should be
     * unicode
     * @return an the start position
     */
    public static int getStart(PyObject self, boolean unicode) {
        int start = self.__getattr__("start").asInt();
        PyObject object;
        if (unicode) {
            object = getUnicode(self.__getattr__("object"), "object");
        } else {
            object = getString(self.__getattr__("object"), "object");
        }
        if (start < 0) {
            start = 0;
        }
        if (start >= object.__len__()) {
            start = object.__len__() - 1;
        }
        return start;
    }

    /**
     * Determine the end position for UnicodeErrors.
     *
     * @param self a UnicodeError value
     * @param unicode whether the UnicodeError object should be
     * unicode
     * @return an the end position
     */
    public static int getEnd(PyObject self, boolean unicode) {
        int end = self.__getattr__("end").asInt();
        PyObject object;
        if (unicode) {
            object = getUnicode(self.__getattr__("object"), "object");
        } else {
            object = getString(self.__getattr__("object"), "object");
        }
        if (end < 1) {
            end = 1;
        }
        if (end > object.__len__()) {
            end = object.__len__();
        }
        return end;
    }

    /**
     * Ensure a PyBytes value for UnicodeErrors
     *
     * @param attr a PyObject
     * @param name of the attribute
     * @return an PyBytes
     */
    public static PyBytes getString(PyObject attr, String name) {
        if (!Py.isInstance(attr, PyBytes.TYPE)) {
            throw Py.TypeError(String.format("%.200s attribute must be bytes", name));
        }
        return (PyBytes)attr;
    }

    /**
     * Ensure a PyUnicode value for UnicodeErrors
     *
     * @param attr a PyObject
     * @param name of the attribute
     * @return an PyUnicode
     */
    public static PyUnicode getUnicode(PyObject attr, String name) {
        if (!(attr instanceof PyUnicode)) {
            throw Py.TypeError(String.format("%.200s attribute must be str", name));
        }
        return (PyUnicode)attr;
    }

    /**
     * Return the basename of a path string.
     *
     * @param name a path String
     * @return the basename'd result String
     */
    private static String basename(String name) {
        int lastSep = name.lastIndexOf(File.separatorChar);
        if (lastSep > -1) {
            return name.substring(lastSep + 1, name.length());
        }
        return name;
    }

    /**
     * Define __slots__ in dict with the specified slot names
     *
     * @param dict a PyObject dict
     * @param slotNames slot String names
     */
    private static void defineSlots(PyObject dict, String... slotNames) {
        PyObject[] slots = new PyObject[slotNames.length];
        for (int i = 0; i < slotNames.length; i++) {
            slots[i] = Py.newString(slotNames[i]);
        }
        dict.__setitem__("__slots__", new PyTuple(slots));
    }

    /**
     * Initialize all __slots__ arguments in the specified dict to
     * None.
     *
     * @param self a PyObject dict
     */
    private static void initSlots(PyObject self) {
        for (PyObject name : self.__findattr__("__slots__").asIterable()) {
            if (name instanceof PyUnicode) {
                self.__setattr__((PyUnicode)name, Py.None);
            }
        }
    }

    private static PyObject buildClass(PyObject dict, String classname, String superclass,
                                       String doc) {
        return buildClass(dict, classname, superclass, new PyStringMap(), doc);
    }

    private static PyObject buildClass(PyObject dict, String classname, String superclass,
                                       PyObject classDict, String doc) {
        classDict.__setitem__("__doc__", Py.newUnicode(doc));
        PyType type = (PyType)Py.makeClass(classname,
                                           dict.__finditem__(superclass), classDict);
        type.builtin = true;
        dict.__setitem__(classname, type);
        return type;
    }

    public static PyObject bindStaticJavaMethod(String name, String methodName) {
        return bindStaticJavaMethod(name, Exceptions.class, methodName);
    }

    public static PyObject bindStaticJavaMethod(String name, Class<?> cls, String methodName) {
        Method javaMethod;
        try {
            javaMethod = cls.getMethod(methodName,
                                       new Class<?>[] {PyObject.class, PyObject[].class,
                                                       String[].class});
        } catch (Exception e) {
            throw Py.JavaError(e);
        }
        return new BoundStaticJavaMethod(name, javaMethod);
    }

    @Untraversable
    static class BoundStaticJavaMethod extends PyBuiltinMethod {

        /** The Java Method to be bound. Its signature must be:
         * (PyObject, PyObject[], String[])PyObject. */
        private Method javaMethod;

        public BoundStaticJavaMethod(String name, Method javaMethod) {
            super(name);
            this.javaMethod = javaMethod;
        }

        protected BoundStaticJavaMethod(PyType type, PyObject self, Info info, Method javaMethod) {
            super(type, self, info);
            this.javaMethod = javaMethod;
        }

        @Override
        public PyBuiltinCallable bind(PyObject self) {
            return new BoundStaticJavaMethod(getType(), self, info, javaMethod);
        }

        @Override
        public PyObject __get__(PyObject obj, PyObject type) {
            if (obj != null) {
                return bind(obj);
            }
            return makeDescriptor((PyType)type);
        }

        @Override
        public PyObject __call__(PyObject[] args, String kwargs[]) {
            try {
                return Py.java2py(javaMethod.invoke(null, self, args, kwargs));
            } catch (Throwable t) {
                throw Py.JavaError(t);
            }
        }
    }
}
