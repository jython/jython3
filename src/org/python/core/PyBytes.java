package org.python.core;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.python.core.buffer.BaseBuffer;
import org.python.core.buffer.SimpleBuffer;
import org.python.core.buffer.SimpleWritableBuffer;
import org.python.core.util.Allocator;
import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * Implementation of Python <code>bytes</code> with a Java API that includes equivalents to most
 * of the Python API. These Python equivalents accept a {@link PyObject} as argument, where you
 * might have expected a <code>byte[]</code> or <code>PyByteArray</code>, in order to accommodate
 * the full range of types accepted by the Python equivalent: usually, any <code>PyObject</code>
 * that implements {@link BufferProtocol}, providing a one-dimensional array of bytes, is an
 * acceptable argument. In the documentation, the reader will often see the terms "bytes" or
 * "object viewable as bytes" instead of <code>bytes</code> when this broader scope is intended.
 * This may relate to parameters, or to the target object itself (in text that applies equally to
 * base or sibling classes).
 */
@Untraversable
@ExposedType(name = "bytes", base = PyObject.class, doc = BuiltinDocs.bytes_doc)
public class PyBytes extends BaseBytes implements BufferProtocol {

    /** The {@link PyType} of <code>bytes</code>. */
    public static final PyType TYPE = PyType.fromClass(PyBytes.class);

    /**
     * Constructs a zero-length Python <code>bytes</code> of explicitly-specified sub-type
     *
     * @param type explicit Jython type
     */
    public PyBytes(PyType type) {
        super(type);
    }

    /**
     * Constructs a zero-length Python <code>bytes</code>.
     */
    public PyBytes() {
        super(TYPE);
    }

    /**
     * Constructs zero-filled Python <code>bytes</code> of specified size.
     *
     * @param size of <code>bytes</code>
     */
    public PyBytes(int size) {
        super(TYPE);
        init(size);
    }

    /**
     * Constructs a <code>bytes</code> by copying values from int[].
     *
     * @param value source of the bytes (and size)
     */
    public PyBytes(int[] value) {
        super(TYPE, value);
    }

    /**
     * Constructs a new array filled exactly by a copy of the contents of the source, which is a
     * <code>bytes</code> (or <code>bytes</code>).
     *
     * @param value source of the bytes (and size)
     */
    public PyBytes(BaseBytes value) {
        super(TYPE);
        init(value);
    }

    /**
     * Constructs a new array filled exactly by a copy of the contents of the source, which is a
     * byte-oriented {@link PyBuffer}.
     *
     * @param value source of the bytes (and size)
     */
    PyBytes(PyBuffer value) {
        super(TYPE);
        init(value);
    }

    /**
     * Constructs a new array filled exactly by a copy of the contents of the source, which is an
     * object supporting the Jython version of the PEP 3118 buffer API.
     *
     * @param value source of the bytes (and size)
     */
    public PyBytes(BufferProtocol value) {
        super(TYPE);
        init(value);
    }

    /**
     * Constructs a new array filled from an iterable of PyObject. The iterable must yield objects
     * convertible to Python bytes (non-negative integers less than 256 or strings of length 1).
     *
     * @param value source of the bytes (and size)
     */
    public PyBytes(Iterable<? extends PyObject> value) {
        super(TYPE);
        init(value);
    }

    /**
     * Constructs a new array by encoding a PyString argument to bytes. If the PyString is actually
     * a PyUnicode, the encoding must be explicitly specified.
     *
     * @param arg primary argument from which value is taken
     * @param encoding name of optional encoding (must be a string type)
     * @param errors name of optional errors policy (must be a string type)
     */
    public PyBytes(PyString arg, PyObject encoding, PyObject errors) {
        super(TYPE);
        init(arg, encoding, errors);
    }

    /**
     * Constructs a new array by encoding a PyString argument to bytes. If the PyString is actually
     * a PyUnicode, the encoding must be explicitly specified.
     *
     * @param arg primary argument from which value is taken
     * @param encoding name of optional encoding (may be <code>null</code> to select the default for
     *            this installation)
     * @param errors name of optional errors policy
     */
    public PyBytes(PyString arg, String encoding, String errors) {
        super(TYPE);
        init(arg, encoding, errors);
    }

    /**
     * Constructs a new array by encoding a PyString argument to bytes. If the PyString is actually
     * a PyUnicode, an exception is thrown saying that the encoding must be explicitly specified.
     *
     * @param arg primary argument from which value is taken
     */
    public PyBytes(PyString arg) {
        super(TYPE);
        init(arg, (String)null, (String)null);
    }

    /**
     * Constructs a <code>bytes</code> by re-using an array of byte as storage initialised by
     * the client.
     *
     * @param storage pre-initialised with desired value: the caller should not keep a reference
     */
    public PyBytes(byte[] storage) {
        super(TYPE);
        setStorage(storage);
    }

    /**
     * Constructs a <code>bytes</code> by re-using an array of byte as storage initialised by
     * the client.
     *
     * @param storage pre-initialised with desired value: the caller should not keep a reference
     * @param size number of bytes actually used
     * @throws IllegalArgumentException if the range [0:size] is not within the array bounds of the
     *             storage.
     */
    public PyBytes(byte[] storage, int size) {
        super(TYPE);
        setStorage(storage, size);
    }

    /**
     * Constructs a new <code>bytes</code> object from an arbitrary Python object according to
     * the same rules as apply in Python to the <code>bytes()</code> constructor:
     * <ul>
     * <li><code>bytes()</code> Construct a zero-length <code>bytes</code>.</li>
     * <li><code>bytes(int)</code> Construct a zero-initialized <code>bytes</code> of the
     * given length.</li>
     * <li><code>bytes(iterable_of_ints)</code> Construct from iterable yielding integers in
     * [0..255]</li>
     * <li><code>bytes(buffer)</code> Construct by reading from any object implementing
     * {@link BufferProtocol}, including <code>str/bytes</code> or another <code>bytes</code>.</li>
     * </ul>
     * When it is necessary to specify an encoding, as in the Python signature
     * <code>bytes(string, encoding [, errors])</code>, use the constructor
     * {@link #PyByteArray(PyString, String, String)}. If the <code>PyString</code> is actually a
     * <code>PyUnicode</code>, an encoding must be specified, and using this constructor will throw
     * an exception about that.
     *
     * @param arg primary argument from which value is taken (may be <code>null</code>)
     * @throws PyException (TypeError) for non-iterable,
     * @throws PyException (ValueError) if iterables do not yield byte [0..255] values.
     */
    public PyBytes(PyObject arg) throws PyException {
        super(TYPE);
        init(arg);
    }

