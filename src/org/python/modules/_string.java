package org.python.modules;

import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.stringlib.FieldNameIterator;
import org.python.core.stringlib.MarkupIterator;
import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedModule;

/**
 * Created by isaiah on 4/25/16.
 */
@ExposedModule(doc = "string helper module")
public class _string {
    @ExposedClassMethod(doc = "parse the argument as a format string")
    public static PyObject formatter_parser(PyType self, PyObject str) {
        return new MarkupIterator((PyString) str);
    }

    @ExposedClassMethod(doc = "split the argument as a field name")
    public static PyObject formatter_field_name_split(PyType self, PyObject str) {
        FieldNameIterator iterator = new FieldNameIterator((PyString) str);
        return new PyTuple(iterator.pyHead(), iterator);
    }
}
