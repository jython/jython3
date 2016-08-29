/*
 * Copyright (c)2013 Jython Developers. Original Java version copyright 2000 Finn Bock.
 *
 * This program contains material copyrighted by: Copyright (c) Corporation for National Research
 * Initiatives. Originally written by Marc-Andre Lemburg (mal@lemburg.com).
 */
package org.python.modules;

import org.python.core.BufferProtocol;
import org.python.core.Py;
import org.python.core.PyBUF;
import org.python.core.PyBuffer;
import org.python.core.PyBytes;
import org.python.core.PyDictionary;
import org.python.core.PyLong;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.core.PyTuple;
import org.python.core.PyUnicode;
import org.python.core.Untraversable;
import org.python.core.codecs;
import org.python.core.stringlib.Encoding;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;
import org.python.expose.ExposedType;
import org.python.modules.sys.SysModule;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * This class corresponds to the Python _codecs module, which in turn lends its functions to the
 * codecs module (in Lib/codecs.py). It exposes the implementing functions of several codec families
 * called out in the Python codecs library Lib/encodings/*.py, where it is usually claimed that they
 * are bound "as C functions". Obviously, C stands for "compiled" in this context, rather than
 * dependence on a particular implementation language. Actual transcoding methods often come from
 * the related {@link codecs} class.
 */
@ExposedModule
public class _codecs {

    @ExposedFunction
    public static void register(PyObject search_function) {
        codecs.register(search_function);
    }

    private static String _castString(PyObject pystr) {
        if (pystr == null) {
            return null;
        }
        return pystr.toString();
    }

    @ExposedFunction
    public static PyObject _forget_codec(PyObject encoding) {
        codecs.forget_codec(encoding);
        return Py.None;
    }

    @ExposedFunction
    public static PyObject lookup(PyObject encoding) {
        return codecs.lookup(_castString(encoding));
    }

    @ExposedFunction
    public static PyObject lookup_error(PyObject handlerName) {
        return codecs.lookup_error(_castString(handlerName));
    }

    @ExposedFunction
    public static void register_error(String name, PyObject errorHandler) {
        codecs.register_error(name, errorHandler);
    }

    /**
     * Decode <code>bytes</code> using the codec registered for the <code>encoding</code>. The
     * <code>encoding</code> defaults to the system default encoding (see
     * {@link codecs#getDefaultEncoding()}). The string <code>errors</code> may name a different
     * error handling policy (built-in or registered with {@link #register_error(String, PyObject)}
     * ). The default error policy is 'strict' meaning that decoding errors raise a
     * <code>ValueError</code>.
     *
     * @param bytes to be decoded
     * @param encoding name of encoding (to look up in codec registry)
     * @param errors error policy name (e.g. "ignore")
     * @return Unicode string decoded from <code>bytes</code>
     */
    @ExposedFunction(defaults = {"null", "null"})
    public static PyObject decode(PyObject bytes, PyObject encoding, PyObject errors) {
        if (!(bytes instanceof PyBytes))
            throw Py.TypeError("a bytes-like object is required");
        return codecs.decode((PyBytes) bytes, _castString(encoding), _castString(errors));
    }


    /**
     * Encode <code>unicode</code> using the codec registered for the <code>encoding</code>. The
     * <code>encoding</code> defaults to the system default encoding (see
     * {@link codecs#getDefaultEncoding()}). The string <code>errors</code> may name a different
     * error handling policy (built-in or registered with {@link #register_error(String, PyObject)}
     * ). The default error policy is 'strict' meaning that encoding errors raise a
     * <code>ValueError</code>.
     *
     * @param unicode string to be encoded
     * @param encoding name of encoding (to look up in codec registry)
     * @param errors error policy name (e.g. "ignore")
     * @return bytes object encoding <code>unicode</code>
     */
    @ExposedFunction(defaults = {"null", "null"})
    public static PyObject encode(PyObject unicode, PyObject encoding, PyObject errors) {
        return new PyBytes(codecs.encode(unicode, _castString(encoding), _castString(errors)));
    }

    /* --- Some codec support methods -------------------------------------------- */

    @ExposedFunction
    public static PyObject charmap_build(PyObject map) {
        return EncodingMap.buildEncodingMap(map);
    }

    /**
     * Enumeration representing the possible endianness of UTF-32 (possibly UTF-16) encodings.
     * Python uses integers <code>{-1, 0, 1}</code>, but we can be more expressive. For encoding
     * UNDEFINED means choose the endianness of the platform and insert a byte order mark (BOM). But
     * since the platform is Java, that is always big-endian. For decoding it means read the BOM
     * from the stream, and it is an error not to find one (compare
     * <code>Lib/encodings/utf_32.py</code>).
     */
    enum ByteOrder {
        LE, UNDEFINED, BE;

        /** Returns the Python equivalent code -1 = LE, 0 = as marked/platform, +1 = BE */
        int code() {
            return ordinal() - 1;
        }

        /** Returns equivalent to the Python code -1 = LE, 0 = as marked/platform, +1 = BE */
        static ByteOrder fromInt(int byteorder) {
            switch (byteorder) {
                case -1:
                    return LE;
                case 1:
                    return BE;
                default:
                    return UNDEFINED;
            }
        }

        boolean be() {
            return this == BE;
        }
    }

    /**
     * Convenience method to construct the return value of decoders, providing the Unicode result as
     * a String, and the number of bytes consumed.
     *
     * @param u the unicode result as a UTF-16 Java String
     * @param bytesConsumed the number of bytes consumed
     * @return the tuple (unicode(u), bytesConsumed)
     */
    private static PyTuple decode_tuple(String u, int bytesConsumed) {
        return new PyTuple(new PyUnicode(u), Py.newInteger(bytesConsumed));
    }

