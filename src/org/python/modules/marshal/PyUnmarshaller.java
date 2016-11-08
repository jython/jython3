package org.python.modules.marshal;

import org.python.core.Py;
import org.python.core.PyBytecode;
import org.python.core.PyBytes;
import org.python.core.PyComplex;
import org.python.core.PyDictionary;
import org.python.core.PyFrozenSet;
import org.python.core.PyList;
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
public class PyUnmarshaller extends PyObject implements Traverseproc {

    private final PyIOFile file;
    private final PyList strings = new PyList();
    private final int version;
    int depth = 0;

    public PyUnmarshaller(PyObject file) {
        this(file, MarshalModule.CURRENT_VERSION);
    }

    public PyUnmarshaller(PyObject file, int version) {
        this.file = PyIOFileFactory.createIOFile(file);
        this.version = version;
    }

    private boolean debug = false;

    public void _debug() {
        debug = true;
    }

    public PyObject load() {
        try {
            PyObject obj = read_object(0);
            if (obj == null) {
                throw Py.TypeError("NULL object in marshal data");
            }
            return obj;
        } catch (StringIndexOutOfBoundsException e) {
            // convert from our PyIOFile abstraction to what marshal in CPython returns
            // (although it's really just looking for no bombing)
            throw Py.EOFError("EOF read where object expected");
        }
    }

    private int read_byte() {
        int b = file.read(1).charAt(0);
        if (debug) {
            System.err.print("[" + b + "]");
        }
        return b;
    }

    private String read_string(int n) {
        return file.read(n);
    }

    private int read_short() {
        int x = read_byte();
        x |= read_byte() << 8;
        return x;
    }

    private int read_int() { // cpython calls this r_long
        int x = read_byte();
        x |= read_byte() << 8;
        x |= read_byte() << 16;
        x |= read_byte() << 24;
        return x;
    }

    private long read_long64() { // cpython calls this r_long64
        long lo4 = read_int();
        long hi4 = read_int();
        long x = (hi4 << 32) | (lo4 & 0xFFFFFFFFL);
        return x;
    }

