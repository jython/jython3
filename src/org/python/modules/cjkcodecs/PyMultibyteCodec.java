package org.python.modules.cjkcodecs;

import org.python.core.BufferProtocol;
import org.python.core.Py;
import org.python.core.PyBUF;
import org.python.core.PyBuffer;
import org.python.core.PyBytes;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

@ExposedType(name = "MultibyteCodec")
public class PyMultibyteCodec extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyMultibyteCodec.class);
    private Charset charset;

    public PyMultibyteCodec(Charset charset) {
        super(TYPE);
        this.charset = charset;
    }

    @ExposedMethod
    public PyObject MultibyteCodec_encode(PyObject o) {
        if (o instanceof PyUnicode) {
            String s = o.toString();
            ByteBuffer buf = charset.encode(s);
            return new PyTuple(new PyBytes(buf), new PyLong(s.length()));
        }
        throw Py.TypeError("expected a str object");
    }

    @ExposedMethod
    public PyObject MultibyteCodec_decode(PyObject o) {
        if (o instanceof BufferProtocol) {
            PyBuffer buf = ((BufferProtocol) o).getBuffer(PyBUF.FULL_RO);
            return new PyTuple(new PyUnicode(charset.decode(buf.getNIOByteBuffer())), new PyLong(o.__len__()));
        }
        throw Py.TypeError("expected a bytes object");
    }
}
