// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.python.core.buffer.BaseBuffer;
import org.python.core.buffer.SimpleStringBuffer;
import org.python.core.stringlib.Encoding;
import org.python.core.stringlib.FieldNameIterator;
import org.python.core.stringlib.MarkupIterator;
import org.python.core.util.StringUtil;
import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static org.python.core.stringlib.Encoding.asUTF16StringOrError;

/**
 * A builtin python string.
 */
@Untraversable
@ExposedType(name = "bytes", base = PyObject.class, doc = BuiltinDocs.bytes_doc)
public class PyBytes extends PySequence implements BufferProtocol {

    private static final String BYTES_REQUIRED_ERROR = "a bytes-like object is required, not 'str'";

    public static final PyType TYPE = PyType.fromClass(PyBytes.class);
    protected String string; // cannot make final because of Python intern support
    protected transient boolean interned = false;
    /** Supports the buffer API, see {@link #getBuffer(int)}. */
    private Reference<BaseBuffer> export;

    public String getString() {
        return string;
    }

    // for PyJavaClass.init()
    public PyBytes() {
        this("", true);
    }

    public PyBytes(byte[] buf) {
        this(buf, 0, buf.length);
    }
    public PyBytes(byte[] buf, int off, int ending) {
        super(TYPE);
        StringBuilder v = new StringBuilder(buf.length);
        for (int i = off; i < ending; i++) {
            v.appendCodePoint(buf[i] & 0xFF);
        }
        string = v.toString();
    }

    public PyBytes(int[] buf) {
        super(TYPE);
        StringBuilder v = new StringBuilder(buf.length);
        for (int i: buf) {
            v.appendCodePoint(i);
        }
        string = v.toString();
    }

    /**
     * Fundamental constructor for <code>PyBytes</code> objects when the client provides a Java
     * <code>String</code>, necessitating that we range check the characters.
     *
     * @param subType the actual type being constructed
     * @param string a Java String to be wrapped
     */
    public PyBytes(PyType subType, CharSequence string) {
        super(subType);
        if (string == null) {
            throw Py.ValueError("Cannot create PyBytes from null");
        } else if (!isBytes(string)) {
            throw Py.ValueError("Cannot create PyBytes with non-byte value");
        }
        this.string = string.toString();
    }

    public PyBytes(ByteBuffer buf) {
        super(TYPE);
        StringBuilder v = new StringBuilder(buf.limit());
        for(int i = 0; i < buf.limit(); i++) {
            v.appendCodePoint(buf.get(i) & 0xFF);
        }
        string = v.toString();
    }

    public PyBytes(CharSequence string) {
        this(TYPE, string);
    }

    public PyBytes(char c) {
        this(TYPE, String.valueOf(c));
    }

    PyBytes(StringBuilder buffer) {
        this(TYPE, new String(buffer));
    }

    /**
     * Local-use constructor in which the client is allowed to guarantee that the
     * <code>String</code> argument contains only characters in the byte range. We do not then
     * range-check the characters.
     *
     * @param string a Java String to be wrapped (not null)
     * @param isBytes true if the client guarantees we are dealing with bytes
     */
    private PyBytes(CharSequence string, boolean isBytes) {
        super(TYPE);
        if (isBytes || isBytes(string)) {
            this.string = string.toString();
        } else {
            throw new IllegalArgumentException("Cannot create PyBytes with non-byte value");
        }
    }

    /**
     * Determine whether a string consists entirely of characters in the range 0 to 255. Only such
     * characters are allowed in the <code>PyBytes</code> (<code>str</code>) type, when it is not a
     * {@link PyUnicode}.
     *
     * @return true if and only if every character has a code less than 256
     */
    private static boolean isBytes(CharSequence s) {
        int k = s.length();
        if (k == 0) {
            return true;
        } else {
            // Bitwise-or the character codes together in order to test once.
            char c = 0;
            // Blocks of 8 to reduce loop tests
            while (k > 8) {
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
            }
            // Now the rest
            while (k > 0) {
                c |= s.charAt(--k);
            }
            // We require there to be no bits set from 0x100 upwards
            return c < 0x100;
        }
    }

    /**
     * Creates a PyBytes from an already interned String. Just means it won't be reinterned if used
     * in a place that requires interned Strings.
     * TODO remove once bootstrapped
     */
    public static PyBytes fromInterned(String interned) {
        PyBytes str = new PyBytes(TYPE, interned);
        str.interned = true;
        return str;
    }

    @ExposedNew
    static PyObject bytes_new(PyNewWrapper new_, boolean init, PyType subtype, PyObject[] args,
            String[] keywords) {
        ArgParser ap = new ArgParser("str", args, keywords, new String[] {"object", "encoding", "errors"}, 0);
        PyObject S = ap.getPyObject(0, null);
        // Get the textual representation of the object into str/bytes form
        String str;
        if (S == null) {
            str = "";
        } else {
            if (S instanceof PyUnicode) {
                String encoding = ap.getString(1, "utf-8");
                String errors = ap.getString(2, "strict");
                // Encoding will raise UnicodeEncodeError if not 7-bit clean.
                str = codecs.encode(S, encoding, errors);
            } else if (S instanceof BufferProtocol) {
                PyBuffer buffer = ((BufferProtocol) S).getBuffer(PyBUF.FULL_RO);
                byte[] buf = new byte[buffer.getLen()];
                buffer.copyTo(buf, 0);
                buffer.close();
                StringBuilder v = new StringBuilder(buf.length);
                for (byte b: buf) {
                    v.appendCodePoint(b & 0xFF);
                }
                str = v.toString();
            } else if (S instanceof PyLong) {
                int n = ((PyLong) S).getValue().intValue();
                byte[] bytes = new byte[n];
                Arrays.fill( bytes, (byte) 0 );
                str = new String(bytes);
            } else { // an iterable yielding integers in range(256)
                StringBuilder v = new StringBuilder();
                for (PyObject x : S.asIterable()) {
                    int i = x.asInt();
                    if (i < 0 || i > 255) {
                        throw Py.ValueError("bytes must be in range(0, 255)");
                    }
                    v.appendCodePoint(i);
                }
                str = v.toString();
            }
        }
        if (new_.for_type == subtype) {
            return new PyBytes(str);
        } else {
            return new PyBytesDerived(subtype, str);
        }
    }

    public int[] toCodePoints() {
        String s = getString();
        int n = s.length();
        int[] codePoints = new int[n];
        for (int i = 0; i < n; i++) {
            codePoints[i] = s.charAt(i);
        }
        return codePoints;
    }

    /**
     * Return a read-only buffer view of the contents of the string, treating it as a sequence of
     * unsigned bytes. The caller specifies its requirements and navigational capabilities in the
     * <code>flags</code> argument (see the constants in interface {@link PyBUF} for an
     * explanation). The method may return the same PyBuffer object to more than one consumer.
     *
     * @param flags consumer requirements
     * @return the requested buffer
     */
    @Override
    public synchronized PyBuffer getBuffer(int flags) {
        // If we have already exported a buffer it may still be available for re-use
        BaseBuffer pybuf = getExistingBuffer(flags);
        if (pybuf == null) {
            /*
             * No existing export we can re-use. Return a buffer, but specialised to defer
             * construction of the buf object, and cache a soft reference to it.
             */
            pybuf = new SimpleStringBuffer(flags, getString());
            export = new SoftReference<BaseBuffer>(pybuf);
        }
        return pybuf;
    }

