/* Copyright (c) Jython Developers */
package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

/**
 * Specially optimized xrange iterator.
 */
@ExposedType(name = "range_iterator", base = PyObject.class, isBaseType = false)
public class PyXRangeIter extends PyIterator {

    public static final PyType TYPE = PyType.fromClass(PyXRangeIter.class);
    static {
        TYPE.setName("range_iterator");
    }

    private long index;
    private long start;
    private long step;
    private long len;

    public PyXRangeIter(long index, long start, long step, long len) {
        super(TYPE);
        this.index = index;
        this.start = start;
        this.step = step;
        this.len = len;
    }

    @ExposedMethod(doc = BuiltinDocs.range_iterator___next___doc)
    final PyObject range_iterator___next__() {
        return super.next();
    }

    @Override
    public PyObject __next__() {
        if (index < len) {
            return Py.newInteger(start + index++ * step);
        }

        return null;
    }

    @Override
    public int __len__() {
        return (int) len;
    }
}
