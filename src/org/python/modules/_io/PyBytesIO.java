package org.python.modules._io;

import org.python.core.Py;
import org.python.core.PyBytes;
import org.python.core.PyDictionary;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

/**
 * XXX This is not a complete implementation
 * it's here to support the importlib bootstrapping
 * see Lib/importlib/_bootstrap_external.py#decode_source
 */
@ExposedType(name = "BytesIO")
public class PyBytesIO extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyBytesIO.class);

    private String[] value;
    private int lineno = 0;
    private int pos;
    private PyDictionary dict;

    public PyBytesIO(PyObject bytes) {
        super(TYPE);
        value = ((PyBytes) bytes).getString().split("\n");
        dict = new PyDictionary();
    }

    @ExposedNew
    static PyObject BytesIO___new__(PyNewWrapper new_, boolean init, PyType subtype,
                                   PyObject[] args, String[] keywords) {
        return new PyBytesIO(args[0]);
    }

    @ExposedMethod
    public PyObject BytesIO_readline() {
        return new PyBytes(value[lineno++]);
    }

    /**
     * pickle protocol
     * @param state the 3 tuple with (value, position, dict)
     * @return
     */
    @ExposedMethod
    public PyObject BytesIO___setstate__(PyObject state) {
        if (!(state instanceof PyTuple) || state.__len__() < 3) {
            throw Py.ErrFormat(Py.TypeError, "%.200s.__setstate__ argument should be 3-tuple, got %.200s",
                    getType().fastGetName(),
                    state.getType().fastGetName());
        }
        PyTuple tup = (PyTuple) state;
        value = new String(Py.unwrapBuffer(tup.pyget(0))).split("\n");
        pos = tup.pyget(1).asInt();
        PyObject dict = tup.pyget(2);
        if (dict != Py.None) {
            if (dict instanceof PyDictionary || dict instanceof PyStringMap){
                this.dict.merge(dict);
            }
            throw Py.ErrFormat(Py.TypeError, "third item of state should be a dict, got a %.200s",
                    dict.getType().fastGetName());
        }
        return Py.None;
    }
}
