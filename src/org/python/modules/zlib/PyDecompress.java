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
    private byte[] input;

    @ExposedGet
    public PyObject unused_data;

    @ExposedGet
    public PyObject unconsumed_tail;

    protected PyDecompress(PyObject zdict) {
        inflater = new Inflater();
        unused_data = Py.EmptyByte;
        unconsumed_tail = Py.EmptyByte;
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
        if (maxLenObj != Py.None && maxLength < 0) {
            throw Py.ValueError("max_length must be positive");
        }
        unconsumed_tail = Py.EmptyByte;
        input = Py.unwrapBuffer(data);
        if (input.length > 0)
            inflater.setInput(input);
        if (maxLength == 0) {
            return Py.EmptyByte;
        } else if (maxLength == -1) {
            return _flush(ZlibModule.DEF_BUF_SIZE);
        }
        byte[] buf = new byte[Math.min(maxLength, ZlibModule.DEF_BUF_SIZE)];
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
            while (totalLen < maxLength) {
                int length = Math.min(maxLength - totalLen, ZlibModule.DEF_BUF_SIZE);
                byte[] tmp = buf;
                buf = new byte[tmp.length + length];
                System.arraycopy(tmp, 0, buf, 0, tmp.length);
                len = inflater.inflate(buf, tmp.length, length);
                totalLen += len;
                if (len < length) break;
            }
            int remaining = inflater.getRemaining();
            if (remaining > 0) {
                if (maxLength > 0 && !inflater.finished()) {
                    if (input.length > remaining) {
                        unconsumed_tail = new PyBytes(input, input.length - remaining, input.length);
                    }
                } else {
                    unused_data = unused_data.__add__(new PyBytes(input, input.length - remaining, input.length));
                }
            }
            if (totalLen == 0) {
                return Py.EmptyByte;
            }
            return new PyBytes(buf, 0, totalLen);
        } catch (DataFormatException e) {
            throw new PyException(ZlibModule.error, e.getMessage());
        }
    }

    @ExposedMethod
    public PyObject Decompress_flush(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("flush", args, keywords, "length");
        // clear the input buf, because the flush logic is shared with decompress
        input = new byte[0];
        int length = ap.getInt(0, ZlibModule.DEF_BUF_SIZE);
        if (length <= 0) {
            throw Py.ValueError("length must be greater than zero");
        }
        return _flush(length);
    }


    @ExposedGet(name = "eof")
    public boolean Decompress_eof() {
        return inflater.finished();
    }

    private PyObject _flush(int length) {
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
            int remaining = inflater.getRemaining();
            if (input.length > 0 && remaining > 0) {
                unused_data = unused_data.__add__(new PyBytes(input, input.length - remaining, input.length));
            }
            return new PyBytes(buf, 0, totalLen);
        } catch (DataFormatException e) {
            throw new PyException(ZlibModule.error, e.getMessage());
        }
    }
}