    public PyBytes(PyType type, PyBytes contents) {
        super(TYPE);
        init((BaseBytes)contents);
    }

    /**
     * The <code>bytes</code> literals stored in .class files are actually stored as strings in
     * fake ISO 8859-1 coding; this function will convert such a string to a <code>bytes</code>
     * @param constant the string constant
     * @return a newly created bytes
     */
    public static PyBytes fromStringConstant(String constant) {
        return new PyBytes(constant.getBytes(StandardCharsets.ISO_8859_1));
    }

    /*
     * ============================================================================================
     * Support for the Buffer API
     * ============================================================================================
     *
     * The buffer API allows other classes to access the storage directly.
     */

    /**
     * Hold weakly a reference to a PyBuffer export not yet released, used to prevent untimely
     * resizing.
     */
    private WeakReference<BaseBuffer> export;

    /**
     * {@inheritDoc}
     * <p>
     * The {@link PyBuffer} returned from this method is a one-dimensional array of single byte
     * items that allows modification of the object state. The existence of this export <b>prohibits
     * resizing</b> the bytes. This prohibition is not only on the consumer of the view but
     * extends to any other operations, such as any kind or insertion or deletion.
     */
    @Override
    public synchronized PyBuffer getBuffer(int flags) {

        // If we have already exported a buffer it may still be available for re-use
        BaseBuffer pybuf = getExistingBuffer(flags);

        if (pybuf == null) {
            // No existing export we can re-use: create a new one
            pybuf = new SimpleBuffer(flags, storage, offset, size);
            // Hold a reference for possible re-use
            export = new WeakReference<BaseBuffer>(pybuf);
        }

        return pybuf;
    }

    /**
     * Try to re-use an existing exported buffer, or return <code>null</code> if we can't.
     *
     * @throws PyException (BufferError) if the the flags are incompatible with the buffer
     */
    private BaseBuffer getExistingBuffer(int flags) throws PyException {
        BaseBuffer pybuf = null;
        if (export != null) {
            // A buffer was exported at some time.
            pybuf = export.get();
            if (pybuf != null) {
                /*
                 * We do not test for pybuf.isReleased() as, if any operation had taken place that
                 * invalidated the buffer, resizeCheck() would have set export=null. The exported
                 * buffer (navigation, buf member, etc.) remains valid through any operation that
                 * does not need a resizeCheck.
                 */
                pybuf = pybuf.getBufferAgain(flags);
            }
        }
        return pybuf;
    }

    /*
     * ============================================================================================
     * API for org.python.core.PySequence
     * ============================================================================================
     */

    /**
     * Returns a slice of elements from this sequence as a <code>PyByteArray</code>.
     *
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @return a <code>PyByteArray</code> corresponding the the given range of elements.
     */
    @Override
    protected synchronized PyBytes getslice(int start, int stop, int step) {
        if (step == 1) {
            // Efficiently copy contiguous slice
            return this.getslice(start, stop);
        } else {
            int n = sliceLength(start, stop, step);
            PyBytes ret = new PyBytes(n);
            n += ret.offset;
            byte[] dst = ret.storage;
            for (int io = start + offset, jo = ret.offset; jo < n; io += step, jo++) {
                dst[jo] = storage[io];
            }
            return ret;
        }
    }

    /**
     * Specialisation of {@link #getslice(int, int, int)} to contiguous slices (of step size 1) for
     * brevity and efficiency.
     */
    @Override
    protected synchronized PyBytes getslice(int start, int stop) {
        // If this were immutable, start==0 and end==size we would return (this).
        // Efficiently copy contiguous slice
        int n = stop - start;
        if (n <= 0) {
            return new PyBytes();
        } else {
            PyBytes ret = new PyBytes(n);
            System.arraycopy(storage, offset + start, ret.storage, ret.offset, n);
            return ret;
        }
    }

    /**
     * Returns a <code>PyByteArray</code> that repeats this sequence the given number of times, as
     * in the implementation of <tt>__mul__</tt> for strings.
     *
     * @param count the number of times to repeat this.
     * @return this bytes repeated count times.
     */
    @Override
    protected synchronized PyBytes repeat(int count) {
        PyBytes ret = new PyBytes();
        ret.setStorage(repeatImpl(count));
        return ret;
    }

    /**
     * Raise error for pyset
     *
     * @param index index of the element to set.
     * @param value the value to set this element to.
     * @throws PyException (AttributeError) if value cannot be converted to an integer
     * @throws PyException (ValueError) if value<0 or value>255
     */
    @Override
    public synchronized void pyset(int index, PyObject value) throws PyException {
        throw Py.TypeError(String.format("'%s' object does not support item assignment", getType()
                .fastGetName()));
    }

    @Override
    protected synchronized void del(int index) {
        throw Py.TypeError(String.format("'%s' object does not support item deletion", getType()
                .fastGetName()));
    }

    @Override
    protected synchronized void delRange(int start, int stop) {
        throw Py.TypeError(String.format("'%s' object does not support item deletion", getType()
                .fastGetName()));
    }

    @Override
    protected synchronized void delslice(int start, int stop, int step, int n) {
        throw Py.TypeError(String.format("'%s' object does not support item deletion", getType()
                .fastGetName()));
    }