    /**
     * Convenience method to construct the return value of decoders, providing the Unicode result as
     * a String, and the number of bytes consumed in decoding as either a single-element array or an
     * int to be used if the array argument is null.
     *
     * @param u the unicode result as a UTF-16 Java String
     * @param consumed if not null, element [0] is the number of bytes consumed
     * @param defConsumed if consumed==null, use this as the number of bytes consumed
     * @return the tuple (unicode(u), bytesConsumed)
     */
    private static PyTuple decode_tuple(String u, int[] consumed, int defConsumed) {
        return decode_tuple(u, consumed != null ? consumed[0] : defConsumed);
    }

    /**
     * Convenience method to construct the return value of decoders that infer the byte order from
     * the byte-order mark.
     *
     * @param u the unicode result as a UTF-16 Java String
     * @param bytesConsumed the number of bytes consumed
     * @param order the byte order (deduced by codec)
     * @return the tuple (unicode(u), bytesConsumed, byteOrder)
     */
    private static PyTuple decode_tuple(String u, int bytesConsumed, ByteOrder order) {
        int bo = order.code();
        return new PyTuple(new PyUnicode(u), Py.newLong(bytesConsumed), Py.newLong(bo));
    }

    private static PyTuple decode_tuple_str(String s, int len) {
        return new PyTuple(new PyBytes(s), Py.newLong(len));
    }

    private static PyTuple encode_tuple(String s, int len) {
        return new PyTuple(new PyBytes(s), Py.newLong(len));
    }

    @ExposedFunction(defaults = {"null", "false"})
    public static PyObject utf_8_decode(String str, String errors, boolean final_) {
        int[] consumed = final_ ? null : new int[1];
        return decode_tuple(codecs.PyUnicode_DecodeUTF8Stateful(str, errors, consumed), final_
                ? str.length() : consumed[0]);
    }

    @ExposedFunction(defaults = {"null"})
    public static PyObject utf_8_encode(String str, String errors) {
        int size = str.length();
        return encode_tuple(codecs.PyUnicode_EncodeUTF8(str, errors), size);
    }

    /* --- UTF-7 Codec --------------------------------------------------- */
    @ExposedFunction(defaults = {"null", "false"})
    public static PyObject utf_7_decode(String bytes, String errors, boolean finalFlag) {
        int[] consumed = finalFlag ? null : new int[1];
        String decoded = codecs.PyUnicode_DecodeUTF7Stateful(bytes, errors, consumed);
        return decode_tuple(decoded, consumed, bytes.length());
    }

    @ExposedFunction(defaults = {"null"})
    public static PyObject utf_7_encode(String str, String errors) {
        int size = str.length();
        return encode_tuple(codecs.PyUnicode_EncodeUTF7(str, false, false, errors), size);
    }

    /* --- string-escape Codec -------------------------------------------- */
    @ExposedFunction(defaults = {"null"})
    public static PyTuple escape_decode(String str, String errors) {
        return decode_tuple_str(Encoding.decode_UnicodeEscape(str, 0, str.length(), errors, true),
                str.length());
    }

    @ExposedFunction(defaults = {"null"})
    public static PyTuple escape_encode(String str, String errors) {
        return encode_tuple(Encoding.encode_UnicodeEscape(str, false), str.length());
    }

    /* --- Character Mapping Codec --------------------------------------- */
    /**
     * Decode a sequence of bytes into Unicode characters via a mapping supplied as a container to
     * be indexed by the byte values (as unsigned integers).
     *
     * @param obj sequence of bytes to decode
     * @param errorsObj error policy
     * @param mapping to convert bytes to characters
     * @return decoded string and number of bytes consumed
     */
    @ExposedFunction(defaults = {"null", "null"})
    public static PyObject charmap_decode(PyObject obj, PyObject errorsObj, PyObject mapping) {
        if (mapping == null || mapping == Py.None) {
            // Default to Latin-1
            return latin_1_decode(obj, errorsObj);
        }
        // XXX bytes: would prefer to accept any object with buffer API
        if (!(obj instanceof BufferProtocol)) {
            throw Py.TypeError("requires a bytes-like object");
        }
        try (PyBuffer buf = ((BufferProtocol) obj).getBuffer(PyBUF.FULL_RO)) {
            int size = buf.getLen();
            StringBuilder v = new StringBuilder(size);

            String errors = _castString(errorsObj);
            byte[] tmp = new byte[size];
            buf.copyTo(tmp, 0);
            String bytes = new String(tmp);
            for (int i = 0; i < size; i++) {

                // Process the i.th input byte
                int b = buf.byteAt(i);
                if (b > 0xff) {
                    i = codecs.insertReplacementAndGetResume(v, errors, "charmap", bytes, //
                            i, i + 1, "ordinal not in range(255)") - 1;
                    continue;
                }

                // Map the byte to an output character code (or possibly string)
                PyObject w = Py.newInteger(b);
                PyObject x = mapping.__finditem__(w);

                // Apply to the output
                if (x == null) {
                    i = codecs.insertReplacementAndGetResume(v, errors, "charmap", bytes, //
                            i, i + 1, "no mapping found") - 1;

                } else if (x instanceof PyLong) {
                    // Mapping was to an int: treat as character code
                    int value = x.asInt();
                    if (value < 0 || value > SysModule.MAXUNICODE) {
                        throw Py.TypeError("character mapping must return "
                                + "integer greater than 0 and less than sys.maxunicode");
                    }
                    v.appendCodePoint(value);

                } else if (x == Py.None) {
                    i = codecs.insertReplacementAndGetResume(v, errors, "charmap", bytes, //
                            i, i + 1, "character maps to <undefined>") - 1;

                } else if (x instanceof PyUnicode) {
                    String s = x.toString();
                    if (s.charAt(0) == 0xfffe) {
                        // Invalid indicates "undefined" see C-API PyUnicode_DecodeCharmap()
                        i = codecs.insertReplacementAndGetResume(v, errors, "charmap", bytes, //
                                i, i + 1, "character maps to <undefined>") - 1;
                    } else {
                        v.append(s);
                    }

                } else {
                /* wrong return value */
                    throw Py.TypeError("character mapping must return " + "integer, None or str");
                }
            }

            return decode_tuple(v.toString(), size);
        }
    }

