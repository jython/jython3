/* Copyright (c) Jython Developers */
package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

/**
 * Specially optimized xrange iterator.
 */
@ExposedType(name = "rangeiterator", base = PyObject.class, isBaseType = false)
public class PyRangeIter extends PyIterator {

    public static final PyType TYPE = PyType.fromClass(PyRangeIter.class);
    static {
        TYPE.setName("rangeiterator");
    }

    private long index;
    private long start;
    private long step;
    private long len;

    public PyRangeIter(long index, long start, long step, long len) {
        super(TYPE);
        this.index = index;
        this.start = start;
        this.step = step;
        this.len = len;
    }

    @ExposedMethod(doc = "x.next() -> the next value, or raise StopIteration")
    final PyObject rangeiterator_next() {
        return super.next();
    }

    @Override
    public PyObject __iternext__() {
        if (index < len) {
            return Py.newInteger(start + index++ * step);
        }

        return null;
    }

}
