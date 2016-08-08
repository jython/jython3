package org.python.modules._io;

import org.python.core.BufferProtocol;
import org.python.core.BuiltinDocs;
import org.python.core.Py;
import org.python.core.PyBUF;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

@ExposedType(name = "_io.BufferedWriter")
public class PyBufferedWriter extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyBufferedWriter.class);

    private BufferedOutputStream output;
    private FileChannel fileChannel;

    public PyBufferedWriter(OutputStream out) {
        output = new BufferedOutputStream(out);
    }

    public PyBufferedWriter(File file) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            output = new BufferedOutputStream(fileOutputStream);
            fileChannel = fileOutputStream.getChannel();
        } catch (FileNotFoundException e) {
            throw Py.IOError(e);
        }
    }

    @ExposedMethod(doc = BuiltinDocs.BufferedWriter_write_doc)
    public final PyObject BufferedWriter_write(PyObject b) {
        if (!(b instanceof BufferProtocol)) {
            throw Py.TypeError("bytes-like object expected");
        }
        try {
            output.write(((BufferProtocol) b).getBuffer(PyBUF.FULL_RO).getNIOByteBuffer().array());
            return new PyLong(b.__len__());
        } catch (IOException e) {
            throw Py.IOError(e);
        }
    }

    @ExposedMethod(doc = BuiltinDocs.BufferedWriter_flush_doc)
    public final void BufferedWriter_flush() {
        try {
            output.flush();
        } catch (IOException e) {
            throw Py.IOError(e);
        }
    }

    @ExposedMethod(doc = BuiltinDocs.BufferedWriter_tell_doc)
    public final PyObject BufferedWriter_tell() {
        try {
            return new PyLong(fileChannel.position());
        } catch (IOException e) {
            throw Py.IOError(e);
        }
    }
}