    // parallel to CPython's PyUnicode_TranslateCharmap
    public static PyObject translateCharmap(PyUnicode str, String errors, PyObject mapping) {
        StringBuilder buf = new StringBuilder(str.toString().length());

        for (Iterator<Integer> iter = str.newSubsequenceIterator(); iter.hasNext();) {
            int codePoint = iter.next();
            PyObject result = mapping.__finditem__(Py.newInteger(codePoint));
            if (result == null) {
                // No mapping found means: use 1:1 mapping
                buf.appendCodePoint(codePoint);
            } else if (result == Py.None) {
                // XXX: We don't support the fancier error handling CPython does here of
                // capturing regions of chars removed by the None mapping to optionally
                // pass to an error handler. Though we don't seem to even use this
                // functionality anywhere either
                ;
            } else if (result instanceof PyLong) {
                int value = result.asInt();
                if (value < 0 || value > SysModule.MAXUNICODE) {
                    throw Py.TypeError(String.format("character mapping must be in range(0x%x)",
                            SysModule.MAXUNICODE + 1));
                }
                buf.appendCodePoint(value);
            } else if (result instanceof PyUnicode) {
                buf.append(result.toString());
            } else {
                // wrong return value
                throw Py.TypeError("character mapping must return integer, None or unicode");
            }
        }
        return new PyUnicode(buf.toString());
    }

    /**
     * Encoder based on an optional character mapping. This mapping is either an
     * <code>EncodingMap</code> of 256 entries, or an arbitrary container indexable with integers
     * using <code>__finditem__</code> and yielding byte strings. If the mapping is null, latin-1
     * (effectively a mapping of character code to the numerically-equal byte) is used
     *
     * @param obj to be encoded
     * @param errors error policy name (e.g. "ignore")
     * @param mapping from character code to output byte (or string)
     * @return (encoded data, size(str)) as a pair
     */
    @ExposedFunction(defaults = {"null", "null"})
    public static PyObject charmap_encode(PyObject obj, PyObject errors, PyObject mapping) {
        if (mapping == null || mapping == Py.None) {
            // Default to Latin-1
            return latin_1_encode(obj, errors);
        } else {
            String str = obj.toString();
            return charmap_encode_internal(str, _castString(errors), mapping, new StringBuilder(str.length()),
                    true);
        }
    }

    /**
     * Helper to implement the several variants of <code>charmap_encode</code>, given an optional
     * mapping. This mapping is either an <code>EncodingMap</code> of 256 entries, or an arbitrary
     * container indexable with integers using <code>__finditem__</code> and yielding byte strings.
     *
     * @param str to be encoded
     * @param errors error policy name (e.g. "ignore")
     * @param mapping from character code to output byte (or string)
     * @param v to contain the encoded bytes
     * @param letLookupHandleError
     * @return (encoded data, size(str)) as a pair
     */
    private static PyTuple charmap_encode_internal(String str, String errors, PyObject mapping,
            StringBuilder v, boolean letLookupHandleError) {

        EncodingMap encodingMap = mapping instanceof EncodingMap ? (EncodingMap)mapping : null;
        int size = str.length();

        for (int i = 0; i < size; i++) {

            // Map the i.th character of str to some value
            char ch = str.charAt(i);
            PyObject x;
            if (encodingMap != null) {
                // The mapping given was an EncodingMap [0,256) => on-negative int
                int result = encodingMap.lookup(ch);
                x = (result == -1) ? null : Py.newInteger(result);
            } else {
                // The mapping was a map or similar: non-negative int -> object
                x = mapping.__finditem__(Py.newInteger(ch));
            }

            // And map this object to an output character
            if (x == null) {
                // Error during lookup
                if (letLookupHandleError) {
                    // Some kind of substitute can be placed in the output
                    i = handleBadMapping(str, errors, mapping, v, size, i);
                } else {
                    // Hard error
                    throw Py.UnicodeEncodeError("charmap", str, i, i + 1,
                            "character maps to <undefined>");
                }

            } else if (x instanceof PyLong) {
                // Look-up had integer result: output as byte value
                int value = x.asInt();
                if (value < 0 || value > 255) {
                    throw Py.TypeError("character mapping must be in range(256)");
                }
                v.append((char)value);

            } else if (x instanceof PyBytes && !(x instanceof PyUnicode)) {
                // Look-up had str or unicode result: output as Java String
                // XXX: (Py3k) Look-up had bytes or str result: output as ... this is a problem
                v.append(x.toString());

            } else if (x instanceof PyNone) {
                i = handleBadMapping(str, errors, mapping, v, size, i);

            } else {
                /* wrong return value */
                throw Py.TypeError("character mapping must return " + "integer, None or str");
            }
        }

        return encode_tuple(v.toString(), size);
    }

    /**
     * Helper for {@link #charmap_encode_internal(String, String, PyObject, StringBuilder, boolean)}
     * called when we need some kind of substitute in the output for an invalid input.
     *
     * @param str to be encoded
     * @param errors error policy name (e.g. "ignore")
     * @param mapping from character code to output byte (or string)
     * @param v to contain the encoded bytes
     * @param size of str
     * @param i index in str of current (and problematic) character
     * @return index of last character of problematic section
     */
    private static int handleBadMapping(String str, String errors, PyObject mapping,
            StringBuilder v, int size, int i) {

        // If error policy specified, execute it
        if (errors != null) {

            if (errors.equals(codecs.IGNORE)) {
                return i;

            } else if (errors.equals(codecs.REPLACE)) {
                String replStr = "?";
                charmap_encode_internal(replStr, errors, mapping, v, false);
                return i;

            } else if (errors.equals(codecs.XMLCHARREFREPLACE)) {
                String replStr = codecs.xmlcharrefreplace(i, i + 1, str).toString();
                charmap_encode_internal(replStr, errors, mapping, v, false);
                return i;

            } else if (errors.equals(codecs.BACKSLASHREPLACE)) {
                String replStr = codecs.backslashreplace(i, i + 1, str).toString();
                charmap_encode_internal(replStr, errors, mapping, v, false);
                return i;
            }
        }

        // Default behaviour (error==null or does not match known case)
        String msg = "character maps to <undefined>";
        PyObject replacement = codecs.encoding_error(errors, "charmap", str, i, i + 1, msg);
        String replStr = replacement.__getitem__(0).toString();
        charmap_encode_internal(replStr, errors, mapping, v, false);

        return codecs.calcNewPosition(size, replacement) - 1;
    }

