package org.python.core;

import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

import java.io.Serializable;

// XXX: isBaseType = false
@Untraversable
@ExposedType(name = "NotImplemented", base = PyObject.class, isBaseType = false, doc = BuiltinDocs.NotImplementedType_doc)
public class PyNotImplemented extends PySingleton implements Serializable
{
    public static final PyType TYPE = PyType.fromClass(PyNotImplemented.class);
    private static PyNotImplemented INST = new PyNotImplemented();

    private PyNotImplemented() {
        super(TYPE, "NotImplemented");
    }

    @ExposedNew
    final static PyObject NotImplemented_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        return INST;
    }

    public static PyNotImplemented getInstance() {
        return INST;
    }

    public Object __tojava__(Class c) {
        // Danger here. java.lang.Object gets null not None
        if (c == PyObject.class) {
            return this;
        }
        if (c.isPrimitive()) {
            return Py.NoConversion;
        }
        return null;
    }

    public boolean isMappingType() { return false; }
    public boolean isSequenceType() { return false; }


    private Object writeReplace() {
        return new Py.SingletonResolver("NotImplemented");
    }
    
}

