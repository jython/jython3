package org.python.modules.subprocess;

import com.google.common.io.ByteStreams;
import org.python.core.Py;
import org.python.core.PyBytes;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

import java.io.IOException;
import java.io.InputStream;

@ExposedType(name = "subprocess.CompletedProcess")
public class PyCompletedProcess extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyCompletedProcess.class);
    private Process proc;

    @ExposedGet
    public PyObject args;

    public PyCompletedProcess(PyObject args, Process proc) {
        super(TYPE);
        this.args = args;
        this.proc = proc;
    }


    @ExposedMethod
    public PyObject CompletedProcess_check_returncode() {
        if (proc.exitValue() != 0) {
            throw SubprocessModule.CalledProcessError();
        }
        return new PyLong(proc.exitValue());
    }

    @ExposedGet
    public PyObject returncode() {
        return new PyLong(proc.exitValue());
    }

    @ExposedGet(name = "stdout")
    public PyObject getStdout() {
        InputStream out = proc.getInputStream();
        byte[] buf = new byte[0];
        try {
            buf = ByteStreams.toByteArray(out);
            return new PyBytes(new String(buf));
        } catch (IOException e) {
            return Py.None;
        }
    }

    @ExposedGet(name = "stderr")
    public PyObject getStderr() {
        InputStream out = proc.getErrorStream();
        byte[] buf = new byte[0];
        try {
            buf = ByteStreams.toByteArray(out);
            return new PyBytes(new String(buf));
        } catch (IOException e) {
            return Py.None;
        }
    }
}