    /**
     * Initialise a mutable <code>bytes</code> object from various arguments. This single
     * initialisation must support:
     * <ul>
     * <li><code>bytes()</code> Construct a zero-length <code>bytes</code>.</li>
     * <li><code>bytes(int)</code> Construct a zero-initialized <code>bytes</code> of the
     * given length.</li>
     * <li><code>bytes(iterable_of_ints)</code> Construct from iterable yielding integers in
     * [0..255]</li>
     * <li><code>bytes(buffer)</code> Construct by reading from any object implementing
     * {@link BufferProtocol}, including <code>str/bytes</code> or another <code>bytes</code>.</li>
     * <li><code>bytes(string, encoding [, errors])</code> Construct from a
     * <code>str/bytes</code>, decoded using the system default encoding, and encoded to bytes using
     * the specified encoding.</li>
     * <li><code>bytes(unicode, encoding [, errors])</code> Construct from a
     * <code>unicode</code> string, encoded to bytes using the specified encoding.</li>
     * </ul>
     * Although effectively a constructor, it is possible to call <code>__init__</code> on a 'used'
     * object so the method does not assume any particular prior state.
     *
     * @param args argument array according to Jython conventions
     * @param kwds Keywords according to Jython conventions
     * @return 
     * @throws PyException (TypeError) for non-iterable,
     * @throws PyException (ValueError) if iterables do not yield byte [0..255] values.
     */
    @ExposedNew
    static PyObject bytes___new__(
            PyNewWrapper new_, boolean init, PyType subtype, 
            PyObject[] args, String[] kwds)
    {
        ArgParser ap = new ArgParser("bytes", args, kwds, "source", "encoding", "errors");
        PyObject arg = ap.getPyObject(0, null);
        // If not null, encoding and errors must be PyString (or PyUnicode)
        PyObject encoding = ap.getPyObjectByType(1, PyBaseString.TYPE, null);
        PyObject errors = ap.getPyObjectByType(2, PyBaseString.TYPE, null);

        PyBytes value;
        if (encoding != null || errors != null) {
            /*
             * bytes(string [, encoding [, errors]]) Construct from a text string by encoding it
             * using the specified encoding.
             */
            if (arg == null || !(arg instanceof PyString)) {
                throw Py.TypeError("encoding or errors without sequence argument");
            }
            
            value = new PyBytes((PyString)arg, encoding, errors);
        }
        else {
            value = new PyBytes(arg);
        }
        
        if (new_.for_type == subtype) {
            return value;
        } else {
            return new PyBytesDerived(subtype, value);
        }
    }

    /*
     * ============================================================================================
     * Support for Builder
     * ============================================================================================
     *
     * Extend BaseBytes.Builder so that it can return a PyByteArray and give the superclass a hook
     * for it.
     */

    @Override
    protected Builder getBuilder(int capacity) {
        // Return a Builder specialised for my class
        return new Builder(capacity) {

            @Override
            PyBytes getResult() {
                // Create a PyByteArray from the storage that the builder holds
                return new PyBytes(getStorage(), getSize());
            }
        };
    }

    /*
     * ============================================================================================
     * Python API rich comparison operations
     * ============================================================================================
     */

    @Override
    public PyObject __eq__(PyObject other) {
        return basebytes___eq__(other);
    }

    @Override
    public PyObject __ne__(PyObject other) {
        return basebytes___ne__(other);
    }

    @Override
    public PyObject __lt__(PyObject other) {
        return basebytes___lt__(other);
    }

    @Override
    public PyObject __le__(PyObject other) {
        return basebytes___le__(other);
    }

    @Override
    public PyObject __ge__(PyObject other) {
        return basebytes___ge__(other);
    }

    @Override
    public PyObject __gt__(PyObject other) {
        return basebytes___gt__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytes___eq___doc)
    final synchronized PyObject bytes___eq__(PyObject other) {
        return basebytes___eq__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytes___ne___doc)
    final synchronized PyObject bytes___ne__(PyObject other) {
        return basebytes___ne__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytes___lt___doc)
    final synchronized PyObject bytes___lt__(PyObject other) {
        return basebytes___lt__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytes___le___doc)
    final synchronized PyObject bytes___le__(PyObject other) {
        return basebytes___le__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytes___ge___doc)
    final synchronized PyObject bytes___ge__(PyObject other) {
        return basebytes___ge__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytes___gt___doc)
    final synchronized PyObject bytes___gt__(PyObject other) {
        return basebytes___gt__(other);
    }

/*
 * ============================================================================================
 * Python API for bytes
 * ============================================================================================
 */

    @Override
    public PyObject __add__(PyObject o) {
        return bytes___add__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytes___add___doc)
    final synchronized PyObject bytes___add__(PyObject o) {
        PyBytes sum = null;

        // XXX re-write using buffer API

        if (o instanceof BaseBytes) {
            BaseBytes ob = (BaseBytes)o;
            // Quick route: allocate the right size bytes and copy the two parts in.
            sum = new PyBytes(size + ob.size);
            System.arraycopy(storage, offset, sum.storage, sum.offset, size);
            System.arraycopy(ob.storage, ob.offset, sum.storage, sum.offset + size, ob.size);

        } else {
            // Unsuitable type
            // XXX note reversed order relative to __iadd__ may be wrong, matches Python 2.7
            throw ConcatenationTypeError(TYPE, o.getType());
        }

        return sum;
    }

    /**
     * Equivalent to the standard Python <code>__mul__</code> method, that for a bytes returns
     * a new bytes containing the same thing n times.
     */
    @Override
    public PyObject __mul__(PyObject n) {
        return bytes___mul__(n);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytes___mul___doc)
    final PyObject bytes___mul__(PyObject n) {
        if (!n.isIndex()) {
            return null;
        }
        return repeat(n.asIndex(Py.OverflowError));
    }

    /**
     * Equivalent to the standard Python <code>__rmul__</code> method, that for a bytes returns
     * a new bytes containing the same thing n times.
     */
    @Override
    public PyObject __rmul__(PyObject n) {
        return bytes___rmul__(n);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytes___rmul___doc)
    final PyObject bytes___rmul__(PyObject n) {
        if (!n.isIndex()) {
            return null;
        }
        return repeat(n.asIndex(Py.OverflowError));
    }

    /**
     * Implement to the standard Python __contains__ method, which in turn implements the
     * <code>in</code> operator.
     *
     * @param o the element to search for in this <code>bytes</code>.
     * @return the result of the search.
     **/
    @Override
    public boolean __contains__(PyObject o) {
        return basebytes___contains__(o);
    }

    @ExposedMethod(doc = BuiltinDocs.bytes___contains___doc)
    final boolean bytes___contains__(PyObject o) {
        return basebytes___contains__(o);
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_decode_doc)
    final PyObject bytes_decode(PyObject[] args, String[] keywords) {
        return basebytes_decode(args, keywords);
    }

