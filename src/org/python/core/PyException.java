// Copyright (c) Corporation for National Research Initiatives
package org.python.core;
import java.io.*;

/**
 * A wrapper for all python exception. Note that the well-known python exceptions are <b>not</b>
 * subclasses of PyException. Instead the python exception class is stored in the <code>type</code>
 * field and value or class instance is stored in the <code>value</code> field.
 */
public class PyException extends RuntimeException implements Traverseproc
{

    /**
     * The python exception class (for class exception) or identifier (for string exception).
     */
    public PyObject type;

    /**
     * The exception instance (for class exception) or exception value (for string exception).
     */
    public PyObject value = Py.None;

    /**
     * The cause of the exception for implicitly chained Exceptions
     */
    public PyBaseException context;

    /**
     * The cause of the exception for explicitly chained Exceptions
     */
    public PyObject cause;

    /** The exception traceback object. */
    public PyTraceback traceback;

    /**
     * Whether the exception was re-raised, such as when a traceback is specified to
     * 'raise', or via a 'finally' block.
     */
    private boolean isReRaise = false;

    private boolean normalized = false;

    public PyException() {
        this(Py.None, Py.None);
    }

    public PyException(PyObject type) {
        this(type, Py.None);
    }

    public PyException(PyObject type, PyObject value) {
        this(type, value, null);
    }

    public PyException(PyObject type, PyObject value, PyObject cause, PyTraceback traceback) {
        this(type, value, cause);
        if (traceback != null) {
            this.traceback = traceback;
            isReRaise = true;
        } else {
            PyFrame frame = Py.getFrame();
            if (frame != null && frame.tracefunc != null) {
                frame.tracefunc = frame.tracefunc.traceException(frame, this);
            }
        }
    }

    public PyException(PyObject type, PyObject value, PyObject cause) {
        this.type = type;
        if (cause != null) {
            this.cause = cause;
            isReRaise = true;
        }
        if (value != null && !isExceptionInstance(value)) {
            PyException pye = Py.getThreadState().exceptions.pollFirst();
            if (pye != null && pye.value instanceof PyBaseException) {
                context = (PyBaseException) pye.value;
            }
        }

        if (value == null) {
            this.value = Py.None;
        } else {
            this.value = value;
        }
        if (value instanceof PyBaseException) {
            ((PyBaseException) value).wrapper = this;
        }
    }

    public PyException(PyObject type, String value) {
        this(type, Py.newUnicode(value));
    }

    private boolean printingStackTrace = false;
    @Override
    public void printStackTrace() {
        Py.printException(this);
    }

    @Override
    public Throwable fillInStackTrace() {
        return Options.includeJavaStackInExceptions ? super.fillInStackTrace() : this;
    }

    @Override
    public synchronized void printStackTrace(PrintStream s) {
        if (printingStackTrace) {
            super.printStackTrace(s);
        } else {
            try {
                printingStackTrace = true;
                Py.displayException(type, value, traceback, new PyFile(s));
            } finally {
                printingStackTrace = false;
            }
        }
    }

    public synchronized void super__printStackTrace(PrintWriter w) {
        try {
            printingStackTrace = true;
            super.printStackTrace(w);
        } finally {
            printingStackTrace = false;
        }
    }

    @Override
    public synchronized String toString() {
        return Py.exceptionToString(type, value, traceback);
    }

    /**
     * Instantiates the exception value if it is not already an
     * instance.
     *
     */
    public PyException normalize() {
//        if (normalized) {
//            return this;
//        }
        PyObject inClass = null;
        if (isExceptionInstance(value)) {
            inClass = value.fastGetClass();
        }

//        if (type instanceof PyJavaType) {
//            type = Py.JavaException;
//        }
        if (inClass == null || !Py.isSubClass(inClass, type)) {
            PyObject[] args;

            // Don't decouple a tuple into args when it's a
            // KeyError, pass it on through below
            if (value == Py.None) {
                args = Py.EmptyObjects;
            } else if (value instanceof PyTuple && type != Py.KeyError) {
                args = ((PyTuple)value).getArray();
            } else {
                args = new PyObject[] {value};
            }

            value = type.__call__(args);
        } else if (inClass != type) {
            type = inClass;
        }
        // FIXME: all exceptions thrown into Python should compliant to PEP-3134
//        if (value instanceof PyBaseException) {
            if (cause != null) {
                ((PyBaseException) value).setCause(cause);
            }
            if (context != null) {
                ((PyBaseException) value).setContext(context);
            }
            normalized = true;
//        }
        return this;
    }

    /**
     * Register frame as having been visited in the traceback.
     *
     * @param here the current PyFrame
     */
    public void tracebackHere(PyFrame here) {
        tracebackHere(here, false);
    }

