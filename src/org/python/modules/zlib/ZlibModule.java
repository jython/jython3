package org.python.modules.zlib;

import org.python.core.ArgParser;
import org.python.core.BufferProtocol;
import org.python.core.Py;
import org.python.core.PyBUF;
import org.python.core.PyBuffer;
import org.python.core.PyBytes;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.expose.ExposedConst;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;
import org.python.expose.ModuleInit;
import org.python.modules.binascii;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Adler32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

@ExposedModule(name = "zlib")
public class ZlibModule {

    @ExposedConst
    public static final int DEFLATED = Deflater.DEFLATED;

    @ExposedConst
    public static final int DEF_BUF_SIZE = 16384;

    @ExposedConst
    public static final int DEF_MEM_LEVEL = 8;

    @ExposedConst
    public static final int MAX_WBITS = 15;

    @ExposedConst
    public static final int Z_DEFAULT_STRATEGY = Deflater.DEFAULT_STRATEGY;

    @ExposedConst
    public static final int Z_FILTERED = Deflater.FILTERED;

    @ExposedConst
    public static final int Z_HUFFMAN_ONLY = Deflater.HUFFMAN_ONLY;

    @ExposedConst
    public static final int Z_BEST_COMPRESSION = Deflater.BEST_COMPRESSION;
    @ExposedConst
    public static final int Z_BEST_SPEED = Deflater.BEST_SPEED;

    @ExposedConst
    public static final int Z_NO_FLUSH = Deflater.NO_FLUSH;
    @ExposedConst
    public static final int Z_SYNC_FLUSH = Deflater.SYNC_FLUSH;
    @ExposedConst
    public static final int Z_FULL_FLUSH = Deflater.FULL_FLUSH;
    @ExposedConst
    public static final int Z_FINISH = 4;

    public static final PyObject error = Py.makeClass("zlib.error", Py.BaseException, new PyStringMap());

    @ModuleInit
    public static void init(PyObject dict) {
        dict.__setitem__("error", error);
        dict.__setitem__("compressobj", PyCompress.TYPE);
        dict.__setitem__("decompressobj", PyDecompress.TYPE);
    }

    @ExposedFunction
    public static final PyObject adler32(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("adler32", args, keywords, "data", "value");
        int value = ap.getInt(1, 1); // not used, always 1
        PyObject data = ap.getPyObject(0);
        if (data instanceof BufferProtocol) {
            Adler32 checksum = new Adler32();
            checksum.update(((BufferProtocol) data).getBuffer(PyBUF.SIMPLE).getNIOByteBuffer());
            return new PyLong(checksum.getValue());
        }
        throw Py.TypeError(String.format("a bytes-like object expected, not '%s'", data.getType().getName()));
    }

    @ExposedFunction
    public static final int crc32(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("crc32", args, keywords, "data", "value");
        PyObject data = ap.getPyObject(0);
        int value = ap.getInt(1, 1); // not used, always 1
        return binascii.crc32(data, value);
    }

    @ExposedFunction
    public static final PyObject compress(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("compress", args, keywords, "bytes", "level");
        PyObject bytes = ap.getPyObject(0);
        int level = ap.getInt(1, 6); // not used, always 1
        Deflater deflater = new Deflater(level);
        PyBuffer buffer = ((BufferProtocol) bytes).getBuffer(PyBUF.SIMPLE);
        try {
            byte[] buf = new byte[buffer.getLen()];
            buffer.copyTo(buf, 0);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DeflaterOutputStream out = new DeflaterOutputStream(byteArrayOutputStream, deflater);
            out.write(buf);
            out.close();
            return new PyBytes(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            throw Py.IOError(e);
        } finally {
            buffer.release();
        }
    }

    @ExposedFunction
    public static final PyObject decompress(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("decompress", args, keywords, "data", "wbits", "bufsize");
        PyObject data = ap.getPyObject(0);
        // unused
        int wbits = ap.getInt(1, 15);
        int bufsize = ap.getInt(2, 16384);
        PyBuffer buffer = ((BufferProtocol) data).getBuffer(PyBUF.SIMPLE);
        byte[] buf = new byte[buffer.getLen()];
        buffer.copyTo(buf, 0);
        Inflater inflater = new Inflater();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        InflaterOutputStream outputStream = new InflaterOutputStream(byteArrayOutputStream);
        try {
            outputStream.write(buf);
            outputStream.close();
            return new PyBytes(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            throw Py.IOError(e);
        } finally {
            buffer.release();
        }
    }
}
