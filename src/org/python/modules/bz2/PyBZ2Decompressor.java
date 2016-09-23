package org.python.modules.bz2;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyByteArray;
import org.python.core.PyBytes;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "bz2.BZ2Decompressor")
public class PyBZ2Decompressor extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyBZ2Decompressor.class);

    private byte[] unusedData = new byte[0];

    @ExposedGet
    public boolean needs_input = false;

    @ExposedGet(name = "eof")
    public boolean eofReached = false;

    private byte[] accumulator = new byte[0];

    public PyBZ2Decompressor() {
        super(TYPE);
    }

    public PyBZ2Decompressor(PyType objtype) {
        super(objtype);
    }

    @ExposedNew
    @ExposedMethod
    final void BZ2Decompressor___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("bz2decompressor", args, kwds,
                new String[0], 0);
    }

    @ExposedMethod
    final PyObject BZ2Decompressor_decompress(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("compress", args, kwds,
                new String[] { "data", "max_length" }, 1);
        PyObject data = ap.getPyObject(0);
        int maxLength = ap.getInt(1, -1);
        PyObject returnData;
        if (eofReached) {
            throw Py.EOFError("Data stream EOF reached");
        }

        byte[] indata = Py.unwrapBuffer(data);
        if (unusedData.length > 0) {
            byte[] tmp = indata;
            indata = new byte[unusedData.length + tmp.length];
            System.arraycopy(unusedData, 0, indata, 0, unusedData.length);
            System.arraycopy(tmp, 0, indata, unusedData.length, tmp.length);
        }
        ByteArrayInputStream compressedData = new ByteArrayInputStream(indata);
        PyByteArray databuf = new PyByteArray();
        int currentByte;
        try {
            BZip2CompressorInputStream decompressStream = new BZip2CompressorInputStream(compressedData);
            while ((currentByte = decompressStream.read()) != -1 && (maxLength < 0 || databuf.__len__() < maxLength)) {
                databuf.append((byte)currentByte);
            }
            returnData = databuf;
            if (compressedData.available() > 0) {
                unusedData = new byte[compressedData.available()];
                compressedData.read(unusedData);
            }
            if (databuf.__len__() == 0 && unusedData.length > 0) {
                needs_input = true;
            } else {
                eofReached = true;
            }
        } catch (IOException e) {
            if (databuf.__len__() == 0) {
                needs_input = true;
            } else {
                eofReached = true;
            }
            return Py.EmptyByte;
        }

        return returnData;
    }

    @ExposedGet(name = "unused_data")
    public PyObject getUnusedData() {
        return new PyBytes(unusedData);
    }
}