    /* --- ascii Codec ---------------------------------------------- */
    @ExposedFunction(defaults = {"null"})
    public static PyTuple ascii_decode(String str, String errors) {
        int size = str.length();
        return decode_tuple(codecs.PyUnicode_DecodeASCII(str, size, errors), size);
    }

    @ExposedFunction(defaults = {"null"})
    public static PyTuple ascii_encode(String str, String errors) {
        int size = str.length();
        return encode_tuple(codecs.PyUnicode_EncodeASCII(str, size, errors), size);
    }

    /* --- Latin-1 Codec -------------------------------------------- */
    @ExposedFunction(defaults = {"null"})
    public static PyObject latin_1_decode(PyObject obj, PyObject errors) {
        String str = obj.toString();
        int size = str.length();
        return decode_tuple(codecs.PyUnicode_DecodeLatin1(str, size, _castString(errors)), size);
    }

    @ExposedFunction(defaults = {"null"})
    public static PyObject latin_1_encode(PyObject obj, PyObject errors) {
        String str = obj.toString();
        int size = str.length();
        return encode_tuple(codecs.PyUnicode_EncodeLatin1(str, size, _castString(errors)), size);
    }

    /* --- UTF-16 Codec ------------------------------------------- */
    @ExposedFunction(defaults = {"null", "0"})
    public static PyObject utf_16_encode(PyObject str, String errors, int byteorder) {
        return encode_tuple(encode_UTF16(str, errors, ByteOrder.fromInt(byteorder)), str.__len__());
    }

    @ExposedFunction(defaults = {"null"})
    public static PyObject utf_16_le_encode(PyObject str, String errors) {
        return encode_tuple(encode_UTF16(str, errors, ByteOrder.LE), str.__len__());
    }

    @ExposedFunction(defaults = {"null"})
    public static PyObject utf_16_be_encode(PyObject str, String errors) {
        return encode_tuple(encode_UTF16(str, errors, ByteOrder.BE), str.__len__());
    }

    public static String encode_UTF16(PyObject str, String errors, ByteOrder order) {
        StringBuilder v = new StringBuilder();
        if (order == ByteOrder.UNDEFINED) {
            v.append("\u00FE\u00FF"); // BOM
            order = ByteOrder.BE;
        }
        if (!(str instanceof PyUnicode)) {
            throw Py.TypeError(String.format("str expected, found '%s'", str.getType().getName()));
        }
        Iterator<Integer> iter = ((PyUnicode) str).newSubsequenceIterator();
        int i = 0;
        while (iter.hasNext()) {
            int ch = iter.next();
            if (Character.isSurrogate((char) ch)) {
                String encoding = order == ByteOrder.BE ? "UTF-16BE" : "UTF-16LE";
                PyObject replacementspec = codecs.encoding_error(errors, encoding, str.toString(), i, i+1, "unpaired surrogate");
                v.append(replacementspec.__getitem__(0).toString());
                i++;
                continue;
            }
            if (order.be()) {
                v.appendCodePoint(0xFF & (ch >> 8));
                v.appendCodePoint(0xFF & ch);
            } else {
                v.appendCodePoint(0xFF & ch);
                v.appendCodePoint(0xFF & (ch >> 8));
            }
            i++;
        }
        return v.toString();
    }

    @ExposedFunction(defaults = {"null", "false"})
    public static PyObject utf_16_decode(String str, String errors, boolean final_) {
        int[] bo = new int[] {0};
        int[] consumed = final_ ? null : new int[1];
        return decode_tuple(decode_UTF16(str, errors, bo, consumed), final_ ? str.length()
                : consumed[0]);
    }

    @ExposedFunction(defaults = {"null", "false"})
    public static PyObject utf_16_le_decode(String str, String errors, boolean final_) {
        int[] bo = new int[] {-1};
        int[] consumed = final_ ? null : new int[1];
        return decode_tuple(decode_UTF16(str, errors, bo, consumed), final_ ? str.length()
                : consumed[0]);
    }

    @ExposedFunction(defaults = {"null", "false"})
    public static PyTuple utf_16_be_decode(String str, String errors, boolean final_) {
        int[] bo = new int[] {1};
        int[] consumed = final_ ? null : new int[1];
        return decode_tuple(decode_UTF16(str, errors, bo, consumed), final_ ? str.length()
                : consumed[0]);
    }

    @ExposedFunction(defaults = {"null", "0", "false"})
    public static PyTuple utf_16_ex_decode(String str, String errors, int byteorder, boolean final_) {
        int[] bo = new int[] {0};
        int[] consumed = final_ ? null : new int[1];
        String decoded = decode_UTF16(str, errors, bo, consumed);
        return new PyTuple(new PyUnicode(decoded), Py.newInteger(final_ ? str.length()
                : consumed[0]), Py.newInteger(bo[0]));
    }

    private static String decode_UTF16(String str, String errors, int[] byteorder) {
        return decode_UTF16(str, errors, byteorder, null);
    }