    private BigInteger read_long() {
        int size = read_int();
        int sign = 1;
        if (size < 0) {
            sign = -1;
            size = -size;
        }
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < size; i++) {
            String digits = String.valueOf(read_short());
            result = result.or(new BigInteger(digits).shiftLeft(i * 15));
        }
        if (sign < 0) {
            result = result.negate();
        }
        return result;
    }

    private double read_float() {
        int size = read_byte();
        return Py.newString(read_string(size)).atof();
    }

    private double read_binary_float() {
        return Double.longBitsToDouble(read_long64());
    }

    private PyObject read_object_notnull(int depth) {
        PyObject v = read_object(depth);
        if (v == null) {
            throw Py.ValueError("bad marshal data");
        }
        return v;
    }

    private String[] read_strings(int depth) {
        PyTuple t = (PyTuple) read_object_notnull(depth);
        String some_strings[] = new String[t.__len__()];
        int i = 0;
        for (PyObject item : t.asIterable()) {
            some_strings[i++] = item.toString().intern();
        }
        return some_strings;
    }

    private PyObject read_object(int depth) {
        if (depth >= MarshalModule.MAX_MARSHAL_STACK_DEPTH) {
            throw Py.ValueError("Maximum marshal stack depth"); // XXX - fix this exception
        }
        int type = read_byte();
        switch (type) {

            case MarshalModule.TYPE_NULL:
                return null;

            case MarshalModule.TYPE_NONE:
                return Py.None;

            case MarshalModule.TYPE_STOPITER:
                return Py.StopIteration;

            case MarshalModule.TYPE_ELLIPSIS:
                return Py.Ellipsis;

            case MarshalModule.TYPE_FALSE:
                return Py.False;

            case MarshalModule.TYPE_TRUE:
                return Py.True;

            case MarshalModule.TYPE_INT:
                return Py.newInteger(read_int());

            case MarshalModule.TYPE_INT64:
                return Py.newInteger(read_long64());

            case MarshalModule.TYPE_LONG: {
                return Py.newLong(read_long());
            }

            case MarshalModule.TYPE_FLOAT:
                return Py.newFloat(read_float());

            case MarshalModule.TYPE_BINARY_FLOAT:
                return Py.newFloat(read_binary_float());

            case MarshalModule.TYPE_COMPLEX: {
                double real = read_float();
                double imag = read_float();
                return new PyComplex(real, imag);
            }

            case MarshalModule.TYPE_BINARY_COMPLEX: {
                double real = read_binary_float();
                double imag = read_binary_float();
                return new PyComplex(real, imag);
            }

            case MarshalModule.TYPE_INTERNED:
            case MarshalModule.TYPE_STRING: {
                int size = read_int();
                String s = read_string(size);
                if (type == MarshalModule.TYPE_INTERNED) {
                    PyUnicode pys = PyUnicode.fromInterned(s.intern());
                    strings.append(pys);
                    return pys;
                } else {
                    return Py.newString(s);
                }
            }

            case MarshalModule.TYPE_STRINGREF: {
                int i = read_int();
                return strings.__getitem__(i);
            }

            case MarshalModule.TYPE_UNICODE: {
                int n = read_int();
                PyBytes buffer = Py.newString(read_string(n));
                return buffer.decode("utf-8");
            }

            case MarshalModule.TYPE_TUPLE: {
                int n = read_int();
                if (n < 0) {
                    throw Py.ValueError("bad marshal data");
                }
                PyObject items[] = new PyObject[n];
                for (int i = 0; i < n; i++) {
                    items[i] = read_object_notnull(depth + 1);
                }
                return new PyTuple(items);
            }

            case MarshalModule.TYPE_LIST: {
                int n = read_int();
                if (n < 0) {
                    throw Py.ValueError("bad marshal data");
                }
                PyObject items[] = new PyObject[n];
                for (int i = 0; i < n; i++) {
                    items[i] = read_object_notnull(depth + 1);
                }
                return new PyList(items);
            }

            case MarshalModule.TYPE_DICT: {
                PyDictionary d = new PyDictionary();
                while (true) {
                    PyObject key = read_object(depth + 1);
                    if (key == null) {
                        break;
                    }
                    PyObject value = read_object(depth + 1);
                    if (value != null) {
                        d.__setitem__(key, value);
                    }
                }
                return d;
            }

            case MarshalModule.TYPE_SET:
            case MarshalModule.TYPE_FROZENSET: {
                int n = read_int();
                PyObject items[] = new PyObject[n];
                for (int i = 0; i < n; i++) {
                    items[i] = read_object(depth + 1);
                }
                PyTuple v = new PyTuple(items);
                if (type == MarshalModule.TYPE_SET) {
                    return new PySet(v);
                } else {
                    return new PyFrozenSet(v);
                }
            }


            case MarshalModule.TYPE_CODE: {
                // XXX - support restricted execution mode? not certain if this is just legacy
                int argcount = read_int();
                int nlocals = read_int();
                int stacksize = read_int();
                int flags = read_int();
                String code = read_object_notnull(depth + 1).toString();
                PyObject consts[] = ((PyTuple) read_object_notnull(depth + 1)).getArray();
                String names[] = read_strings(depth + 1);
                String varnames[] = read_strings(depth + 1);
                String freevars[] = read_strings(depth + 1);
                String cellvars[] = read_strings(depth + 1);
                String filename = read_object_notnull(depth + 1).toString();
                String name = read_object_notnull(depth + 1).toString();
                int firstlineno = read_int();
                String lnotab = read_object_notnull(depth + 1).toString();

                return new PyBytecode(
                        argcount, nlocals, stacksize, flags,
                        code, consts, names, varnames,
                        filename, name, firstlineno, lnotab,
                        cellvars, freevars);
            }

            default:
                throw Py.ValueError("bad marshal data");
        }
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        if (file instanceof Traverseproc) {
            int retVal = ((Traverseproc) file).traverse(visit, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        return visit.visit(strings, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        if (ob == null) {
            return false;
        } else if (file != null && file instanceof Traverseproc
                && ((Traverseproc) file).refersDirectlyTo(ob)) {
            return true;
        } else {
            return ob == strings;
        }
    }
}
