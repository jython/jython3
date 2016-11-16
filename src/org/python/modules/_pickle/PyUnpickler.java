package org.python.modules._pickle;

import org.python.core.Py;
import org.python.core.PyBytes;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.core.codecs;
import org.python.core.stringlib.Encoding;
import org.python.expose.ExposedType;
import org.python.modules.PyIOFile;
import org.python.modules.PyIOFileFactory;
import org.python.util.Generic;

import java.math.BigInteger;
import java.util.Map;

@ExposedType(name = "_pickle.Unpickler")
public class PyUnpickler extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyUnpickler.class);
    private PyIOFile file;

    public Map<String,PyObject> memo = Generic.map();

    /**
     * For the benefit of persistency modules written using pickle,
     * it supports the notion of a reference to an object outside
     * the pickled data stream.
     * Such objects are referenced by a name, which is an arbitrary
     * string of printable ASCII characters.
     * The resolution of such names is not defined by the pickle module
     * -- the persistent object module will have to add a method
     * persistent_load().
     */
    public PyObject persistent_load = null;
    public PyObject find_global = null;

    private PyObject mark = new PyBytes("spam");

    private int stackTop;
    private PyObject[] stack;


    PyUnpickler(PyObject file) {
        this.file = PyIOFileFactory.createIOFile(file);
    }


    /**
     * Unpickle and return an instance of the object represented by
     * the file.
     */
    public PyObject load() {
        stackTop = 0;
        stack = new PyObject[10];

        while (true) {
            String s = file.read(1);
//              System.out.println("load:" + s);
//              for (int i = 0; i < stackTop; i++)
//                  System.out.println("   " + stack[i]);
            if (s.length() < 1)
                load_eof();
            char key = s.charAt(0);
            switch (key) {
                case PickleModule.PERSID:          load_persid(); break;
                case PickleModule.BINPERSID:       load_binpersid(); break;
                case PickleModule.NONE:            load_none(); break;
                case PickleModule.INT:             load_int(); break;
                case PickleModule.BININT:          load_binint(); break;
                case PickleModule.BININT1:         load_binint1(); break;
                case PickleModule.BININT2:         load_binint2(); break;
                case PickleModule.LONG:            load_long(); break;
                case PickleModule.FLOAT:           load_float(); break;
                case PickleModule.BINFLOAT:        load_binfloat(); break;
                case PickleModule.STRING:          load_string(); break;
                case PickleModule.BINSTRING:       load_binstring(); break;
                case PickleModule.SHORT_BINSTRING: load_short_binstring(); break;
                case PickleModule.UNICODE:         load_unicode(); break;
                case PickleModule.BINUNICODE:      load_binunicode(); break;
                case PickleModule.TUPLE:           load_tuple(); break;
                case PickleModule.EMPTY_TUPLE:     load_empty_tuple(); break;
                case PickleModule.EMPTY_LIST:      load_empty_list(); break;
                case PickleModule.EMPTY_DICT:      load_empty_dictionary(); break;
                case PickleModule.LIST:            load_list(); break;
                case PickleModule.DICT:            load_dict(); break;
                case PickleModule.INST:            load_inst(); break;
                case PickleModule.OBJ:             load_obj(); break;
                case PickleModule.GLOBAL:          load_global(); break;
                case PickleModule.REDUCE:          load_reduce(); break;
                case PickleModule.POP:             load_pop(); break;
                case PickleModule.POP_MARK:        load_pop_mark(); break;
                case PickleModule.DUP:             load_dup(); break;
                case PickleModule.GET:             load_get(); break;
                case PickleModule.BINGET:          load_binget(); break;
                case PickleModule.LONG_BINGET:     load_long_binget(); break;
                case PickleModule.PUT:             load_put(); break;
                case PickleModule.BINPUT:          load_binput(); break;
                case PickleModule.LONG_BINPUT:     load_long_binput(); break;
                case PickleModule.APPEND:          load_append(); break;
                case PickleModule.APPENDS:         load_appends(); break;
                case PickleModule.SETITEM:         load_setitem(); break;
                case PickleModule.SETITEMS:        load_setitems(); break;
                case PickleModule.BUILD:           load_build(); break;
                case PickleModule.MARK:            load_mark(); break;
                case PickleModule.PROTO:           load_proto(); break;
                case PickleModule.NEWOBJ:          load_newobj(); break;
                case PickleModule.EXT1:            load_ext(1); break;
                case PickleModule.EXT2:            load_ext(2); break;
                case PickleModule.EXT4:            load_ext(4); break;
                case PickleModule.TUPLE1:          load_small_tuple(1); break;
                case PickleModule.TUPLE2:          load_small_tuple(2); break;
                case PickleModule.TUPLE3:          load_small_tuple(3); break;
                case PickleModule.NEWTRUE:         load_boolean(true); break;
                case PickleModule.NEWFALSE:        load_boolean(false); break;
                case PickleModule.LONG1:           load_bin_long(1); break;
                case PickleModule.LONG4:           load_bin_long(4); break;
                case PickleModule.STOP:
                    return load_stop();
                default:
                    throw new PyException(PickleModule.UnpicklingError,
                            String.format("invalid load key, '%s'.", key));
            }
        }
    }


    final private int marker() {
        for (int k = stackTop-1; k >= 0; k--)
            if (stack[k] == mark)
                return stackTop-k-1;
        throw new PyException(PickleModule.UnpicklingError,
                "Inputstream corrupt, marker not found");
    }


    final private void load_eof() {
        throw new PyException(Py.EOFError);
    }

    private void load_proto() {
        int proto = file.read(1).charAt(0);
        if (proto < 0 || proto > 2)
            throw Py.ValueError("unsupported pickle protocol: " + proto);
    }


    final private void load_persid() {
        load_persid(new PyBytes(file.readlineNoNl()));
    }


    final private void load_binpersid() {
        load_persid(pop());
    }

    final private void load_persid(PyObject pid) {
        if (persistent_load == null) {
            throw new PyException(PickleModule.UnpicklingError,
                    "A load persistent id instruction was encountered,\n"
                            + "but no persistent_load function was specified.");
        }

        if (persistent_load instanceof PyList) {
            ((PyList)persistent_load).append(pid);
        } else {
            pid = persistent_load.__call__(pid);
        }
        push(pid);
    }


    final private void load_none() {
        push(Py.None);
    }

    final private void load_int() {
        String line = file.readlineNoNl();
        PyObject value;
        // The following could be abstracted into a common string
        // -> int/long method.
        if (line.equals("01")) {
            value = Py.True;
        }
        else if (line.equals("00")) {
            value = Py.False;
        }
        else {
            try {
                value = Py.newInteger(Integer.parseInt(line));
            } catch(NumberFormatException e) {
                try {
                    value = Py.newLong(line);
                } catch(NumberFormatException e2) {
                    throw Py.ValueError("could not convert string to int");
                }
            }
        }
        push(value);
    }

    private void load_boolean(boolean value) {
        push(value ? Py.True : Py.False);
    }

    final private void load_binint() {
        int x = read_binint();
        push(new PyInteger(x));
    }

    private int read_binint() {
        String s = file.read(4);
        return s.charAt(0) |
                (s.charAt(1)<<8) |
                (s.charAt(2)<<16) |
                (s.charAt(3)<<24);
    }


    final private void load_binint1() {
        int val = file.read(1).charAt(0);
        push(new PyInteger(val));
    }

    final private void load_binint2() {
        int val = read_binint2();
        push(new PyInteger(val));
    }

    private int read_binint2() {
        String s = file.read(2);
        return (s.charAt(1)) << 8 | (s.charAt(0));
    }

    final private void load_long() {
        String line = file.readlineNoNl();
        push(new PyLong(line));
    }

    private void load_bin_long(int length) {
        int longLength = read_binint(length);
        if (longLength == 0) {
            push(new PyLong(BigInteger.ZERO));
            return;
        }
        String s = file.read(longLength);
        byte[] bytes = new byte[s.length()];
        // Write to the byte array in reverse order: pickle orders
        // by little endian whereas BigInteger orders by big
        // endian
        int n = s.length() - 1;
        for (int i = 0; i < s.length(); i++, n--) {
            char c = s.charAt(i);
            if(c >= 128) {
                bytes[n] = (byte)(c - 256);
            } else {
                bytes[n] = (byte)c;
            }
        }
        BigInteger bigint = new BigInteger(bytes);
        push(new PyLong(bigint));
    }

    private int read_binint(int length) {
        if (length == 1)
            return file.read(1).charAt(0);
        else if (length == 2)
            return read_binint2();
        else
            return read_binint();
    }

    final private void load_float() {
        String line = file.readlineNoNl();
        push(new PyFloat(Double.valueOf(line).doubleValue()));
    }

    final private void load_binfloat() {
        String s = file.read(8);
        long bits = s.charAt(7) |
                ((long)s.charAt(6) << 8) |
                ((long)s.charAt(5) << 16) |
                ((long)s.charAt(4) << 24) |
                ((long)s.charAt(3) << 32) |
                ((long)s.charAt(2) << 40) |
                ((long)s.charAt(1) << 48) |
                ((long)s.charAt(0) << 56);
        push(new PyFloat(Double.longBitsToDouble(bits)));
    }

    final private void load_string() {
        String line = file.readlineNoNl();

        String value;
        char quote = line.charAt(0);
        if (quote != '"' && quote != '\'')
            throw Py.ValueError("insecure string pickle");

        int nslash = 0;
        int i;
        char ch = '\0';
        int n = line.length();
        for (i = 1; i < n; i++) {
            ch = line.charAt(i);
            if (ch == quote && nslash % 2 == 0)
                break;
            if (ch == '\\')
                nslash++;
            else
                nslash = 0;
        }
        if (ch != quote)
            throw Py.ValueError("insecure string pickle");

        for (i++ ; i < line.length(); i++) {
            if (line.charAt(i) > ' ')
                throw Py.ValueError("insecure string pickle " + i);
        }
        value = Encoding.decode_UnicodeEscape(line, 1, n-1,
                "strict", false);

        push(new PyBytes(value));
    }


    final private void load_binstring() {
        int len = read_binint();
        push(new PyBytes(file.read(len)));
    }


    final private void load_short_binstring() {
        int len = file.read(1).charAt(0);
        push(new PyBytes(file.read(len)));
    }


    final private void load_unicode() {
        String line = file.readlineNoNl();
        String value = codecs.PyUnicode_DecodeRawUnicodeEscape(line,
                "strict");
        push(new PyUnicode(value));
    }

    final private void load_binunicode() {
        int len = read_binint();
        String line = file.read(len);
        push(new PyUnicode(codecs.PyUnicode_DecodeUTF8(line, "strict")));
    }

    final private void load_tuple() {
        PyObject[] arr = new PyObject[marker()];
        pop(arr);
        pop();
        push(new PyTuple(arr));
    }

    final private void load_empty_tuple() {
        push(new PyTuple(Py.EmptyObjects));
    }

    private void load_small_tuple(int length) {
        PyObject[] data = new PyObject[length];
        for(int i=length-1; i >= 0; i--) {
            data [i] = pop();
        }
        push(new PyTuple(data));
    }

    final private void load_empty_list() {
        push(new PyList(Py.EmptyObjects));
    }

    final private void load_empty_dictionary() {
        push(new PyDictionary());
    }


    final private void load_list() {
        PyObject[] arr = new PyObject[marker()];
        pop(arr);
        pop();
        push(new PyList(arr));
    }


    final private void load_dict() {
        int k = marker();
        PyDictionary d = new PyDictionary();
        for (int i = 0; i < k; i += 2) {
            PyObject value = pop();
            PyObject key = pop();
            d.__setitem__(key, value);
        }
        pop();
        push(d);
    }


    final private void load_inst() {
        PyObject[] args = new PyObject[marker()];
        pop(args);
        pop();

        String module = file.readlineNoNl();
        String name = file.readlineNoNl();
        PyObject klass = find_class(module, name);

        PyObject value = null;
        value = klass.__call__(args);
        push(value);
    }


    final private void load_obj() {
        PyObject[] args = new PyObject[marker()-1];
        pop(args);
        PyObject klass = pop();
        pop();

        PyObject value = null;
        value = klass.__call__(args);
        push(value);
    }

    final private void load_global() {
        String module = file.readlineNoNl();
        String name = file.readlineNoNl();
        PyObject klass = find_class(module, name);
        push(klass);
    }


    final private PyObject find_class(String module, String name) {
        if (find_global != null) {
            if (find_global == Py.None)
                throw new PyException(PickleModule.UnpicklingError,
                        "Global and instance pickles are not supported.");
            return find_global.__call__(new PyBytes(module), new PyBytes(name));
        }

        PyObject modules = Py.getSystemState().modules;
        PyObject mod = modules.__finditem__(module.intern());
        if (mod == null) {
            mod = PickleModule.importModule(module);
        }
        PyObject global = mod.__findattr__(name.intern());
        if (global == null) {
            throw new PyException(Py.SystemError,
                    "Failed to import class " + name + " from module " +
                            module);
        }
        return global;
    }

    private void load_ext(int length) {
        int code = read_binint(length);
        // TODO: support _extension_cache
        PyObject key = PickleModule.inverted_registry.get(Py.newInteger(code));
        if (key == null) {
            throw new PyException(Py.ValueError, "unregistered extension code " + code);
        }
        String module = key.__finditem__(0).toString();
        String name = key.__finditem__(1).toString();
        push(find_class(module, name));
    }


    final private void load_reduce() {
        PyObject arg_tup = pop();
        PyObject callable = pop();
        PyObject value = null;
        if (arg_tup == Py.None) {
            // XXX __basicnew__ ?
            value = callable.__findattr__("__basicnew__").__call__();
        } else {
            value = callable.__call__(make_array(arg_tup));
        }
        push(value);
    }

    private void load_newobj() {
        PyObject arg_tup = pop();
        PyObject cls = pop();
        PyObject[] args = new PyObject[arg_tup.__len__() + 1];
        args [0] = cls;
        for(int i=1; i<args.length; i++) {
            args [i] = arg_tup.__finditem__(i-1);
        }
        push(cls.__getattr__("__new__").__call__(args));
    }

    final private PyObject[] make_array(PyObject seq) {
        int n = seq.__len__();
        PyObject[] objs= new PyObject[n];

        for(int i=0; i<n; i++)
            objs[i] = seq.__finditem__(i);
        return objs;
    }

    final private void load_pop() {
        pop();
    }


    final private void load_pop_mark() {
        pop(marker());
    }

    final private void load_dup() {
        push(peek());
    }

    final private void load_get() {
        String py_str = file.readlineNoNl();
        PyObject value = memo.get(py_str);
        if (value == null) {
            throw new PyException(PickleModule.BadPickleGet, py_str);
        }
        push(value);
    }

    final private void load_binget() {
        String py_key = String.valueOf((int)file.read(1).charAt(0));
        PyObject value = memo.get(py_key);
        if (value == null) {
            throw new PyException(PickleModule.BadPickleGet, py_key);
        }
        push(value);
    }

    final private void load_long_binget() {
        int i = read_binint();
        String py_key = String.valueOf(i);
        PyObject value = memo.get(py_key);
        if (value == null) {
            throw new PyException(PickleModule.BadPickleGet, py_key);
        }
        push(value);
    }


    final private void load_put() {
        memo.put(file.readlineNoNl(), peek());
    }


    final private void load_binput() {
        int i = file.read(1).charAt(0);
        memo.put(String.valueOf(i), peek());
    }


    final private void load_long_binput() {
        int i = read_binint();
        memo.put(String.valueOf(i), peek());
    }

    final private void load_append() {
        PyObject value = pop();
        PyObject obj = peek();
        if(obj instanceof PyList) {
            ((PyList)obj).append(value);
        } else {
            PyObject appender = obj.__getattr__("append");
            appender.__call__(value);
        }
    }

    final private void load_appends() {
        int mark = marker();
        PyObject obj = peek(mark + 1);
        if(obj instanceof PyList) {
            for(int i = mark - 1; i >= 0; i--) {
                ((PyList)obj).append(peek(i));
            }
        } else {
            PyObject appender = obj.__getattr__("append");
            for(int i = mark - 1; i >= 0; i--) {
                appender.__call__(peek(i));
            }
        }
        pop(mark + 1);
    }

    final private void load_setitem() {
        PyObject value = pop();
        PyObject key   = pop();
        PyDictionary dict = (PyDictionary)peek();
        dict.__setitem__(key, value);
    }


    final private void load_setitems() {
        int mark = marker();
        PyDictionary dict = (PyDictionary)peek(mark+1);
        for (int i = 0; i < mark; i += 2) {
            PyObject key   = peek(i+1);
            PyObject value = peek(i);
            dict.__setitem__(key, value);
        }
        pop(mark+1);
    }

    private void load_build() {
        PyObject state = pop();
        PyObject inst  = peek();
        PyObject setstate = inst.__findattr__("__setstate__");
        if (setstate != null) {
            // The explicit __setstate__ is responsible for everything.
            setstate.__call__(state);
            return;
        }

        // A default __setstate__.  First see whether state embeds a slot state dict
        // too (a proto 2 addition).
        PyObject slotstate = null;
        if (state instanceof PyTuple && state.__len__() == 2) {
            PyObject temp = state;
            state = temp.__getitem__(0);
            slotstate = temp.__getitem__(1);
        }

        if (state != Py.None) {
            if (!(state instanceof PyDictionary)) {
                throw new PyException(PickleModule.UnpicklingError, "state is not a dictionary");
            }
            PyObject dict = inst.__getattr__("__dict__");
            for (PyObject item : ((PyDictionary)state).dict_iteritems().asIterable()) {
                dict.__setitem__(item.__getitem__(0), item.__getitem__(1));
            }
        }

        // Also set instance attributes from the slotstate dict (if any).
        if (slotstate != null) {
            if (!(slotstate instanceof PyDictionary)) {
                throw new PyException(PickleModule.UnpicklingError, "slot state is not a dictionary");
            }
            for (PyObject item : ((PyDictionary)slotstate).dict_iteritems().asIterable()) {
                inst.__setattr__(PyObject.asName(item.__getitem__(0)),
                        item.__getitem__(1));
            }
        }
    }

    final private void load_mark() {
        push(mark);
    }

    final private PyObject load_stop() {
        return pop();
    }



    final private PyObject peek() {
        return stack[stackTop-1];
    }

    final private PyObject peek(int count) {
        return stack[stackTop-count-1];
    }


    final private PyObject pop() {
        PyObject val = stack[--stackTop];
        stack[stackTop] = null;
        return val;
    }

    final private void pop(int count) {
        for (int i = 0; i < count; i++)
            stack[--stackTop] = null;
    }


    final private void pop(PyObject[] arr) {
        int len = arr.length;
        System.arraycopy(stack, stackTop - len, arr, 0, len);
        stackTop -= len;
    }

    final private void push(PyObject val) {
        if (stackTop >= stack.length) {
            PyObject[] newStack = new PyObject[(stackTop+1) * 2];
            System.arraycopy(stack, 0, newStack, 0, stack.length);
            stack = newStack;
        }
        stack[stackTop++] = val;
    }
}