    private static String decode_UTF16(String str, String errors, int[] byteorder, int[] consumed) {
        int bo = 0;
        if (byteorder != null) {
            bo = byteorder[0];
        }
        int size = str.length();
        StringBuilder v = new StringBuilder(size / 2);
        int i;
        for (i = 0; i < size; i += 2) {
            char ch1 = str.charAt(i);
            if (i + 1 == size) {
                if (consumed != null) {
                    break;
                }
                i = codecs.insertReplacementAndGetResume(v, errors, "utf-16", str, //
                        i, i + 1, "truncated data");
                continue;
            }
            char ch2 = str.charAt(i + 1);
            if (ch1 == 0xFE && ch2 == 0xFF) {
                bo = 1;
                continue;
            } else if (ch1 == 0xFF && ch2 == 0xFE) {
                bo = -1;
                continue;
            }
            int W1;
            if (bo == -1) {
                W1 = (ch2 << 8 | ch1);
            } else {
                W1 = (ch1 << 8 | ch2);
            }

            if (W1 < 0xD800 || W1 > 0xDFFF) {
                if (Character.isValidCodePoint(W1)) {
                    v.appendCodePoint(W1);
                    continue;
                }
                throw Py.UnicodeError("invalid codepoint");
            } else if (W1 >= 0xD800 && W1 <= 0xDBFF && i < size - 1) {
                i += 2;
                char ch3 = str.charAt(i);
                char ch4 = str.charAt(i + 1);
                int W2;
                if (bo == -1) {
                    W2 = (ch4 << 8 | ch3);
                } else {
                    W2 = (ch3 << 8 | ch4);
                }
                if (W2 >= 0xDC00 && W2 <= 0xDFFF) {
                    int U = (((W1 & 0x3FF) << 10) | (W2 & 0x3FF)) + 0x10000;
                    v.appendCodePoint(U);
                    continue;
                }
                i = codecs.insertReplacementAndGetResume(v, errors, "utf-16", str, //
                        i, i + 1, "illegal UTF-16 surrogate");
                continue;
            }

            i = codecs.insertReplacementAndGetResume(v, errors, "utf-16", str, //
                    i, i + 1, "illegal encoding");
        }
        if (byteorder != null) {
            byteorder[0] = bo;
        }
        if (consumed != null) {
            consumed[0] = i;
        }
        return v.toString();
    }

    /* --- UTF-32 Codec ------------------------------------------- */
    /**
     * Encode a Unicode Java String as UTF-32 in specified byte order with byte order mark.
     *
     * @param unicode to be encoded
     * @param errors error policy name or null meaning "strict"
     * @param byteorder decoding "endianness" specified (in the Python -1, 0, +1 convention)
     * @return tuple (encoded_bytes, unicode_consumed)
     */
    @ExposedFunction(defaults = {"null", "0"})
    public static PyObject utf_32_encode(PyObject unicode, String errors, int byteorder) {
        ByteOrder order = ByteOrder.fromInt(byteorder);
        return PyUnicode_EncodeUTF32(unicode, errors, order);
    }

    /**
     * Encode a Unicode Java String as UTF-32 with little-endian byte order. No byte-order mark is
     * generated.
     *
     * @param unicode to be encoded
     * @param errors error policy name or null meaning "strict"
     * @return tuple (encoded_bytes, unicode_consumed)
     */
    @ExposedFunction(defaults = {"null"})
    public static PyObject utf_32_le_encode(PyObject unicode, String errors) {
        return PyUnicode_EncodeUTF32(unicode, errors, ByteOrder.LE);
    }

    /**
     * Encode a Unicode Java String as UTF-32 with big-endian byte order. No byte-order mark is
     * generated.
     *
     * @param unicode to be encoded
     * @param errors error policy name or null meaning "strict"
     * @return tuple (encoded_bytes, unicode_consumed)
     */
    @ExposedFunction(defaults = {"null"})
    public static PyTuple utf_32_be_encode(PyObject unicode, String errors) {
        return PyUnicode_EncodeUTF32(unicode, errors, ByteOrder.BE);
    }

    /**
     * Encode a Unicode Java String as UTF-32 in specified byte order. A byte-order mark is
     * generated if <code>order = ByteOrder.UNDEFINED</code>, and the byte order in that case will
     * be the platform default, which is BE since the platform is Java.
     * <p>
     * The input String <b>must</b> be valid UTF-16, in particular, if it contains surrogate code
     * units they must be ordered and paired correctly. The last char in <code>unicode</code> is not
     * allowed to be an unpaired surrogate. These criteria will be met if the String
     * <code>unicode</code> is the contents of a valid {@link PyUnicode} or {@link PyBytes}.
     *
     * @param unicode to be encoded
     * @param errors error policy name or null meaning "strict"
     * @param order byte order to use BE, LE or UNDEFINED (a BOM will be written)
     * @return tuple (encoded_bytes, unicode_consumed)
     */
    private static PyTuple PyUnicode_EncodeUTF32(PyObject unicode, String errors, ByteOrder order) {
        int len = unicode.__len__();
        // We use a StringBuilder but we are really storing encoded bytes
        StringBuilder v = new StringBuilder(4 * (len + 1));
        int uptr = 0;

        // Write a BOM (if required to)
        if (order == ByteOrder.UNDEFINED) {
            v.append("\u0000\u0000\u00fe\u00ff");
            order = ByteOrder.BE;
        }

        if (!(unicode instanceof PyUnicode)) {
            throw Py.TypeError(String.format("str expected, find '%s'", unicode.getType().getName()));
        }

        Iterator<Integer> iter = ((PyUnicode) unicode).newSubsequenceIterator();
        int i = 0;
        while(iter.hasNext()) {
            int ch = iter.next();
            if (Character.isSurrogate((char) ch)) {
                String encoding = order == ByteOrder.BE ? "UTF-32BE" : "UTF-32LE";
                PyObject replacementSpec = codecs.encoding_error(errors, encoding, unicode.toString(), i, i+1, "unpaired surrogate");
                v.append(replacementSpec.__getitem__(0).toString());
                i++;
                continue;
            }
            if (order == ByteOrder.BE) {
                v.appendCodePoint(ch >> 24);
                v.appendCodePoint((ch >> 16) & 0xFF);
                v.appendCodePoint((ch >> 8) & 0xFF);
                v.appendCodePoint(ch & 0xFF);
            } else {
                v.appendCodePoint(ch & 0xFF);
                v.appendCodePoint((ch >> 8) & 0xFF);
                v.appendCodePoint((ch >> 16) & 0xFF);
                v.appendCodePoint(ch >> 24);
            }
            i++;
        }
        uptr = unicode.__len__();

        return encode_tuple(v.toString(), uptr);
    }

