package org.python.modules.bz2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

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
import org.python.core.Traverseproc;
import org.python.core.Visitproc;

@ExposedType(name = "bz2.BZ2Decompressor")
public class PyBZ2Decompressor extends PyObject implements Traverseproc {
    public static final PyType TYPE = PyType.fromClass(PyBZ2Decompressor.class);

    @ExposedGet
    public PyObject unused_data = Py.EmptyByte;

    @ExposedGet(name = "eof")
    public boolean eofReached = false;

    private BZip2CompressorInputStream decompressStream = null;

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
        PyObject returnData;
        if (eofReached) {
            throw Py.EOFError("Data stream EOF reached");
        }

        byte[] indata = Py.unwrapBuffer(data);
        if (indata.length > 0) {
            ByteBuffer bytebuf = ByteBuffer.allocate(accumulator.length
                    + indata.length);
            bytebuf.put(accumulator);
            bytebuf.put(indata);
            accumulator = bytebuf.array();
        }

        ByteArrayOutputStream decodedStream = new ByteArrayOutputStream();
        final byte[] buf = accumulator;
        for (int i = 0; i < buf.length; i++) {
            if (((i + 3) < buf.length) &&
                (((char) buf[i] == '\\') && ((char) buf[i + 1] == 'x'))) {
                int decodedByte = ((Character.digit((char) buf[i + 2], 16) << 4) + Character
                        .digit((char) buf[i + 3], 16));
                decodedStream.write(decodedByte);
                i += 3;
            } else {
                decodedStream.write(buf[i]);
            }
        }

        ByteArrayInputStream compressedData = new ByteArrayInputStream(
                decodedStream.toByteArray());

        try {
            decompressStream = new BZip2CompressorInputStream(compressedData);
        } catch (IOException e) {
            return Py.EmptyByte;
        }

        PyByteArray databuf = new PyByteArray();
        int currentByte = -1;
        try {
            while ((currentByte = decompressStream.read()) != -1) {
                databuf.append((byte)currentByte);
            }
            returnData = databuf;
            if (compressedData.available() > 0) {
                byte[] unusedbuf = new byte[compressedData.available()];
                compressedData.read(unusedbuf);
                unused_data = unused_data.__add__(new PyBytes(unusedbuf));
            }
            eofReached = true;
        } catch (IOException e) {
            return Py.EmptyByte;
        }

        return returnData;
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        return unused_data != null ? visit.visit(unused_data, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && unused_data == ob;
    }
}