    /**
     * Helper for {@link #getBuffer(int)} that tries to re-use an existing exported buffer, or
     * returns null if can't.
     */
    private BaseBuffer getExistingBuffer(int flags) {
        BaseBuffer pybuf = null;
        if (export != null) {
            // A buffer was exported at some time.
            pybuf = export.get();
            if (pybuf != null) {
                /*
                 * And this buffer still exists. Even in the case where the buffer has been released
                 * by all its consumers, it remains safe to re-acquire it because the target String
                 * has not changed.
                 */
                pybuf = pybuf.getBufferAgain(flags);
            }
        }
        return pybuf;
    }

    /**
     * Return a substring of this object as a Java String.
     *
     * @param start the beginning index, inclusive.
     * @param end the ending index, exclusive.
     * @return the specified substring.
     */
    public String substring(int start, int end) {
        return getString().substring(start, end);
    }

    @Override
    public PyUnicode __str__() {
        return bytes___str__();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes___str___doc)
    final PyUnicode bytes___str__() {
        return new PyUnicode("b" + Encoding.encode_UnicodeEscape(getString(), true));
    }

    public PyUnicode __unicode__() {
        return new PyUnicode(this);
    }

    @Override
    public int __len__() {
        return bytes___len__();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes___len___doc)
    final int bytes___len__() {
        return getString().length();
    }

    @Override
    public String toString() {
        return getString();
    }

    @Override
    public PyUnicode __repr__() {
        return bytes___repr__();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes___repr___doc)
    final PyUnicode bytes___repr__() {
        return new PyUnicode("b" + Encoding.encode_UnicodeEscapeAsASCII(getString()));
    }

    @ExposedMethod(doc = BuiltinDocs.bytes___getitem___doc)
    final PyObject bytes___getitem__(PyObject index) {
        PyObject ret = seq___finditem__(index);
        if (ret == null) {
            throw Py.IndexError("string index out of range");
        }
        return ret;
    }

    @Override
    public PyObject richCompare(PyObject other, CompareOp op) {
        String s = coerce(other);
        if (s == null) {
            if (op == CompareOp.EQ) {
                return Py.False;
            }
            if (op == CompareOp.NE) {
                return Py.True;
            }
            return Py.NotImplemented;
        }
        return op.bool(getString().compareTo(s));
    }

    private static String coerce(PyObject o) {
        if (o instanceof PyBytes) {
            return o.toString();
        }
        return null;
    }

    @Override
    public int hashCode() {
        return bytes___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes___hash___doc)
    final int bytes___hash__() {
        return getString().hashCode();
    }

    /**
     * @return a byte array with one byte for each char in this object's underlying String. Each
     *         byte contains the low-order bits of its corresponding char.
     */
    public byte[] toBytes() {
        return StringUtil.toBytes(getString());
    }

    @Override
    public Object __tojava__(Class<?> c) {
        if (c.isAssignableFrom(String.class)) {
            return getString();
        }

        if (c == Character.TYPE || c == Character.class) {
            if (getString().length() == 1) {
                return new Character(getString().charAt(0));
            }
        }

        if (c.isArray()) {
            if (c.getComponentType() == Byte.TYPE) {
                return toBytes();
            }
            if (c.getComponentType() == Character.TYPE) {
                return getString().toCharArray();
            }
        }

        if (c.isAssignableFrom(Collection.class)) {
            List<Object> list = new ArrayList();
            for (int i = 0; i < __len__(); i++) {
                list.add(pyget(i).__tojava__(String.class));
            }
            return list;
        }

        if (c.isInstance(this)) {
            return this;
        }

        return Py.NoConversion;
    }

    @Override
    protected PyObject pyget(int i) {
        // Method is overridden in PyUnicode, so definitely a PyBytes
        return new PyLong(string.charAt(i));
    }

    public int getInt(int i) {
        return string.charAt(i);
    }

    @Override
    public PyObject getslice(int start, int stop, int step) {
        CharSequence s = Encoding.getslice(getString(), start, stop, step, sliceLength(start, stop, step));
        return new PyBytes(s);
    }

    @Override
    public boolean __contains__(PyObject o) {
        return bytes___contains__(o);
    }

    @ExposedMethod(doc = BuiltinDocs.bytes___contains___doc)
    final boolean bytes___contains__(PyObject o) {
        String other = asUTF16StringOrError(o);
        return getString().indexOf(other) >= 0;
    }

    @Override
    protected PyObject repeat(int count) {
        if (count < 0) {
            count = 0;
        }
        int s = getString().length();
        if ((long)s * count > Integer.MAX_VALUE) {
            // Since Strings store their data in an array, we can't make one
            // longer than Integer.MAX_VALUE. Without this check we get
            // NegativeArraySize Exceptions when we create the array on the
            // line with a wrapped int.
            throw Py.OverflowError("max str len is " + Integer.MAX_VALUE);
        }
        char new_chars[] = new char[s * count];
        for (int i = 0; i < count; i++) {
            getString().getChars(0, s, new_chars, i * s);
        }
        return new PyBytes(new String(new_chars));
    }

    @Override
    public PyObject __mul__(PyObject o) {
        return bytes___mul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytes___mul___doc)
    final PyObject bytes___mul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    @Override
    public PyObject __rmul__(PyObject o) {
        return bytes___rmul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytes___rmul___doc)
    final PyObject bytes___rmul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    /**
     * {@inheritDoc} For a <code>str</code> addition means concatenation and returns a
     * <code>str</code> ({@link PyBytes}) result, except when a {@link PyUnicode} argument is
     * given, when a <code>PyUnicode</code> results.
     */
    @Override
    public PyObject __add__(PyObject other) {
        return bytes___add__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytes___add___doc)
    final PyObject bytes___add__(PyObject other) {
        // Expect other to be some kind of byte-like object.
        String otherStr = Encoding.asStringOrNull(other);
        if (otherStr != null) {
            // Yes it is: concatenate as strings, which are guaranteed byte-like.
            return new PyBytes(getString().concat(otherStr), true);
        } else if (other instanceof PyUnicode) {
            return decode().__add__(other);
        } else {
            // Allow PyObject._basic_add to pick up the pieces or raise informative error
            return null;
        }
    }

    @ExposedMethod(doc = BuiltinDocs.bytes___getnewargs___doc)
    final PyTuple bytes___getnewargs__() {
        return new PyTuple(new PyBytes(this.getString()));
    }

    @Override
    public PyTuple __getnewargs__() {
        return bytes___getnewargs__();
    }

    @Override
    public PyObject __mod__(PyObject other) {
        return bytes___mod__(other);
    }

    @ExposedMethod(doc = BuiltinDocs.str___mod___doc)
    public PyObject bytes___mod__(PyObject other) {
        StringFormatter fmt = new StringFormatter(getString(), false);
        return fmt.format(other);
    }

    public PyObject atol(int base) {
        return Encoding.atol(getString(), base);
    }

    public PyLong atoi(int base) {
        return new PyLong(Encoding.atoi(getString(), base));
    }

    public double atof() {
        return Encoding.atof(getString());
    }

    @Override
    public PyObject __int__() {
        return Encoding.atol(getString(), 10);
    }

    @Override
    public PyFloat __float__() {
        return new PyFloat(Encoding.atof(getString()));
    }

    @Override
    public PyObject __pos__() {
        throw Py.TypeError("bad operand type for unary +");
    }

    @Override
    public PyObject __neg__() {
        throw Py.TypeError("bad operand type for unary -");
    }

    @Override
    public PyObject __invert__() {
        throw Py.TypeError("bad operand type for unary ~");
    }

    @Override
    public PyComplex __complex__() {
        return Encoding.atocx(getString());
    }