    /**
     * Decode (perhaps partially) a sequence of bytes representing the UTF-32 encoded form of a
     * Unicode string and return as a tuple the unicode text, and the amount of input consumed. The
     * endianness used will have been deduced from a byte-order mark, if present, or will be
     * big-endian (Java platform default). The unicode text is presented as a Java String (the
     * UTF-16 representation used by {@link PyUnicode}).
     *
     * @param bytes to be decoded (Jython {@link PyBytes} convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @param isFinal if a "final" call, meaning the input must all be consumed
     * @return tuple (unicode_result, bytes_consumed)
     */
    @ExposedFunction(defaults = {"null", "false"})
    public static PyObject utf_32_decode(String bytes, String errors, boolean isFinal) {
        return PyUnicode_DecodeUTF32Stateful(bytes, errors, ByteOrder.UNDEFINED, isFinal, false);
    }

    /**
     * Decode (perhaps partially) a sequence of bytes representing the UTF-32 little-endian encoded
     * form of a Unicode string and return as a tuple the unicode text, and the amount of input
     * consumed. A (correctly-oriented) byte-order mark will pass as a zero-width non-breaking
     * space. The unicode text is presented as a Java String (the UTF-16 representation used by
     * {@link PyUnicode}).
     *
     * @param bytes to be decoded (Jython {@link PyBytes} convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @param isFinal if a "final" call, meaning the input must all be consumed
     * @return tuple (unicode_result, bytes_consumed)
     */
    @ExposedFunction(defaults = {"null", "false"})
    public static PyObject utf_32_le_decode(String bytes, String errors, boolean isFinal) {
        return PyUnicode_DecodeUTF32Stateful(bytes, errors, ByteOrder.LE, isFinal, false);
    }

    /**
     * Decode (perhaps partially) a sequence of bytes representing the UTF-32 big-endian encoded
     * form of a Unicode string and return as a tuple the unicode text, and the amount of input
     * consumed. A (correctly-oriented) byte-order mark will pass as a zero-width non-breaking
     * space. Unicode string and return as a tuple the unicode text, the amount of input consumed.
     * The unicode text is presented as a Java String (the UTF-16 representation used by
     * {@link PyUnicode}).
     *
     * @param bytes to be decoded (Jython {@link PyBytes} convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @param isFinal if a "final" call, meaning the input must all be consumed
     * @return tuple (unicode_result, bytes_consumed)
     */
    @ExposedFunction(defaults = {"null", "false"})
    public static PyObject utf_32_be_decode(String bytes, String errors, boolean isFinal) {
        return PyUnicode_DecodeUTF32Stateful(bytes, errors, ByteOrder.BE, isFinal, false);
    }

    /**
     * Decode (perhaps partially) a sequence of bytes representing the UTF-32 encoded form of a
     * Unicode string and return as a tuple the unicode text, the amount of input consumed, and the
     * decoding "endianness" used (in the Python -1, 0, +1 convention). The endianness will be that
     * specified, will have been deduced from a byte-order mark, if present, or will be big-endian
     * (Java platform default). Or it may still be undefined if fewer than 4 bytes are presented.
     * (This codec entrypoint is used in the utf-32 codec only untile the byte order is known.) The
     * unicode text is presented as a Java String (the UTF-16 representation used by
     * {@link PyUnicode}).
     *
     * @param bytes to be decoded (Jython {@link PyBytes} convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @param byteorder decoding "endianness" specified (in the Python -1, 0, +1 convention)
     * @param isFinal if a "final" call, meaning the input must all be consumed
     * @return tuple (unicode_result, bytes_consumed, endianness)
     */
    @ExposedFunction(defaults = {"null", "0", "false"})
    public static PyObject utf_32_ex_decode(String bytes, String errors, int byteorder, boolean isFinal) {
        ByteOrder order = ByteOrder.fromInt(byteorder);
        return PyUnicode_DecodeUTF32Stateful(bytes, errors, order, isFinal, true);
    }

