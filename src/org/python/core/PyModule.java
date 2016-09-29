// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

/**
 * The Python Module object.
 *
 */
@ExposedType(name = "module")
public class PyModule extends PyObject implements Traverseproc {
    public static final PyType TYPE = PyType.fromClass(PyModule.class);
    private static final PyUnicode __MAIN__ = new PyUnicode("__main__");

    private final PyObject moduleDoc = new PyBytes(
        "module(name[, doc])\n" +
        "\n" +
        "Create a module object.\n" +
        "The name must be a string; the optional doc argument can have any type.");

    /** The module's mutable dictionary */
    @ExposedGet
    public PyObject __dict__;

    public PyModule() {
        super();
    }

    public PyModule(PyType subType) {
        super(subType);
    }

    public PyModule(PyType subType, String name) {
        super(subType);
        module___init__(new PyUnicode(name), Py.None);
    }

    public PyModule(String name) {
        this(name, null);
    }

    public PyModule(String name, PyObject dict) {
        super(TYPE);
        __dict__ = dict;
        module___init__(new PyUnicode(name), Py.None);
    }

    @ExposedNew
    @ExposedMethod
    final void module___init__(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("__init__", args, keywords, new String[] {"name", "doc"});
        PyObject name = ap.getPyObject(0);
        PyObject docs = ap.getPyObject(1, Py.None);
        module___init__(name, docs);
    }

    private void module___init__(PyObject name, PyObject doc) {
        ensureDict();
        __dict__.__setitem__("__name__", name);
        __dict__.__setitem__("__doc__", doc);
        __dict__.__setitem__("__loader__", Py.None);
        __dict__.__setitem__("__package__", Py.None);
        __dict__.__setitem__("__spec__", Py.None);
        if (name.equals(__MAIN__)) {
            __dict__.__setitem__("__builtins__", Py.getSystemState().modules.__finditem__("__builtin__"));
            __dict__.__setitem__("__package__", Py.None);
        }
    }

    public PyObject fastGetDict() {
        return __dict__;
    }

    public PyObject getDict() {
        return __dict__;
    }

    @ExposedSet(name = "__dict__")
    public void setDict(PyObject newDict) {
        throw Py.TypeError("readonly attribute");
    }

    @ExposedDelete(name = "__dict__")
    public void delDict() {
        throw Py.TypeError("readonly attribute");
    }

    public void __setattr__(String name, PyObject value) {
        module___setattr__(name, value);
    }

    @ExposedMethod
    final void module___setattr__(String name, PyObject value) {
        if (name != "__dict__") {
            ensureDict();
        }
        super.__setattr__(name, value);
    }

    public void __delattr__(String name) {
        module___delattr__(name);
    }

    @ExposedMethod
    final void module___delattr__(String name) {
        super.__delattr__(name);
    }

    public String toString()  {
        return module_toString().toString();
    }

    @ExposedMethod(names = {"__repr__"})
    final PyObject module_toString()  {
        return Py.getSystemState().importlib.invoke("_module_repr", this);
    }

    public PyObject __dir__() {
        // Some special casing to ensure that classes deriving from PyModule
        // can use their own __dict__. Although it would be nice to do this in
        // PyModuleDerived, current templating in gderived.py does not support
        // including from object, then overriding a specific method.
        PyObject d;
        if (this instanceof PyModuleDerived) {
            d = __findattr_ex__("__dict__");
        } else {
            d = __dict__;
        }
        if (d == null || d.__finditem__("__name__") == null) {
            throw Py.SystemError("nameless module");
        }
        if (!(d instanceof PyDictionary ||
                  d instanceof PyStringMap ||
                  d instanceof PyDictProxy)) {
            throw Py.TypeError(String.format("%.200s.__dict__ is not a dictionary",
                    getType().fastGetName().toLowerCase()));
        }
        return d.invoke("keys");
    }

    private void ensureDict() {
        if (__dict__ == null) {
            __dict__ = new PyStringMap();
        }
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        return __dict__ == null ? 0 : visit.visit(__dict__, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && ob == __dict__;
    }

    @Override
    public void noAttributeError(String name) {
        if (__dict__ == null) {
            throw Py.AttributeError(String.format("module has no attribute '%.400s'", name));
        }
        throw Py.AttributeError(String.format("module '%.50s' has no attribute '%.400s'",
                __dict__.__finditem__("__name__"), name));
    }

}