    /**
     * Java API equivalent of Python <code>center(width)</code>: return the bytes centered in an
     * array of length <code>width</code>, padded by spaces. A copy of the original bytes is
     * returned if width is less than <code>this.size()</code>.
     *
     * @param width desired
     * @return new bytes containing result
     */
    public PyBytes center(int width) {
        return (PyBytes)basebytes_center(width, " ");
    }

    /**
     * Java API equivalent of Python <code>center(width [, fillchar])</code>: return the bytes
     * centered in an array of length <code>width</code>. Padding is done using the specified
     * fillchar (default is a space). A copy of the original bytes is returned if
     * <code>width</code> is less than <code>this.size()</code>.
     *
     * @param width desired
     * @param fillchar one-byte String to fill with, or <code>null</code> implying space
     * @return new bytes containing the result
     */
    public PyBytes center(int width, String fillchar) {
        return (PyBytes)basebytes_center(width, fillchar);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytes_center_doc)
    final PyBytes bytes_center(int width, String fillchar) {
        return (PyBytes)basebytes_center(width, fillchar);
    }

    /**
     * Implementation of Python <code>count(sub)</code>. Return the number of non-overlapping
     * occurrences of <code>sub</code> in this bytes.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @return count of occurrences of sub within this bytes
     */
    public int count(PyObject sub) {
        return basebytes_count(sub, null, null);
    }

    /**
     * Implementation of Python <code>count( sub [, start ] )</code>. Return the number of
     * non-overlapping occurrences of <code>sub</code> in the range [start:].
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @return count of occurrences of sub within this bytes
     */
    public int count(PyObject sub, PyObject start) {
        return basebytes_count(sub, start, null);
    }