    // Add in methods from string module
    public String lower() {
        return bytes_lower();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_lower_doc)
    final String bytes_lower() {
        return getString().toLowerCase(Locale.ROOT);
    }

    public String upper() {
        return bytes_uuper();
    }

    @ExposedMethod(doc = BuiltinDocs.str_upper_doc)
    final String bytes_uuper() {
        return getString().toUpperCase(Locale.ROOT);
    }

    public String title() {
        return bytes_title();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_title_doc)
    final String bytes_title() {
        return Encoding.title(getString());
    }

    public String swapcase() {
        return bytes_swapcase();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_swapcase_doc)
    final String bytes_swapcase() {
        return Encoding.swapcase(getString());
    }

    /**
     * Equivalent of Python <code>str.strip()</code> with no argument, meaning strip whitespace. Any
     * whitespace byte/character will be discarded from either end of this <code>str</code>.
     *
     * @return a new String, stripped of the whitespace characters/bytes
     */
    public String strip() {
        return Encoding._strip(getString()).toString();
    }

    /**
     * Equivalent of Python <code>str.strip()</code>.
     *
     * @param stripChars characters to strip from either end of this str/bytes, or null
     * @return a new String, stripped of the specified characters/bytes
     */
    public String strip(String stripChars) {
        return Encoding._strip(getString(), stripChars).toString();
    }

    /**
     * Equivalent of Python <code>str.strip()</code>. Any byte/character matching one of those in
     * <code>stripChars</code> will be discarded from either end of this <code>str</code>. If
     * <code>stripChars == null</code>, whitespace will be stripped. If <code>stripChars</code> is a
     * <code>PyUnicode</code>, the result will also be a <code>PyUnicode</code>.
     *
     * @param stripChars characters to strip from either end of this str/bytes, or null
     * @return a new <code>PyBytes</code> (or {@link PyUnicode}), stripped of the specified
     *         characters/bytes
     */
    public PyObject strip(PyObject stripChars) {
        return bytes_strip(stripChars);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytes_strip_doc)
    final PyObject bytes_strip(PyObject chars) {
        if (chars instanceof PyUnicode) {
            throw Py.TypeError(BYTES_REQUIRED_ERROR);
        } else {
            // It ought to be None, null, some kind of bytes with the buffer API.
            String stripChars = Encoding.asStringNullOrError(chars, "strip");
            // Strip specified characters or whitespace if stripChars == null
            return new PyBytes(Encoding._strip(getString(), stripChars), true);
        }
    }

    /**
     * Equivalent of Python <code>str.lstrip()</code> with no argument, meaning strip whitespace.
     * Any whitespace byte/character will be discarded from the left of this <code>str</code>.
     *
     * @return a new String, stripped of the whitespace characters/bytes
     */
    public String lstrip() {
        return Encoding._lstrip(getString()).toString();
    }

    /**
     * Equivalent of Python <code>str.lstrip()</code>.
     *
     * @param stripChars characters to strip from the left end of this str/bytes, or null
     * @return a new String, stripped of the specified characters/bytes
     */
    public String lstrip(String stripChars) {
        return Encoding._lstrip(getString(), stripChars).toString();
    }

    /**
     * Equivalent of Python <code>str.lstrip()</code>. Any byte/character matching one of those in
     * <code>stripChars</code> will be discarded from the left end of this <code>str</code>. If
     * <code>stripChars == null</code>, whitespace will be stripped. If <code>stripChars</code> is a
     * <code>PyUnicode</code>, the result will also be a <code>PyUnicode</code>.
     *
     * @param stripChars characters to strip from the left end of this str/bytes, or null
     * @return a new <code>PyBytes</code> (or {@link PyUnicode}), stripped of the specified
     *         characters/bytes
     */
    public PyObject lstrip(PyObject stripChars) {
        return bytes_lstrip(stripChars);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytes_lstrip_doc)
    final PyObject bytes_lstrip(PyObject chars) {
        if (chars instanceof PyUnicode) {
            throw Py.TypeError(BYTES_REQUIRED_ERROR);
        } else {
            // It ought to be None, null, some kind of bytes with the buffer API.
            String stripChars = Encoding.asStringNullOrError(chars, "lstrip");
            // Strip specified characters or whitespace if stripChars == null
            return new PyBytes(Encoding._lstrip(getString(), stripChars), true);
        }
    }

    /**
     * Equivalent of Python <code>str.rstrip()</code> with no argument, meaning strip whitespace.
     * Any whitespace byte/character will be discarded from the right end of this <code>str</code>.
     *
     * @return a new String, stripped of the whitespace characters/bytes
     */
    public String rstrip() {
        return Encoding._rstrip(getString());
    }

    /**
     * Equivalent of Python <code>str.rstrip()</code>.
     *
     * @param stripChars characters to strip from either end of this str/bytes, or null
     * @return a new String, stripped of the specified characters/bytes
     */
    public String rstrip(String stripChars) {
        return Encoding._rstrip(getString(), stripChars);
    }

    /**
     * Equivalent of Python <code>str.rstrip()</code>. Any byte/character matching one of those in
     * <code>stripChars</code> will be discarded from the right end of this <code>str</code>. If
     * <code>stripChars == null</code>, whitespace will be stripped. If <code>stripChars</code> is a
     * <code>PyUnicode</code>, the result will also be a <code>PyUnicode</code>.
     *
     * @param stripChars characters to strip from the right end of this str/bytes, or null
     * @return a new <code>PyBytes</code> (or {@link PyUnicode}), stripped of the specified
     *         characters/bytes
     */
    public PyObject rstrip(PyObject stripChars) {
        return bytes_rstrip(stripChars);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytes_rstrip_doc)
    final PyObject bytes_rstrip(PyObject chars) {
        if (chars instanceof PyUnicode) {
            throw Py.TypeError(BYTES_REQUIRED_ERROR);
        } else {
            // It ought to be None, null, some kind of bytes with the buffer API.
            String stripChars = Encoding.asStringNullOrError(chars, "rstrip");
            // Strip specified characters or whitespace if stripChars == null
            return new PyBytes(Encoding._rstrip(getString(), stripChars), true);
        }
    }

    /**
     * Equivalent to Python <code>str.split()</code>, splitting on runs of whitespace.
     *
     * @return list(str) result
     */
    public PyList split() {
        return toPyList(Encoding._split(getString(), null, -1));
    }

    /**
     * Equivalent to Python <code>str.split()</code>, splitting on a specified string.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @return list(str) result
     */
    public PyList split(String sep) {
        return toPyList(Encoding._split(getString(), sep, -1));
    }

    /**
     * Equivalent to Python <code>str.split()</code>, splitting on a specified string.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @param maxsplit maximum number of splits to make (there may be <code>maxsplit+1</code>
     *            parts).
     * @return list(str) result
     */
    public PyList split(String sep, int maxsplit) {
        return toPyList(Encoding._split(getString(), sep, maxsplit));
    }

    /**
     * Equivalent to Python <code>str.split()</code> returning a {@link PyList} of
     * <code>PyBytes</code>s (or <code>PyUnicode</code>s). The <code>str</code> will be split at
     * each occurrence of <code>sep</code>. If <code>sep == null</code>, whitespace will be used as
     * the criterion. If <code>sep</code> has zero length, a Python <code>ValueError</code> is
     * raised.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @return list(str) result
     */
    public PyList split(PyObject sep) {
        return bytes_split(new PyObject[]{sep}, Py.NoKeywords);
    }

