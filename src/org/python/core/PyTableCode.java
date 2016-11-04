// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * An implementation of PyCode where the actual executable content
 * is stored as a PyFunctionTable instance and an integer index.
 */

import org.python.expose.ExposedGet;
import org.python.expose.ExposedType;
import org.python.modules._systemrestart;

import java.util.ArrayList;

@Untraversable
@ExposedType(name = "code", base = PyObject.class, doc = BuiltinDocs.code_doc)
public class PyTableCode extends PyBaseCode
{

    PyFunctionTable funcs;
    int func_id;
    public String co_code = ""; // only used by inspect

    public PyTableCode(int argcount, String varnames[],
                       String filename, String name,
                       int firstlineno,
                       boolean varargs, boolean varkwargs,
                       PyFunctionTable funcs, int func_id)
    {
        this(argcount, varnames, filename, name, firstlineno, varargs,
             varkwargs, funcs, func_id, null, null, null, null, 0, 0, 0);
    }

    public PyTableCode(int argcount, String[] varnames,
                       String filename, String name,
                       int firstlineno,
                       boolean varargs, boolean varkwargs,
                       PyFunctionTable funcs, int func_id,
                       String[] cellvars, String[] freevars, String[] names,
                       PyObject[] consts, int npurecell,
                       int kwonlyargcount, int moreflags) // may change
    {
        co_argcount = nargs = argcount;
        co_varnames = varnames;
        co_names = names;
        co_consts = consts;
        co_nlocals = varnames.length;
        co_filename = filename;
        co_firstlineno = firstlineno;
        co_cellvars = cellvars;
        co_freevars = freevars;
        this.jy_npurecell = npurecell;
        this.varargs = varargs;
        co_name = name;
        if (varargs) {
            co_argcount--;
            co_flags.setFlag(CodeFlag.CO_VARARGS);
        }
        this.varkwargs = varkwargs;
        if (varkwargs) {
            co_argcount--;
            co_flags.setFlag(CodeFlag.CO_VARKEYWORDS);
        }
        co_flags = new CompilerFlags(co_flags.toBits() | moreflags);
        this.funcs = funcs;
        this.func_id = func_id;
        co_kwonlyargcount = kwonlyargcount;
    }

    private static final String[] __members__ = {
        "co_name", "co_argcount",
        "co_varnames", "co_filename", "co_firstlineno",
        "co_flags","co_cellvars","co_freevars", "co_nlocals",
        "co_kwonlyargcount"
        // not supported: co_code, co_consts, co_names,
        // co_lnotab, co_stacksize
    };

//    public PyObject __dir__() {
//        PyUnicode members[] = new PyUnicode[__members__.length];
//        for (int i = 0; i < __members__.length; i++)
//            members[i] = new PyUnicode(__members__[i]);
//        return new PyList(members);
//    }

    private void throwReadonly(String name) {
        for (int i = 0; i < __members__.length; i++)
            if (__members__[i] == name)
                throw Py.TypeError("readonly attribute");
        throw Py.AttributeError(name);
    }

    public void __setattr__(String name, PyObject value) {
        // no writable attributes
        throwReadonly(name);
    }

    public void __delattr__(String name) {
        throwReadonly(name);
    }

    private static PyTuple toPyStringTuple(String[] ar) {
        if (ar == null) return Py.EmptyTuple;
        int sz = ar.length;
        PyUnicode[] pystr = new PyUnicode[sz];
        for (int i = 0; i < sz; i++) {
            pystr[i] = new PyUnicode(ar[i]);
        }
        return new PyTuple(pystr);
    }

    @ExposedGet
    final PyObject co_argcount() {
        return new PyLong(co_argcount);
    }

    @ExposedGet
    final PyObject co_varnames() {
        return toPyStringTuple(co_varnames);
    }

    @ExposedGet
    final PyObject co_names() {
        return toPyStringTuple(co_names);
    }

