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
        PyObject ret = send_gen_exp(Py.getThreadState(), value);
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
        PyException pye = Py.makeException(type, value);
        pye.traceback = (PyTraceback) tb;
        return raiseException(pye);
    }

    public PyObject close() {
        return generator_close();
    }

    @ExposedMethod(doc = BuiltinDocs.generator_close_doc)
    final PyObject generator_close() {
        try {
            PyGenerator yf = (PyGenerator) gi_frame.f_yieldfrom;
            if (yf != null) {
                yf.close();
            } else {
                raiseException(Py.makeException(Py.GeneratorExit));
                throw Py.RuntimeError("generator ignored GeneratorExit");
            }
        } catch (PyException e) {
            if (!(e.type == Py.StopIteration || e.type == Py.GeneratorExit)) {
                throw e;
            }
        }
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
            return ((PyGenerator) yf).raiseException(ex);
        } else {
            return send_gen_exp(Py.getThreadState(), ex);
        }
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
    public PyObject __iternext__() {
        return send_gen_exp(Py.getThreadState(), Py.None);
    }

    private PyObject send_gen_exp(ThreadState state, Object value) {
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
            if (!(pye.type == Py.StopIteration || pye.type == Py.GeneratorExit)) {
                gi_frame = null;
                throw pye;
            } else {
                if (gi_frame.f_yieldfrom != null) {
                    gi_frame.f_yieldfrom = null;
                    gi_frame.f_lasti++;
                    return send_gen_exp(state, value);
                }
                stopException = pye;
                gi_frame = null;
                throw pye;
            }
        } finally {
            gi_running = false;
        }
        if (result == null && gi_frame.f_yieldfrom != null) {
            gi_frame.f_yieldfrom = null;
            gi_frame.f_lasti++;
            return send_gen_exp(state, value);
        }

        if (result == Py.None && gi_frame.f_lasti == -1) {
            gi_frame = null;
            return null;
        }
        return result;
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
