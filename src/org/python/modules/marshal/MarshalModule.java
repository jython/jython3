package org.python.modules.marshal;

import java.io.ByteArrayInputStream;

import org.python.core.PyFile;
import org.python.core.PyObject;
import org.python.core.PyBytes;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;
import org.python.modules._io.PyStringIO;
import org.python.modules.cStringIO;

@ExposedModule(name = "marshal")
public class MarshalModule {
    final static char TYPE_NULL = '0';
    final static char TYPE_NONE = 'N';
    final static char TYPE_FALSE = 'F';
    final static char TYPE_TRUE = 'T';
    final static char TYPE_STOPITER = 'S';
    final static char TYPE_ELLIPSIS = '.';
    final static char TYPE_INT = 'i';
    final static char TYPE_INT64 = 'I';
    final static char TYPE_FLOAT = 'f';
    final static char TYPE_BINARY_FLOAT = 'g';
    final static char TYPE_COMPLEX = 'x';
    final static char TYPE_BINARY_COMPLEX = 'y';
    final static char TYPE_LONG = 'l';
    final static char TYPE_STRING = 's';
    final static char TYPE_INTERNED = 't';
    final static char TYPE_STRINGREF = 'R';
    final static char TYPE_TUPLE = '(';
    final static char TYPE_LIST = '[';
    final static char TYPE_DICT = '{';
    final static char TYPE_CODE = 'c';
    final static char TYPE_UNICODE = 'u';
    final static char TYPE_UNKNOWN = '?';
    final static char TYPE_SET = '<';
    final static char TYPE_FROZENSET = '>';
    final static int MAX_MARSHAL_STACK_DEPTH = 2000;
    final static int CURRENT_VERSION = 2;

    @ExposedFunction(defaults = {"2"})
    public static void dump(PyObject value, PyObject file, int version) {
        new PyMarshaller(file).dump(value);
    }

    @ExposedFunction(defaults = {"2"})
    public static PyObject dumps(PyObject value, int version) {
        PyStringIO f = new PyStringIO();
        dump(value, f, version);
        return f.getvalue();
    }

    @ExposedFunction
    public static PyObject load(PyObject f) {
        return new PyUnmarshaller(f).load();
    }

    @ExposedFunction
    public static PyObject loads(PyObject bytes) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(((PyBytes) bytes).getString().getBytes());
        PyObject f = new PyFile(inputStream);
        return new PyUnmarshaller(f).load();
    }
}
