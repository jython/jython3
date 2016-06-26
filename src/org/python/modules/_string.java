package org.python.modules;

import org.python.core.BuiltinDocs;
import org.python.core.ClassDictInit;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyUnicode;
import org.python.core.stringlib.FieldNameIterator;
import org.python.core.stringlib.MarkupIterator;

/**
 * Created by isaiah on 4/25/16.
 */
public class _string implements ClassDictInit {

    public static final PyUnicode __doc__ = new PyUnicode("string helper module");

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyUnicode("_string"));
        dict.__setitem__("__doc__", __doc__);

        // Hide from Python
        dict.__setitem__("classDictInit", null);
    }

    public static PyUnicode __doc__formatter_parser = new PyUnicode("'parse the argument as a format string'");
    public static PyObject formatter_parser(PyString str) {
        return new MarkupIterator(str);
    }

    public static PyUnicode __doc__formatter_field_name_split = new PyUnicode("split the argument as a field name");
    public static PyObject formatter_field_name_split(PyString str) {
        FieldNameIterator iterator = new FieldNameIterator(str);
        return new PyTuple(iterator.pyHead(), iterator);
    }
}
