package org.python.modules._io;

import com.google.common.io.ByteStreams;
import org.python.core.BuiltinDocs;
import org.python.core.Py;
import org.python.core.PyBytes;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

@ExposedType(name = "_io.BufferedReader")
public class PyBufferedReader extends PyObject {
    private BufferedInputStream input;
    private FileChannel fileChannel;

    public PyBufferedReader(InputStream in) {
        super(TYPE);
        input = new BufferedInputStream(in);
        if (in instanceof FileInputStream) {
            fileChannel = ((FileInputStream) in).getChannel();
        }
    }

    public PyBufferedReader(File file) {
        super(TYPE);
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            input = new BufferedInputStream(fileInputStream);
            fileChannel = fileInputStream.getChannel();
        } catch (FileNotFoundException e) {
            throw Py.IOError(e);
        }
    }

    @ExposedMethod(names = {"read", "read1"}, defaults = {"-1"}, doc = BuiltinDocs.BufferedReader_read_doc)
    public final PyObject BufferedReader_read(PyObject sizeObj) {
        int size = sizeObj.asInt();
        if (size < -1) {
            throw Py.ValueError("invalid number of bytes to read");
        }
        byte[] buf;
        if (size > 0) {
            buf = new byte[size];
            try {
                int n = input.read(buf);
                return new PyBytes(new String(buf, 0, n));
            } catch (IOException e) {
                throw Py.IOError(e);
            }
        }
        try {
            buf = ByteStreams.toByteArray(input);
            return new PyBytes(new String(buf));
        } catch (IOException e) {
            throw Py.IOError(e);
        }
    }

    @ExposedMethod(defaults = {"0"}, doc = BuiltinDocs.BufferedReader_peek_doc)
    public final PyObject BufferedReader_peek(PyObject sizeObj) {
        int size = sizeObj.asInt();
        byte[] buf = new byte[size];
        try {
            int n = input.read(buf);
            input.mark(n);
            input.reset();
            return new PyBytes(new String(buf, 0, n));
        } catch (IOException e) {
            throw Py.IOError(e);
        }
    }

    @ExposedMethod
    public final PyObject BufferedReader_tell() {
        try {
            return new PyLong(fileChannel.position());
        } catch (IOException e) {
            throw Py.IOError(e);
        }
    }

    @ExposedMethod(defaults = {"0"}, doc = BuiltinDocs.BufferedReader_seek_doc)
    public final PyObject BufferedReader_seek(PyObject pos, PyObject whence) {
        try {
            return new PyLong(fileChannel.position());
        } catch (IOException e) {
            throw Py.IOError(e);
        }
    }

    @ExposedMethod
    public final PyObject BufferedReader_readable() {
        return Py.True;
    }

    @ExposedMethod
    public final PyObject BufferedReader_writable() {
        return Py.False;
    }

    @ExposedMethod
    public final PyObject BufferedReader_seekable() {
        return Py.newBoolean(fileChannel != null);
    }
}