    /**
     * Decode (perhaps partially) a sequence of bytes representing the UTF-32 encoded form of a
     * Unicode string and return as a tuple the (Jython internal representation of) the unicode
     * text, the amount of input consumed, and if requested, the decoding "endianness" used (in
     * Python -1, 0, +1 conventions). The state we preserve is our read position, i.e. how many
     * bytes we have consumed and the byte order (endianness). If the input ends part way through a
     * UTF-32 sequence (4 bytes) the data reported as consumed is just that up to and not including
     * the first of these bytes. The Java String in the returned tuple is a UTF-16 representation of
     * the Unicode result, in line with Java conventions, where Unicode characters above the BMP are
     * represented as surrogate pairs.
     *
     * @param bytes input represented as String (Jython PyBytes convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @param order LE, BE or UNDEFINED (meaning bytes may begin with a byte order mark)
     * @param isFinal if a "final" call, meaning the input must all be consumed
     * @param findOrder if the returned tuple should include a report of the byte order
     * @return tuple (unicode_result, bytes_consumed [, endianness])
     */
    private static PyTuple PyUnicode_DecodeUTF32Stateful(String bytes, String errors,
            ByteOrder order, boolean isFinal, boolean findOrder) {

        int size = bytes.length();  // Number of bytes waiting (not necessarily multiple of 4)
        int limit = size & ~0x3;    // First index at which fewer than 4 bytes will be available

        // Output Unicode characters will build up here (as UTF-16:
        StringBuilder unicode = new StringBuilder(1 + limit / 4);
        int q = 0;                  // Read pointer in bytes

        if (limit > 0) {
            /*
             * Check for BOM (U+FEFF) in the input and adjust current byte order setting
             * accordingly. If we know the byte order (it is LE or BE) then bytes ressembling a byte
             * order mark are actually a ZERO WIDTH NON-BREAKING SPACE and will be passed through to
             * the output in the main codec loop as such.
             */
            if (order == ByteOrder.UNDEFINED) {
                /*
                 * The byte order is not known. If the first 4 bytes is a BOM for LE or BE, that
                 * will set the byte order and the BOM will not be copied to the output. Otherwise
                 * these bytes are data and will be left for the main codec loop to consume.
                 */
                char a = bytes.charAt(q);
                if (a == 0xff) {
                    if (bytes.charAt(q + 1) == 0xfe && bytes.charAt(q + 2) == 0
                            && bytes.charAt(q + 3) == 0) {
                        // Somebody set up us the BOM (0xff 0xfe 0x00 0x00) - LE
                        order = ByteOrder.LE;
                        q += 4;
                    }

                } else if (a == 0) {
                    if (bytes.charAt(q + 1) == 0 && bytes.charAt(q + 2) == 0xfe
                            && bytes.charAt(q + 3) == 0xff) {
                        // Other (big-endian) BOM (0x00 0x00 0xfe 0xff) - already set BE
                        order = ByteOrder.BE;
                        q += 4;
                    }
                }
                /*
                 * If no BOM found, order is still undefined. This is an error to utf_32.py, but
                 * here is treated as big-endian.
                 */
            }

            /*
             * Main codec loop consumes 4 bytes and emits one code point with each pass, until there
             * are fewer than 4 bytes left. There's a version for each endianness
             */
            if (order != ByteOrder.LE) {
                q = PyUnicode_DecodeUTF32BELoop(unicode, bytes, q, limit, errors);
            } else {
                q = PyUnicode_DecodeUTF32LELoop(unicode, bytes, q, limit, errors);
            }

        }

        /*
         * We have processed all we can: if we have some bytes left over that we can't store for
         * next time, that's an error.
         */
        if (isFinal && q < size) {
            q = codecs.insertReplacementAndGetResume(unicode, errors, "utf-32", //
                    bytes, q, size, "truncated data");
        }

        // Finally, the return depends whether we were asked to work out the byte order
        if (findOrder) {
            return decode_tuple(unicode.toString(), q, order);
        } else {
            return decode_tuple(unicode.toString(), q);
        }
    }

    /**
     * Helper to {@link #PyUnicode_DecodeUTF32Stateful(String, String, ByteOrder, boolean, boolean)}
     * when big-endian decoding is to be carried out.
     *
     * @param unicode character output
     * @param bytes input represented as String (Jython PyBytes convention)
     * @param q number of elements already consumed from <code>bytes</code> array
     * @param limit (multiple of 4) first byte not to process
     * @param errors error policy name (e.g. "ignore", "replace")
     * @return number of elements consumed now from <code>bytes</code> array
     */
    private static int PyUnicode_DecodeUTF32BELoop(StringBuilder unicode, String bytes, int q,
            int limit, String errors) {

        /*
         * Main codec loop consumes 4 bytes and emits one code point with each pass, until there are
         * fewer than 4 bytes left.
         */
        while (q < limit) {
            // Read 4 bytes in two 16-bit chunks according to byte order
            int hi, lo;
            hi = (bytes.charAt(q) << 8) | bytes.charAt(q + 1);
            lo = (bytes.charAt(q + 2) << 8) | bytes.charAt(q + 3);

            if (hi == 0) {
                // It's a BMP character so we can't go wrong
                unicode.append((char)lo);
                q += 4;
            } else {
                // Code may be invalid: let the appendCodePoint method detect that
                try {
                    unicode.appendCodePoint((hi << 16) + lo);
                    q += 4;
                } catch (IllegalArgumentException e) {
                    q = codecs.insertReplacementAndGetResume(unicode, errors, "utf-32", //
                            bytes, q, q + 4, "codepoint not in range(0x110000)");
                }
            }
        }

        return q;
    }

    /**
     * Helper to {@link #PyUnicode_DecodeUTF32Stateful(String, String, ByteOrder, boolean, boolean)}
     * when little-endian decoding is to be carried out.
     *
     * @param unicode character output
     * @param bytes input represented as String (Jython PyBytes convention)
     * @param q number of elements already consumed from <code>bytes</code> array
     * @param limit (multiple of 4) first byte not to process
     * @param errors error policy name (e.g. "ignore", "replace")
     * @return number of elements consumed now from <code>bytes</code> array
     */
    private static int PyUnicode_DecodeUTF32LELoop(StringBuilder unicode, String bytes, int q,
            int limit, String errors) {
        /*
         * Main codec loop consumes 4 bytes and emits one code point with each pass, until there are
         * fewer than 4 bytes left.
         */
        while (q < limit) {
            // Read 4 bytes in two 16-bit chunks according to byte order
            int hi, lo;
            hi = (bytes.charAt(q + 3) << 8) | bytes.charAt(q + 2);
            lo = (bytes.charAt(q + 1) << 8) | bytes.charAt(q);

            if (hi == 0) {
                // It's a BMP character so we can't go wrong
                unicode.append((char)lo);
                q += 4;
            } else {
                // Code may be invalid: let the appendCodePoint method detect that
                try {
                    unicode.appendCodePoint((hi << 16) + lo);
                    q += 4;
                } catch (IllegalArgumentException e) {
                    q = codecs.insertReplacementAndGetResume(unicode, errors, "utf-32", //
                            bytes, q, q + 4, "codepoint not in range(0x110000)");
                }
            }
        }

        return q;
    }

