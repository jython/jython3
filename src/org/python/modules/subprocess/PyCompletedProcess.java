package org.python.modules.subprocess;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "subprocess.CompletedProcess")
public class PyCompletedProcess extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyCompletedProcess.class);

    @ExposedGet
    public PyObject args;
    @ExposedGet
    public PyObject returncode;
    @ExposedGet
    public PyObject stdout;
    @ExposedGet
    public PyObject stderr;


    public PyCompletedProcess(PyObject args, PyObject stdout, PyObject stderr, PyObject returnCode) {
        super(TYPE);
        this.args = args;
        this.stdout = stdout;
        this.stderr = stderr;
        this.returncode = returnCode;
    }


    @ExposedMethod
    public PyObject CompletedProcess_check_returncode() {
        if (returncode != Py.Zero) {
            throw Py.CalledProcessError();
        }
        return Py.None;
    }

}
