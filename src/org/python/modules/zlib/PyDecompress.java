package org.python.modules.zlib;

import org.python.core.ArgParser;
import org.python.core.BufferProtocol;
import org.python.core.Py;
import org.python.core.PyBytes;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

@ExposedType(name = "zlib.Decompress")
public class PyDecompress extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyDecompress.class);

    private Inflater inflater;

    protected PyDecompress(PyObject zdict) {
        inflater = new Inflater();
        if (zdict instanceof BufferProtocol) {
            inflater.setDictionary(Py.unwrapBuffer(zdict));
        }
    }
    @ExposedNew
    final static PyObject Compress_new(PyNewWrapper new_, boolean init, PyType subtype,
                                       PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("Decompress", args, keywords, "wbits", "zdict");
        int wbits = ap.getInt(0, 15);
        if (wbits < 0) {
            throw Py.ValueError("Invalid initialization option");
        }
        PyObject zdict = ap.getPyObject(1, Py.None);
        return new PyDecompress(zdict);
    }

    @ExposedMethod
    public PyObject Decompress_decompress(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("decompress", args, keywords, "data", "max_length");
        PyObject data = ap.getPyObject(0);
        int maxLength = ap.getInt(1, ZlibModule.DEF_BUF_SIZE);
        byte[] buf = new byte[maxLength];
        inflater.setInput(Py.unwrapBuffer(data));
        try {
            int len = inflater.inflate(buf);
            return new PyBytes(buf, 0, len);
        } catch (DataFormatException e) {
            throw Py.ValueError(e.getMessage());
        }
    }

    @ExposedMethod
    public PyObject Decompress_flush(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("flush", args, keywords, "length");
        int length = ap.getInt(0, ZlibModule.DEF_BUF_SIZE);
        if (length <= 0) {
            throw Py.ValueError("length must be greater than zero");
        }
        byte[] buf = new byte[length];
        try {
            int len = inflater.inflate(buf);
            int totalLen = len;
            while (len == length) {
                byte[] tmp = buf;
                buf = new byte[tmp.length * 2];
                System.arraycopy(tmp, 0, buf, 0, tmp.length);
                len = inflater.inflate(buf, tmp.length, length);
                totalLen += len;
                if (len < length) break;
            }
            return new PyBytes(buf, 0, totalLen);
        } catch (DataFormatException e) {
            throw Py.ValueError(e.getMessage());
        }
    }

    // both methods doesn't make much sense to support in java
    @ExposedMethod(names = {"unused_data", "uncomsumed_tail"})
    public PyObject Decompress_unused_data() {
        return Py.EmptyByte;
    }

    @ExposedMethod
    public boolean Decompress_eof() {
        return inflater.finished();
    }
}
