package org.python.modules.subprocess;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyLong;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.io.StreamIO;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.modules._io.OpenMode;
import org.python.modules._io.PyFileIO;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ExposedType(name = "subprocess.Popen")
public class PyPopen extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyPopen.class);

    private Process proc;

    @ExposedGet
    public PyObject args;

    public PyPopen(PyObject args, int stdin, int stdout, int stderr, PyObject env, String cwd) {
        super(TYPE);
        String[] argv = new String[args.__len__()];
        int i = 0;
        for (PyObject arg: args.asIterable()) {
            argv[i++] = arg.toString();
        }
        ProcessBuilder pb = new ProcessBuilder(argv);
        if (cwd != null) {
            pb.directory(new File(cwd));
        }
        if (stdout == SubprocessModule.PIPE) {
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        } else {
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }
        if (stdin == SubprocessModule.PIPE) {
            pb.redirectInput(ProcessBuilder.Redirect.PIPE);
        } else {
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        }
        if (stderr == SubprocessModule.PIPE) {
            pb.redirectError(ProcessBuilder.Redirect.PIPE);
        } else if (stderr == SubprocessModule.STDOUT) {
            pb.redirectErrorStream(true);
        } else {
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        }
        if (env != Py.None) {
            Map<String, String> environ = pb.environment();
            Map<?, ?> map = ((PyDictionary) env).getMap();
            for (Object key : map.keySet()) {
                environ.put(key.toString(), map.get(key).toString());
            }
        }
        try {
            proc = pb.start();
        } catch (IOException e) {
            proc = null;
        }
    }

    @ExposedNew
    final static PyObject Popen_new(PyNewWrapper new_, boolean init, PyType subtype,
                                    PyObject[] arguments, String[] keywords) {
        ArgParser ap = new ArgParser("Popen", arguments, keywords,
                new String[] {"args", "bufsize", "executable",
                        "stdin", "stdout", "stderr", "preexec_fn", "close_fds", "shell", "cwd",
                        "env", "universal_newlines", "startupinfo", "creationflags", "restore_signals",
                        "start_new_session", "pass_fds"}, 1);
        PyObject args = ap.getPyObject(0);
        int bufsize = ap.getInt(1, -1);
        String executable = ap.getString(2, null);
        int stdin = ap.getInt(3, 0);
        int stdout = ap.getInt(4, 0);
        int stderr = ap.getInt(5, 0);
        String cwd = ap.getString(9, null);
        PyObject env = ap.getPyObject(10, Py.None);
        return new PyPopen(args, stdin, stdout, stderr, env, cwd);
    }

    @ExposedMethod
    public PyObject Popen_communicate(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("communicate", args, keywords, "input", "timeout");
        PyObject input = ap.getPyObject(0, Py.None);
        PyObject timeout = ap.getPyObject(1, Py.None);
        if (input != Py.None) {
            OutputStream out = proc.getOutputStream();
            try {
                out.write(input.toString().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new PyTuple(new PyFileIO(new StreamIO(proc.getInputStream(), true), OpenMode.R_ONLY),
                new PyFileIO(new StreamIO(proc.getErrorStream(), true), OpenMode.W_ONLY));
    }

    @ExposedMethod(names = {"kill", "terminate"})
    public PyObject Popen_kill() {
        if (proc != null) {
            proc.destroy();
        }
        return Py.None;
    }

    @ExposedMethod
    public PyObject Popen_poll() {
        if (proc.isAlive()) {
            return Py.None;
        }
        return new PyLong(proc.exitValue());
    }

    @ExposedMethod
    public PyObject Popen_wait(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("wait", args, kwds, "timeout");
        int timeout = ap.getInt(0, -1);
        int res;
        try {
            if (timeout == -1) {
                res = proc.waitFor();
            } else {
                boolean finished = proc.waitFor(timeout, TimeUnit.SECONDS);
                if (finished) {
                    res = proc.exitValue();
                } else {
                    throw Py.ChildProcessError();
                }
            }
        } catch (InterruptedException e) {
            throw Py.ChildProcessError(e.getMessage());
        }
        return new PyLong(res);
    }

    @ExposedGet
    public PyObject getStdout() {
        return new PyFileIO(new StreamIO(proc.getInputStream(), true), OpenMode.R_ONLY);
    }

    @ExposedGet
    public PyObject getStderr() {
        return new PyFileIO(new StreamIO(proc.getErrorStream(), true), OpenMode.R_ONLY);
    }

    @ExposedGet
    public PyObject getStdin() {
        return new PyFileIO(new StreamIO(proc.getOutputStream(), true), OpenMode.W_ONLY);
    }

    @ExposedGet
    public PyObject pid() {
        return Py.java2py(proc); // no native stuff
    }

    @ExposedGet
    public PyObject returncode() {
        return new PyLong(proc.exitValue());
    }

    public void check() {
        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (proc.exitValue() != 0) {
            throw SubprocessModule.CalledProcessError();
        }
    }

    public Process process() {
        return proc;
    }
}