    /**
     * As {@link #split(PyObject)} but if <code>maxsplit</code> &gt;=0 and there are more feasible
     * splits than <code>maxsplit</code>, the last element of the list contains the rest of the
     * string.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @param maxsplit maximum number of splits to make (there may be <code>maxsplit+1</code>
     *            parts).
     * @return list(str) result
     */
    public PyList split(PyObject sep, PyObject maxsplit) {
        return bytes_split(new PyObject[]{sep, maxsplit}, Py.NoKeywords);
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_split_doc)
    final PyList bytes_split(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("split", args, keywords, "sep", "maxsplit");
        PyObject sep = ap.getPyObject(0, Py.None);
        int maxsplit = ap.getInt(1, -1);
        // Split on specified string or whitespace if sep == null
        return toPyList(Encoding._split(getString(), Encoding.asStringNullOrError(sep, "sep"), maxsplit));
    }

    /**
     * Equivalent to Python <code>str.rsplit()</code>, splitting on runs of whitespace.
     *
     * @return list(str) result
     */
    public PyList rsplit() {
        return toPyList(Encoding._rsplit(getString(), null, -1));
    }

    /**
     * Equivalent to Python <code>str.rsplit()</code>, splitting on a specified string.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @return list(str) result
     */
    public PyList rsplit(String sep) {
        return toPyList(Encoding._rsplit(getString(), sep, -1));
    }

    /**
     * Equivalent to Python <code>str.rsplit()</code>, splitting on a specified string.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @param maxsplit maximum number of splits to make (there may be <code>maxsplit+1</code>
     *            parts).
     * @return list(str) result
     */
    public PyList rsplit(String sep, int maxsplit) {
        return toPyList(Encoding._rsplit(getString(), sep, maxsplit));
    }

    private static PyList toPyList(List<CharSequence> list) {
        return new PyList(Lists.transform(list, new Function<CharSequence, PyBytes>() {
            @Override
            public PyBytes apply(CharSequence charSequence) {
                return new PyBytes(charSequence);
            }
        }));
    }

    /**
     * Equivalent to Python <code>str.rsplit()</code> returning a {@link PyList} of
     * <code>PyBytes</code>s (or <code>PyUnicode</code>s). The <code>str</code> will be split at
     * each occurrence of <code>sep</code>, working from the right. If <code>sep == null</code>,
     * whitespace will be used as the criterion. If <code>sep</code> has zero length, a Python
     * <code>ValueError</code> is raised.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @return list(str) result
     */
    public PyList rsplit(PyObject sep) {
        return bytes_rsplit(new PyObject[]{sep}, Py.NoKeywords);
    }

    /**
     * As {@link #rsplit(PyObject)} but if <code>maxsplit</code> &gt;=0 and there are more feasible
     * splits than <code>maxsplit</code> the last element of the list contains the rest of the
     * string.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @param maxsplit maximum number of splits to make (there may be <code>maxsplit+1</code>
     *            parts).
     * @return list(str) result
     */
    public PyList rsplit(PyObject sep, PyObject maxsplit) {
        return bytes_rsplit(new PyObject[]{sep, maxsplit}, Py.NoKeywords);
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_split_doc)
    final PyList bytes_rsplit(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("rsplit", args, keywords, "sep", "maxsplit");
        PyObject sep = ap.getPyObject(0, Py.None);
        int maxsplit = ap.getInt(1, -1);
        // Split on specified string or whitespace if sep == null
        List<CharSequence> list = Encoding._rsplit(getString(), Encoding.asStringNullOrError(sep, "sep"), maxsplit);
        return new PyList(Lists.transform(list, new Function<CharSequence, PyBytes>() {
            @Override
            public PyBytes apply(CharSequence charSequence) {
                return new PyBytes(charSequence);
            }
        }));
    }

    /**
     * Equivalent to Python <code>str.partition()</code>, splits the <code>PyBytes</code> at the
     * first occurrence of <code>sepObj</code> returning a {@link PyTuple} containing the part
     * before the separator, the separator itself, and the part after the separator.
     *
     * @param sepObj str, unicode or object implementing {@link BufferProtocol}
     * @return tuple of parts
     */
    public PyTuple partition(PyObject sepObj) {
        return bytes_partition(sepObj);
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_partition_doc)
    final PyTuple bytes_partition(PyObject sepObj) {

        if (sepObj instanceof PyUnicode) {
            // Deal with Unicode separately
            throw Py.TypeError(BYTES_REQUIRED_ERROR);

        } else {
            // It ought to be some kind of bytes with the buffer API.
            String sep = Encoding.asStringOrError(sepObj);

            if (sep.length() == 0) {
                throw Py.ValueError("empty separator");
            }

            int index = getString().indexOf(sep);
            if (index != -1) {
                return new PyTuple(fromSubstring(0, index), sepObj, fromSubstring(
                        index + sep.length(), getString().length()));
            } else {
                return new PyTuple(this, Py.EmptyByte, Py.EmptyByte);
            }
        }
    }

    /**
     * Equivalent to Python <code>str.rpartition()</code>, splits the <code>PyBytes</code> at the
     * last occurrence of <code>sepObj</code> returning a {@link PyTuple} containing the part before
     * the separator, the separator itself, and the part after the separator.
     *
     * @param sepObj str, unicode or object implementing {@link BufferProtocol}
     * @return tuple of parts
     */
    public PyTuple rpartition(PyObject sepObj) {
        return bytes_rpartition(sepObj);
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_rpartition_doc)
    final PyTuple bytes_rpartition(PyObject sepObj) {

        if (sepObj instanceof PyUnicode) {
            throw Py.TypeError(BYTES_REQUIRED_ERROR);
        } else {
            // It ought to be some kind of bytes with the buffer API.
            String sep = Encoding.asStringOrError(sepObj);

            if (sep.length() == 0) {
                throw Py.ValueError("empty separator");
            }

            int index = getString().lastIndexOf(sep);
            if (index != -1) {
                return new PyTuple(fromSubstring(0, index), sepObj, fromSubstring(
                        index + sep.length(), getString().length()));
            } else {
                return new PyTuple(Py.EmptyByte, Py.EmptyByte, this);
            }
        }
    }

    public PyList splitlines() {
        return splitlines(false);
    }

    public PyList splitlines(boolean keepends) {
        return bytes_splitlines(new PyObject[]{Py.newBoolean(keepends)}, new String[]{});
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_splitlines_doc)
    final PyList bytes_splitlines(PyObject[] args, String[] keywords) {
        ArgParser arg = new ArgParser("splitlines", args, keywords, "keepends");
        boolean keepends = arg.getPyObject(0, Py.False).__bool__();

        return new PyList(Lists.transform(Encoding.splitlines(getString(), keepends), new Function<CharSequence, PyBytes>() {
            @Override
            public PyBytes apply(CharSequence charSequence) {
                return new PyBytes(charSequence);
            }
        }));
    }

    /**
     * Return a new object <em>of the same type as this one</em> equal to the slice
     * <code>[begin:end]</code>. (Python end-relative indexes etc. are not supported.) Subclasses (
     * {@link PyUnicode#fromSubstring(int, int)}) override this to return their own type.)
     *
     * @param begin first included character.
     * @param end first excluded character.
     * @return new object.
     */
    protected PyBytes fromSubstring(int begin, int end) {
        // Method is overridden in PyUnicode, so definitely a PyBytes
        return new PyBytes(getString().substring(begin, end), true);
    }

