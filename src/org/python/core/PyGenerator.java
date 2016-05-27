/* Copyright (c) Jython Developers */
package org.python.core;

import org.python.core.finalization.FinalizableBuiltin;
import org.python.core.finalization.FinalizeTrigger;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "generator", base = PyObject.class, isBaseType = false, doc = BuiltinDocs.generator_doc)
public class PyGenerator extends PyIterator implements FinalizableBuiltin {

    public static final PyType TYPE = PyType.fromClass(PyGenerator.class);

    @ExposedGet
    protected PyFrame gi_frame;

    @ExposedGet
    protected PyCode gi_code = null;

    @ExposedGet
    protected boolean gi_running;

    private PyObject closure;

    public PyGenerator(PyFrame frame, PyObject closure) {
        this(TYPE, frame, closure);
    }

    public PyGenerator(PyType subType, PyFrame frame, PyObject closure) {
        super(subType);
        gi_frame = frame;
        if (gi_frame != null) {
            gi_code = gi_frame.f_code;
        }
        this.closure = closure;
        FinalizeTrigger.ensureFinalizer(this);
    }

    public PyObject send(PyObject value) {
        return generator_send(value);
    }

    @ExposedMethod(doc = BuiltinDocs.generator_send_doc)
    final PyObject generator_send(PyObject value) {
        if (gi_frame == null) {
            return value;
        }

        if (gi_frame.f_lasti == 0 && value != Py.None && value != null) {
            throw Py.TypeError("can't send non-None value to a just-started generator");
        }
        PyObject ret = gen_send_ex(Py.getThreadState(), value);
        if (ret == null) {
            throw Py.StopIteration();
        }
        return ret;
    }

    public PyObject throw$(PyObject type, PyObject value, PyObject tb) {
        return generator_throw$(type, value, tb);
    }

    @ExposedMethod(names="throw", defaults={"null", "null"}, doc = BuiltinDocs.generator_throw_doc)
    final PyObject generator_throw$(PyObject type, PyObject value, PyObject tb) {
        if (tb == Py.None) {
            tb = null;
        } else if (tb != null && !(tb instanceof PyTraceback)) {
            throw Py.TypeError("throw() third argument must be a traceback object");
        }
        PyException pye = new PyException(type, value);
        pye.traceback = (PyTraceback) tb;
        PyObject yf = gi_frame.f_yieldfrom;
        if (yf != null) {
            if (pye.match(Py.GeneratorExit)) {
                gen_close_iter(yf);
            }
            return gen_send_ex(Py.getThreadState(), pye);
        }
        throw pye;
    }

    public PyObject close() {
        return generator_close();
    }

    @ExposedMethod(doc = BuiltinDocs.generator_close_doc)
    final PyObject generator_close() {
        PyObject retval;
        PyObject yf = gi_frame.f_yieldfrom;
        if (yf != null) {
            gi_running = true;
            try {
                gi_frame.f_yieldfrom = null;
                gen_close_iter(yf);
            } catch (PyException e) {
                if (e.match(Py.StopIteration) || e.match(Py.GeneratorExit)) {
                    return Py.None;
                }
            } finally {
                gi_running = false;
            }
        }
        PyException pye = stopException = Py.GeneratorExit();
        try {
            // clean up
            retval = gen_send_ex(Py.getThreadState(), pye);
        } catch (PyException e) {
            if (e.match(Py.StopIteration) || e.match(Py.GeneratorExit)) {
                return Py.None;
            }
            throw e;
        }
        if (retval != null || retval != Py.None) {
            throw Py.RuntimeError("generator ignored GeneratorExit");
        }
        // not reachable
        return Py.None;
    }

    @Override
    public PyObject next() {
        return generator___next__();
    }

    @ExposedMethod(doc="x.next() -> the next value, or raise StopIteration")
    final PyObject generator___next__() {
        return super.next();
    }

    @Override
    public PyObject __iter__() {
        return generator___iter__();
    }

    @ExposedMethod(doc = BuiltinDocs.generator___iter___doc)
    final PyObject generator___iter__() {
        return this;
    }

    private PyObject raiseException(PyException ex) {
        if (gi_frame == null || gi_frame.f_lasti == 0) {
            gi_frame = null;
            throw ex;
        }
        PyObject yf = gi_frame.f_yieldfrom;
        if (yf != null) {
            ((PyGenerator) yf).raiseException(ex);
        }
        return gen_send_ex(Py.getThreadState(), ex);
    }
    
    @Override
    public void __del_builtin__() {
        if (gi_frame == null || gi_frame.f_lasti == -1) {
            return;
        }
        try {
            close();
        } catch (PyException pye) {
            // PEP 342 specifies that if an exception is raised by close,
            // we output to stderr and then forget about it;
            String className =  PyException.exceptionClassName(pye.type);
            int lastDot = className.lastIndexOf('.');
            if (lastDot != -1) {
                className = className.substring(lastDot + 1);
            }
            String msg = String.format("Exception %s: %s in %s", className, pye.value.__repr__(),
                                       __repr__());
            Py.println(Py.getSystemState().stderr, Py.newString(msg));
        } catch (Throwable t) {
            // but we currently ignore any Java exception completely. perhaps we
            // can also output something meaningful too?
        }
    }

    @Override
    public PyObject __next__() {
        try {
            return gen_send_ex(Py.getThreadState(), Py.None);
        } catch (PyException e) {
            // for iteration use null as stop iteration
            if (e.match(Py.StopIteration)) {
                return null;
            }
            throw e;
        }
    }

    private PyObject gen_send_ex(ThreadState state, Object value) {
        if (stopException != null) throw stopException;
        if (gi_running) {
            throw Py.ValueError("generator already executing");
        }
        if (gi_frame == null) {
            return null;
        }

        if (gi_frame.f_lasti == -1) {
            gi_frame = null;
            return null;
        }
        // if value is null, means the input is passed implicitly by frame, don't reset to None
        if (value != null) {
            gi_frame.setGeneratorInput(value);
        }
        gi_running = true;
        PyObject result = null;
        try {
            result = gi_frame.f_code.call(state, gi_frame, closure);
        } catch (PyException pye) {
            gi_frame = null;
            throw pye;
        } finally {
            gi_running = false;
        }
        if (result == null && gi_frame.f_yieldfrom != null) {
            gi_frame.f_yieldfrom = null;
            gi_frame.f_lasti++;
            return gen_send_ex(state, value);
        }

        if (result == Py.None && gi_frame.f_lasti == -1) {
            gi_frame = null;
            return null;
        }
        return result;
    }

    private void gen_close_iter(PyObject iter) {
        if (iter instanceof PyGenerator) {
            ((PyGenerator) iter).close();
            return;
        }
        try {
            PyObject closeMeth = iter.__finditem__("close");
            closeMeth.__call__();
        } catch (PyException e) {
            if (! e.match(Py.AttributeError)) throw e;
        }
    }

    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retValue = super.traverse(visit, arg);
        if (retValue != 0) {
            return retValue;
        }
        if (gi_frame != null) {
            retValue = visit.visit(gi_frame, arg);
            if (retValue != 0) {
                return retValue;
            }
        }
        if (gi_code != null) {
            retValue = visit.visit(gi_code, arg);
            if (retValue != 0) {
                return retValue;
            }
        }
        return closure == null ? 0 : visit.visit(closure, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (ob == gi_frame || ob == gi_code
            || ob == closure || super.refersDirectlyTo(ob));
    }
}
