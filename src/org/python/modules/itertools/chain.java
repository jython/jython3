/* Copyright (c) 2012 Jython Developers */
package org.python.modules.itertools;

import org.python.core.ArgParser;
import org.python.core.BuiltinDocs;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyIterator;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.Visitproc;
import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "itertools.chain", base = PyObject.class, doc = BuiltinDocs.chain_doc)
public class chain extends PyIterator {

    public static final PyType TYPE = PyType.fromClass(chain.class);
    private itertools.ItertoolsIterator iter;

    public chain() {
        super();
    }

    public chain(PyType subType) {
        super(subType);
    }

    public chain(PyObject iterable) {
        super();
        chain___init__(iterable.__iter__());
    }

    @ExposedClassMethod
    public static final PyObject from_iterable(PyType type, PyObject iterable) {
        return new chain(iterable);
    }

    /**
     * Creates an iterator that iterates over a <i>chain</i> of iterables.
     */
    @ExposedNew
    @ExposedMethod
    final void chain___init__(final PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("chain", args, kwds, "iterables");
        ap.noKeywords();

        //ArgParser always returns a PyTuple - I wonder why we make it pass back a PyObject?
        PyTuple tuple = (PyTuple)ap.getList(0);
        chain___init__(tuple.__iter__());
    }

    private void chain___init__(final PyObject superIterator) {

        iter = new itertools.ItertoolsIterator() {
            PyObject currentIterator = superIterator.__next__().__iter__();

            public PyObject __next__() {
                try {
                    return currentIterator.__next__();
                } catch (PyException e) {
                    if (e.match(Py.StopIteration)) {
                        currentIterator = superIterator.__next__().__iter__();
                        return __next__();
                    }
                    throw e;
                }
            }

        };
    }

    @Override
    public PyObject __iter__() {
        return chain___iter__();
    }

    @ExposedMethod(doc = BuiltinDocs.chain___iter___doc)
    final PyObject chain___iter__() {
        return this;
    }

    @Override
    public PyObject __next__() {
        return chain___next__();
    }

    @ExposedMethod(doc = BuiltinDocs.chain___next___doc)
    final PyObject chain___next__() {
        return iter.__next__();
    }

    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retVal = super.traverse(visit, arg);
        if (retVal != 0) {
            return retVal;
        }
        return iter != null ? visit.visit(iter, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (iter == ob || super.refersDirectlyTo(ob));
    }
}