    @ExposedGet
    final PyObject co_cellvars() {
        return toPyStringTuple(co_cellvars);
    }

    @ExposedGet
    final PyObject co_freevars() {
        return toPyStringTuple(co_freevars);
    }

    @ExposedGet
    final PyObject co_consts() {
        return new PyTuple(co_consts);
    }
    @ExposedGet
    final PyObject co_filename() {
        return new PyUnicode(co_filename);
    }

    @ExposedGet
    final PyObject co_nlocals() {
        return new PyLong(co_nlocals);
    }

    @ExposedGet
    final PyObject co_name() {
        return new PyUnicode(co_name);
    }

    @ExposedGet
    final PyObject co_flags() {
        return Py.newLong(co_flags.toBits());
    }

    @ExposedGet
    final PyObject co_kwonlyargcount() {
        return Py.newLong(co_kwonlyargcount);
    }

    @ExposedGet
    final PyObject co_firstlineno() {
        return new PyLong(co_firstlineno);
    }

    @Override
    public PyObject call(ThreadState ts, PyFrame frame, PyObject closure) {
        if (ts.systemState == null) {
            ts.systemState = Py.defaultSystemState;
        }

        // Cache previously defined exception
//        PyException previous_exception = ts.exceptions.peekFirst();
        int exceptionsLength = ts.exceptions.size();

        // Push frame
        frame.f_back = ts.frame;
        if (frame.f_builtins == null) {
            if (frame.f_back != null) {
                frame.f_builtins = frame.f_back.f_builtins;
            } else {
                frame.f_builtins = ts.systemState.builtins;
            }
        }
        // nested scopes: setup env with closure
        // this should only be done once, so let the frame take care of it
        frame.setupEnv((PyTuple)closure);

        ts.frame = frame;

        // Handle trace function for debugging
        if (ts.tracefunc != null) {
            frame.f_lineno = co_firstlineno;
            frame.tracefunc = ts.tracefunc.traceCall(frame);
        }

        // Handle trace function for profiling
        if (ts.profilefunc != null) {
            ts.profilefunc.traceCall(frame);
        }

        PyObject ret;
        ThreadStateMapping.enterCall(ts);
        try {
            ret = funcs.call_function(func_id, frame, ts);
        } catch (Throwable t) {
            // Convert Exceptions that occurred in Java code to PyExceptions
            PyException pye = Py.JavaError(t);
            pye.normalize();
            pye.tracebackHere(frame);

            frame.f_lasti = -1;

            if (frame.tracefunc != null) {
                frame.tracefunc.traceException(frame, pye);
            }
            if (ts.profilefunc != null) {
                ts.profilefunc.traceException(frame, pye);
            }

            // Rethrow the exception to the next stack frame
//            ts.exceptions.addFirst(previous_exception);
            while(ts.exceptions.size() > exceptionsLength) {
                ts.exceptions.pop();
            }

            ts.frame = ts.frame.f_back;
            throw pye;
        } finally {
            ThreadStateMapping.exitCall(ts);
        }

        if (frame.tracefunc != null) {
            frame.tracefunc.traceReturn(frame, ret);
        }
        // Handle trace function for profiling
        if (ts.profilefunc != null) {
            ts.profilefunc.traceReturn(frame, ret);
        }

        // Restore previously defined exception
//        ts.exceptions.poll();
//        ts.exceptions.addFirst(previous_exception);
        while(ts.exceptions.size() > exceptionsLength) {
            ts.exceptions.pop();
        }


        ts.frame = ts.frame.f_back;

        // Check for interruption, which is used for restarting the interpreter
        // on Jython
        if (ts.systemState._systemRestart && Thread.currentThread().isInterrupted()) {
            throw new PyException(_systemrestart.SystemRestart);
        }
        return ret;
    }

    @Override
    protected PyObject interpret(PyFrame f, ThreadState ts) {
        throw new UnsupportedOperationException("Inlined interpret to improve call performance (may want to reconsider in the future).");
    }
}
