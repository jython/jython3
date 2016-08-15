// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * The builtin range type.
 */
@Untraversable
@ExposedType(name = "range", base = PyObject.class, isBaseType = false,
             doc = BuiltinDocs.range_doc)
public class PyRange extends PySequence {

    public static final PyType TYPE = PyType.fromClass(PyRange.class);

    private final long start;
    private final long step;
    private final long stop;
    private final long len;

    public PyRange(long ihigh) {
        this(0, ihigh, 1);
    }

    public PyRange(long ilow, long ihigh) {
        this(ilow, ihigh, 1);
    }

    public PyRange(long ilow, long ihigh, int istep) {
        super(TYPE);

        if (istep == 0) {
            throw Py.ValueError("range() arg 3 must not be zero");
        }

        int n;
        long listep = istep;
        if (listep > 0) {
            n = getLenOfRange(ilow, ihigh, listep);
        } else {
            n = getLenOfRange(ihigh, ilow, -listep);
        }
        if (n < 0) {
            throw Py.OverflowError("range() result has too many items");
        }
        start = ilow;
        len = n;
        step = istep;
        stop = ihigh;
    }

    @ExposedNew
    static final PyObject range___new__(PyNewWrapper new_, boolean init, PyType subtype,
                                         PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("range", args, keywords,
                                     new String[] {"ilow", "ihigh", "istep"}, 1);
        ap.noKeywords();

        long ilow = 0;
        long ihigh;
        int istep = 1;
        if (args.length == 1) {
            ihigh = ap.getLong(0);
        } else {
            ilow = ap.getLong(0);
            ihigh = ap.getLong(1);
            istep = ap.getInt(2, 1);
            if (istep == 0) {
                throw Py.ValueError("range() arg 3 must not be zero");
            }
        }
        return new PyRange(ilow, ihigh, istep);
    }

    /**
     * Return number of items in range (lo, hi, step).  step > 0 required.  Return
     * a value < 0 if & only if the true value is too large to fit in a Java int.
     *
     * @param lo int value
     * @param hi int value
     * @param step int value (> 0)
     * @return int length of range
     */
    static int getLenOfRange(long lo, long hi, long step) {
        if (lo < hi) {
            // the base difference may be > Integer.MAX_VALUE
            long diff = hi - lo - 1;
            // any long > Integer.MAX_VALUE or < Integer.MIN_VALUE gets cast to a
            // negative number
            return (int)((diff / step) + 1);
        } else {
            return 0;
        }
    }

    @Override
    public int __len__() {
        return range___len__();
    }

    @ExposedMethod(doc = BuiltinDocs.range___len___doc)
    final int range___len__() {
        return (int)len;
    }

    @Override
    public PyObject __getitem__(PyObject index) {
        return range___getitem__(index);
    }

    @ExposedMethod(doc = BuiltinDocs.range___getitem___doc)
    final PyObject range___getitem__(PyObject index) {
        PyObject ret = seq___finditem__(index);
        if (ret == null) {
            throw Py.IndexError("range object index out of range");
        }
        return ret;
    }

    @Override
    public PyObject __iter__() {
        return range___iter__();
    }

    @ExposedMethod(doc = BuiltinDocs.range___iter___doc)
    public PyObject range___iter__() {
        return range_iter();
    }

    @ExposedMethod(doc = BuiltinDocs.range___reversed___doc)
    public PyObject range___reversed__() {
        return range_reverse();
    }

    private final PyXRangeIter range_iter() {
        return new PyXRangeIter(0, (long)start, (long)step, (long)len);
    }

    private final PyXRangeIter range_reverse() {
        return new PyXRangeIter(0,
                (start + (len - 1) * step),   // start
                (0 - step),                   // step (negative value)
                len);
    }

    @ExposedMethod
    public PyObject range___reduce__() {
        return new PyTuple(getType(),
                new PyTuple(Py.newInteger(start), Py.newInteger(stop), Py.newInteger(step)));
    }

    @Override
    public PyObject __reduce__() {
        return range___reduce__();
    }

    @Override
    protected PyObject pyget(int i) {
        return Py.newInteger(start + (i % len) * step);
    }

    @Override
    public PyObject getslice(int start, int stop, int step) {
        throw Py.TypeError("range index must be integer, not 'slice'");
    }

    @Override
    protected PyObject repeat(int howmany) {
        // not supported
        return null;
    }

    @Override
    protected String unsupportedopMessage(String op, PyObject o2) {
        // always return the default unsupported messages instead of PySequence's
        return null;
    }

    @Override
    public String toString() {
        long lstop = start + len * step;
        if (lstop > PySystemState.maxint) { lstop = PySystemState.maxint; }
        else if (lstop < PySystemState.minint) { lstop = PySystemState.minint; }
        int stop = (int)lstop;
        
        // TODO: needs to support arbitrary length!
        if (start == 0 && step == 1) {
            return String.format("range(%d)", stop);
        } else if (step == 1) {
            return String.format("range(%d, %d)", start, stop);
        } else {
            return String.format("range(%d, %d, %d)", start, stop, step);
        }
    }

    @Override
    public Object __tojava__(Class<?> c) {
        if (c.isAssignableFrom(Iterable.class)) {
            return new JavaIterator(range_iter());
        }
        if (c.isAssignableFrom(Iterator.class)) {
            return (new JavaIterator(range_iter())).iterator();
        }
        if (c.isAssignableFrom(Collection.class)) {
            List<Object> list = new ArrayList<>();
            for (Object obj : new JavaIterator(range_iter())) {
                list.add(obj);
            }
            return list;
        }
        return super.__tojava__(c);
    }
}
