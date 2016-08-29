package org.python.modules.zlib;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyBytes;
import org.python.core.PyException;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static org.python.modules.zlib.ZlibModule.validateWbits;

@ExposedType(name = "zlib.Decompress")
public class PyDecompress extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyDecompress.class);

    private Inflater inflater;
    // the dict can only be used when input is not empty
    private byte[] dict = null;

    protected PyDecompress(PyObject zdict) {
        inflater = new Inflater();
        if (zdict != Py.None) {
            dict = Py.unwrapBuffer(zdict);
        }
    }
    @ExposedNew
    final static PyObject Decompress_new(PyNewWrapper new_, boolean init, PyType subtype,
                                       PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("Decompress", args, keywords, "wbits", "zdict");
        int wbits = ap.getInt(0, 15);
        validateWbits(wbits);
        PyObject zdict = ap.getPyObject(1, Py.None);
        return new PyDecompress(zdict);
    }

    @ExposedMethod
    public PyObject Decompress_decompress(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("decompress", args, keywords, "data", "max_length");
        PyObject data = ap.getPyObject(0);
        PyObject maxLenObj = ap.getPyObject(1, Py.None);
        int maxLength = maxLenObj == Py.None ? -1 : maxLenObj.asInt();
        if (maxLenObj != Py.None) {
            if (maxLength < 0) {
                throw Py.ValueError("value must be positive");
            }
        }
        byte[] input = Py.unwrapBuffer(data);
        if (input.length > 0)
            inflater.setInput(input);
        if (maxLength == 0) {
            return Py.EmptyByte;
        } else if (maxLength == -1) {
            return Decompress_flush(Py.EmptyObjects, Py.NoKeywords);
        }
        byte[] buf = new byte[ZlibModule.DEF_BUF_SIZE];
        try {
            int len;
            int totalLen = 0;
            do {
                len = inflater.inflate(buf);
                if (len == 0 && inflater.needsDictionary()) {
                    if (dict == null) {
                        throw new PyException(ZlibModule.error, "Error 2 while decompressing data");
                    }
                    inflater.setDictionary(dict);
                    len = inflater.inflate(buf);
                }
                totalLen += len;

                if (totalLen == 0) {
                    return Py.EmptyByte;
                }
            } while (len > 0 && totalLen < maxLength);
            return new PyBytes(buf, 0, totalLen);
        } catch (DataFormatException e) {
            throw new PyException(ZlibModule.error, e.getMessage());
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
            if (len == 0 && inflater.needsDictionary()) {
                if (dict == null) {
                    throw new PyException(ZlibModule.error, "Error 2 while decompressing data");
                }
                inflater.setDictionary(dict);
                len = inflater.inflate(buf);
            }
            int totalLen = len;
            while (len == length) {
                byte[] tmp = buf;
                buf = new byte[tmp.length + length];
                System.arraycopy(tmp, 0, buf, 0, tmp.length);
                len = inflater.inflate(buf, tmp.length, length);
                totalLen += len;
                if (len < length) break;
            }
            return new PyBytes(buf, 0, totalLen);
        } catch (DataFormatException e) {
            throw new PyException(ZlibModule.error, e.getMessage());
        }
    }

    // both methods doesn't make much sense to support in java
    @ExposedGet(name = "unused_data")
    public PyObject Decompress_unused_data() {
        return Py.EmptyByte;
    }

    @ExposedGet(name = "unconsumed_tail")
    public PyObject Decompress_uncomsumed_tail() {
        return Py.EmptyByte;
    }

    @ExposedGet(name = "eof")
    public boolean Decompress_eof() {
        return inflater.finished();
    }
}