    /**
     * Register frame as having been visited in the traceback.
     *
     * @param here the current PyFrame
     * @param isFinally whether caller is a Python finally block
     */
    public void tracebackHere(PyFrame here, boolean isFinally) {
        if (!isReRaise && here != null) {
            // the frame is either inapplicable or already registered (from a finally)
            // during a re-raise
            traceback = new PyTraceback(traceback, here);
            // since this is called after normalize, we can only amend it
            if (value instanceof PyBaseException) {
                traceback.tb_next = ((PyBaseException) value).__traceback__;
                ((PyBaseException) value).__traceback__ = traceback;
            } else {
                System.out.println("foo");
            }
        }
        // finally blocks immediately tracebackHere: so they toggle isReRaise to skip the
        // next tracebackHere hook
        isReRaise = isFinally;
    }

    public static PyException doRaise(PyObject value) {
        return doRaise(value, null);
    }

    public static PyException doRaise(ThreadState state) {
        PyException pye = state.exceptions.peekFirst();

        if (pye == null) {
            throw Py.RuntimeError("No active exception to reraise");
        }
        PyObject type = pye.type;
        PyObject value = pye.value;
        PyObject cause = pye.cause;
        return new PyException(type, value, cause);
    }

    /**
     * Logic for the raise statement
     *
     * @param value the second arg, the instance of the class or arguments to its
     * constructor
     * @param cause the chained exception
     * @return a PyException wrapper
     */
    public static PyException doRaise(PyObject value, PyObject cause) {
        PyObject type;
        ThreadState state = Py.getThreadState();

        PyException pye;
        if (isExceptionClass(value)) {
            type = value;
            // null flags context has been take care of
            pye = new PyException(type, null, cause);
            PyException context = state.exceptions.pollFirst();
            if (context != null && context.value instanceof PyBaseException) {
                pye.context = (PyBaseException) context.value;
            }
            pye.normalize();
            if (!isExceptionInstance(pye.value)) {
                throw Py.TypeError(String.format(
                    "calling %s() should have returned an instance of BaseException, not '%s'",
                    pye.type, pye.value));
            }
            return pye;
        } else if (isExceptionInstance(value)) {
            // ignore the context for now
            type = value.getType();
            return new PyException(type, value, cause);
        } else {
            throw Py.TypeError("exceptions must derive from BaseException");
        }
    }

    /**
     * Determine if this PyException is a match for exc.
     *
     * @param exc a PyObject exception type
     * @return true if a match
     */
    public boolean match(PyObject exc) {
        if (exc instanceof PyTuple) {
            for (PyObject item : ((PyTuple)exc).getArray()) {
                if (match(item)) {
                    return true;
                }
            }
            return false;
        }

        if (exc instanceof PyUnicode) {
            Py.DeprecationWarning("catching of string Exceptions is deprecated");
        } else if (Options.py3k_warning && !isExceptionClass(exc)) {
            Py.DeprecationWarning("catching classes that don't inherit from BaseException is not "
                                  + "allowed in 3.x");
        }

        normalize();
        // FIXME, see bug 737978
        //
        // A special case for IOError's to allow them to also match
        // java.io.IOExceptions.  This is a hack for 1.0.x until I can do
        // it right in 1.1
        if (exc == Py.IOError) {
            if (__builtin__.isinstance(value, PyType.fromClass(IOException.class))) {
                return true;
            }
        }
        // FIXME too, same approach for OutOfMemoryError
        if (exc == Py.MemoryError) {
            if (__builtin__.isinstance(value,
                                       PyType.fromClass(OutOfMemoryError.class))) {
                return true;
            }
        }

        if (isExceptionClass(type) && isExceptionClass(exc)) {
            try {
                return Py.isSubClass(type, exc);
            } catch (PyException pye) {
                // This function must not fail, so print the error here
                Py.writeUnraisable(pye, type);
                return false;
            }
        }

        return type == exc;
    }

    /**
     * Determine whether obj is a Python exception class
     *
     * @param obj a PyObject
     * @return true if an exception
     */
    public static boolean isExceptionClass(PyObject obj) {
        if (!(obj instanceof PyType)) {
            return false;
        }
        PyType type = ((PyType)obj);
        if (type.isSubType(PyBaseException.TYPE)) {
            return true;
        }
        return type.getProxyType() != null
                && Throwable.class.isAssignableFrom(type.getProxyType());
    }

    /**
     * Determine whether obj is an Python exception instance
     *
     * @param obj a PyObject
     * @return true if an exception instance
     */
    public static boolean isExceptionInstance(PyObject obj) {
        return obj instanceof PyBaseException;
    }

    /**
     * Get the name of the exception's class
     *
     * @param obj a PyObject exception
     * @return String exception name
     */
    public static String exceptionClassName(PyObject obj) {
        return ((PyType)obj).fastGetName();
    }


    /* Traverseproc support */

    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retValue;
        if (type != null) {
            retValue = visit.visit(type, arg);
            if (retValue != 0) {
                return retValue;
            }
        } if (value != null) {
            retValue = visit.visit(value, arg);
            if (retValue != 0) {
                return retValue;
            }
        } if (traceback != null) {
            retValue = visit.visit(traceback, arg);
            if (retValue != 0) {
                return retValue;
            }
        }
        return 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
    	return ob != null && (type == ob || value == ob || traceback == ob);
    }
}
