package org.python.modules.zlib;

import com.jcraft.jzlib.JZlib;
import org.python.core.ArgParser;
import org.python.core.BufferProtocol;
import org.python.core.Py;
import org.python.core.PyBytes;
import org.python.core.PyException;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.expose.ExposedConst;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;
import org.python.expose.ModuleInit;
import org.python.modules.binascii;

import java.util.zip.Adler32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

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
    public static final int Z_DEFAULT_COMPRESSION = Deflater.DEFAULT_COMPRESSION;
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

    @ExposedConst
    public static final String ZLIB_VERSION = "1.2.8";
    @ExposedConst
    public static final String ZLIB_RUNTIME_VERSION = "1.2.8";

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
        long start = ap.getLong(1, 1);
        PyObject data = ap.getPyObject(0);
        if (data instanceof BufferProtocol) {
            Adler32 checksum = new Adler32();
            byte[] bytes = Py.unwrapBuffer(data);
            checksum.update(bytes);
            long result = checksum.getValue();
            if (start != 1) {
                result = JZlib.adler32_combine(start, result, bytes.length);
            }
            return new PyLong(result);
        }
        throw Py.TypeError(String.format("a bytes-like object expected, not '%s'", data.getType().getName()));
    }

    @ExposedFunction
    public static final long crc32(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("crc32", args, keywords, "data", "value");
        PyObject data = ap.getPyObject(0);
        long value = ap.getLong(1, 0);
        return binascii.crc32(data, value);
    }

    @ExposedFunction
    public static final PyObject compress(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("compress", args, keywords, "bytes", "level");
        PyObject bytes = ap.getPyObject(0);
        int level = ap.getInt(1, 6); // not used, always 1
        if (level > 9 || level < 0) {
            throw new PyException(error, "Bad compress level");
        }
        Deflater deflater = new Deflater(level);
        deflater.setInput(Py.unwrapBuffer(bytes));
        return deflate(deflater, Deflater.SYNC_FLUSH);
    }

    @ExposedFunction
    public static final PyObject decompress(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("decompress", args, keywords, "data", "wbits", "bufsize");
        PyObject data = ap.getPyObject(0);
        // unused
        int wbits = ap.getInt(1, 15);
        int bufsize = ap.getInt(2, 16384);
        Inflater inflater = new Inflater();
        inflater.setInput(Py.unwrapBuffer(data));
        return inflate(inflater);
    }

    protected static final PyBytes deflate(Deflater deflater, int mode) {
        byte[] buf = new byte[DEF_BUF_SIZE];
        deflater.finish();
        int len = deflater.deflate(buf, 0, buf.length, mode);
        int totalLen = len;
        while (len == DEF_BUF_SIZE) {
            byte[] tmp = buf;
            buf = new byte[tmp.length + DEF_BUF_SIZE ];
            System.arraycopy(tmp, 0, buf, 0, tmp.length);
            len = deflater.deflate(buf, tmp.length, DEF_BUF_SIZE, mode);
            totalLen += len;
            if (len < DEF_BUF_SIZE) break;
        }
        deflater.end();
        return new PyBytes(buf, 0, totalLen);
    }

    protected static final PyBytes inflate(Inflater inflater) {
        byte[] buf = new byte[DEF_BUF_SIZE];
        try {
            int len = inflater.inflate(buf);
            int totalLen = len;
            while (len == DEF_BUF_SIZE) {
                byte[] tmp = buf;
                buf = new byte[tmp.length + DEF_BUF_SIZE];
                System.arraycopy(tmp, 0, buf, 0, tmp.length);
                len = inflater.inflate(buf, tmp.length, DEF_BUF_SIZE);
                totalLen += len;
                if (len < DEF_BUF_SIZE) break;
            }
            inflater.end();
            return new PyBytes(buf, 0, totalLen);
        } catch (DataFormatException e) {
            throw new PyException(error, e.getMessage());
        }
    }

    protected static final void validateWbits(int wbits) {
        if ((wbits > -9 && wbits < 0) || (wbits >= 0 && wbits < 9) || wbits > MAX_WBITS) {
            throw Py.ValueError("Invalid initialization option");
        }
    }
}
