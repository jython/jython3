/* Copyright (c) 2008 Jython Developers */
package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * Readonly proxy for dictionaries (actually any mapping).
 *
 */
@ExposedType(name = "mappingproxy", isBaseType = false)
public class PyDictProxy extends PyObject implements Traverseproc {

    /** The dict proxied to. */
    PyObject dict;

    public PyDictProxy(PyObject dict) {
        super();
        this.dict = dict;
    }

    @ExposedNew
    static PyObject mappingproxy_new(PyNewWrapper new_, boolean init, PyType subtype, PyObject[] args,
                            String[] keywords) {
        ArgParser ap = new ArgParser("mappingproxy", args, keywords, new String[] {"object"}, 0);
        PyObject d = ap.getPyObject(0);
        return new PyDictProxy(d);
    }

    @Override
    public PyObject __iter__() {
        return dict.__iter__();
    }

    @Override
    public PyObject __finditem__(PyObject key) {
        return dict.__finditem__(key);
    }

    @Override
    public int __len__() {
        return dict.__len__();
    }

    @ExposedMethod
    public PyObject mappingproxy___getitem__(PyObject key) {
        return dict.__getitem__(key);
    }

    @ExposedMethod
    public boolean mappingproxy___contains__(PyObject value) {
        return dict.__contains__(value);
    }

    @ExposedMethod
    public boolean mappingproxy_has_key(PyObject key) {
        return dict.__contains__(key);
    }

    @ExposedMethod(defaults = "Py.None")
    public PyObject mappingproxy_get(PyObject key, PyObject default_object) {
        return dict.invoke("get", key, default_object);
    }

    @ExposedMethod
    public PyObject mappingproxy_keys() {
        return dict.invoke("keys");
    }

    @ExposedMethod
    public PyObject mappingproxy_values() {
        return dict.invoke("values");
    }

    @ExposedMethod
    public PyObject mappingproxy_items() {
        return dict.invoke("items");
    }

    @ExposedMethod
    public PyObject mappingproxy_iterkeys() {
        return dict.invoke("iterkeys");
    }

    @ExposedMethod
    public PyObject mappingproxy_itervalues() {
        return dict.invoke("itervalues");
    }

    @ExposedMethod
    public PyObject mappingproxy_iteritems() {
        return dict.invoke("iteritems");
    }

    @ExposedMethod
    public PyObject mappingproxy_copy() {
        return dict.invoke("copy");
    }

    @ExposedMethod(type = MethodType.BINARY)
    public PyObject mappingproxy___lt__(PyObject other) {
        return dict.__lt__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    public PyObject mappingproxy___le__(PyObject other) {
        return dict.__le__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    public PyObject mappingproxy___eq__(PyObject other) {
        return dict.__eq__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    public PyObject mappingproxy___ne__(PyObject other) {
        return dict.__ne__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    public PyObject mappingproxy___gt__(PyObject other) {
        return dict.__gt__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    public PyObject mappingproxy___ge__(PyObject other) {
        return dict.__ge__(other);
    }

    @Override
    @ExposedMethod
    public PyUnicode __str__() {
        return dict.__str__();
    }

    @Override
    public boolean isMappingType() {
        return true;
    }

    @Override
    public boolean isSequenceType() {
        return false;
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        return dict == null ? 0 : visit.visit(dict, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && ob == dict;
    }
}