    /* --- RawUnicodeEscape Codec ----------------------------------------- */
    @ExposedFunction(defaults = {"null"})
    public static PyTuple raw_unicode_escape_encode(String str, String errors) {
        return encode_tuple(codecs.PyUnicode_EncodeRawUnicodeEscape(str, errors, false),
                str.codePointCount(0, str.length()));
    }

    @ExposedFunction(defaults = {"null"})
    public static PyTuple raw_unicode_escape_decode(String str, String errors) {
        return decode_tuple(codecs.PyUnicode_DecodeRawUnicodeEscape(str, errors), str.length());
    }

    /* --- unicode-escape Codec ------------------------------------------- */
    @ExposedFunction(defaults = {"null"})
    public static PyTuple unicode_escape_encode(String str, String errors) {
        return encode_tuple(Encoding.encode_UnicodeEscape(str, false), str.codePointCount(0, str.length()));
    }

    @ExposedFunction(defaults = {"null"})
    public static PyTuple unicode_escape_decode(String str, String errors) {
        int n = str.length();
        return decode_tuple(Encoding.decode_UnicodeEscape(str, 0, n, errors, true), n);
    }

    /* --- UnicodeInternal Codec ------------------------------------------ */

    /*
     * This codec is supposed to deal with an encoded form equal to the internal representation of
     * the unicode object considered as bytes in memory. This was confusing in CPython as it varied
     * with machine architecture (width and endian-ness). In Jython, where both are fixed, the most
     * compatible choice is UTF-32BE. The codec is deprecated in v3.3 as irrelevant, or impossible,
     * in view of the flexible string representation (which Jython emulates in its own way).
     *
     * See http://mail.python.org/pipermail/python-dev/2011-November/114415.html
     */
    /**
     * Optimized charmap encoder mapping.
     *
     * Uses a trie structure instead of a dictionary; the speedup primarily comes from not creating
     * integer objects in the process. The trie is created by inverting the encoding map.
     */
    @Untraversable
    @ExposedType(name = "EncodingMap", isBaseType = false)
    public static class EncodingMap extends PyObject {

        char[] level1;
        char[] level23;
        int count2;
        int count3;

        private EncodingMap(char[] level1, char[] level23, int count2, int count3) {
            this.level1 = level1;
            this.level23 = level23;
            this.count2 = count2;
            this.count3 = count3;
        }

        /**
         * Create and populate an EncodingMap from a 256 length PyUnicode char. Returns a
         * PyDictionary if the mapping isn't easily optimized.
         *
         * @param string a 256 length unicode mapping
         * @return an encoder mapping
         */
        public static PyObject buildEncodingMap(PyObject string) {
            if (!(string instanceof PyUnicode) || string.__len__() != 256) {
                throw Py.TypeError("bad argument type for built-in operation");
            }

            boolean needDict = false;
            char[] level1 = new char[32];
            char[] level23 = new char[512];
            int i;
            int count2 = 0;
            int count3 = 0;
            String decode = string.toString();
            for (i = 0; i < level1.length; i++) {
                level1[i] = 0xFF;
            }
            for (i = 0; i < level23.length; i++) {
                level23[i] = 0xFF;
            }
            if (decode.charAt(0) != 0) {
                needDict = true;
            }
            for (i = 1; i < 256; i++) {
                int l1, l2;
                char charAt = decode.charAt(i);
                if (charAt == 0) {
                    needDict = true;
                }
                if (charAt == 0xFFFE) {
                    // unmapped character
                    continue;
                }
                l1 = charAt >> 11;
                l2 = charAt >> 7;
                if (level1[l1] == 0xFF) {
                    level1[l1] = (char)count2++;
                }
                if (level23[l2] == 0xFF) {
                    level23[l2] = (char)count3++;
                }
            }

            if (count2 > 0xFF || count3 > 0xFF) {
                needDict = true;
            }

            if (needDict) {
                PyObject result = new PyDictionary();
                for (i = 0; i < 256; i++) {
                    result.__setitem__(Py.newInteger(decode.charAt(i)), Py.newInteger(i));
                }
                return result;
            }

            // Create a three-level trie
            int length2 = 16 * count2;
            int length3 = 128 * count3;
            level23 = new char[length2 + length3];
            PyObject result = new EncodingMap(level1, level23, count2, count3);
            for (i = 0; i < length2; i++) {
                level23[i] = 0xFF;
            }
            for (i = length2; i < length2 + length3; i++) {
                level23[i] = 0;
            }
            count3 = 0;
            for (i = 1; i < 256; i++) {
                int o1, o2, o3, i2, i3;
                char charAt = decode.charAt(i);
                if (charAt == 0xFFFE) {
                    // unmapped character
                    continue;
                }
                o1 = charAt >> 11;
                o2 = (charAt >> 7) & 0xF;
                i2 = 16 * level1[o1] + o2;
                if (level23[i2] == 0xFF) {
                    level23[i2] = (char)count3++;
                }
                o3 = charAt & 0x7F;
                i3 = 128 * level23[i2] + o3;
                level23[length2 + i3] = (char)i;
            }
            return result;
        }

        /**
         * Lookup a char in the EncodingMap.
         *
         * @param c a char
         * @return an int, -1 for failure
         */
        public int lookup(char c) {
            int l1 = c >> 11;
            int l2 = (c >> 7) & 0xF;
            int l3 = c & 0x7F;
            int i;
            if (c == 0) {
                return 0;
            }
            // level 1
            i = level1[l1];
            if (i == 0xFF) {
                return -1;
            }
            // level 2
            i = level23[16 * i + l2];
            if (i == 0xFF) {
                return -1;
            }
            // level 3
            i = level23[16 * count2 + 128 * i + l3];
            if (i == 0) {
                return -1;
            }
            return i;
        }
    }
}
