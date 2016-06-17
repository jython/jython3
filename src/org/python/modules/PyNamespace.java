package org.python.modules;

import org.python.core.BuiltinDocs;
import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

import java.util.Map;

/**
 * namespace object implementation
 */
@ExposedType(name = "namespace", doc = BuiltinDocs.SimpleNamespace___class___doc)
public class PyNamespace extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyNamespace.class);

    @ExposedGet(name = "__dict__")
    public Map<String, PyObject> dict;

    public PyNamespace(Map<String, PyObject> dict) {
        super();
        this.dict = dict;
    }

    @Override
    public String toString() {
        Iterable<String> keys = dict.keySet();
        StringBuilder items = new StringBuilder();
        for (String key : dict.keySet()) {
            items.append(String.format("%s=%s", key, dict.get(key)));
            items.append(", ");
        }
        return String.format("%s(%s)", getType().fastGetName(), items.substring(0, items.length() - 2));
    }

    final PyObject namespace___eq__(PyObject other) {
        return Py.newBoolean(dict.equals(other.__getattr__("__dict__")));
    }

    @ExposedMethod(doc = BuiltinDocs.SimpleNamespace___getattribute___doc)
    final PyObject namespace___getattribute__(PyObject name) {
        return dict.get(name.asString());
    }

    @ExposedMethod(doc = BuiltinDocs.SimpleNamespace___setattr___doc)
    final PyObject namespace___setattr__(PyObject name, PyObject value) {
        dict.put(name.asString(), value);
        return Py.None;
    }
}
