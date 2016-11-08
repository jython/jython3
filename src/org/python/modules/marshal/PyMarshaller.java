package org.python.modules.marshal;

import org.python.core.BaseSet;
import org.python.core.Py;
import org.python.core.PyBytecode;
import org.python.core.PyBytes;
import org.python.core.PyComplex;
import org.python.core.PyDictionary;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PySet;
import org.python.core.PyTuple;
import org.python.core.PyUnicode;
import org.python.core.Traverseproc;
import org.python.core.Visitproc;
import org.python.modules.PyIOFile;
import org.python.modules.PyIOFileFactory;

import java.math.BigInteger;

/**
 * Created by isaiah on 11/8/16.
 */
public class PyMarshaller extends PyObject implements Traverseproc {

    private final PyIOFile file;
    private final int version;

    public PyMarshaller(PyObject file) {
        this(file, MarshalModule.CURRENT_VERSION);
    }

    public PyMarshaller(PyObject file, int version) {
        this.file = PyIOFileFactory.createIOFile(file);
        this.version = version;
    }

    private boolean debug = false;

    public void _debug() {
        debug = true;
    }

    public void dump(PyObject obj) {
        write_object(obj, 0);
    }

    private void write_byte(char c) {
        if (debug) {
            System.err.print("[" + (int) c + "]");
        }
        file.write(c);
    }

    private void write_string(String s) {
        file.write(s);
    }

    private void write_strings(String[] some_strings, int depth) {
        PyObject items[] = new PyObject[some_strings.length];
        for (int i = 0; i < some_strings.length; i++) {
            items[i] = Py.newString(some_strings[i]);
        }
        write_object(new PyTuple(items), depth + 1);
    }

    private void write_short(short x) {
        write_byte((char) (x & 0xff));
        write_byte((char) ((x >> 8) & 0xff));
    }

    private void write_int(int x) {
        write_byte((char) (x & 0xff));
        write_byte((char) ((x >> 8) & 0xff));
        write_byte((char) ((x >> 16) & 0xff));
        write_byte((char) ((x >> 24) & 0xff));
    }

    private void write_long64(long x) {
        write_int((int) (x & 0xffffffff));
        write_int((int) ((x >> 32) & 0xffffffff));
    }

    // writes output in 15 bit "digits"
    private void write_long(BigInteger x) {
        int sign = x.signum();
        if (sign < 0) {
            x = x.negate();
        }
        int num_bits = x.bitLength();
        int num_digits = num_bits / 15 + (num_bits % 15 == 0 ? 0 : 1);
        write_int(sign < 0 ? -num_digits : num_digits);
        BigInteger mask = BigInteger.valueOf(0x7FFF);
        for (int i = 0; i < num_digits; i++) {
            write_short(x.and(mask).shortValue());
            x = x.shiftRight(15);
        }
    }

    private void write_float(PyFloat f) {
        write_string(f.__repr__().toString());
    }

    private void write_binary_float(PyFloat f) {
        write_long64(Double.doubleToLongBits(f.getValue()));
    }