    /**
     * Return the lowest index in the string where substring <code>sub</code> is found. Raises
     * <code>ValueError</code> if the substring is not found.
     *
     * @param sub substring to find.
     * @return index of <code>sub</code> in this object.
     * @throws PyException(ValueError) if not found.
     */
    public int index(PyObject sub) {
        return bytes_index(sub, null, null);
    }

    /**
     * Return the lowest index in the string where substring <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>s[start:]</code>. Raises
     * <code>ValueError</code> if the substring is not found.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @return index of <code>sub</code> in this object.
     * @throws PyException(ValueError) if not found.
     */
    public int index(PyObject sub, PyObject start) throws PyException {
        return bytes_index(sub, start, null);
    }

    /**
     * Return the lowest index in the string where substring <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>s[start:end]</code>. Arguments
     * <code>start</code> and <code>end</code> are interpreted as in slice notation, with null or
     * {@link Py#None} representing "missing". Raises <code>ValueError</code> if the substring is
     * not found.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @param end end of slice.
     * @return index of <code>sub</code> in this object.
     * @throws PyException(ValueError) if not found.
     */
    public int index(PyObject sub, PyObject start, PyObject end) throws PyException {
        return checkIndex(bytes_index(sub, start, end));
    }

    /** Equivalent to {@link #index(PyObject)} specialized to <code>String</code>. */
    public int index(String sub) {
        return index(sub, null, null);
    }

    /** Equivalent to {@link #index(PyObject, PyObject)} specialized to <code>String</code>. */
    public int index(String sub, PyObject start) {
        return index(sub, start, null);
    }

    /**
     * Equivalent to {@link #index(PyObject, PyObject, PyObject)} specialized to <code>String</code>
     * .
     */
    public int index(String sub, PyObject start, PyObject end) {
        return checkIndex(Encoding._find(getString(), sub, start, end, __len__()));
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytes_index_doc)
    final int bytes_index(PyObject subObj, PyObject start, PyObject end) {
        return checkIndex(bytes_find(subObj, start, end));
    }

    /**
     * Return the highest index in the string where substring <code>sub</code> is found. Raises
     * <code>ValueError</code> if the substring is not found.
     *
     * @param sub substring to find.
     * @return index of <code>sub</code> in this object.
     * @throws PyException(ValueError) if not found.
     */
    public int rindex(PyObject sub) {
        return bytes_rindex(sub, null, null);
    }

    /**
     * Return the highest index in the string where substring <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>s[start:]</code>. Raises
     * <code>ValueError</code> if the substring is not found.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @return index of <code>sub</code> in this object.
     * @throws PyException(ValueError) if not found.
     */
    public int rindex(PyObject sub, PyObject start) throws PyException {
        return bytes_rindex(sub, start, null);
    }

    /**
     * Return the highest index in the string where substring <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>s[start:end]</code>. Arguments
     * <code>start</code> and <code>end</code> are interpreted as in slice notation, with null or
     * {@link Py#None} representing "missing". Raises <code>ValueError</code> if the substring is
     * not found.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @param end end of slice.
     * @return index of <code>sub</code> in this object.
     * @throws PyException(ValueError) if not found.
     */
    public int rindex(PyObject sub, PyObject start, PyObject end) throws PyException {
        return checkIndex(bytes_rindex(sub, start, end));
    }

    /** Equivalent to {@link #rindex(PyObject)} specialized to <code>String</code>. */
    public int rindex(String sub) {
        return rindex(sub, null, null);
    }

    /** Equivalent to {@link #rindex(PyObject, PyObject)} specialized to <code>String</code>. */
    public int rindex(String sub, PyObject start) {
        return rindex(sub, start, null);
    }

    /**
     * Equivalent to {@link #rindex(PyObject, PyObject, PyObject)} specialized to
     * <code>String</code>.
     */
    public int rindex(String sub, PyObject start, PyObject end) {
        return checkIndex(Encoding._rfind(getString(), sub, start, end, __len__()));
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytes_rindex_doc)
    final int bytes_rindex(PyObject subObj, PyObject start, PyObject end) {
        return checkIndex(bytes_rfind(subObj, start, end));
    }

    /**
     * A little helper for converting str.find to str.index that will raise
     * <code>ValueError("substring not found")</code> if the argument is negative, otherwise passes
     * the argument through.
     *
     * @param index to check
     * @return <code>index</code> if non-negative
     * @throws PyException(ValueError) if not found
     */
    protected final int checkIndex(int index) throws PyException {
        if (index >= 0) {
            return index;
        } else {
            throw Py.ValueError("substring not found");
        }
    }

    /**
     * Return the number of non-overlapping occurrences of substring <code>sub</code>.
     *
     * @param sub substring to find.
     * @return count of occurrences.
     */
    public int count(PyObject sub) {
        return count(sub, null, null);
    }

    /**
     * Return the number of non-overlapping occurrences of substring <code>sub</code> in the range
     * <code>[start:]</code>.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @return count of occurrences.
     */
    public int count(PyObject sub, PyObject start) {
        return count(sub, start, null);
    }

    /**
     * Return the number of non-overlapping occurrences of substring <code>sub</code> in the range
     * <code>[start:end]</code>. Optional arguments <code>start</code> and <code>end</code> are
     * interpreted as in slice notation.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @param end end of slice.
     * @return count of occurrences.
     */
    public int count(PyObject sub, PyObject start, PyObject end) {
        return bytes_count(sub, start, end);
    }

    /** Equivalent to {@link #count(PyObject)} specialized to <code>String</code>. */
    public int count(String sub) {
        return count(sub, null, null);
    }

    /** Equivalent to {@link #count(PyObject, PyObject)} specialized to <code>String</code>. */
    public int count(String sub, PyObject start) {
        return count(sub, start, null);
    }

    /**
     * Equivalent to {@link #count(PyObject, PyObject, PyObject)} specialized to <code>String</code>
     * .
     */
    public int count(String sub, PyObject start, PyObject end) {
        return Encoding._count(getString(), sub, start, end, __len__());
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytes_count_doc)
    final int bytes_count(PyObject subObj, PyObject start, PyObject end) {
        if (subObj instanceof PyUnicode) {
            throw Py.TypeError(BYTES_REQUIRED_ERROR);
        } else {
            // It ought to be some kind of bytes with the buffer API.
            String sub = Encoding.asStringOrError(subObj);
            return Encoding._count(getString(), sub, start, end, __len__());
        }
    }

    /**
     * Return the lowest index in the string where substring <code>sub</code> is found.
     *
     * @param sub substring to find.
     * @return index of <code>sub</code> in this object or -1 if not found.
     */
    public int find(PyObject sub) {
        return find(sub, null, null);
    }

    /**
     * Return the lowest index in the string where substring <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>s[start:]</code>.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @return index of <code>sub</code> in this object or -1 if not found.
     */
    public int find(PyObject sub, PyObject start) {
        return find(sub, start, null);
    }

    /**
     * Return the lowest index in the string where substring <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>s[start:end]</code>. Arguments
     * <code>start</code> and <code>end</code> are interpreted as in slice notation, with null or
     * {@link Py#None} representing "missing".
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @param end end of slice.
     * @return index of <code>sub</code> in this object or -1 if not found.
     */
    public int find(PyObject sub, PyObject start, PyObject end) {
        return bytes_find(sub, start, end);
    }

    /** Equivalent to {@link #find(PyObject)} specialized to <code>String</code>. */
    public int find(String sub) {
        return find(sub, null, null);
    }

    /** Equivalent to {@link #find(PyObject, PyObject)} specialized to <code>String</code>. */
    public int find(String sub, PyObject start) {
        return find(sub, start, null);
    }