    /**
     * Implementation of Python <code>count( sub [, start [, end ]] )</code>. Return the number of
     * non-overlapping occurrences of <code>sub</code> in the range [start, end]. Optional arguments
     * <code>start</code> and <code>end</code> (which may be <code>null</code> or
     * <code>Py.None</code> ) are interpreted as in slice notation.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @param end of slice to search
     * @return count of occurrences of sub within this bytes
     */
    public int count(PyObject sub, PyObject start, PyObject end) {
        return basebytes_count(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytes_count_doc)
    final int bytes_count(PyObject sub, PyObject start, PyObject end) {
        return basebytes_count(sub, start, end);
    }

    /**
     * Implementation of Python <code>endswith(suffix)</code>.
     *
     * When <code>suffix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this <code>bytes</code> ends with the
     * <code>suffix</code>. <code>suffix</code> can also be a tuple of suffixes to look for.
     *
     * @param suffix bytes to match, or object viewable as such, or a tuple of them
     * @return true if and only if this <code>bytes</code> ends with the suffix (or one of them)
     */
    public boolean endswith(PyObject suffix) {
        return basebytes_starts_or_endswith(suffix, null, null, true);
    }

    /**
     * Implementation of Python <code>endswith( suffix [, start ] )</code>.
     *
     * When <code>suffix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this <code>bytes</code> ends with the
     * <code>suffix</code>. <code>suffix</code> can also be a tuple of suffixes to look for. With
     * optional <code>start</code> (which may be <code>null</code> or <code>Py.None</code>), define
     * the effective <code>bytes</code> to be the slice <code>[start:]</code> of this
     * <code>bytes</code>.
     *
     * @param suffix bytes to match, or object viewable as such, or a tuple of them
     * @param start of slice in this <code>bytes</code> to match
     * @return true if and only if this[start:] ends with the suffix (or one of them)
     */
    public boolean endswith(PyObject suffix, PyObject start) {
        return basebytes_starts_or_endswith(suffix, start, null, true);
    }

    /**
     * Implementation of Python <code>endswith( suffix [, start [, end ]] )</code>.
     *
     * When <code>suffix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this <code>bytes</code> ends with the
     * <code>suffix</code>. <code>suffix</code> can also be a tuple of suffixes to look for. With
     * optional <code>start</code> and <code>end</code> (which may be <code>null</code> or
     * <code>Py.None</code>), define the effective <code>bytes</code> to be the slice
     * <code>[start:end]</code> of this <code>bytes</code>.
     *
     * @param suffix bytes to match, or object viewable as such, or a tuple of them
     * @param start of slice in this <code>bytes</code> to match
     * @param end of slice in this <code>bytes</code> to match
     * @return true if and only if this[start:end] ends with the suffix (or one of them)
     */
    public boolean endswith(PyObject suffix, PyObject start, PyObject end) {
        return basebytes_starts_or_endswith(suffix, start, end, true);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytes_endswith_doc)
    final boolean bytes_endswith(PyObject suffix, PyObject start, PyObject end) {
        return basebytes_starts_or_endswith(suffix, start, end, true);
    }

    /**
     * Implementation of Python <code>expandtabs()</code>: return a copy of the bytes where all
     * tab characters are replaced by one or more spaces, as {@link #expandtabs(int)} with a tab
     * size of 8 characters.
     *
     * @return copy of this bytes with tabs expanded
     */
    public PyBytes expandtabs() {
        return (PyBytes)basebytes_expandtabs(8);
    }

    /**
     * Implementation of Python <code>expandtabs(tabsize)</code>: return a copy of the bytes
     * where all tab characters are replaced by one or more spaces, depending on the current column
     * and the given tab size. The column number is reset to zero after each newline occurring in
     * the array. This treats other non-printing characters or escape sequences as regular
     * characters.
     *
     * @param tabsize number of character positions between tab stops
     * @return copy of this bytes with tabs expanded
     */
    public PyBytes expandtabs(int tabsize) {
        return (PyBytes)basebytes_expandtabs(tabsize);
    }

    @ExposedMethod(defaults = "8", doc = BuiltinDocs.bytes_expandtabs_doc)
    final PyBytes bytes_expandtabs(int tabsize) {
        return (PyBytes)basebytes_expandtabs(tabsize);
    }

    /**
     * Implementation of Python <code>find(sub)</code>. Return the lowest index in the bytes
     * where byte sequence <code>sub</code> is found. Return -1 if <code>sub</code> is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @return index of start of occurrence of sub within this bytes
     */
    public int find(PyObject sub) {
        return basebytes_find(sub, null, null);
    }

    /**
     * Implementation of Python <code>find( sub [, start ] )</code>. Return the lowest index in the
     * bytes where byte sequence <code>sub</code> is found, such that <code>sub</code> is
     * contained in the slice <code>[start:]</code>. Return -1 if <code>sub</code> is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @return index of start of occurrence of sub within this bytes
     */
    public int find(PyObject sub, PyObject start) {
        return basebytes_find(sub, start, null);
    }

    /**
     * Implementation of Python <code>find( sub [, start [, end ]] )</code>. Return the lowest index
     * in the bytes where byte sequence <code>sub</code> is found, such that <code>sub</code>
     * is contained in the slice <code>[start:end]</code>. Arguments <code>start</code> and
     * <code>end</code> (which may be <code>null</code> or <code>Py.None</code> ) are interpreted as
     * in slice notation. Return -1 if <code>sub</code> is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @param end of slice to search
     * @return index of start of occurrence of sub within this bytes
     */
    public int find(PyObject sub, PyObject start, PyObject end) {
        return basebytes_find(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytes_find_doc)
    final int bytes_find(PyObject sub, PyObject start, PyObject end) {
        return basebytes_find(sub, start, end);
    }

    /**
     * Implementation of Python class method <code>bytes.fromhex(string)</code>, that returns .
     * a new <code>PyByteArray</code> with a value taken from a string of two-digit hexadecimal
     * numbers. Spaces (but not whitespace in general) are acceptable around the numbers, not
     * within. Non-hexadecimal characters or un-paired hex digits raise a <code>ValueError</code>. *
     * Example:
     *
     * <pre>
     * bytes.fromhex('B9 01EF') -> * bytes(b'\xb9\x01\xef')."
     * </pre>
     *
     * @param hex specification of the bytes
     * @throws PyException (ValueError) if non-hex characters, or isolated ones, are encountered
     */
    static PyBytes fromhex(String hex) throws PyException {
        return bytes_fromhex(TYPE, hex);
    }

    @ExposedClassMethod(doc = BuiltinDocs.bytes_fromhex_doc)
    static PyBytes bytes_fromhex(PyType type, String hex) {
        // I think type tells us the actual class but we always return exactly a bytes
        // PyObject ba = type.__call__();
        PyBytes result = new PyBytes();
        basebytes_fromhex(result, hex);
        return result;
    }

    @ExposedMethod(doc = BuiltinDocs.bytes___getitem___doc)
    final synchronized PyObject bytes___getitem__(PyObject index) {
        // Let the SequenceIndexDelegate take care of it
        return delegator.checkIdxAndGetItem(index);
    }

    /**
     * Implementation of Python <code>index(sub)</code>. Like {@link #find(PyObject)} but raise
     * {@link Py#ValueError} if <code>sub</code> is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @return index of start of occurrence of sub within this bytes
     */
    public int index(PyObject sub) {
        return bytes_index(sub, null, null);
    }

    /**
     * Implementation of Python <code>index( sub [, start ] )</code>. Like
     * {@link #find(PyObject,PyObject)} but raise {@link Py#ValueError} if <code>sub</code> is not
     * found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @return index of start of occurrence of sub within this bytes
     */
    public int index(PyObject sub, PyObject start) {
        return bytes_index(sub, start, null);
    }

    /**
     * This type is not hashable.
     *
     * @throws PyException (TypeError) as this type is not hashable.
     */
    @Override
    public int hashCode() throws PyException {
        return bytes___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes___hash___doc)
    final int bytes___hash__() {
        throw Py.TypeError(String.format("unhashable type: '%.200s'", getType().fastGetName()));
    }

    /**
     * Implementation of Python <code>index( sub [, start [, end ]] )</code>. Like
     * {@link #find(PyObject,PyObject,PyObject)} but raise {@link Py#ValueError} if <code>sub</code>
     * is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @param end of slice to search
     * @return index of start of occurrence of sub within this bytes
     * @throws PyException ValueError if sub not found in bytes
     */
    public int index(PyObject sub, PyObject start, PyObject end) throws PyException {
        return bytes_index(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytes_index_doc)
    final int bytes_index(PyObject sub, PyObject start, PyObject end) {
        // Like find but raise a ValueError if not found
        int pos = basebytes_find(sub, start, end);
        if (pos < 0) {
            throw Py.ValueError("subsection not found");
        }
        return pos;
    }

    //
    // Character class operations
    //

    @ExposedMethod(doc = BuiltinDocs.bytes_isalnum_doc)
    final boolean bytes_isalnum() {
        return basebytes_isalnum();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_isalpha_doc)
    final boolean bytes_isalpha() {
        return basebytes_isalpha();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_isdigit_doc)
    final boolean bytes_isdigit() {
        return basebytes_isdigit();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_islower_doc)
    final boolean bytes_islower() {
        return basebytes_islower();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_isspace_doc)
    final boolean bytes_isspace() {
        return basebytes_isspace();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_istitle_doc)
    final boolean bytes_istitle() {
        return basebytes_istitle();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_isupper_doc)
    final boolean bytes_isupper() {
        return basebytes_isupper();
    }

    //
    // Case transformations
    //

    @ExposedMethod(doc = BuiltinDocs.bytes_capitalize_doc)
    final PyBytes bytes_capitalize() {
        return (PyBytes)basebytes_capitalize();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_lower_doc)
    final PyBytes bytes_lower() {
        return (PyBytes)basebytes_lower();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_swapcase_doc)
    final PyBytes bytes_swapcase() {
        return (PyBytes)basebytes_swapcase();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_title_doc)
    final PyBytes bytes_title() {
        return (PyBytes)basebytes_title();
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_upper_doc)
    final PyBytes bytes_upper() {
        return (PyBytes)basebytes_upper();
    }

    /**
     * Implementation of Python <code>join(iterable)</code>. Return a <code>bytes</code> which
     * is the concatenation of the bytess in the iterable <code>iterable</code>. The separator
     * between elements is the bytes providing this method.
     *
     * @param iterable of bytes objects, or objects viewable as such.
     * @return bytes produced by concatenation.
     */
    public PyBytes join(PyObject iterable) {
        return bytes_join(iterable);
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_join_doc)
    final PyBytes bytes_join(PyObject iterable) {
        return basebytes_join(iterable.asIterable(), new Allocator<PyBytes>() {
            @Override
            public PyBytes allocate(int size) {
                return new PyBytes(size);
            }
        });
    }

    @ExposedMethod(doc = BuiltinDocs.bytes___len___doc)
    final int bytes___len__() {
        return super.__len__();
    }

    /**
     * Java API equivalent of Python <code>ljust(width)</code>: return the bytes left justified in
     * an array of length <code>width</code>, padded by spaces. A copy of the original bytes is
     * returned if width is less than <code>this.size()</code>.
     *
     * @param width desired
     * @return new bytes containing result
     */
    public PyBytes ljust(int width) {
        return (PyBytes)basebytes_ljust(width, " ");
    }

    /**
     * Java API equivalent of Python <code>ljust(width [, fillchar])</code>: return the bytes
     * left-justified in an array of length <code>width</code>. Padding is done using the specified
     * fillchar (default is a space). A copy of the original bytes is returned if
     * <code>width</code> is less than <code>this.size()</code>.
     *
     * @param width desired
     * @param fillchar one-byte String to fill with, or <code>null</code> implying space
     * @return new bytes containing the result
     */
    public PyBytes ljust(int width, String fillchar) {
        return (PyBytes)basebytes_ljust(width, fillchar);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytes_ljust_doc)
    final PyBytes bytes_ljust(int width, String fillchar) {
        // If this was immutable and width<=this.size we could return (this).
        return (PyBytes)basebytes_ljust(width, fillchar);
    }

    /**
     * Implementation of Python <code>lstrip()</code>. Return a copy of the bytes with the
     * leading whitespace characters removed.
     *
     * @return a bytes containing this value stripped of those bytes
     */
    public PyBytes lstrip() {
        return bytes_lstrip(null);
    }

    /**
     * Implementation of Python <code>lstrip(bytes)</code>
     *
     * Return a copy of the bytes with the leading characters removed. The bytes argument is an
     * object specifying the set of characters to be removed. If <code>null</code> or
     * <code>None</code>, the bytes argument defaults to removing whitespace. The bytes argument is
     * not a prefix; rather, all combinations of its values are stripped.
     *
     * @param bytes treated as a set of bytes defining what values to strip
     * @return a bytes containing this value stripped of those bytes (at the left)
     */
    public PyBytes lstrip(PyObject bytes) {
        return bytes_lstrip(bytes);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytes_lstrip_doc)
    final synchronized PyBytes bytes_lstrip(PyObject bytes) {
        int left;
        if (bytes == null || bytes == Py.None) {
            // Find left bound of the slice that results from the stripping of whitespace
            left = lstripIndex();
        } else {
            // Find left bound of the slice that results from the stripping of the specified bytes
            ByteSet byteSet = new ByteSet(getViewOrError(bytes));
            left = lstripIndex(byteSet);
        }
        return getslice(left, size);
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_partition_doc)
    final PyTuple bytes_partition(PyObject sep) {
        return basebytes_partition(sep);
    }

    @ExposedMethod(doc = BuiltinDocs.bytes___reduce___doc)
    final PyObject bytes___reduce__() {
        return basebytes___reduce__();
    }

    /**
     * Implementation of Python <code>rfind(sub)</code>. Return the highest index in the bytes
     * where byte sequence <code>sub</code> is found. Return -1 if <code>sub</code> is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @return index of start of rightmost occurrence of sub within this bytes
     */
    public int rfind(PyObject sub) {
        return basebytes_rfind(sub, null, null);
    }

    /**
     * Implementation of Python <code>rfind( sub [, start ] )</code>. Return the highest index in
     * the bytes where byte sequence <code>sub</code> is found, such that <code>sub</code> is
     * contained in the slice <code>[start:]</code>. Return -1 if <code>sub</code> is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @return index of start of rightmost occurrence of sub within this bytes
     */
    public int rfind(PyObject sub, PyObject start) {
        return basebytes_rfind(sub, start, null);
    }

    /**
     * Implementation of Python <code>rfind( sub [, start [, end ]] )</code>. Return the highest
     * index in the bytes where byte sequence <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>[start:end]</code>. Arguments
     * <code>start</code> and <code>end</code> (which may be <code>null</code> or
     * <code>Py.None</code> ) are interpreted as in slice notation. Return -1 if <code>sub</code> is
     * not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @param end of slice to search
     * @return index of start of rightmost occurrence of sub within this bytes
     */
    public int rfind(PyObject sub, PyObject start, PyObject end) {
        return basebytes_rfind(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytes_rfind_doc)
    final int bytes_rfind(PyObject sub, PyObject start, PyObject end) {
        return basebytes_rfind(sub, start, end);
    }

    /**
     * Implementation of Python <code>rindex(sub)</code>. Like {@link #find(PyObject)} but raise
     * {@link Py#ValueError} if <code>sub</code> is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @return index of start of occurrence of sub within this bytes
     */
    public int rindex(PyObject sub) {
        return bytes_rindex(sub, null, null);
    }

    /**
     * Implementation of Python <code>rindex( sub [, start ] )</code>. Like
     * {@link #find(PyObject,PyObject)} but raise {@link Py#ValueError} if <code>sub</code> is not
     * found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @return index of start of occurrence of sub within this bytes
     */
    public int rindex(PyObject sub, PyObject start) {
        return bytes_rindex(sub, start, null);
    }

    /**
     * Java API equivalent of Python <code>rjust(width)</code>: return the bytes right justified in
     * an array of length <code>width</code>, padded by spaces. A copy of the original bytes is
     * returned if width is less than <code>this.size()</code>.
     *
     * @param width desired
     * @return new bytes containing result
     */
    public PyBytes rjust(int width) {
        return (PyBytes)basebytes_rjust(width, " ");
    }

    /**
     * Java API equivalent of Python <code>rjust(width [, fillchar])</code>: return the bytes
     * right-justified in an array of length <code>width</code>. Padding is done using the specified
     * fillchar (default is a space). A copy of the original bytes is returned if
     * <code>width</code> is less than <code>this.size()</code>.
     *
     * @param width desired
     * @param fillchar one-byte String to fill with, or <code>null</code> implying space
     * @return new bytes containing the result
     */
    public PyBytes rjust(int width, String fillchar) {
        return (PyBytes)basebytes_rjust(width, fillchar);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytes_rjust_doc)
    final PyBytes bytes_rjust(int width, String fillchar) {
        return (PyBytes)basebytes_rjust(width, fillchar);
    }

    /**
     * Implementation of Python <code>rindex( sub [, start [, end ]] )</code>. Like
     * {@link #find(PyObject,PyObject,PyObject)} but raise {@link Py#ValueError} if <code>sub</code>
     * is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @param end of slice to search
     * @return index of start of occurrence of sub within this bytes
     */
    public int rindex(PyObject sub, PyObject start, PyObject end) {
        return bytes_rindex(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytes_rindex_doc)
    final int bytes_rindex(PyObject sub, PyObject start, PyObject end) {
        // Like rfind but raise a ValueError if not found
        int pos = basebytes_rfind(sub, start, end);
        if (pos < 0) {
            throw Py.ValueError("subsection not found");
        }
        return pos;
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_rpartition_doc)
    final PyTuple bytes_rpartition(PyObject sep) {
        return basebytes_rpartition(sep);
    }

    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.bytes_rsplit_doc)
    final PyList bytes_rsplit(PyObject sep, int maxsplit) {
        return basebytes_rsplit(sep, maxsplit);
    }

    /**
     * Implementation of Python <code>rstrip()</code>. Return a copy of the bytes with the
     * trailing whitespace characters removed.
     *
     * @return a bytes containing this value stripped of those bytes (at right)
     */
    public PyBytes rstrip() {
        return bytes_rstrip(null);
    }

    /**
     * Implementation of Python <code>rstrip(bytes)</code>
     *
     * Return a copy of the bytes with the trailing characters removed. The bytes argument is
     * an object specifying the set of characters to be removed. If <code>null</code> or
     * <code>None</code>, the bytes argument defaults to removing whitespace. The bytes argument is
     * not a suffix; rather, all combinations of its values are stripped.
     *
     * @param bytes treated as a set of bytes defining what values to strip
     * @return a bytes containing this value stripped of those bytes (at right)
     */
    public PyBytes rstrip(PyObject bytes) {
        return bytes_rstrip(bytes);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytes_rstrip_doc)
    final synchronized PyBytes bytes_rstrip(PyObject bytes) {
        int right;
        if (bytes == null || bytes == Py.None) {
            // Find right bound of the slice that results from the stripping of whitespace
            right = rstripIndex();
        } else {
            // Find right bound of the slice that results from the stripping of the specified bytes
            ByteSet byteSet = new ByteSet(getViewOrError(bytes));
            right = rstripIndex(byteSet);
        }
        return getslice(0, right);
    }

    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.bytes_split_doc)
    final PyList bytes_split(PyObject sep, int maxsplit) {
        return basebytes_split(sep, maxsplit);
    }

    @ExposedMethod(defaults = "false", doc = BuiltinDocs.bytes_splitlines_doc)
    final PyList bytes_splitlines(boolean keepends) {
        return basebytes_splitlines(keepends);
    }

    /**
     * Implementation of Python <code>startswith(prefix)</code>.
     *
     * When <code>prefix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this <code>bytes</code> starts with the
     * <code>prefix</code>. <code>prefix</code> can also be a tuple of prefixes to look for.
     *
     * @param prefix bytes to match, or object viewable as such, or a tuple of them
     * @return true if and only if this <code>bytes</code> starts with the prefix (or one of
     *         them)
     */
    public boolean startswith(PyObject prefix) {
        return basebytes_starts_or_endswith(prefix, null, null, false);
    }

    /**
     * Implementation of Python <code>startswith( prefix [, start ] )</code>.
     *
     * When <code>prefix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this <code>bytes</code> starts with the
     * <code>prefix</code>. <code>prefix</code> can also be a tuple of prefixes to look for. With
     * optional <code>start</code> (which may be <code>null</code> or <code>Py.None</code>), define
     * the effective <code>bytes</code> to be the slice <code>[start:]</code> of this
     * <code>bytes</code>.
     *
     * @param prefix bytes to match, or object viewable as such, or a tuple of them
     * @param start of slice in this <code>bytes</code> to match
     * @return true if and only if this[start:] starts with the prefix (or one of them)
     */
    public boolean startswith(PyObject prefix, PyObject start) {
        return basebytes_starts_or_endswith(prefix, start, null, false);
    }

    /**
     * Implementation of Python <code>startswith( prefix [, start [, end ]] )</code>.
     *
     * When <code>prefix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this <code>bytes</code> starts with the
     * <code>prefix</code>. <code>prefix</code> can also be a tuple of prefixes to look for. With
     * optional <code>start</code> and <code>end</code> (which may be <code>null</code> or
     * <code>Py.None</code>), define the effective <code>bytes</code> to be the slice
     * <code>[start:end]</code> of this <code>bytes</code>.
     *
     * @param prefix bytes to match, or object viewable as such, or a tuple of them
     * @param start of slice in this <code>bytes</code> to match
     * @param end of slice in this <code>bytes</code> to match
     * @return true if and only if this[start:end] starts with the prefix (or one of them)
     */
    public boolean startswith(PyObject prefix, PyObject start, PyObject end) {
        return basebytes_starts_or_endswith(prefix, start, end, false);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytes_startswith_doc)
    final boolean bytes_startswith(PyObject prefix, PyObject start, PyObject end) {
        return basebytes_starts_or_endswith(prefix, start, end, false);
    }

    /**
     * Implementation of Python <code>strip()</code>. Return a copy of the bytes with the
     * leading and trailing whitespace characters removed.
     *
     * @return a bytes containing this value stripped of those bytes (left and right)
     */
    public PyBytes strip() {
        return bytes_strip(null);
    }

    /**
     * Implementation of Python <code>strip(bytes)</code>
     *
     * Return a copy of the bytes with the leading and trailing characters removed. The bytes
     * argument is anbytest specifying the set of characters to be removed. If
     * <code>null</code> or <code>None</code>, the bytes argument defaults to removing whitespace.
     * The bytes argument is not a prefix or suffix; rather, all combinations of its values are
     * stripped.
     *
     * @param bytes treated as a set of bytes defining what values to strip
     * @return a bytes containing this value stripped of those bytes (left and right)
     */
    public PyBytes strip(PyObject bytes) {
        return bytes_strip(bytes);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytes_strip_doc)
    final synchronized PyBytes bytes_strip(PyObject bytes) {
        int left, right;
        if (bytes == null || bytes == Py.None) {
            // Find bounds of the slice that results from the stripping of whitespace
            left = lstripIndex();
            // If we hit the end that time, no need to work backwards
            right = (left == size) ? size : rstripIndex();
        } else {
            // Find bounds of the slice that results from the stripping of the specified bytes
            ByteSet byteSet = new ByteSet(getViewOrError(bytes));
            left = lstripIndex(byteSet);
            // If we hit the end that time, no need to work backwards
            right = (left == size) ? size : rstripIndex(byteSet);
        }
        return getslice(left, right);
    }

    /**
     * An overriding of the standard Java {@link #toString()} method, returning a printable
     * expression of this bytes in the form <code>bytes(b'hello')</code>, where in the
     * "inner string", any special characters are escaped to their well-known backslash equivalents
     * or a hexadecimal escape. The built-in function <code>repr()</code> is expected to call this
     * method, and wraps the result in a Python <code>str</code>.
     */
    @Override
    public String toString() {
        return bytes_repr();
    }

    @ExposedMethod(names = {"__repr__"}, doc = BuiltinDocs.bytes___repr___doc)
    final synchronized String bytes_repr() {
        return basebytes_repr("b", "");
    }

    /**
     * An overriding of the {@link PyObject#__str__()} method, returning <code>PyString</code>,
     * where in the characters are simply those with a point-codes given in this bytes. The
     * built-in function <code>str()</code> is expected to call this method.
     */
    @Override
    public PyString __str__() {
        return bytes_str();
    }

    @ExposedMethod(names = {"__str__"}, doc = BuiltinDocs.bytes___str___doc)
    final PyString bytes_str() {
        return new PyString(this.asString());
    }

    /**
     * Implementation of Python <code>translate(table).</code>
     *
     * Return a copy of the bytes where all bytes occurring in the optional argument
     * <code>deletechars</code> are removed, and the remaining bytes have been mapped through the
     * given translation table, which must be of length 256.
     *
     * @param table length 256 translation table (of a type that may be regarded as a bytes)
     * @return translated bytes
     */
    public PyBytes translate(PyObject table) {
        return bytes_translate(table, null);
    }

    /**
     * Implementation of Python <code>translate(table[, deletechars]).</code>
     *
     * Return a copy of the bytes where all bytes occurring in the optional argument
     * <code>deletechars</code> are removed, and the remaining bytes have been mapped through the
     * given translation table, which must be of length 256.
     *
     * You can use the Python <code>maketrans()</code> helper function in the <code>string</code>
     * module to create a translation table. For string objects, set the table argument to
     * <code>None</code> for translations that only delete characters:
     *
     * @param table length 256 translation table (of a type that may be regarded as a bytes)
     * @param deletechars object that may be regarded as a bytes, defining bytes to delete
     * @return translated bytes
     */
    public PyBytes translate(PyObject table, PyObject deletechars) {
        return bytes_translate(table, deletechars);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytes_translate_doc)
    final PyBytes bytes_translate(PyObject table, PyObject deletechars) {

        // Work with the translation table (if there is one) as a PyBuffer view.
        try (PyBuffer tab = getTranslationTable(table)) {

            // Accumulate the result here
            PyByteArray result = new PyByteArray();

            // There are 4 cases depending on presence/absence of table and deletechars

            if (deletechars != null) {

                // Work with the deletion characters as a buffer too.
                try (PyBuffer d = getViewOrError(deletechars)) {
                    // Use a ByteSet to express which bytes to delete
                    ByteSet del = new ByteSet(d);
                    int limit = offset + size;
                    if (tab == null) {
                        // No translation table, so we're just copying with omissions
                        for (int i = offset; i < limit; i++) {
                            int b = storage[i] & 0xff;
                            if (!del.contains(b)) {
                                result.append((byte)b);
                            }
                        }
                    } else {
                        // Loop over this bytes and write translated bytes to the result
                        for (int i = offset; i < limit; i++) {
                            int b = storage[i] & 0xff;
                            if (!del.contains(b)) {
                                result.append(tab.byteAt(b));
                            }
                        }
                    }
                }

            } else {
                // No deletion set.
                if (tab == null) {
                    // And no translation so we may return
                    // this without modification, or a new 
                    // PyBytes with the same contents.
                    if (this.getClass() == PyBytes.class) {
                        return this;
                    }
                    else {
                        return new PyBytes((BaseBytes)this);
                    }
                } else {
                    int limit = offset + size;
                    // Loop over this bytes and write translated bytes to the result
                    for (int i = offset; i < limit; i++) {
                        int b = storage[i] & 0xff;
                        result.append(tab.byteAt(b));
                    }
                }
            }

            return new PyBytes((BaseBytes)result);
        }
    }

    /**
     * Return a {@link PyBuffer} representing a translation table, or raise an exception if it is
     * the wrong size. The caller is responsible for calling {@link PyBuffer#release()} on any
     * returned buffer.
     *
     * @param table the translation table (or <code>null</code> or {@link PyNone})
     * @return the buffer view of the table or null if there is no table
     * @throws PyException if the table is not exacltly 256 bytes long
     */
    private PyBuffer getTranslationTable(PyObject table) throws PyException {
        PyBuffer tab = null;
        // Normalise the translation table to a View (if there is one).
        if (table != null && table != Py.None) {
            tab = getViewOrError(table);
            if (tab.getLen() != 256) {
                throw Py.ValueError("translation table must be 256 bytes long");
            }
        }
        return tab;
    }

    /**
     * Implementation of Python <code>zfill(width):</code> return the numeric string left filled
     * with zeros in a bytes of length <code>width</code>. A sign prefix is handled correctly
     * if it is in the first byte. A copy of the original is returned if width is less than the
     * current size of the array.
     *
     * @param width desired
     * @return left-filled bytes
     */
    public PyBytes zfill(int width) {
        return (PyBytes)basebytes_zfill(width);
    }

    @ExposedMethod(doc = BuiltinDocs.bytes_zfill_doc)
    final PyBytes bytes_zfill(int width) {
        return (PyBytes)basebytes_zfill(width);
    }
}
