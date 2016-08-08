//Copyright (c) Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.BuiltinDocs;
import org.python.core.ClassDictInit;
import org.python.core.PyArray;
import org.python.core.PyBytes;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.expose.ExposedConst;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;
import org.python.expose.ModuleInit;

/**
 * The python array module, plus jython extensions from jarray.
 */
@ExposedModule(name = "array", doc = BuiltinDocs.array_doc)
public class ArrayModule {

    enum machine_format_code {
        UNSIGNED_INT8(0),
        SIGNED_INT8(1),
        UNSIGNED_INT16_LE(2),
        UNSIGNED_INT16_BE(3),
        SIGNED_INT16_LE(4),
        SIGNED_INT16_BE(5),
        UNSIGNED_INT32_LE(6),
        UNSIGNED_INT32_BE(7),
        SIGNED_INT32_LE(8),
        SIGNED_INT32_BE(9),
        UNSIGNED_INT64_LE(10),
        UNSIGNED_INT64_BE(11),
        SIGNED_INT64_LE(12),
        SIGNED_INT64_BE(13),
        IEEE_754_FLOAT_LE(14),
        IEEE_754_FLOAT_BE(15),
        IEEE_754_DOUBLE_LE(16),
        IEEE_754_DOUBLE_BE(7),
        UTF16_LE(18),
        UTF16_BE(19),
        UTF32_LE(20),
        UTF32_BE(21);

        private int n;
        machine_format_code(int x) {
            n = x;
        }
    }
    @ExposedConst(name = "typecodes")
    public static final String TYPE_CODES = "bBuhHiIlLqQfd";

    @ModuleInit
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("array", PyType.fromClass(PyArray.class));
        dict.__setitem__("ArrayType", PyType.fromClass(PyArray.class));
    }

    @ExposedFunction
    public static final PyObject _array_reconstructor(PyObject arraytype, PyObject typecode, PyObject mformat_code,
                                                      PyObject items) {
        // TODO: currently a placeholder to make the tests run 
        return new PyArray((PyType) arraytype);
    }

    /*
     * These are jython extensions (from jarray module).
     * Note that the argument order is consistent with
     * python array module, but is reversed from jarray module.
     */
    public static PyArray zeros(char typecode, int n) {
        return PyArray.zeros(n, typecode);
    }

    public static PyArray zeros(Class type, int n) {
        return PyArray.zeros(n, type);
    }
}
