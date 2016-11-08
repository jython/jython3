package org.python.modules._pickle;

import org.python.core.Py;
import org.python.core.PyBoolean;
import org.python.core.PyBytes;
import org.python.core.PyException;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PySlice;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.codecs;
import org.python.expose.ExposedType;
import org.python.modules.PyIOFile;
import org.python.modules.PyIOFileFactory;

import java.math.BigInteger;

@ExposedType(name = "_pickle.Pickler")
public class PyPickler extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyPickler.class);

    private PyIOFile file;
    private int protocol;

    /**
     * The undocumented attribute fast of the C version of PickleModule disables
     * memoization. Since having memoization on won't break anything, having
     * this dummy setter for fast here won't break any code expecting it to
     * do something. However without it code that sets fast fails(ie
     * test_cpickle.py), so it's worth having.
     */
    public boolean fast = false;

    private PickleModule.PickleMemo memo = new PickleModule.PickleMemo();

    /**
     * To write references to persistent objects, the persistent module
     * must assign a method to persistent_id which returns either None
     * or the persistent ID of the object.
     * For the benefit of persistency modules written using pickle,
     * it supports the notion of a reference to an object outside
     * the pickled data stream.
     * Such objects are referenced by a name, which is an arbitrary
     * string of printable ASCII characters.
     */
    public PyObject persistent_id = null;

    /**
     * Hmm, not documented, perhaps it shouldn't be public? XXX: fixme.
     */
    public PyObject inst_persistent_id = null;


    public PyPickler(PyObject file, int protocol) {
        this.file = PyIOFileFactory.createIOFile(file);
        this.protocol = protocol;
    }


    /**
     * Write a pickled representation of the object.
     * @param object        The object which will be pickled.
     */
    public void dump(PyObject object) {
        if (protocol >= 2) {
            file.write(PickleModule.PROTO);
            file.write((char) protocol);
        }
        save(object);
        file.write(PickleModule.STOP);
        file.flush();
    }

    private static final int get_id(PyObject o) {
        // we don't pickle Java instances so we don't have to consider that case
        return System.identityHashCode(o);
    }


    // Save name as in pickle.py but semantics are slightly changed.
    private void put(int i) {
        if (protocol > 0) {
            if (i < 256) {
                file.write(PickleModule.BINPUT);
                file.write((char)i);
                return;
            }
            file.write(PickleModule.LONG_BINPUT);
            file.write((char)( i         & 0xFF));
            file.write((char)((i >>>  8) & 0xFF));
            file.write((char)((i >>> 16) & 0xFF));
            file.write((char)((i >>> 24) & 0xFF));
            return;
        }
        file.write(PickleModule.PUT);
        file.write(String.valueOf(i));
        file.write("\n");
    }


    // Same name as in pickle.py but semantics are slightly changed.
    private void get(int i) {
        if (protocol > 0) {
            if (i < 256) {
                file.write(PickleModule.BINGET);
                file.write((char)i);
                return;
            }
            file.write(PickleModule.LONG_BINGET);
            file.write((char)( i         & 0xFF));
            file.write((char)((i >>>  8) & 0xFF));
            file.write((char)((i >>> 16) & 0xFF));
            file.write((char)((i >>> 24) & 0xFF));
            return;
        }
        file.write(PickleModule.GET);
        file.write(String.valueOf(i));
        file.write("\n");
    }


    private void save(PyObject object) {
        save(object, false);
    }


    private void save(PyObject object, boolean pers_save) {
        if (!pers_save && persistent_id != null && save_pers(object, persistent_id)) {
            return;
        }

        int d = get_id(object);

        PyType t = object.getType();

        if (t == PyTuple.TYPE && object.__len__() == 0) {
            if (protocol > 0)
                save_empty_tuple(object);
            else
                save_tuple(object);
            return;
        }

        int m = getMemoPosition(d, object);
        if (m >= 0) {
            get(m);
            return;
        }

        if (save_type(object, t))
            return;

        if (!pers_save && inst_persistent_id != null && save_pers(object, inst_persistent_id)) {
            return;
        }

        if (Py.isSubClass(t, PyType.TYPE)) {
            save_global(object);
            return;
        }

        PyObject tup = null;
        PyObject reduce = PickleModule.dispatch_table.__finditem__(t);
        if (reduce == null) {
            reduce = object.__findattr__("__reduce_ex__");
            if (reduce != null) {
                tup = reduce.__call__(Py.newInteger(protocol));
            } else {
                reduce = object.__findattr__("__reduce__");
                if (reduce == null)
                    throw new PyException(PickleModule.UnpickleableError, object);
                tup = reduce.__call__();
            }
        } else {
            tup = reduce.__call__(object);
        }

        if (tup instanceof PyBytes) {
            save_global(object, tup);
            return;
        }

        if (!(tup instanceof PyTuple)) {
            throw new PyException(PickleModule.PicklingError,
                    "Value returned by " + reduce.__repr__() +
                            " must be a tuple");
        }

        int l = tup.__len__();
        if (l < 2 || l > 5) {
            throw new PyException(PickleModule.PicklingError,
                    "tuple returned by " + reduce.__repr__() +
                            " must contain two to five elements");
        }

        PyObject callable = tup.__finditem__(0);
        PyObject arg_tup = tup.__finditem__(1);
        PyObject state = (l > 2) ? tup.__finditem__(2) : Py.None;
        PyObject listitems = (l > 3) ? tup.__finditem__(3) : Py.None;
        PyObject dictitems = (l > 4) ? tup.__finditem__(4) : Py.None;

        if (!(arg_tup instanceof PyTuple) && arg_tup != Py.None) {
            throw new PyException(PickleModule.PicklingError,
                    "Second element of tupe returned by " +
                            reduce.__repr__() + " must be a tuple");
        }
        save_reduce(callable, arg_tup, state, listitems, dictitems, object);
    }


    final private boolean save_pers(PyObject object, PyObject pers_func) {
        PyObject pid = pers_func.__call__(object);
        if (pid == Py.None) {
            return false;
        }

        if (protocol == 0) {
            if (!Py.isInstance(pid, PyBytes.TYPE)) {
                throw new PyException(PickleModule.PicklingError, "persistent id must be string");
            }
            file.write(PickleModule.PERSID);
            file.write(pid.toString());
            file.write("\n");
        } else {
            save(pid, true);
            file.write(PickleModule.BINPERSID);
        }
        return true;
    }

    final private void save_reduce(PyObject callable, PyObject arg_tup,
                                   PyObject state, PyObject listitems, PyObject dictitems,
                                   PyObject object)
    {
        PyObject callableName = callable.__findattr__("__name__");
        if(protocol >= 2 && callableName != null
                && "__newobj__".equals(callableName.toString())) {
            PyObject cls = arg_tup.__finditem__(0);
            if(cls.__findattr__("__new__") == null)
                throw new PyException(PickleModule.PicklingError,
                        "args[0] from __newobj__ args has no __new__");
            // TODO: check class
            save(cls);
            save(arg_tup.__getitem__(new PySlice(Py.One, Py.None, Py.None)));
            file.write(PickleModule.NEWOBJ);
        } else {
            save(callable);
            save(arg_tup);
            file.write(PickleModule.REDUCE);
        }

        // Memoize
        put(putMemo(get_id(object), object));

        if (listitems != Py.None) {
            batch_appends(listitems);
        }
        if (dictitems != Py.None) {
            batch_setitems(dictitems);
        }
        if (state != Py.None) {
            save(state);
            file.write(PickleModule.BUILD);
        }
    }



    final private boolean save_type(PyObject object, PyType type) {
        //System.out.println("save_type " + object + " " + cls);
        if (type == PickleModule.NoneType)
            save_none(object);
        else if (type == PickleModule.StringType)
            save_string(object);
        else if (type == PickleModule.UnicodeType)
            save_unicode(object);
        else if (type == PickleModule.IntType)
            save_int(object);
        else if (type == PickleModule.LongType)
            save_long(object);
        else if (type == PickleModule.FloatType)
            save_float(object);
        else if (type == PickleModule.TupleType)
            save_tuple(object);
        else if (type == PickleModule.ListType)
            save_list(object);
        else if (type == PickleModule.DictionaryType || type == PickleModule.StringMapType)
            save_dict(object);
        else if (type == PickleModule.TypeType)
            save_global(object);
        else if (type == PickleModule.FunctionType)
            save_global(object);
        else if (type == PickleModule.BuiltinCallableType)
            save_global(object);
        else if (type == PickleModule.ReflectedFunctionType)
            save_global(object);
        else if (type == PickleModule.BoolType)
            save_bool(object);
        else
            return false;
        return true;
    }



    final private void save_none(PyObject object) {
        file.write(PickleModule.NONE);
    }

    final private void save_int(PyObject object) {
        if (protocol > 0) {
            int l = ((PyInteger)object).getValue();
            char i1 = (char)( l         & 0xFF);
            char i2 = (char)((l >>> 8 ) & 0xFF);
            char i3 = (char)((l >>> 16) & 0xFF);
            char i4 = (char)((l >>> 24) & 0xFF);

            if (i3 == '\0' && i4 == '\0') {
                if (i2 == '\0') {
                    file.write(PickleModule.BININT1);
                    file.write(i1);
                    return;
                }
                file.write(PickleModule.BININT2);
                file.write(i1);
                file.write(i2);
                return;
            }
            file.write(PickleModule.BININT);
            file.write(i1);
            file.write(i2);
            file.write(i3);
            file.write(i4);
        } else {
            file.write(PickleModule.INT);
            file.write(object.toString());
            file.write("\n");
        }
    }

    private void save_bool(PyObject object) {
        int value = ((PyBoolean)object).getValue();
        if(protocol >= 2) {
            file.write(value != 0 ? PickleModule.NEWTRUE : PickleModule.NEWFALSE);
        } else {
            file.write(PickleModule.INT);
            file.write(value != 0 ? "01" : "00");
            file.write("\n");
        }
    }

    private void save_long(PyObject object) {
        if(protocol >= 2) {
            BigInteger integer = ((PyLong)object).getValue();

            if (integer.compareTo(BigInteger.ZERO) == 0) {
                // It's 0 -- an empty bytestring.
                file.write(PickleModule.LONG1);
                file.write((char)0);
                return;
            }

            byte[] bytes = integer.toByteArray();
            int l = bytes.length;
            if (l < 256) {
                file.write(PickleModule.LONG1);
                file.write((char)l);
            } else {
                file.write(PickleModule.LONG4);
                writeInt4(l);
            }
            // Write in reverse order: pickle orders by little
            // endian whereas BigInteger orders by big endian
            for (int i = l - 1; i >= 0; i--) {
                int b = bytes[i] & 0xff;
                file.write((char)b);
            }
        } else {
            file.write(PickleModule.LONG);
            file.write(object.toString());
            file.write("\n");
        }
    }

    private void writeInt4(int l) {
        char i1 = (char)( l         & 0xFF);
        char i2 = (char)((l >>> 8 ) & 0xFF);
        char i3 = (char)((l >>> 16) & 0xFF);
        char i4 = (char)((l >>> 24) & 0xFF);
        file.write(i1);
        file.write(i2);
        file.write(i3);
        file.write(i4);
    }


    final private void save_float(PyObject object) {
        if (protocol > 0) {
            file.write(PickleModule.BINFLOAT);
            double value= ((PyFloat) object).getValue();
            // It seems that struct.pack('>d', ..) and doubleToLongBits
            // are the same. Good for me :-)
            long bits = Double.doubleToLongBits(value);
            file.write((char)((bits >>> 56) & 0xFF));
            file.write((char)((bits >>> 48) & 0xFF));
            file.write((char)((bits >>> 40) & 0xFF));
            file.write((char)((bits >>> 32) & 0xFF));
            file.write((char)((bits >>> 24) & 0xFF));
            file.write((char)((bits >>> 16) & 0xFF));
            file.write((char)((bits >>>  8) & 0xFF));
            file.write((char)((bits >>>  0) & 0xFF));
        } else {
            file.write(PickleModule.FLOAT);
            file.write(object.toString());
            file.write("\n");
        }
    }


    final private void save_string(PyObject object) {
        String str = object.toString();

        if (protocol > 0) {
            int l = str.length();
            if (l < 256) {
                file.write(PickleModule.SHORT_BINSTRING);
                file.write((char)l);
            } else {
                file.write(PickleModule.BINSTRING);
                file.write((char)( l         & 0xFF));
                file.write((char)((l >>> 8 ) & 0xFF));
                file.write((char)((l >>> 16) & 0xFF));
                file.write((char)((l >>> 24) & 0xFF));
            }
            file.write(str);
        } else {
            file.write(PickleModule.STRING);
            file.write(object.__repr__().toString());
            file.write("\n");
        }
        put(putMemo(get_id(object), object));
    }

    private void save_unicode(PyObject object) {
        if (protocol > 0) {
            String str = codecs.PyUnicode_EncodeUTF8(object.toString(), "struct");
            file.write(PickleModule.BINUNICODE);
            writeInt4(str.length());
            file.write(str);
        } else {
            file.write(PickleModule.UNICODE);
            file.write(codecs.PyUnicode_EncodeRawUnicodeEscape(object.toString(),
                    "strict", true));
            file.write("\n");
        }
        put(putMemo(get_id(object), object));
    }

    private void save_tuple(PyObject object) {
        int d = get_id(object);

        int len = object.__len__();

        if (len > 0 && len <= 3 && protocol >= 2) {
            for (int i = 0; i < len; i++)
                save(object.__finditem__(i));
            int m = getMemoPosition(d, object);
            if (m >= 0) {
                for (int i = 0; i < len; i++)
                    file.write(PickleModule.POP);
                get(m);
            }
            else {
                char opcode = (char) (PickleModule.TUPLE1 + len - 1);
                file.write(opcode);
                put(putMemo(d, object));
            }
            return;
        }

        file.write(PickleModule.MARK);

        for (int i = 0; i < len; i++)
            save(object.__finditem__(i));

        if (len > 0) {
            int m = getMemoPosition(d, object);
            if (m >= 0) {
                if (protocol > 0) {
                    file.write(PickleModule.POP_MARK);
                    get(m);
                    return;
                }
                for (int i = 0; i < len+1; i++)
                    file.write(PickleModule.POP);
                get(m);
                return;
            }
        }
        file.write(PickleModule.TUPLE);
        put(putMemo(d, object));
    }


    final private void save_empty_tuple(PyObject object) {
        file.write(PickleModule.EMPTY_TUPLE);
    }

    private void save_list(PyObject object) {
        if (protocol > 0)
            file.write(PickleModule.EMPTY_LIST);
        else {
            file.write(PickleModule.MARK);
            file.write(PickleModule.LIST);
        }

        put(putMemo(get_id(object), object));

        batch_appends(object);
    }

    private void batch_appends(PyObject object) {
        int countInBatch = 0;
        for (PyObject nextObj : object.asIterable()) {
            if(protocol == 0) {
                save(nextObj);
                file.write(PickleModule.APPEND);
            } else {
                if(countInBatch == 0) {
                    file.write(PickleModule.MARK);
                }
                countInBatch++;
                save(nextObj);
                if(countInBatch == PickleModule.BATCHSIZE) {
                    file.write(PickleModule.APPENDS);
                    countInBatch = 0;
                }
            }
        }
        if (countInBatch > 0)
            file.write(PickleModule.APPENDS);
    }


    private void save_dict(PyObject object) {
        if (protocol > 0)
            file.write(PickleModule.EMPTY_DICT);
        else {
            file.write(PickleModule.MARK);
            file.write(PickleModule.DICT);
        }

        put(putMemo(get_id(object), object));

        batch_setitems(object.invoke("items"));
    }

    private void batch_setitems(PyObject object) {
        if (protocol == 0) {
            // SETITEMS isn't available; do one at a time.
            for (PyObject p : object.asIterable()) {
                if (!(p instanceof PyTuple) || p.__len__() != 2) {
                    throw Py.TypeError("dict items iterator must return 2-tuples");
                }
                save(p.__getitem__(0));
                save(p.__getitem__(1));
                file.write(PickleModule.SETITEM);
            }
        } else {
            // proto > 0:  write in batches of BATCHSIZE.
            PyObject obj;
            PyObject[] slice = new PyObject[PickleModule.BATCHSIZE];
            int n;
            do {
                // Get next group of (no more than) BATCHSIZE elements.
                for (n = 0; n < PickleModule.BATCHSIZE; n++) {
                    obj = object.__next__();
                    if (obj == null) {
                        break;
                    }
                    slice[n] = obj;
                }

                if (n > 1) {
                    // Pump out MARK, slice[0:n], APPENDS.
                    file.write(PickleModule.MARK);
                    for (int i = 0; i < n; i++) {
                        obj = slice[i];
                        save(obj.__getitem__(0));
                        save(obj.__getitem__(1));
                    }
                    file.write(PickleModule.SETITEMS);
                } else if (n == 1) {
                    obj = slice[0];
                    save(obj.__getitem__(0));
                    save(obj.__getitem__(1));
                    file.write(PickleModule.SETITEM);
                }
            } while (n == PickleModule.BATCHSIZE);
        }
    }

    final private void save_global(PyObject object) {
        save_global(object, null);
    }


    final private void save_global(PyObject object, PyObject name) {
        if (name == null)
            name = object.__findattr__("__name__");

        PyObject module = object.__findattr__("__module__");
        if (module == null || module == Py.None)
            module = PickleModule.whichmodule(object, name);

        if(protocol >= 2) {
            PyTuple extKey = new PyTuple(module, name);
            PyObject extCode = PickleModule.extension_registry.get(extKey);
            if(extCode != Py.None) {
                int code = ((PyInteger)extCode).getValue();
                if(code <= 0xFF) {
                    file.write(PickleModule.EXT1);
                    file.write((char)code);
                } else if(code <= 0xFFFF) {
                    file.write(PickleModule.EXT2);
                    file.write((char)(code & 0xFF));
                    file.write((char)(code >> 8));
                } else {
                    file.write(PickleModule.EXT4);
                    writeInt4(code);
                }
                return;
            }
        }

        file.write(PickleModule.GLOBAL);
        file.write(module.toString());
        file.write("\n");
        file.write(name.toString());
        file.write("\n");
        put(putMemo(get_id(object), object));
    }


    final private int getMemoPosition(int id, Object o) {
        return memo.findPosition(id, o);
    }

    final private int putMemo(int id, PyObject object) {
        int memo_len = memo.size() + 1;
        memo.put(id, memo_len, object);
        return memo_len;
    }


    /**
     * Keeps a reference to the object x in the memo.
     *
     * Because we remember objects by their id, we have
     * to assure that possibly temporary objects are kept
     * alive by referencing them.
     * We store a reference at the id of the memo, which should
     * normally not be used unless someone tries to deepcopy
     * the memo itself...
     */
    final private void keep_alive(PyObject obj) {
        int id = System.identityHashCode(memo);
        PyList list = (PyList) memo.findValue(id, memo);
        if (list == null) {
            list = new PyList();
            memo.put(id, -1, list);
        }
        list.append(obj);
    }

}