    /**
     * Equivalent to {@link #find(PyObject, PyObject, PyObject)} specialized to <code>String</code>.
     */
    public int find(String sub, PyObject start, PyObject end) {
        return Encoding._find(getString(), sub, start, end, __len__());
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytes_find_doc)
    final int bytes_find(PyObject subObj, PyObject start, PyObject end) {
        if (subObj instanceof PyUnicode) {
            throw Py.TypeError(BYTES_REQUIRED_ERROR);
        } else {
            // It ought to be some kind of bytes with the buffer API.
            String sub = Encoding.asStringOrError(subObj);
            return Encoding._find(getString(), sub, start, end, __len__());
        }
    }


    /**
     * Return the highest index in the string where substring <code>sub</code> is found.
     *
     * @param sub substring to find.
     * @return index of <code>sub</code> in this object or -1 if not found.
     */
    public int rfind(PyObject sub) {
        return rfind(sub, null, null);
    }

    /**
     * Return the highest index in the string where substring <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>s[start:]</code>.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @return index of <code>sub</code> in this object or -1 if not found.
     */
    public int rfind(PyObject sub, PyObject start) {
        return rfind(sub, start, null);
    }

    /**
     * Return the highest index in the string where substring <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>s[start:end]</code>. Arguments
     * <code>start</code> and <code>end</code> are interpreted as in slice notation, with null or
     * {@link Py#None} representing "missing".
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @param end end of slice.
     * @return index of <code>sub</code> in this object or -1 if not found.
     */
    public int rfind(PyObject sub, PyObject start, PyObject end) {
        return bytes_rfind(sub, start, end);
    }

    /** Equivalent to {@link #find(PyObject)} specialized to <code>String</code>. */
    public int rfind(String sub) {
        return rfind(sub, null, null);
    }

    /** Equivalent to {@link #find(PyObject, PyObject)} specialized to <code>String</code>. */
    public int rfind(String sub, PyObject start) {
        return rfind(sub, start, null);
    }

    /**
     * Equivalent to {@link #find(PyObject, PyObject, PyObject)} specialized to <code>String</code>.
     */
    public int rfind(String sub, PyObject start, PyObject end) {
        return Encoding._rfind(getString(), sub, start, end, __len__());
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytes_rfind_doc)
    final int bytes_rfind(PyObject subObj, PyObject start, PyObject end) {
        if (subObj instanceof PyUnicode) {
            throw Py.TypeError(BYTES_REQUIRED_ERROR);
        } else {
            // It ought to be some kind of bytes with the buffer API.
            String sub = Encoding.asStringOrError(subObj);
            return Encoding._rfind(getString(), sub, start, end, __len__());
        }
    }

    private static String padding(int n, char pad) {
        char[] chars = new char[n];
        for (int i = 0; i < n; i++) {
            chars[i] = pad;
        }
        return new String(chars);
    }

    private static char parse_fillchar(String function, String fillchar) {
        if (fillchar == null) {
            return ' ';
        }
        if (fillchar.length() != 1) {
            throw Py.TypeError(function + "() argument 2 must be char, not str");
        }
        return fillchar.charAt(0);
    }

    public PyBytes ljust(int width) {
        return bytes_ljust(width, null);
    }

    public PyBytes ljust(int width, String padding) {
        return bytes_ljust(width, padding);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytes_ljust_doc)
    final PyBytes bytes_ljust(int width, String fillchar) {
        char pad = parse_fillchar("ljust", fillchar);
        int n = width - getString().length();
        if (n <= 0) {
            return this;
        }
        return new PyBytes(getString() + padding(n, pad));
    }

    public PyBytes rjust(int width) {
        return bytes_rjust(width, null);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytes_rjust_doc)
    final PyBytes bytes_rjust(int width, String fillchar) {
        char pad = parse_fillchar("rjust", fillchar);
        int n = width - getString().length();
        if (n <= 0) {
            return this;
        }
        return new PyBytes(padding(n, pad) + getString());
    }

    public PyBytes center(int width) {
        return bytes_center(width, null);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytes_center_doc)
    final PyBytes bytes_center(int width, String fillchar) {
        char pad = parse_fillchar("center", fillchar);
        int n = width - getString().length();
        if (n <= 0) {
            return this;
        }
        int half = n / 2;
        if (n % 2 > 0 && width % 2 > 0) {
            half += 1;
        }

        return new PyBytes(padding(half, pad) + getString() + padding(n - half, pad));
    }

    public PyBytes zfill(int width) {
        return bytes_zfill(width);
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_zfill_doc)
    final PyBytes bytes_zfill(int width) {
        return new PyBytes(Encoding.zfill(getString(), width).toString());
    }

    public PyBytes expandtabs() {
        return bytes_expandtabs(8);
    }

    public PyBytes expandtabs(int tabsize) {
        return bytes_expandtabs(tabsize);
    }

    @ExposedMethod(defaults = "8", doc = BuiltinDocs.bytes_expandtabs_doc)
    final PyBytes bytes_expandtabs(int tabsize) {
        return new PyBytes(Encoding.expandtabs(getString(), tabsize));
    }

    public PyBytes capitalize() {
        return bytes_capitalize();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_capitalize_doc)
    final PyBytes bytes_capitalize() {
        return new PyBytes(Encoding.capitalize(getString()));
    }

    /**
     * Equivalent to Python str.replace(old, new), returning a copy of the string with all
     * occurrences of substring old replaced by new. If either argument is a {@link PyUnicode} (or
     * this object is), the result will be a <code>PyUnicode</code>.
     *
     * @param oldPiece to replace where found.
     * @param newPiece replacement text.
     * @return PyBytes (or PyUnicode if any string is one), this string after replacements.
     */
    public PyBytes replace(PyObject oldPiece, PyObject newPiece) {
        return bytes_replace(oldPiece, newPiece, -1);
    }

    /**
     * Equivalent to Python str.replace(old, new[, count]), returning a copy of the string with all
     * occurrences of substring old replaced by new. If argument <code>count</code> is nonnegative,
     * only the first <code>count</code> occurrences are replaced. If either argument is a
     * {@link PyUnicode} (or this object is), the result will be a <code>PyUnicode</code>.
     *
     * @param oldPiece to replace where found.
     * @param newPiece replacement text.
     * @param count maximum number of replacements to make, or -1 meaning all of them.
     * @return PyBytes (or PyUnicode if any string is one), this string after replacements.
     */
    public PyBytes replace(PyObject oldPiece, PyObject newPiece, int count) {
        return bytes_replace(oldPiece, newPiece, count);
    }

    @ExposedMethod(defaults = "-1", doc = BuiltinDocs.bytes_replace_doc)
    final PyBytes bytes_replace(PyObject oldPieceObj, PyObject newPieceObj, int count) {
        if (oldPieceObj instanceof PyUnicode || newPieceObj instanceof PyUnicode) {
            throw Py.TypeError(BYTES_REQUIRED_ERROR);
        } else {
            // Neither is a PyUnicode: both ought to be some kind of bytes with the buffer API.
            String oldPiece = Encoding.asStringOrError(oldPieceObj, false);
            String newPiece = Encoding.asStringOrError(newPieceObj, false);
            return new PyBytes(Encoding._replace(getString(), oldPiece, newPiece, count));
        }
    }