    private void write_object(PyObject v, int depth) {
        if (depth >= MarshalModule.MAX_MARSHAL_STACK_DEPTH) {
            throw Py.ValueError("Maximum marshal stack depth"); // XXX - fix this exception
        } else if (v == null) {
            write_byte(MarshalModule.TYPE_NULL);
        } else if (v == Py.None) {
            write_byte(MarshalModule.TYPE_NONE);
        } else if (v == Py.StopIteration) {
            write_byte(MarshalModule.TYPE_STOPITER);
        } else if (v == Py.Ellipsis) {
            write_byte(MarshalModule.TYPE_ELLIPSIS);
        } else if (v == Py.False) {
            write_byte(MarshalModule.TYPE_FALSE);
        } else if (v == Py.True) {
            write_byte(MarshalModule.TYPE_TRUE);
        } else if (v instanceof PyInteger) {
            write_byte(MarshalModule.TYPE_INT);
            write_int(((PyInteger) v).asInt());
        } else if (v instanceof PyLong) {
            write_byte(MarshalModule.TYPE_LONG);
            write_long(((PyLong) v).getValue());
        } else if (v instanceof PyFloat) {
            if (version == MarshalModule.CURRENT_VERSION) {
                write_byte(MarshalModule.TYPE_BINARY_FLOAT);
                write_binary_float((PyFloat) v);
            } else {
                write_byte(MarshalModule.TYPE_FLOAT);
                write_float((PyFloat) v);
            }
        } else if (v instanceof PyComplex) {
            PyComplex x = (PyComplex) v;
            if (version == MarshalModule.CURRENT_VERSION) {
                write_byte(MarshalModule.TYPE_BINARY_COMPLEX);
                write_binary_float(x.getReal());
                write_binary_float(x.getImag());
            } else {
                write_byte(MarshalModule.TYPE_COMPLEX);
                write_float(x.getReal());
                write_float(x.getImag());
            }
        } else if (v instanceof PyUnicode) {
            write_byte(MarshalModule.TYPE_UNICODE);
            String buffer = ((PyUnicode) v).encode("utf-8").toString();
            write_int(buffer.length());
            write_string(buffer);
        } else if (v instanceof PyBytes) {
            // ignore interning
            write_byte(MarshalModule.TYPE_STRING);
            write_int(v.__len__());
            write_string(v.toString());
        } else if (v instanceof PyTuple) {
            write_byte(MarshalModule.TYPE_TUPLE);
            PyTuple t = (PyTuple) v;
            int n = t.__len__();
            write_int(n);
            for (int i = 0; i < n; i++) {
                write_object(t.__getitem__(i), depth + 1);
            }
        } else if (v instanceof PyList) {
            write_byte(MarshalModule.TYPE_LIST);
            PyList list = (PyList) v;
            int n = list.__len__();
            write_int(n);
            for (int i = 0; i < n; i++) {
                write_object(list.__getitem__(i), depth + 1);
            }
        } else if (v instanceof PyDictionary) {
            write_byte(MarshalModule.TYPE_DICT);
            PyDictionary dict = (PyDictionary) v;
            for (PyObject item : dict.dict_iteritems().asIterable()) {
                PyTuple pair = (PyTuple) item;
                write_object(pair.__getitem__(0), depth + 1);
                write_object(pair.__getitem__(1), depth + 1);
            }
            write_object(null, depth + 1);
        } else if (v instanceof BaseSet) {
            if (v instanceof PySet) {
                write_byte(MarshalModule.TYPE_SET);
            } else {
                write_byte(MarshalModule.TYPE_FROZENSET);
            }
            int n = v.__len__();
            write_int(n);
            BaseSet set = (BaseSet) v;
            for (PyObject item : set.asIterable()) {
                write_object(item, depth + 1);
            }
        } else if (v instanceof PyBytecode) {
            PyBytecode code = (PyBytecode) v;
            write_byte(MarshalModule.TYPE_CODE);
            write_int(code.co_argcount);
            write_int(code.co_nlocals);
            write_int(code.co_stacksize);
            write_int(code.co_flags.toBits());
            write_object(Py.newString(new String(code.co_code)), depth + 1);
            write_object(new PyTuple(code.co_consts), depth + 1);
            write_strings(code.co_names, depth + 1);
            write_strings(code.co_varnames, depth + 1);
            write_strings(code.co_freevars, depth + 1);
            write_strings(code.co_cellvars, depth + 1);
            write_object(Py.newString(code.co_name), depth + 1);
            write_int(code.co_firstlineno);
            write_object(Py.newString(new String(code.co_lnotab)), depth + 1);
        } else {
            write_byte(MarshalModule.TYPE_UNKNOWN);
        }

        depth--;
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        return file != null && file instanceof Traverseproc ?
                ((Traverseproc) file).traverse(visit, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return file != null && file instanceof Traverseproc ?
                ((Traverseproc) file).refersDirectlyTo(ob) : false;
    }
}
