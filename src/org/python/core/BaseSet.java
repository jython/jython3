/* Copyright (c) Jython Developers */
package org.python.core;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public abstract class BaseSet extends PyObject implements Set, Traverseproc {

    /** The underlying Set. */
    protected Set<PyObject> _set;

    /**
     * Create a new Python set of type from the specified Set object.
     */
    protected BaseSet(PyType type, Set<PyObject> set) {
        super(type);
        _set = set;
    }

    public Set<PyObject> getSet() {
        return _set;
    }

    public void _update(PyObject data) {
        _update(_set, data);
    }

    protected void _update(PyObject [] args) {
        _update(_set, args);
    }
    
    /**
     * Update the underlying set with the contents of the iterable.
     */
    protected static Set<PyObject> _update(Set<PyObject> set, PyObject data) {
        if (data == null) {
            return set;
        }
        if (data instanceof BaseSet) {
            // Skip the iteration if both are sets
            set.addAll(((BaseSet)data)._set);
        } else {
            for (PyObject item : data.asIterable()) {
                set.add(item);
            }
        }
        return set;
    }

    /**
     * Update the underlying set with the contents of the array.
     */
    protected static Set<PyObject> _update(Set<PyObject> set, PyObject[] data) {
        if (data == null) {
            return set;
        }
        for (PyObject item : data) {
            set.add(item);
        }
        return set;
    }

    /**
     * The union of <code>this</code> with <code>other</code>. <p/> <br/> (I.e. all elements
     * that are in either set)
     *
     * @param other A <code>BaseSet</code> instance.
     * @return The union of the two sets as a new set.
     */
    public PyObject __or__(PyObject other) {
        return baseset___or__(other);
    }

    final PyObject baseset___or__(PyObject other) {
        if (!(other instanceof BaseSet)) {
            return null;
        }
        return baseset_union(other);
    }

    /**
     * The intersection of the <code>this</code> with <code>other</code>.
     * <p/>
     * <br/>
     * (I.e. all elements that are in both sets)
     *
     * @param other A <code>BaseSet</code> instance.
     * @return The intersection of the two sets as a new set.
     */
    public PyObject __and__(PyObject other) {
        return baseset___and__(other);
    }

    final PyObject baseset___and__(PyObject other) {
        if (!(other instanceof BaseSet)) {
            return null;
        }
        return baseset_intersection(other);
    }

    /**
     * The difference of the <code>this</code> with <code>other</code>.
     * <p/>
     * <br/>
     * (I.e. all elements that are in this set and not in the other)
     *
     * @param other A <code>BaseSet</code> instance.
     * @return The difference of the two sets as a new set.
     */
    public PyObject __sub__(PyObject other) {
        return baseset___sub__(other);
    }

    final PyObject baseset___sub__(PyObject other) {
        if (!(other instanceof BaseSet)) {
            return null;
        }
        return baseset_difference(other);
    }

    public PyObject difference(PyObject other) {
        return baseset_difference(other);
    }
    
    final PyObject baseset_difference(PyObject other) {
        return baseset_difference(new PyObject[] {other});
    }
    
    final PyObject baseset_difference(PyObject [] args) {
        if (args.length == 0) {
            return BaseSet.makeNewSet(getType(), this);
        }
        
        BaseSet o = BaseSet.makeNewSet(getType(), this);
        for (PyObject item: args) {
            BaseSet bs = args[0] instanceof BaseSet ? (BaseSet)item : new PySet(item);
            Set<PyObject> set = bs._set;

            for (PyObject p : set) {
                if (_set.contains(p)) {
                    o._set.remove(p);
                }
            }
        }
        return o;
    }

    /**
     * The symmetric difference of the <code>this</code> with <code>other</code>.
     * <p/>
     * <br/>
     * (I.e. all elements that are in exactly one of the sets)
     *
     * @param other A <code>BaseSet</code> instance.
     * @return The symmetric difference of the two sets as a new set.
     */
    public PyObject __xor__(PyObject other) {
        return baseset___xor__(other);
    }

    final PyObject baseset___xor__(PyObject other) {
        if (!(other instanceof BaseSet)) {
            return null;
        }
        return baseset_symmetric_difference(other);
    }

    public PyObject symmetric_difference(PyObject other) {
        return baseset_symmetric_difference(other);
    }

    final PyObject baseset_symmetric_difference(PyObject other) {
        BaseSet bs = other instanceof BaseSet ? (BaseSet)other : new PySet(other);
        BaseSet o = BaseSet.makeNewSet(getType());
        for (PyObject p : _set) {
            if (!bs._set.contains(p)) {
                o._set.add(p);
            }
        }
        for (PyObject p : bs._set) {
            if (!_set.contains(p)) {
                o._set.add(p);
            }
        }
        return o;
    }

    /**
     * The hashCode of the set.  Only immutable instances can be hashed.
     *
     * @return The hashCode of the set.
     */
    public abstract int hashCode();

    /**
     * The length of the set.
     *
     * @return The length of the set.
     */
    public int __len__() {
        return baseset___len__();
    }

    final int baseset___len__() {
        return _set.size();
    }

    /**
     * Determines if the instance is considered <code>true</code> by Python.
     * This implementation returns true if the set is not empty.
     *
     * @return <code>true</code> if the set is not empty, <code>false</code> otherwise
     */
    public boolean __bool__() {
        return !_set.isEmpty();
    }

    /**
     * Produce an iterable object.
     *
     * @return An iteration of the set.
     */
    public PyObject __iter__() {
        return baseset___iter__();
    }

    final PyObject baseset___iter__() {
        return new PyIterator() {
            private int size = size();

            private Iterator<PyObject> iterator = _set.iterator();

            @Override
            public PyObject __iternext__() {
                if (iterator.hasNext()) {
                    return iterator.next();
                }
                return null;
            }
        };
    }

    public boolean __contains__(PyObject other) {
        return baseset___contains__(other);
    }

    final boolean baseset___contains__(PyObject other) {
        try {
            return _set.contains(other);
        } catch (PyException pye) {
            PyFrozenSet frozen = asFrozen(pye, other);
            return _set.contains(frozen);
        }
    }

    public int __cmp__(PyObject other) {
        return baseset___cmp__(other);
    }

    final int baseset___cmp__(PyObject other) {
        throw Py.TypeError("cannot compare sets using cmp()");
    }

    public PyObject __eq__(PyObject other) {
        return baseset___eq__(other);
    }

    final PyObject baseset___eq__(PyObject other) {
        if (other instanceof BaseSet) {
            return Py.newBoolean(_set.equals(((BaseSet)other)._set));
        }
        return Py.False;
    }

    public PyObject __ne__(PyObject other) {
        return baseset___ne__(other);
    }

    final PyObject baseset___ne__(PyObject other) {
        if (other instanceof BaseSet) {
            return Py.newBoolean(!_set.equals(((BaseSet)other)._set));
        }
        return Py.True;
    }

    public PyObject __le__(PyObject other) {
        return baseset___le__(other);
    }

    final PyObject baseset___le__(PyObject other) {
        return baseset_issubset(asBaseSet(other));
    }

    public PyObject __ge__(PyObject other) {
        return baseset___ge__(other);
    }

    final PyObject baseset___ge__(PyObject other) {
        return baseset_issuperset(asBaseSet(other));
    }

    public PyObject __lt__(PyObject other) {
        return baseset___lt__(other);
    }

    final PyObject baseset___lt__(PyObject other) {
        BaseSet bs = asBaseSet(other);
        return Py.newBoolean(size() < bs.size() && baseset_issubset(other).__bool__());
    }

    public PyObject __gt__(PyObject other) {
        return baseset___gt__(other);
    }

    final PyObject baseset___gt__(PyObject other) {
        BaseSet bs = asBaseSet(other);
        return Py.newBoolean(size() > bs.size() && baseset_issuperset(other).__bool__());
    }

    /**
     * Used for pickling.  Uses the module <code>setsfactory</sets> to
     * export safe constructors.
     *
     * @return a tuple of (constructor, (elements))
     */
    public PyObject __reduce__() {
        return baseset___reduce__();
    }

    final PyObject baseset___reduce__(){
        PyObject args = new PyTuple(new PyList((PyObject)this));
        PyObject dict = __findattr__("__dict__");
        if (dict == null) {
            dict = Py.None;
        }
        return new PyTuple(getType(), args, dict);
    }

    final PyObject baseset_union(PyObject other) {
        BaseSet result = BaseSet.makeNewSet(getType(), this);
        result._update(other);
        return result;
    }
    
    final PyObject baseset_union(PyObject [] args) {
        BaseSet result = BaseSet.makeNewSet(getType(), this);
        for (PyObject item: args) {
            result._update(item);
        }
        return result;
    }

    final PyObject baseset_intersection(PyObject other) {
        PyObject little, big;
        if (!(other instanceof BaseSet)) {
            other = new PySet(other);
        }

        if (size() <= __builtin__.len(other)) {
            little = this;
            big = other;
        } else {
            little = other;
            big = this;
        }

        PyObject common = __builtin__.filter(big.__getattr__("__contains__"), little);
        return BaseSet.makeNewSet(getType(), common);
    }
    
    final PyObject baseset_intersection(PyObject [] args) {
        BaseSet result = BaseSet.makeNewSet(getType(), this);
        if (args.length == 0) {
            return result;
        }
        
        for (PyObject other: args) {
            result = (BaseSet)result.baseset_intersection(other);
        }
        return result;
    }

    final PyObject baseset_copy() {
        BaseSet copy = BaseSet.makeNewSet(getType(), this);
        return copy;
    }

    final PyObject baseset_issubset(PyObject other) {
        BaseSet bs = other instanceof BaseSet ? (BaseSet)other : new PySet(other);
        if (size() > bs.size()) {
            return Py.False;
        }
        for (Object p : _set) {
            if (!bs._set.contains(p)) {
                return Py.False;
            }
        }
        return Py.True;
    }

    final PyObject baseset_issuperset(PyObject other) {
        BaseSet bs = other instanceof BaseSet ? (BaseSet)other : new PySet(other);
        return bs.baseset_issubset(this);
    }
    
    final PyObject baseset_isdisjoint(PyObject other) {
        BaseSet bs = other instanceof BaseSet ? (BaseSet)other : new PySet(other);
        return Collections.disjoint(_set, bs._set) ? Py.True : Py.False;
    }

    public String toString() {
        return baseset_toString();
    }

    final String baseset_toString() {
        String name = getType().fastGetName();
        ThreadState ts = Py.getThreadState();
        if (!ts.enterRepr(this)) {
            return name + "(...)";
        }
        StringBuilder buf = new StringBuilder(name).append("([");
        for (Iterator<PyObject> i = _set.iterator(); i.hasNext();) {
            buf.append((i.next()).__repr__().toString());
            if (i.hasNext()) {
                buf.append(", ");
            }
        }
        buf.append("])");
        ts.exitRepr(this);
        return buf.toString();
    }

    /**
     * Casts other as BaseSet, throwing a TypeError tailored for the rich comparison
     * methods when not applicable.
     *
     * @param other a PyObject
     * @return a BaseSet
     */
    protected final BaseSet asBaseSet(PyObject other) {
        if (other instanceof BaseSet) {
            return (BaseSet)other;
        }
        throw Py.TypeError("can only compare to a set");
    }

    /**
     * Return a PyFrozenSet whose contents are shared with value when value is a BaseSet and pye is
     * a TypeError.
     *
     * WARNING: The PyFrozenSet returned is only intended to be used temporarily (and internally);
     * since its contents are shared with value, it could be mutated!
     *
     * This is better than special-casing behavior based on isinstance, because a Python subclass
     * can override, say, __hash__ and all of a sudden you can't assume that a non-PyFrozenSet is
     * unhashable anymore.
     *
     * @param pye The exception thrown from a hashable operation.
     * @param value The object which was unhashable.
     * @return A PyFrozenSet if appropriate, otherwise the pye is rethrown
     */
    protected final PyFrozenSet asFrozen(PyException pye, PyObject value) {
        if (!(value instanceof BaseSet) || !pye.match(Py.TypeError)) {
            throw pye;
        }
        PyFrozenSet tmp = new PyFrozenSet();
        tmp._set = ((BaseSet)value)._set;
        return tmp;
    }

    /**
     * Create a new set of type.
     *
     * @param type a set type
     * @return a new set
     */
    protected static BaseSet makeNewSet(PyType type) {
        return makeNewSet(type, null);
    }

    /**
     * Create a new set of type from iterable.
     *
     * @param type a set type
     * @param iterable an iterable or null
     * @return a new set
     */
    protected static BaseSet makeNewSet(PyType type, PyObject iterable) {
        BaseSet so;
        if (type == PySet.TYPE) {
            so = new PySet(iterable);
        } else if (type == PyFrozenSet.TYPE) {
            so = new PyFrozenSet(iterable);
        } else if (Py.isSubClass(type, PySet.TYPE)) {
            so = (BaseSet)(type.__call__(iterable == null ? Py.EmptyTuple : iterable));
        } else {
            so = new PyFrozenSetDerived(type, iterable);
        }
        return so;
    }

    public int size() {
        return _set.size();
    }

    public void clear() {
        _set.clear();
    }

    public boolean isEmpty() {
        return _set.isEmpty();
    }

    public boolean add(Object o) {
        return _set.add(Py.java2py(o));
    }

    public boolean contains(Object o) {
        return _set.contains(Py.java2py(o));
    }

    public boolean remove(Object o) {
        return _set.remove(Py.java2py(o));
    }

    public boolean addAll(Collection c) {
        boolean added = false;
        for (Object object : c) {
            added |= add(object);
        }
        return added;
    }

    public boolean containsAll(Collection c) {
        for (Object object : c) {
            if (!_set.contains(Py.java2py(object))) {
                return false;
            }
        }
        return true;
    }

    public boolean removeAll(Collection c) {
        boolean removed = false;
        for (Object object : c) {
            removed |= _set.remove(Py.java2py(object));
        }
        return removed;
    }

    public boolean retainAll(Collection c) {
        boolean modified = false;
        Iterator e = iterator();
        while (e.hasNext()) {
            if (!c.contains(e.next())) {
                e.remove();
                modified = true;
            }
        }
        return modified;
    }

    public Iterator iterator() {
        return new Iterator() {
            Iterator<PyObject> real = _set.iterator();

            public boolean hasNext() {
                return real.hasNext();
            }

            public Object next() {
                return Py.tojava(real.next(), Object.class);
            }

            public void remove() {
                real.remove();
            }
        };
    }

    public Object[] toArray() {
        return toArray(new Object[size()]);
    }

    public Object[] toArray(Object a[]) {
        int size = size();
        if (a.length < size) {
            a = (Object[])Array.newInstance(a.getClass().getComponentType(), size);
        }
        Iterator<PyObject> it = iterator();
        for (int i = 0; i < size; i++) {
            a[i] = it.next();
        }
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retValue;
        for (PyObject ob: _set) {
            if (ob != null) {
                retValue = visit.visit(ob, arg);
                if (retValue != 0) {
                    return retValue;
                }
            }
        }
        return 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && _set.contains(ob);
    }
}
