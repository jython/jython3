/* Copyright (c)2012 Jython Developers */
package org.python.modules._io;

import org.python.core.ArgParser;
import org.python.core.BuiltinDocs;
import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.core.imp;
import org.python.core.io.IOBase;
import org.python.expose.ExposedConst;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;
import org.python.expose.ModuleInit;

/**
 * The Python _io module implemented in Java.
 */
@ExposedModule(doc = BuiltinDocs.io__io_doc)
public class _io {

    /**
     * This method is called when the module is loaded, to populate the namespace (dictionary) of
     * the module. The dictionary has been initialised at this point reflectively from the methods
     * of this class and this method nulls those entries that ought not to be exposed.
     *
     * @param dict namespace of the module
     */
    @ModuleInit
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("_IOBase", PyIOBase.TYPE);
        dict.__setitem__("_RawIOBase", PyRawIOBase.TYPE);
        dict.__setitem__("FileIO", PyFileIO.TYPE);
        dict.__setitem__("BytesIO", PyBytesIO.TYPE);
        dict.__setitem__("StringIO", PyStringIO.TYPE);

        // Define UnsupportedOperation exception by constructing the type

        PyObject exceptions = imp.load("builtins");
        PyObject ValueError = exceptions.__getattr__("ValueError");
        PyObject IOError = exceptions.__getattr__("IOError");
        // Equivalent to class UnsupportedOperation(ValueError, IOError) : pass
        // UnsupportedOperation = makeException(dict, "UnsupportedOperation", ValueError, IOError);
        // XXX Work-around: slots not properly initialised unless IOError comes first
        UnsupportedOperation = makeException(dict, "UnsupportedOperation", IOError, ValueError);
    }

    /** A Python class for the <code>UnsupportedOperation</code> exception. */
    public static PyType UnsupportedOperation;

    /**
     * A function that returns a {@link PyException}, which is a Java exception suitable for
     * throwing, and that will be raised as an <code>UnsupportedOperation</code> Python exception.
     *
     * @param message text message parameter to the Python exception
     * @return nascent <code>UnsupportedOperation</code> Python exception
     */
    public static PyException UnsupportedOperation(String message) {
        return new PyException(UnsupportedOperation, message);
    }

    /**
     * Convenience method for constructing a type object of a Python exception, named as given, and
     * added to the namespace of the "_io" module.
     *
     * @param dict module dictionary
     * @param excname name of the exception
     * @param bases one or more bases (superclasses)
     * @return the constructed exception type
     */
    private static PyType makeException(PyObject dict, String excname, PyObject... bases) {
        PyStringMap classDict = new PyStringMap();
        classDict.__setitem__("__module__", Py.newString("_io"));
        PyType type = (PyType)Py.makeClass(excname, bases, classDict);
        dict.__setitem__(excname, type);
        return type;
    }

    /** Default buffer size obtained from {@link IOBase#DEFAULT_BUFFER_SIZE}. */
    @ExposedConst
    public static final int DEFAULT_BUFFER_SIZE = IOBase.DEFAULT_BUFFER_SIZE;

    /**
     * Open file and return a stream. Raise IOError upon failure. This is a port to Java of the
     * CPython _io.open (Modules/_io/_iomodule.c) following the same logic, but expressed with the
     * benefits of Java syntax.
     *
     * @param args array of arguments from Python call via Jython framework
     * @param kwds array of keywords from Python call via Jython framework
     * @return the stream object
     */
    @ExposedFunction(doc = BuiltinDocs.io_open_doc)
    public static PyObject open(PyObject[] args, String[] kwds) {

        // Get the arguments to variables
        ArgParser ap = new ArgParser("open", args, kwds, openKwds, 1);
        PyObject file = ap.getPyObject(0);
        String m = ap.getString(1, "r");
        int buffering = ap.getInt(2, -1);
        final String encoding = ap.getString(3, null);
        final String errors = ap.getString(4, null);
        final String newline = ap.getString(5, null);
        boolean closefd = Py.py2boolean(ap.getPyObject(6, Py.True));

        // Decode the mode string
        OpenMode mode = new OpenMode(m) {

            @Override
            public void validate() {
                super.validate();
                validate(encoding, errors, newline);
            }
        };

        mode.checkValid();

        /*
         * Create the Raw file stream. Let the constructor deal with the variants and argument
         * checking.
         */
        PyFileIO raw = new PyFileIO(file, mode, closefd);

        /*
         * From the Python documentation for io.open() buffering = 0 to switch buffering off (only
         * allowed in binary mode), 1 to select line buffering (only usable in text mode), and an
         * integer > 1 to indicate the size of a fixed-size buffer.
         *
         * When no buffering argument is given, the default buffering policy works as follows:
         * Binary files are buffered in fixed-size chunks; "Interactive" text files (files for which
         * isatty() returns True) use line buffering. Other text files use the policy described
         * above for binary files.
         *
         * In Java, it seems a stream never is *known* to be interactive, but we ask anyway, and
         * maybe one day we shall know.
         */
        boolean line_buffering = false;

        if (buffering == 0) {
            if (!mode.binary) {
                throw Py.ValueError("can't have unbuffered text I/O");
            }
            return raw;

        } else if (buffering == 1) {
            // The stream is to be read line-by-line.
            line_buffering = true;
            // Force default size for actual buffer
            buffering = -1;

        } else if (buffering < 0 && raw.isatty()) {
            // No buffering argument given but stream is inteeractive.
            line_buffering = true;
        }

        if (buffering < 0) {
            /*
             * We are still being asked for the default buffer size. CPython establishes the default
             * buffer size using fstat(fd), but Java appears to give no clue. A useful study of
             * buffer sizes in NIO is http://www.evanjones.ca/software/java-bytebuffers.html . This
             * leads us to the fixed choice of _DEFAULT_BUFFER_SIZE (=8KB).
             */
            buffering = DEFAULT_BUFFER_SIZE;
        }

        /*
         * We now know just what particular class of file we are opening, and therefore what stack
         * (buffering and text encoding) we should build.
         */
        if (buffering == 0) {
            // Not buffering, return the raw file object
            return raw;

        } else {
            // We are buffering, so wrap raw into a buffered file
            PyObject bufferType = null;
            PyObject io = imp.load("io");

            if (mode.updating) {
                bufferType = io.__getattr__("BufferedRandom");
            } else if (mode.writing || mode.appending || mode.creating) {
                bufferType = io.__getattr__("BufferedWriter");
            } else {                        // = reading
                bufferType = io.__getattr__("BufferedReader");
            }

            PyLong pyBuffering = new PyLong(buffering);
            PyObject buffer = bufferType.__call__(raw, pyBuffering);

            if (mode.binary) {
                // If binary, return the just the buffered file
                return buffer;

            } else {
                // We are opening in text mode, so wrap buffered file in a TextIOWrapper.
                PyObject textType = io.__getattr__("TextIOWrapper");
                PyObject[] textArgs =
                        {buffer, ap.getPyObject(3, Py.None), ap.getPyObject(4, Py.None),
                                ap.getPyObject(5, Py.None), Py.newBoolean(line_buffering)};
                PyObject wrapper = textType.__call__(textArgs);
                wrapper.__setattr__("mode", new PyUnicode(m));
                return wrapper;
            }
        }
    }

    private static final String[] openKwds = {"file", "mode", "buffering", "encoding", "errors",
            "newline", "closefd"};
}