    public PyBytes join(PyObject seq) {
        return bytes_join(seq);
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_join_doc)
    final PyBytes bytes_join(PyObject obj) {
        PySequence seq = fastSequence(obj, "");
        int seqLen = seq.__len__();
        if (seqLen == 0) {
            return Py.EmptyByte;
        }

        PyObject item;
        if (seqLen == 1) {
            item = seq.pyget(0);
            return new PyBytes(Py.unwrapBuffer(item));
        }

        // There are at least two things to join, or else we have a subclass of the
        // builtin types in the sequence. Do a pre-pass to figure out the total amount of
        // space we'll need, see whether any argument is absurd, and defer to the Unicode
        // join if appropriate

        // Catenate everything
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < seqLen; i++) {
            item = seq.pyget(i);
            if (i != 0) {
                buf.append(getString());
            }
            for (byte b : Py.unwrapBuffer(item)) {
                buf.appendCodePoint(b & 0xFF);
            }
        }
        return new PyBytes(buf.toString(), true); // Guaranteed to be byte-like
    }

    /**
     * Equivalent to the Python <code>str.startswith</code> method testing whether a string starts
     * with a specified prefix. <code>prefix</code> can also be a tuple of prefixes to look for.
     *
     * @param prefix string to check for (or a <code>PyTuple</code> of them).
     * @return <code>true</code> if this string slice starts with a specified prefix, otherwise
     *         <code>false</code>.
     */
    public boolean startswith(PyObject prefix) {
        return bytes_startswith(prefix, null, null);
    }

    /**
     * Equivalent to the Python <code>str.startswith</code> method, testing whether a string starts
     * with a specified prefix, where a sub-range is specified by <code>[start:]</code>.
     * <code>start</code> is interpreted as in slice notation, with null or {@link Py#None}
     * representing "missing". <code>prefix</code> can also be a tuple of prefixes to look for.
     *
     * @param prefix string to check for (or a <code>PyTuple</code> of them).
     * @param start start of slice.
     * @return <code>true</code> if this string slice starts with a specified prefix, otherwise
     *         <code>false</code>.
     */
    public boolean startswith(PyObject prefix, PyObject start) {
        return bytes_startswith(prefix, start, null);
    }

    /**
     * Equivalent to the Python <code>str.startswith</code> method, testing whether a string starts
     * with a specified prefix, where a sub-range is specified by <code>[start:end]</code>.
     * Arguments <code>start</code> and <code>end</code> are interpreted as in slice notation, with
     * null or {@link Py#None} representing "missing". <code>prefix</code> can also be a tuple of
     * prefixes to look for.
     *
     * @param prefix string to check for (or a <code>PyTuple</code> of them).
     * @param start start of slice.
     * @param end end of slice.
     * @return <code>true</code> if this string slice starts with a specified prefix, otherwise
     *         <code>false</code>.
     */
    public boolean startswith(PyObject prefix, PyObject start, PyObject end) {
        return bytes_startswith(prefix, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_startswith_doc)
    final boolean bytes_startswith(PyObject prefix, PyObject startObj, PyObject endObj) {
        if (prefix instanceof PyUnicode) {
            throw Py.TypeError("startswith first arg must be bytes or a tuple of bytes, not str");
        }
        return Encoding.startswith(getString(), prefix, startObj, endObj, __len__());
    }

    /**
     * Equivalent to the Python <code>str.endswith</code> method, testing whether a string ends with
     * a specified suffix. <code>suffix</code> can also be a tuple of suffixes to look for.
     *
     * @param suffix string to check for (or a <code>PyTuple</code> of them).
     * @return <code>true</code> if this string slice ends with a specified suffix, otherwise
     *         <code>false</code>.
     */
    public boolean endswith(PyObject suffix) {
        return bytes_endswith(suffix, null, null);
    }

    /**
     * Equivalent to the Python <code>str.endswith</code> method, testing whether a string ends with
     * a specified suffix, where a sub-range is specified by <code>[start:]</code>.
     * <code>start</code> is interpreted as in slice notation, with null or {@link Py#None}
     * representing "missing". <code>suffix</code> can also be a tuple of suffixes to look for.
     *
     * @param suffix string to check for (or a <code>PyTuple</code> of them).
     * @param start start of slice.
     * @return <code>true</code> if this string slice ends with a specified suffix, otherwise
     *         <code>false</code>.
     */
    public boolean endswith(PyObject suffix, PyObject start) {
        return bytes_endswith(suffix, start, null);
    }

    /**
     * Equivalent to the Python <code>str.endswith</code> method, testing whether a string ends with
     * a specified suffix, where a sub-range is specified by <code>[start:end]</code>. Arguments
     * <code>start</code> and <code>end</code> are interpreted as in slice notation, with null or
     * {@link Py#None} representing "missing". <code>suffix</code> can also be a tuple of suffixes
     * to look for.
     *
     * @param suffix string to check for (or a <code>PyTuple</code> of them).
     * @param start start of slice.
     * @param end end of slice.
     * @return <code>true</code> if this string slice ends with a specified suffix, otherwise
     *         <code>false</code>.
     */
    public boolean endswith(PyObject suffix, PyObject start, PyObject end) {
        return bytes_endswith(suffix, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_endswith_doc)
    final boolean bytes_endswith(PyObject suffix, PyObject startObj, PyObject endObj) {
        return Encoding.endswith(getString(), suffix, startObj, endObj, __len__());
    }

    /**
     * Many of the string methods deal with slices specified using Python slice semantics:
     * endpoints, which are <code>PyObject</code>s, may be <code>null</code> or <code>None</code>
     * (meaning default to one end or the other) or may be negative (meaning "from the end").
     * Meanwhile, the implementation methods need integer indices, both within the array, and
     * <code>0&lt;=start&lt;=end&lt;=N</code> the length of the array.
     * <p>
     * This method first translates the Python slice <code>startObj</code> and <code>endObj</code>
     * according to the slice semantics for null and negative values, and stores these in elements 2
     * and 3 of the result. Then, since the end points of the range may lie outside this sequence's
     * bounds (in either direction) it reduces them to the nearest points satisfying
     * <code>0&lt;=start&lt;=end&lt;=N</code>, and stores these in elements [0] and [1] of the
     * result.
     *
     * @param startObj Python start of slice
     * @param endObj Python end of slice
     * @return a 4 element array of two range-safe indices, and two original indices.
     */
    protected int[] translateIndices(PyObject startObj, PyObject endObj) {
        return Encoding.translateIndices(getString(), startObj, endObj, __len__());
    }

    /**
     * Equivalent to Python <code>str.translate</code> returning a copy of this string where the
     * characters have been mapped through the translation <code>table</code>. <code>table</code>
     * must be equivalent to a string of length 256 (if it is not <code>null</code>).
     *
     * @param table of character (byte) translations (or <code>null</code>)
     * @return transformed byte string
     */
    public PyBytes translate(PyObject table) {
        return translate(table, null);
    }

    /**
     * Equivalent to Python <code>str.translate</code> returning a copy of this string where all
     * characters (bytes) occurring in the argument <code>deletechars</code> are removed (if it is
     * not <code>null</code>), and the remaining characters have been mapped through the translation
     * <code>table</code>. <code>table</code> must be equivalent to a string of length 256 (if it is
     * not <code>null</code>).
     *
     * @param table of character (byte) translations (or <code>null</code>)
     * @param deletechars set of characters to remove (or <code>null</code>)
     * @return transformed byte string
     */
    public PyBytes translate(PyObject table, PyObject deletechars) {
        return bytes_translate(table, deletechars);
    }

    /**
     * Equivalent to {@link #translate(PyObject)} specialized to <code>String</code>.
     */
    public PyBytes translate(String table) {
        return _translate(table, null);
    }

    /**
     * Equivalent to {@link #translate(PyObject, PyObject)} specialized to <code>String</code>.
     */
    public PyBytes translate(String table, String deletechars) {
        return _translate(table, deletechars);
    }

    @ExposedMethod(defaults = {"null"}, doc = BuiltinDocs.bytes_translate_doc)
    final PyBytes bytes_translate(PyObject tableObj, PyObject deletecharsObj) {
        String table = Encoding.asStringOrNull(tableObj);
        String deletechars = null;
        if (deletecharsObj != null) {
            deletechars = Encoding.asStringOrError(deletecharsObj);
        }
        // Accept anythiong with the buffer API or null
        return _translate(table, deletechars);
    }

    /**
     * Helper common to the Python and Java API implementing <code>str.translate</code> returning a
     * copy of this string where all characters (bytes) occurring in the argument
     * <code>deletechars</code> are removed (if it is not <code>null</code>), and the remaining
     * characters have been mapped through the translation <code>table</code>, which must be
     * equivalent to a string of length 256 (if it is not <code>null</code>).
     *
     * @param table of character (byte) translations (or <code>null</code>)
     * @param deletechars set of characters to remove (or <code>null</code>)
     * @return transformed byte string
     */
    private final PyBytes _translate(String table, String deletechars) {

        if (table != null && table.length() != 256) {
            throw Py.ValueError("translation table must be 256 characters long");
        }

        StringBuilder buf = new StringBuilder(getString().length());

        for (int i = 0; i < getString().length(); i++) {
            char c = getString().charAt(i);
            if (deletechars != null && deletechars.indexOf(c) >= 0) {
                continue;
            }
            if (table == null) {
                buf.append(c);
            } else {
                try {
                    buf.append(table.charAt(c));
                } catch (IndexOutOfBoundsException e) {
                    throw Py.TypeError("translate() only works for 8-bit character strings");
                }
            }
        }
        return new PyBytes(buf.toString());
    }

    public static PyObject maketrans(PyUnicode fromstr, PyUnicode tostr) {
        return bytes_maketrans(TYPE, fromstr, tostr, null);
    }

    public static PyObject maketrans(PyUnicode fromstr, PyUnicode tostr, PyUnicode other) {
        return bytes_maketrans(TYPE, fromstr, tostr, other);
    }

    @ExposedClassMethod(defaults = {"null"}, doc = BuiltinDocs.bytes_maketrans_doc)
    static final PyObject bytes_maketrans(PyType type, PyObject fromstr, PyObject tostr, PyObject other) {
        if (fromstr.__len__() != tostr.__len__()) {
            throw Py.ValueError("maketrans arguments must have same length");
        }
        byte[] res = new byte[256];
        for (int i = 0; i < 256; i++) {
            res[i] = (byte) i;
        }
        try(
                PyBuffer frm = BaseBytes.getViewOrError(fromstr);
                PyBuffer to = BaseBytes.getViewOrError(tostr)
                ) {
            for (int i = 0; i < frm.getLen(); i++) {
                res[frm.byteAt(i) & 0xFF] = to.byteAt(i);
            }
        }
        return new PyBytes(new String(res, StandardCharsets.ISO_8859_1));
    }

    public boolean islower() {
        return bytes_islower();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_islower_doc)
    final boolean bytes_islower() {
        return Encoding.isLowercase(getString());
    }

    public boolean isupper() {
        return bytes_isupper();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_isupper_doc)
    final boolean bytes_isupper() {
        return Encoding.isUppercase(getString());
    }

    public boolean isalpha() {
        return bytes_isalpha();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_isalpha_doc)
    final boolean bytes_isalpha() {
        return Encoding.isAlpha(getString());
    }

    public boolean isalnum() {
        return bytes_isalnum();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_isalnum_doc)
    final boolean bytes_isalnum() {
        return Encoding.isAlnum(getString());
    }

    private boolean _isalnum(char ch) {
        // This can ever be entirely compatible with CPython. In CPython
        // The type is not used, the numeric property is determined from
        // the presense of digit, decimal or numeric fields. These fields
        // are not available in exactly the same way in java.
        return Character.isLetterOrDigit(ch) || Character.getType(ch) == Character.LETTER_NUMBER;
    }

    public boolean isdecimal() {
        return bytes_isdecimal();
    }

    @ExposedMethod(doc = BuiltinDocs.str_isdecimal_doc)
    final boolean bytes_isdecimal() {
        return Encoding.isDecimal(getString());
    }

    public boolean isdigit() {
        return bytes_isdigit();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_isdigit_doc)
    final boolean bytes_isdigit() {
        return Encoding.isDigit(getString());
    }

    public boolean isnumeric() {
        return bytes_isnumeric();
    }

    @ExposedMethod(doc = BuiltinDocs.str_isnumeric_doc)
    final boolean bytes_isnumeric() {
        return Encoding.isNumeric(getString());
    }

    public boolean istitle() {
        return bytes_istitle();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_istitle_doc)
    final boolean bytes_istitle() {
        return Encoding.isTitle(getString());
    }

    public boolean isspace() {
        return bytes_isspace();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_isspace_doc)
    final boolean bytes_isspace() {
        return Encoding.isSpace(getString());
    }

    public PyObject decode() {
        return decode(null, null);
    }

    public PyObject decode(String encoding) {
        return decode(encoding, null);
    }

    public PyObject decode(String encoding, String errors) {
        return codecs.decode(this, encoding, errors);
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_decode_doc)
    final PyObject bytes_decode(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("decode", args, keywords, "encoding", "errors");
        String encoding = ap.getString(0, "UTF-8");
        String errors = ap.getString(1, "strict");
        return decode(encoding, errors);
    }

    @ExposedMethod(doc = ""/*BuiltinDocs.str__formatter_parser_doc*/)
    final PyObject bytes__formatter_parser() {
        return new MarkupIterator(getString());
    }

    @ExposedMethod(doc = ""/*BuiltinDocs.str__formatter_field_name_split_doc*/)
    final PyObject bytes__formatter_field_name_split() {
        FieldNameIterator iterator = new FieldNameIterator(getString(), true);
        return new PyTuple(iterator.pyHead(), iterator);
    }

    @Override
    public PyObject __format__(PyObject formatSpec) {
        return bytes___format__(formatSpec);
    }

    @ExposedMethod(doc = BuiltinDocs.bytes___format___doc)
    final PyObject bytes___format__(PyObject formatSpec) {
        return Encoding.format(getString(), formatSpec, true);
    }

    @Override
    public String asString(int index) throws PyObject.ConversionException {
        return getString();
    }

    @Override
    public String asString() {
        return getString();
    }

    @Override
    public int asInt() {
        // We have to override asInt/Long/Double because we override __int/long/float__,
        // but generally don't want implicit atoi conversions for the base types. blah
        asNumberCheck("__int__", "an integer");
        return super.asInt();
    }

    @Override
    public long asLong() {
        asNumberCheck("__int__", "an integer");
        return super.asLong();
    }

    @Override
    public double asDouble() {
        asNumberCheck("__float__", "a float");
        return super.asDouble();
    }

    private void asNumberCheck(String methodName, String description) {
        PyType type = getType();
        if (type == PyBytes.TYPE || type == PyUnicode.TYPE || type.lookup(methodName) == null) {
            throw Py.TypeError(description + " is required");
        }
    }

    @Override
    protected String unsupportedopMessage(String op, PyObject o2) {
        if (op.equals("+")) {
            return "cannot concatenate ''{1}'' and ''{2}'' objects";
        }
        return super.unsupportedopMessage(op, o2);
    }
}


