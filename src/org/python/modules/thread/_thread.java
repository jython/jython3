// Copyright (c) Corporation for National Research Initiatives
package org.python.modules.thread;

import org.python.core.FunctionThread;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.expose.ExposedConst;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;
import org.python.expose.ModuleInit;

import java.util.concurrent.atomic.AtomicInteger;

@ExposedModule(name = "_thread")
public class _thread {
    private static volatile long stack_size = 0; // XXX - can we figure out the current stack size?
    private static ThreadGroup group = new ThreadGroup("jython-threads");
    private static final AtomicInteger _count = new AtomicInteger(0);

    @ExposedConst
    public static final double TIMEOUT_MAX = 9223372036.0;

    @ModuleInit
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("LockType", PyLock.TYPE);
        dict.__setitem__("_local", PyLocal.TYPE);
        dict.__setitem__("error", Py.RuntimeError);
    }

    @ExposedFunction
    public static PyLong _count() {
        return new PyLong(_count.get());
    }

    @ExposedFunction
    public static void start_new_thread(PyObject func, PyObject args) {
        Thread pt = newFunctionThread(func, (PyTuple) args);
        PyObject currentThread = func.__findattr__("__self__");
        if (currentThread != null) {
            PyObject isDaemon = currentThread.__findattr__("isDaemon");
            if (isDaemon != null && isDaemon.isCallable()) {
                PyObject po = isDaemon.__call__();
                pt.setDaemon(po.__bool__());
            }
            PyObject getName = currentThread.__findattr__("getName");
            if (getName != null && getName.isCallable()) {
                PyObject pname = getName.__call__();
                pt.setName(String.valueOf(pname));
            }
        }
        pt.start();
    }

    /**
     * Initializes a {@link FunctionThread}, using the configured stack_size and
     * registering the thread in the @link {@link #group} of threads spawned by
     * the thread module.
     *
     * Also used from the threading.py module.
     */
    @ExposedFunction
    public static PyObject _newFunctionThread(PyObject func, PyObject args) {
        return Py.java2py(newFunctionThread(func, (PyTuple) args));
    }

    public static FunctionThread newFunctionThread(PyObject func, PyTuple args) {
        return new FunctionThread(func, args.getArray(), stack_size, group);
    }

    /**
     * Interrupts all running threads spawned by the thread module.
     *
     * This works in conjunction with:<ul> <li>
     * {@link org.python.core.PyTableCode#call}: checks for the interrupted
     * status of the current thread and raise a SystemRestart exception if a
     * interruption is detected.</li>
     * <li>{@link FunctionThread#run()}: exits the current thread when a
     * SystemRestart exception is not caught.</li>
     *
     * Thus, it is possible that this doesn't make all running threads to stop,
     * if SystemRestart exception is caught.
     */
    @ExposedFunction
    public static void interruptAllThreads() {
        group.interrupt();
    }

    @ExposedFunction
    public static PyLock allocate_lock() {
        return new PyLock();
    }

    @ExposedFunction
    public static void exit() {
        exit_thread();
    }

    @ExposedFunction
    public static void exit_thread() {
        throw new PyException(Py.SystemExit, new PyInteger(0));
    }

    @ExposedFunction
    public static PyObject _set_sentinel() {
        return new PyLock();
    }

    @ExposedFunction
    public static long get_ident() {
        return Thread.currentThread().getId();
    }

    @ExposedFunction
    public static long stack_size(PyObject[] args, String[] keywords) {
        switch (args.length) {
            case 0:
                return stack_size;
            case 1:
                long old_stack_size = stack_size;
                int proposed_stack_size = args[0].asInt();
                if (proposed_stack_size != 0 && proposed_stack_size < 32768) {
                    // as specified by Python, Java quietly ignores what
                    // it considers are too small
                    throw Py.ValueError("size not valid: " + proposed_stack_size + " bytes");
                }
                stack_size = proposed_stack_size;
                return old_stack_size;
            default:
                throw Py.TypeError("stack_size() takes at most 1 argument (" + args.length + "given)");
        }
    }
}
