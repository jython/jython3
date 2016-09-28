package org.python.core.stringlib;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.ibm.icu.lang.UCharacter;
import org.python.core.BufferProtocol;
import org.python.core.Py;
import org.python.core.PyBUF;
import org.python.core.PyBuffer;
import org.python.core.PyBytes;
import org.python.core.PyComplex;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.core.PyTuple;
import org.python.core.PyUnicode;
import org.python.core.codecs;
import org.python.modules.sys.SysModule;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper methods for unicode encoding shared between bytes-like object and str
 */
public class Encoding {
    private static char[] hexdigit = "0123456789abcdef".toCharArray();

    public static String encode_UnicodeEscape(String str, boolean use_quotes) {
        int size = str.length();
        StringBuilder v = new StringBuilder(str.length());

        char quote = 0;

        if (use_quotes) {
            quote = str.indexOf('\'') >= 0 && str.indexOf('"') == -1 ? '"' : '\'';
            v.append(quote);
        }

        for (int i = 0; size-- > 0; ) {
            char ch = str.charAt(i++);
            /* Escape quotes */
            if ((use_quotes && ch == quote) || ch == '\\') {
                v.append('\\');
                v.append(ch);
                continue;
            }
            /* Map UTF-16 surrogate pairs to Unicode \UXXXXXXXX escapes */
            else if (size > 0 && Character.isHighSurrogate(ch)) {
                char ch2 = str.charAt(i++);
                size--;
                if (Character.isLowSurrogate(ch2)) {
                    int ucs = Character.toCodePoint(ch, ch2);//((ch & 0x03FF) << 10) | (ch2 & 0x03FF)) + 0x00010000;
                    if (UCharacter.isPrintable(ucs)) {
                        v.appendCodePoint(ucs);
                    } else {
                        v.append('\\');
                        v.append('U');
                        v.append(hexdigit[(ucs >> 28) & 0xf]);
                        v.append(hexdigit[(ucs >> 24) & 0xf]);
                        v.append(hexdigit[(ucs >> 20) & 0xf]);
                        v.append(hexdigit[(ucs >> 16) & 0xf]);
                        v.append(hexdigit[(ucs >> 12) & 0xf]);
                        v.append(hexdigit[(ucs >> 8) & 0xf]);
                        v.append(hexdigit[(ucs >> 4) & 0xf]);
                        v.append(hexdigit[ucs & 0xf]);
                    }
                    continue;
                }
                /* Fall through: isolated surrogates are copied as-is */
                i--;
                size++;
            }
            /* Map 16-bit characters to '\\uxxxx' */
            if (ch >= 256 && !UCharacter.isPrintable(ch)) {
                v.append('\\');
                v.append('u');
                v.append(hexdigit[(ch >> 12) & 0xf]);
                v.append(hexdigit[(ch >> 8) & 0xf]);
                v.append(hexdigit[(ch >> 4) & 0xf]);
                v.append(hexdigit[ch & 15]);
            }
            /* Map special whitespace to '\t', \n', '\r' */
            else if (ch == '\t') {
                v.append("\\t");
            } else if (ch == '\n') {
                v.append("\\n");
            } else if (ch == '\r') {
                v.append("\\r");
            } else if (ch < ' ' || ch == 127) {
                /* Map non-printable US ASCII to '\xNN' */
                v.append('\\');
                v.append('x');
                v.append(hexdigit[(ch >> 4) & 0xf]);
                v.append(hexdigit[ch & 0xf]);
            } else {/* Copy everything else as-is */
                v.append(ch);
            }
        }
        if (use_quotes) {
            v.append(quote);
        }
        return v.toString();
    }

    public static String decode_UnicodeEscape(String str, int start, int end, String errors,
                                              boolean unicode) {
        StringBuilder v = new StringBuilder(end - start);
        for (int s = start; s < end; ) {
            char ch = str.charAt(s);
            /* Non-escape characters are interpreted as Unicode ordinals */
            if (ch != '\\') {
                v.append(ch);
                s++;
                continue;
            }
            int loopStart = s;
            /* \ - Escapes */
            s++;
            if (s == end) {
                s = codecs.insertReplacementAndGetResume(v, errors, "unicodeescape", //
                        str, loopStart, s + 1, "\\ at end of string");
                continue;
            }
            ch = str.charAt(s++);
            switch (ch) {
            /* \x escapes */
                case '\n':
                    break;
                case '\\':
                    v.append('\\');
                    break;
                case '\'':
                    v.append('\'');
                    break;
                case '\"':
                    v.append('\"');
                    break;
                case 'b':
                    v.append('\b');
                    break;
                case 'f':
                    v.append('\014');
                    break; /* FF */
                case 't':
                    v.append('\t');
                    break;
                case 'n':
                    v.append('\n');
                    break;
                case 'r':
                    v.append('\r');
                    break;
                case 'v':
                    v.append('\013');
                    break; /* VT */
                case 'a':
                    v.append('\007');
                    break; /* BEL, not classic C */
                /* \OOO (octal) escapes */
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    int x = Character.digit(ch, 8);
                    for (int j = 0; j < 2 && s < end; j++, s++) {
                        ch = str.charAt(s);
                        if (ch < '0' || ch > '7') {
                            break;
                        }
                        x = (x << 3) + Character.digit(ch, 8);
                    }
                    v.append((char) x);
                    break;
                case 'x':
                    s = hexescape(v, errors, 2, s, str, end, "truncated \\xXX");
                    break;
                case 'u':
                    if (!unicode) {
                        v.append('\\');
                        v.append('u');
                        break;
                    }
                    s = hexescape(v, errors, 4, s, str, end, "truncated \\uXXXX");
                    break;
                case 'U':
                    if (!unicode) {
                        v.append('\\');
                        v.append('U');
                        break;
                    }
                    s = hexescape(v, errors, 8, s, str, end, "truncated \\UXXXXXXXX");
                    break;
                case 'N':
                    if (!unicode) {
                        v.append('\\');
                        v.append('N');
                        break;
                    }
                    /*
                     * Ok, we need to deal with Unicode Character Names now, make sure we've
                     * imported the hash table data...
                     */
                    if (str.charAt(s) == '{') {
                        int startName = s + 1;
                        int endBrace = startName;
                        /*
                         * look for either the closing brace, or we exceed the maximum length of the
                         * unicode character names
                         */
                        endBrace = str.indexOf('}', startName);
                        if (endBrace != -1) {
                            int value = UCharacter.getCharFromName(str.substring(startName, endBrace));
                            if (storeUnicodeCharacter(value, v)) {
                                s = endBrace + 1;
                            } else {
                                s = codecs.insertReplacementAndGetResume( //
                                        v, errors, "unicodeescape", //
                                        str, loopStart, endBrace + 1, "illegal Unicode character");
                            }
                        } else {
                            s = codecs.insertReplacementAndGetResume(v, errors, "unicodeescape", //
                                    str, loopStart, endBrace, "malformed \\N character escape");
                        }
                        break;
                    } else {
                        s = codecs.insertReplacementAndGetResume(v, errors, "unicodeescape", //
                                str, loopStart, s + 1, "malformed \\N character escape");
                    }
                    break;
                default:
                    v.append('\\');
                    v.append(str.charAt(s - 1));
                    break;
            }
        }
        return v.toString();
    }

    private static int hexescape(StringBuilder partialDecode, String errors, int digits,
                                 int hexDigitStart, String str, int size, String errorMessage) {
        int i = 0;
        int x = 0;
        for (; i < digits; ++i) {
            int index = hexDigitStart + i;
            if (index >= size) {
                return codecs.insertReplacementAndGetResume(partialDecode, errors, "unicodeescape",
                        str, hexDigitStart - 2, size, errorMessage);
            }
            char c = str.charAt(index);
            int d = Character.digit(c, 16);
            if (d == -1) {
                return codecs.insertReplacementAndGetResume(partialDecode, errors, "unicodeescape",
                        str, hexDigitStart - 2, index + 1, errorMessage) - 1;
            }
            x = (x << 4) & ~0xF;
            if (c >= '0' && c <= '9') {
                x += c - '0';
            } else if (c >= 'a' && c <= 'f') {
                x += 10 + c - 'a';
            } else {
                x += 10 + c - 'A';
            }
        }
        if (storeUnicodeCharacter(x, partialDecode)) {
            return hexDigitStart + i;
        } else {
            return codecs.insertReplacementAndGetResume(partialDecode, errors, "unicodeescape",
                    str, hexDigitStart - 2, hexDigitStart + i + 1, "illegal Unicode character");
        }
    }

    /* pass in an int since this can be a UCS-4 character */
    private static boolean storeUnicodeCharacter(int value, StringBuilder partialDecode) {
        if (value >= 0 && value <= SysModule.MAXUNICODE) {
            partialDecode.appendCodePoint(value);
            return true;
        }
        return false;
    }

    /**
     * Helper common to the Python and Java API for <code>str.replace</code>, returning a new string
     * equal to this string with ocurrences of <code>oldPiece</code> replaced by
     * <code>newPiece</code>, up to a maximum of <code>count</code> occurrences, or all of them.
     * This method also supports {@link PyUnicode#str_replace(PyObject, PyObject, int)}, in
     * which context it returns a <code>PyUnicode</code>
     *
     * @param oldPiece to replace where found.
     * @param newPiece replacement text.
     * @param count    maximum number of replacements to make, or -1 meaning all of them.
     * @return PyBytes (or PyUnicode if this string is one), this string after replacements.
     */
    public static final String _replace(String s, String oldPiece, String newPiece, int count) {

        int len = s.length();
        int oldLen = oldPiece.length();
        int newLen = newPiece.length();

        if (len == 0) {
            if (count < 0 && oldLen == 0) {
                return newPiece;
            }
            return s;
        } else if (oldLen == 0 && newLen != 0 && count != 0) {
            /*
             * old="" and new != "", interleave new piece with each char in original, taking into
             * account count
             */
            int i = 0;
            StringBuilder buffer = new StringBuilder(newPiece);
            for (; i < len && (count < 0 || i < count - 1); i++) {
                buffer.append(s.charAt(i)).append(newPiece);
            }
            buffer.append(s.substring(i));
            return buffer.toString();
        } else {
            if (count < 0) {
                count = (oldLen == 0) ? len + 1 : len;
            }
            return Joiner.on(newPiece).join(Pattern.compile(oldPiece, Pattern.LITERAL).split(s, count + 1));
        }
    }

    public static final boolean isLowercase(CharSequence s) {
        return s.length() != 0 && CharMatcher.JAVA_LOWER_CASE.matchesAllOf(s);
    }

    public static final boolean isUppercase(CharSequence s) {
        return s.length() != 0 && CharMatcher.JAVA_UPPER_CASE.matchesAllOf(s);
    }

    public static final boolean isAlpha(CharSequence s) {
        return s.length() != 0 && CharMatcher.JAVA_LETTER.matchesAllOf(s);
    }

    public static final boolean isAlnum(CharSequence s) {
        return s.length() != 0 && CharMatcher.JAVA_LETTER_OR_DIGIT.matchesAllOf(s);
    }

    public static final boolean isDecimal(CharSequence s) {
        return s.length() != 0 && CharMatcher.forPredicate(new Predicate<Character>() {
            @Override
            public boolean apply(Character ch) {
                return Character.getType(ch) == Character.DECIMAL_DIGIT_NUMBER;
            }
        }).matchesAllOf(s);
    }

    public static final boolean isDigit(CharSequence s) {
        return s.length() != 0 && CharMatcher.DIGIT.matchesAllOf(s);
    }

    public static final boolean isNumeric(CharSequence s) {
        return s.length() != 0 && CharMatcher.forPredicate(new Predicate<Character>() {
            @Override
            public boolean apply(Character ch) {
                int type = Character.getType(ch);
                return type == Character.DECIMAL_DIGIT_NUMBER || type == Character.LETTER_NUMBER
                        || type == Character.OTHER_NUMBER;
            }
        }).matchesAllOf(s);
    }

    public static final boolean isTitle(CharSequence s) {
        int n = s.length();

        /* Shortcut for single character strings */
        if (n == 1) {
            return Character.isTitleCase(s.charAt(0))
                    || Character.isUpperCase(s.charAt(0));
        }

        boolean cased = false;
        boolean previous_is_cased = false;
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);

            if (Character.isUpperCase(ch) || Character.isTitleCase(ch)) {
                if (previous_is_cased) {
                    return false;
                }
                previous_is_cased = true;
                cased = true;
            } else if (Character.isLowerCase(ch)) {
                if (!previous_is_cased) {
                    return false;
                }
                previous_is_cased = true;
                cased = true;
            } else {
                previous_is_cased = false;
            }
        }
        return cased;
    }

    public static final boolean isSpace(CharSequence s) {
        return s.length() != 0 && CharMatcher.WHITESPACE.matchesAllOf(s);
    }

    public static PyObject format(CharSequence s, PyObject formatSpec, boolean bytes) {
        // Parse the specification
        InternalFormat.Spec spec = InternalFormat.fromText(formatSpec, "__format__");

        // Get a formatter for the specification
        TextFormatter f = prepareFormatter(spec);
        if (f == null) {
            // The type code was not recognised
            throw InternalFormat.Formatter.unknownFormat(spec.type, "string");
        }

        f.setBytes(bytes);

        // Convert as per specification.
        f.format(s);

        // Return a result that has the same type (str or unicode) as the formatSpec argument.
        return f.pad().getPyResult();
    }

    /**
     * Common code for {@link PyBytes} and {@link PyUnicode} to prepare a {@link TextFormatter}
     * from a parsed specification. The object returned has format method
     * {@link TextFormatter#format(String)} that treats its argument as UTF-16 encoded unicode (not
     * just <code>char</code>s). That method will format its argument ( <code>str</code> or
     * <code>unicode</code>) according to the PEP 3101 formatting specification supplied here. This
     * would be used during <code>text.__format__(".5s")</code> or
     * <code>"{:.5s}".format(text)</code> where <code>text</code> is this Python string.
     *
     * @param spec a parsed PEP-3101 format specification.
     * @return a formatter ready to use, or null if the type is not a string format type.
     * @throws PyException(ValueError) if the specification is faulty.
     */
    @SuppressWarnings("fallthrough")
    public static final TextFormatter prepareFormatter(InternalFormat.Spec spec) throws PyException {
        // Slight differences between format types
        switch (spec.type) {

            case InternalFormat.Spec.NONE:
            case 's':
                // Check for disallowed parts of the specification
                if (spec.grouping) {
                    throw InternalFormat.Formatter.notAllowed("Grouping", "string", spec.type);
                } else if (InternalFormat.Spec.specified(spec.sign)) {
                    throw InternalFormat.Formatter.signNotAllowed("string", '\0');
                } else if (spec.alternate) {
                    throw InternalFormat.Formatter.alternateFormNotAllowed("string");
                } else if (spec.align == '=') {
                    throw InternalFormat.Formatter.alignmentNotAllowed('=', "string");
                }
                // spec may be incomplete. The defaults are those commonly used for string formats.
                spec = spec.withDefaults(InternalFormat.Spec.STRING);
                // Get a formatter for the specification
                return new TextFormatter(spec);

            default:
                // The type code was not recognised
                return null;
        }
    }

    public static final int[] translateIndices(CharSequence s, PyObject startObj, PyObject endObj, int len) {
        int start, end;
        int n = len;
        int[] result = new int[4];

        // Decode the start using slice semantics
        if (startObj == null || startObj == Py.None) {
            start = 0;
            // result[2] = 0 already
        } else {
            // Convert to int but limit to Integer.MIN_VALUE <= start <= Integer.MAX_VALUE
            start = startObj.asIndex(null);
            if (start < 0) {
                // Negative value means "from the end"
                start = n + start;
            }
            result[2] = start;
        }

        // Decode the end using slice semantics
        if (endObj == null || endObj == Py.None) {
            result[1] = result[3] = end = n;
        } else {
            // Convert to int but limit to Integer.MIN_VALUE <= end <= Integer.MAX_VALUE
            end = endObj.asIndex(null);
            if (end < 0) {
                // Negative value means "from the end"
                result[3] = end = end + n;
                // Ensure end is safe for String.substring(start,end).
                if (end < 0) {
                    end = 0;
                    // result[1] = 0 already
                } else {
                    result[1] = end;
                }
            } else {
                result[3] = end;
                // Ensure end is safe for String.substring(start,end).
                if (end > n) {
                    result[1] = end = n;
                } else {
                    result[1] = end;
                }
            }
        }

        // Ensure start is safe for String.substring(start,end).
        if (start < 0) {
            start = 0;
            // result[0] = 0 already
        } else if (start > end) {
            result[0] = start = end;
        } else {
            result[0] = start;
        }

        return result;
    }

    public static final CharSequence getslice(CharSequence s, int start, int stop, int step, int sliceLength) {
        if (step > 0 && stop < start) {
            stop = start;
        }
        if (step == 1) {
            return s.subSequence(start, stop);
        }
        int n = sliceLength;
        char new_chars[] = new char[n];
        int j = 0;
        for (int i = start; j < n; i += step) {
            new_chars[j++] = s.charAt(i);
        }

        return new String(new_chars);
    }

    /**
     * Return a String equivalent to the argument according to the calling conventions of the
     * certain methods of <code>str</code>. Those methods accept as a byte string anything bearing
     * the buffer interface, or accept a <code>unicode</code> argument which they interpret from its
     * UTF-16 encoded form (the internal representation returned by {@link PyUnicode#getString()}).
     *
     * @param obj to coerce to a String
     * @return coerced value
     * @throws PyException if the coercion fails
     */
    public static String asUTF16StringOrError(PyObject obj) {
        // PyUnicode accepted here. Care required in the client if obj is not basic plane.
        String ret = asUTF16StringOrNull(obj);
        if (ret != null) {
            return ret;
        } else {
            throw Py.TypeError(String.format("must be bytes or a tuple of bytes, not '%s'", obj.getType().fastGetName()));
        }
    }

        /**
     * Return a String equivalent to the argument. This is a helper function to those methods that
     * accept any byte array type (any object that supports a one-dimensional byte buffer), or
     * accept a <code>unicode</code> argument which they interpret from its UTF-16 encoded form (the
     * internal representation returned by {@link PyUnicode#getString()}).
     *
     * @param obj to coerce to a String
     * @return coerced value or <code>null</code> if it can't be
     */
    private static String asUTF16StringOrNull(PyObject obj) {
        if (obj instanceof PyUnicode) {
            return ((PyUnicode)obj).getString();
        } else if (obj instanceof BufferProtocol) {
            // Other object with buffer API: briefly access the buffer
            try (PyBuffer buf = ((BufferProtocol)obj).getBuffer(PyBUF.FULL_RO)) {
                return buf.toString();
            }
        }
        return null;
    }

    /**
     * Return a String equivalent to the argument. This is a helper function to those methods that
     * accept any byte array type (any object that supports a one-dimensional byte buffer), but
     * <b>not</b> a <code>unicode</code>.
     *
     * @param obj to coerce to a String
     * @return coerced value or <code>null</code> if it can't be (including <code>unicode</code>)
     */
    public static String asStringOrNull(PyObject obj) {
        return (obj instanceof PyUnicode) ? null : asUTF16StringOrNull(obj);
    }

    /**
     * Return a String equivalent to the argument. This is a helper function to those methods that
     * accept any byte array type (any object that supports a one-dimensional byte buffer), but
     * <b>not</b> a <code>unicode</code>.
     * Added support for integer, as it can be interpreted as a byte
     *
     * @param obj to coerce to a String
     * @return coerced value
     * @throws PyException if the coercion fails (including <code>unicode</code>)
     */
    public static String asStringOrError(PyObject obj) throws PyException {
        return asStringOrError(obj, true);
    }

    public static String asStringOrError(PyObject obj, boolean allowInt) throws PyException {
        if (allowInt && obj instanceof PyLong) {
            int val = ((PyLong) obj).getValue().intValue();
            if (val < 0 || val > 255) {
                throw Py.ValueError("byte must be in range(0, 256)");
            }
            return String.valueOf((char) val);
        }
        String ret = (obj instanceof PyUnicode) ? null : asUTF16StringOrNull(obj);
        if (ret != null) {
            return ret;
        }
        throw Py.TypeError("expected str, bytearray or other buffer compatible object");
    }

    /**
     * Return a String equivalent to the argument according to the calling conventions of methods
     * that accept as a byte string anything bearing the buffer interface, or accept
     * <code>PyNone</code>, but <b>not</b> a <code>unicode</code>. (Or the argument may be omitted,
     * showing up here as null.) These include the <code>strip</code> and <code>split</code> methods
     * of <code>str</code>, where a null indicates that the criterion is whitespace, and
     * <code>str.translate</code>.
     *
     * @param obj to coerce to a String or null
     * @param name of method
     * @return coerced value or null
     * @throws PyException if the coercion fails (including <code>unicode</code>)
     */
    public static String asStringNullOrError(PyObject obj, String name) throws PyException {

        if (obj == null || obj == Py.None) {
            return null;
        }
        String ret = (obj instanceof PyUnicode) ? null : asUTF16StringOrNull(obj);
        if (ret != null) {
            return ret;
        }
        // A nameless method is the client
        throw Py.TypeError(String.format("a bytes-like object is required, not '%s'",
                obj.getType().fastGetName()));
    }
        /**
     * Implementation of Python <code>str.rsplit()</code> common to exposed and Java API returning a
     * {@link PyList} of <code>PyBytes</code>s. The <code>str</code> will be split at each
     * occurrence of <code>sep</code>, working from the right. If <code>sep == null</code>,
     * whitespace will be used as the criterion. If <code>sep</code> has zero length, a Python
     * <code>ValueError</code> is raised. If <code>maxsplit</code> &gt;=0 and there are more
     * feasible splits than <code>maxsplit</code> the first element of the list contains the what is
     * left over after the last split.
     * <p>
     * Implementation note: although a str contains only bytes, this method is also called by
     * {@link PyUnicode#unicode_rsplit(PyObject, int)} .
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @param maxsplit maximum number of splits to make (there may be <code>maxsplit+1</code>
     *            parts).
     * @return list(str) result
     */
    public static final List<CharSequence> _rsplit(CharSequence s, String sep, int maxsplit) {
        if (sep == null) {
            // Split on runs of whitespace
            return rsplitfields(s, maxsplit);
        } else if (sep.length() == 0) {
            throw Py.ValueError("empty separator");
        } else {
            // Split on specified (non-empty) string
            return rsplitfields(s, sep, maxsplit);
        }
    }

    /**
     * Helper function for <code>.rsplit</code>, in <code>str</code> and <code>unicode</code>,
     * splitting on white space and returning a list of the separated parts. If there are more than
     * <code>maxsplit</code> feasible the first element of the list is the remainder of the original
     * (this) string. The split sections will be {@link PyUnicode} if this object is a
     * <code>PyUnicode</code>.
     *
     * @param maxsplit limit on the number of splits (if &gt;=0)
     * @return <code>PyList</code> of split sections
     */
    public static List<CharSequence> rsplitfields(CharSequence s, int maxsplit) {
        /*
         * Result built here (in reverse) is a list of split parts, exactly as required for
         * s.rsplit(None, maxsplit). If there are to be n splits, there will be n+1 elements.
         */
        List<CharSequence> list = new ArrayList<>();

        int length = s.length(), end = length - 1, splits = 0, index;

        if (maxsplit < 0) {
            // Make all possible splits: there can't be more than:
            maxsplit = length;
        }

        // end is always the rightmost character not consumed into a piece on the list
        while (end >= 0) {

            // Find the next occurrence of non-whitespace (working leftwards)
            while (end >= 0) {
                if (!isWhitespace(s.charAt(end))) {
                    // Break leaving end pointing at non-whitespace
                    break;
                }
                --end;
            }

            if (end < 0) {
                // Only found whitespace so there is no next segment
                break;

            } else if (splits >= maxsplit) {
                // The next segment is the last and contains all characters back to the beginning
                index = -1;

            } else {
                // The next segment runs back to the next next whitespace or beginning
                for (index = end; index >= 0; --index) {
                    if (isWhitespace(s.charAt(index))) {
                        // Break leaving index pointing at whitespace
                        break;
                    }
                }
            }

            // Make a piece from index+1 start up to end+1
            list.add(s.subSequence(index + 1, end + 1));
            splits++;

            // Start next segment search at that point
            end = index;
        }

        return Lists.reverse(list);
    }

    /**
     * Helper function for <code>.rsplit</code>, in <code>str</code> and <code>unicode</code>,
     * returning a list of the separated parts, <em>in the reverse order</em> of their occurrence in
     * this string. If there are more than <code>maxsplit</code> occurrences of <code>sep</code> the
     * first element of the list is the left end of the original (this) string. The split sections
     * will be {@link PyUnicode} if this object is a <code>PyUnicode</code>.
     *
     * @param sep at occurrences of which this string should be split
     * @param maxsplit limit on the number of splits (if &gt;=0)
     * @return <code>PyList</code> of split sections
     */
    public static final List<CharSequence> rsplitfields(CharSequence s, String sep, int maxsplit) {
        /*
         * Result built here (in reverse) is a list of split parts, exactly as required for
         * s.rsplit(sep, maxsplit). If there are to be n splits, there will be n+1 elements.
         */
        List<CharSequence> list = new ArrayList<>();

        int length = s.length();
        int sepLength = sep.length();

        if (maxsplit < 0) {
            // Make all possible splits: there can't be more than:
            maxsplit = length + 1;
        }

        if (maxsplit == 0) {
            // Degenerate case
            list.add(s);

        } else if (sepLength == 0) {
            // Empty separator is not allowed
            throw Py.ValueError("empty separator");

        } else {
            // Index of first character of the last piece already on the list
            int end = length;

            // Add at most maxsplit pieces
            for (int splits = 0; splits < maxsplit; splits++) {

                // Find the next occurrence of sep (working leftwards)
                int index = s.toString().lastIndexOf(sep, end - sepLength);

                if (index < 0) {
                    // No more occurrences of sep: we're done
                    break;

                } else {
                    // Make a piece from where we found sep up to end
                    list.add(s.subSequence(index + sepLength, end));
                    // New end (of next piece) is where we found sep
                    end = index;
                }
            }

            // Last piece is the rest of the string (even if end==0)
            list.add(s.subSequence(0, end));
        }

        return Lists.reverse(list);
    }

        /**
     * Helper common to the Python and Java API returning the last index of the substring or -1 for
     * not found. It accepts slice-like arguments, which may be <code>None</code> or end-relative
     * (negative). This method also supports
     * {@link PyUnicode#str_frind(PyObject, PyObject, PyObject)}.
     *
     * @param sub substring to find.
     * @param startObj start of slice.
     * @param endObj end of slice.
     * @return index of <code>sub</code> in this object or -1 if not found.
     */
    public static final int _rfind(CharSequence s, String sub, PyObject startObj, PyObject endObj, int len) {
        // Interpret the slice indices as concrete values
        int[] indices = translateIndices(s, startObj, endObj, len);
        int subLen = sub.length();

        if (subLen == 0) {
            // Special case: an empty string may be found anywhere, ...
            int start = indices[2], end = indices[3];
            if (end < 0 || end < start || start > len) {
                // ... except ln a reverse slice or beyond the end of the string,
                return -1;
            } else {
                // ... and will be reported at the end of the overlap.
                return indices[1];
            }

        } else {
            // General case: search for first match then check against slice.
            int start = indices[0], end = indices[1];
            int found = s.toString().lastIndexOf(sub, end - subLen);
            if (found >= start) {
                return found;
            } else {
                return -1;
            }
        }
    }

    // only for BMP
    public static final String title(CharSequence s) {
        char[] chars = new char[s.length()];
        int n = chars.length;

        boolean previous_is_cased = false;
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            if (previous_is_cased) {
                chars[i] = Character.toLowerCase(ch);
            } else {
                chars[i] = Character.toTitleCase(ch);
            }

            if (Character.isLowerCase(ch) || Character.isUpperCase(ch) || Character.isTitleCase(ch)) {
                previous_is_cased = true;
            } else {
                previous_is_cased = false;
            }
        }
        return new String(chars);
    }

    public static final String swapcase(CharSequence s) {
        char[] chars = new char[s.length()];
        int n = chars.length;
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c)) {
                chars[i] = Character.toLowerCase(c);
            } else if (Character.isLowerCase(c)) {
                chars[i] = Character.toUpperCase(c);
            }
        }
        return new String(chars);
    }

    /**
     * Implementation of Python <code>str.rstrip()</code> common to exposed and Java API, when
     * stripping whitespace. Any whitespace byte/character will be discarded from the right end of
     * this <code>str</code>.
     * <p>
     * Implementation note: although a <code>str</code> contains only bytes, this method is also
     * called by {@link PyUnicode#str_rstrip(PyObject)} when this is a basic-plane string.
     *
     * @return a new String, stripped of the whitespace characters/bytes
     */
    public static final String _rstrip(CharSequence s) {
        // Rightmost non-whitespace
        int right = _stripRight(s);
        if (right < 0) {
            // They're all whitespace
            return "";
        } else {
            // Substring up to and including this rightmost non-whitespace
            return s.subSequence(0, right + 1).toString();
        }
    }

    /**
     * Implementation of Python <code>str.rstrip()</code> common to exposed and Java API. Any
     * byte/character matching one of those in <code>stripChars</code> will be discarded from the
     * right end of this <code>str</code>. If <code>stripChars == null</code>, whitespace will be
     * stripped.
     * <p>
     * Implementation note: although a <code>str</code> contains only bytes, this method is also
     * called by {@link PyUnicode#str_strip(PyObject)} when both arguments are basic-plane
     * strings.
     *
     * @param stripChars characters to strip or null
     * @return a new String, stripped of the specified characters/bytes
     */
    public static final String _rstrip(CharSequence s, String stripChars) {
        if (stripChars == null) {
            // Divert to the whitespace version
            return _rstrip(s);
        } else {
            // Rightmost non-matching character
            int right = _stripRight(s, stripChars);
            // Substring up to and including this rightmost non-matching character (or "")
            return s.subSequence(0, right + 1).toString();
        }
    }

    /**
     * Helper for <code>strip</code>, <code>rstrip</code> implementation, when stripping whitespace.
     *
     * @param s string to search.
     * @return index of rightmost non-whitespace character or -1 if they all are.
     */
    private static final int _stripRight(CharSequence s) {
        for (int right = s.length(); --right >= 0;) {
            if (!isWhitespace(s.charAt(right))) {
                return right;
            }
        }
        return -1;
    }

    /**
     * Helper for <code>strip</code>, <code>rstrip</code> implementation, when stripping specified
     * characters.
     *
     * @param s string to search.
     * @param stripChars specifies set of characters to strip
     * @return index of rightmost character not in <code>stripChars</code> or -1 if they all are.
     */
    private static final int _stripRight(CharSequence s, String stripChars) {
        for (int right = s.length(); --right >= 0;) {
            if (stripChars.indexOf(s.charAt(right)) < 0) {
                return right;
            }
        }
        return -1;
    }

        /**
     * Implementation of Python <code>str.strip()</code> common to exposed and Java API, when
     * stripping whitespace. Any whitespace byte/character will be discarded from either end of this
     * <code>str</code>.
     * <p>
     * Implementation note: although a <code>str</code> contains only bytes, this method is also
     * called by {@link PyUnicode#str_strip(PyObject)} when this is a basic-plane string.
     *
     * @return a new String, stripped of the whitespace characters/bytes
     */
    public static final CharSequence _strip(CharSequence s) {
        // Rightmost non-whitespace
        int right = _stripRight(s);
        if (right < 0) {
            // They're all whitespace
            return "";
        } else {
            // Leftmost non-whitespace character: right known not to be a whitespace
            int left = _stripLeft(s, right);
            return s.subSequence(left, right + 1);
        }
    }

    /**
     * Implementation of Python <code>str.strip()</code> common to exposed and Java API. Any
     * byte/character matching one of those in <code>stripChars</code> will be discarded from either
     * end of this <code>str</code>. If <code>stripChars == null</code>, whitespace will be
     * stripped.
     * <p>
     * Implementation note: although a <code>str</code> contains only bytes, this method is also
     * called by {@link PyUnicode#str_strip(PyObject)} when both arguments are basic-plane
     * strings.
     *
     * @param stripChars characters to strip or null
     * @return a new String, stripped of the specified characters/bytes
     */
    public static final CharSequence _strip(CharSequence s, String stripChars) {
        if (stripChars == null) {
            // Divert to the whitespace version
            return _strip(s);
        } else {
            // Rightmost non-matching character
            int right = _stripRight(s, stripChars);
            if (right < 0) {
                // They all match
                return "";
            } else {
                // Leftmost non-matching character: right is known not to match
                int left = _stripLeft(s, stripChars, right);
                return s.subSequence(left, right + 1);
            }
        }
    }

    /**
     * Helper for <code>strip</code>, <code>lstrip</code> implementation, when stripping whitespace.
     *
     * @param s string to search (only <code>s[0:right]</code> is searched).
     * @param right rightmost extent of string search
     * @return index of lefttmost non-whitespace character or <code>right</code> if they all are.
     */
    private static final int _stripLeft(CharSequence s, int right) {
        for (int left = 0; left < right; left++) {
            if (!isWhitespace(s.charAt(left))) {
                return left;
            }
        }
        return right;
    }

    /**
     * Helper for <code>strip</code>, <code>lstrip</code> implementation, when stripping specified
     * characters.
     *
     * @param s string to search (only <code>s[0:right]</code> is searched).
     * @param stripChars specifies set of characters to strip
     * @param right rightmost extent of string search
     * @return index of leftmost character not in <code>stripChars</code> or <code>right</code> if
     *         they all are.
     */
    private static final int _stripLeft(CharSequence s, String stripChars, int right) {
        for (int left = 0; left < right; left++) {
            if (stripChars.indexOf(s.charAt(left)) < 0) {
                return left;
            }
        }
        return right;
    }
        /**
     * Implementation of Python <code>str.lstrip()</code> common to exposed and Java API, when
     * stripping whitespace. Any whitespace byte/character will be discarded from the left end of
     * this <code>str</code>.
     * <p>
     * Implementation note: although a str contains only bytes, this method is also called by
     * {@link PyUnicode#str_lstrip(PyObject)} when this is a basic-plane string.
     *
     * @return a new String, stripped of the whitespace characters/bytes
     */
    public static final CharSequence _lstrip(CharSequence s) {
        // Leftmost non-whitespace character: cannot exceed length
        int left = _stripLeft(s, s.length());
        return s.subSequence(left, s.length());
    }

    /**
     * Implementation of Python <code>str.lstrip()</code> common to exposed and Java API. Any
     * byte/character matching one of those in <code>stripChars</code> will be discarded from the
     * left end of this <code>str</code>. If <code>stripChars == null</code>, whitespace will be
     * stripped.
     * <p>
     * Implementation note: although a <code>str</code> contains only bytes, this method is also
     * called by {@link PyUnicode#str_lstrip(PyObject)} when both arguments are basic-plane
     * strings.
     *
     * @param stripChars characters to strip or null
     * @return a new String, stripped of the specified characters/bytes
     */
    public static final CharSequence _lstrip(CharSequence s, String stripChars) {
        if (stripChars == null) {
            // Divert to the whitespace version
            return _lstrip(s);
        } else {
            // Leftmost matching character: cannot exceed length
            int left = Encoding._stripLeft(s, stripChars, s.length());
            return s.subSequence(left, s.length());
        }
    }

        /**
     * Implementation of Python str.split() common to exposed and Java API returning a
     * {@link PyList} of <code>PyBytes</code>s. The <code>str</code> will be split at each
     * occurrence of <code>sep</code>. If <code>sep == null</code>, whitespace will be used as the
     * criterion. If <code>sep</code> has zero length, a Python <code>ValueError</code> is raised.
     * If <code>maxsplit</code> &gt;=0 and there are more feasible splits than <code>maxsplit</code>
     * the last element of the list contains the what is left over after the last split.
     * <p>
     * Implementation note: although a str contains only bytes, this method is also called by
     * {@link PyUnicode#str_split(PyObject, int)}.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @param maxsplit maximum number of splits to make (there may be <code>maxsplit+1</code>
     *            parts).
     * @return list(str) result
     */
    public static final List<CharSequence> _split(CharSequence s, String sep, int maxsplit) {
        if (sep == null) {
            // Split on runs of whitespace
            return splitfields(s, maxsplit);
        } else if (sep.length() == 0) {
            throw Py.ValueError("empty separator");
        } else {
            // Split on specified (non-empty) string
            return splitfields(s, sep, maxsplit);
        }
    }

    /**
     * Helper function for <code>.split</code>, in <code>str</code> and <code>unicode</code>,
     * splitting on white space and returning a list of the separated parts. If there are more than
     * <code>maxsplit</code> feasible the last element of the list is the remainder of the original
     * (this) string. The split sections will be {@link PyUnicode} if this object is a
     * <code>PyUnicode</code>.
     *
     * @param maxsplit limit on the number of splits (if &gt;=0)
     * @return <code>PyList</code> of split sections
     */
    public static final List<CharSequence> splitfields(CharSequence s, int maxsplit) {
        /*
         * Result built here is a list of split parts, exactly as required for s.split(None,
         * maxsplit). If there are to be n splits, there will be n+1 elements in L.
         */
        List<CharSequence> list = new ArrayList<>();

        int length = s.length(), start = 0, splits = 0, index;

        if (maxsplit < 0) {
            // Make all possible splits: there can't be more than:
            maxsplit = length;
        }

        // start is always the first character not consumed into a piece on the list
        while (start < length) {

            // Find the next occurrence of non-whitespace
            while (start < length) {
                if (!isWhitespace(s.charAt(start))) {
                    // Break leaving start pointing at non-whitespace
                    break;
                }
                start++;
            }

            if (start >= length) {
                // Only found whitespace so there is no next segment
                break;

            } else if (splits >= maxsplit) {
                // The next segment is the last and contains all characters up to the end
                index = length;

            } else {
                // The next segment runs up to the next next whitespace or end
                for (index = start; index < length; index++) {
                    if (isWhitespace(s.charAt(index))) {
                        // Break leaving index pointing at whitespace
                        break;
                    }
                }
            }

            // Make a piece from start up to index
            list.add(s.subSequence(start, index));
            splits++;

            // Start next segment search at that point
            start = index;
        }

        return list;
    }

    /**
     * Helper function for <code>.split</code> and <code>.replace</code>, in <code>str</code> and
     * <code>unicode</code>, returning a list of the separated parts. If there are more than
     * <code>maxsplit</code> occurrences of <code>sep</code> the last element of the list is the
     * remainder of the original (this) string. If <code>sep</code> is the zero-length string, the
     * split is between each character (as needed by <code>.replace</code>). The split sections will
     * be {@link PyUnicode} if this object is a <code>PyUnicode</code>.
     *
     * @param sep at occurrences of which this string should be split
     * @param maxsplit limit on the number of splits (if &gt;=0)
     * @return <code>PyList</code> of split sections
     */
    public static final List<CharSequence> splitfields(CharSequence s, String sep, int maxsplit) {
        /*
         * Result built here is a list of split parts, exactly as required for s.split(sep), or to
         * produce the result of s.replace(sep, r) by a subsequent call r.join(L). If there are to
         * be n splits, there will be n+1 elements in L.
         */
        List<CharSequence> list = new ArrayList<>();

        int length = s.length();
        int sepLength = sep.length();

        if (maxsplit < 0) {
            // Make all possible splits: there can't be more than:
            maxsplit = length + 1;
        }

        if (maxsplit == 0) {
            // Degenerate case
            list.add(s);

        } else if (sepLength == 0) {
            /*
             * The separator is "". This cannot happen with s.split(""), as that's an error, but it
             * is used by s.replace("", A) and means that the result should be A interleaved between
             * the characters of s, before the first, and after the last, the number always limited
             * by maxsplit.
             */

            // There will be m+1 parts, where m = maxsplit or length+1 whichever is smaller.
            int m = (maxsplit > length) ? length + 1 : maxsplit;

            // Put an empty string first to make one split before the first character
            list.add(""); // PyBytes or PyUnicode as this class
            int index;

            // Add m-1 pieces one character long
            for (index = 0; index < m - 1; index++) {
                list.add(s.subSequence(index, index + 1));
            }

            // And add the last piece, so there are m+1 splits (m+1 pieces)
            list.add(s.subSequence(index, length));

        } else {
            // Index of first character not yet in a piece on the list
            int start = 0;

            // Add at most maxsplit pieces
            for (int splits = 0; splits < maxsplit; splits++) {

                // Find the next occurrence of sep
                int index = s.toString().indexOf(sep, start);

                if (index < 0) {
                    // No more occurrences of sep: we're done
                    break;

                } else {
                    // Make a piece from start up to where we found sep
                    list.add(s.subSequence(start, index));
                    // New start (of next piece) is just after sep
                    start = index + sepLength;
                }
            }

            // Last piece is the rest of the string (even if start==length)
            list.add(s.subSequence(start, length));
        }

        return list;
    }

    public static final List<CharSequence> splitlines(CharSequence s, boolean keepends) {
        List<CharSequence> list = new ArrayList<>();

        int n = s.length();

        int j = 0;
        for (int i = 0; i < n; ) {
            /* Find a line and append it */
            while (i < n && s.charAt(i) != '\n' && s.charAt(i) != '\r'
                    && Character.getType(s.charAt(i)) != Character.LINE_SEPARATOR) {
                i++;
            }

            /* Skip the line break reading CRLF as one line break */
            int eol = i;
            if (i < n) {
                if (s.charAt(i) == '\r' && i + 1 < n && s.charAt(i+1) == '\n') {
                    i += 2;
                } else {
                    i++;
                }
                if (keepends) {
                    eol = i;
                }
            }
            list.add(s.subSequence(j, eol));
            j = i;
        }
        if (j < n) {
            list.add(s.subSequence(j, n));
        }
        return list;
    }

    /**
     * Helper common to the Python and Java API returning the index of the substring or -1 for not
     * found. It accepts slice-like arguments, which may be <code>None</code> or end-relative
     * (negative). This method also supports
     * {@link PyUnicode#str_find(PyObject, PyObject, PyObject)}.
     *
     * @param sub substring to find.
     * @param startObj start of slice.
     * @param endObj end of slice.
     * @return index of <code>sub</code> in this object or -1 if not found.
     */
    public static final int _find(CharSequence s, String sub, PyObject startObj, PyObject endObj, int len) {
        // Interpret the slice indices as concrete values
        int[] indices = translateIndices(s, startObj, endObj, len);
        int subLen = sub.length();

        if (subLen == 0) {
            // Special case: an empty string may be found anywhere, ...
            int start = indices[2], end = indices[3];
            if (end < 0 || end < start || start > len) {
                // ... except ln a reverse slice or beyond the end of the string,
                return -1;
            } else {
                // ... and will be reported at the start of the overlap.
                return indices[0];
            }

        } else {
            // General case: search for first match then check against slice.
            int start = indices[0], end = indices[1];
            int found = s.toString().indexOf(sub, start);
            if (found >= 0 && found + subLen <= end) {
                return found;
            } else {
                return -1;
            }
        }
    }

    /**
     * Helper common to the Python and Java API returning the number of occurrences of a substring.
     * It accepts slice-like arguments, which may be <code>None</code> or end-relative (negative).
     * This method also supports {@link PyUnicode#str_count(PyObject, PyObject, PyObject)}.
     *
     * @param sub substring to find.
     * @param startObj start of slice.
     * @param endObj end of slice.
     * @return count of occurrences
     */
    public static final int _count(CharSequence s, String sub, PyObject startObj, PyObject endObj, int len) {

        // Interpret the slice indices as concrete values
        int[] indices = translateIndices(s, startObj, endObj, len);
        int subLen = sub.length();

        if (subLen == 0) {
            // Special case counting the occurrences of an empty string.
            int start = indices[2], end = indices[3], n = len;
            if (end < 0 || end < start || start > n) {
                // Slice is reversed or does not overlap the string.
                return 0;
            } else {
                // Count of '' is one more than number of characters in overlap.
                return Math.min(end, n) - Math.max(start, 0) + 1;
            }

        } else {

            // Skip down this string finding occurrences of sub
            int start = indices[0], end = indices[1];
            int limit = end - subLen, count = 0;

            while (start <= limit) {
                int index = s.toString().indexOf(sub, start);
                if (index >= 0 && index <= limit) {
                    // Found at index.
                    count += 1;
                    // Next search begins after this instance, at:
                    start = index + subLen;
                } else {
                    // not found, or found too far right (index>limit)
                    break;
                }
            }
            return count;
        }
    }
    public static final CharSequence zfill(CharSequence s, int width) {
        int n = s.length();
        if (n >= width) {
            return s;
        }
        StringBuilder ret = new StringBuilder();
        int nzeros = width - n;
        int i = 0;
        if (n > 0) {
            char start = s.charAt(0);
            if (start == '+' || start == '-') {
                ret.append(start);
                i += 1;
                nzeros++;
            }
        }
        for (; i < nzeros; i++) {
            ret.append('0');
        }
        ret.append(s);
        return ret;
    }

    public static final String expandtabs(CharSequence s, int tabsize) {
        StringBuilder buf = new StringBuilder((int)(s.length() * 1.5));
        int n = s.length();
        int position = 0;

        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c == '\t') {
                int spaces = tabsize - position % tabsize;
                position += spaces;
                while (spaces-- > 0) {
                    buf.append(' ');
                }
                continue;
            }
            if (c == '\n' || c == '\r') {
                position = -1;
            }
            buf.append(c);
            position++;
        }
        return buf.toString();
    }

    public static final String capitalize(CharSequence s) {
        if (s.length() == 0) {
            return s.toString();
        }
        String first = s.toString().substring(0, 1).toUpperCase();
        return first.concat(s.toString().substring(1).toLowerCase());
    }

    public static final boolean startswith(CharSequence s, PyObject prefix, PyObject startObj, PyObject endObj, int len) {
        int[] indices = translateIndices(s, startObj, endObj, len);
        int start = indices[0];
        int sliceLen = indices[1] - start;

        if (!(prefix instanceof PyTuple)) {
            String p = asUTF16StringOrError(prefix);
            // If s is non-BMP, and this is a PyBytes (bytes), result will correctly be false.
            return sliceLen >= p.length() && s.toString().startsWith(p, start);

        } else {
            // Loop will return true if this slice starts with any prefix in the tuple
            for (PyObject prefixObj : ((PyTuple)prefix).getArray()) {
                // It ought to be PyUnicode or some kind of bytes with the buffer API.
                String p = asUTF16StringOrError(prefixObj);
                // If s is non-BMP, and this is a PyBytes (bytes), result will correctly be false.
                if (sliceLen >= p.length() && s.toString().startsWith(p, start)) {
                    return true;
                }
            }
            // None matched
            return false;
        }
    }

    public static final boolean endswith(CharSequence s, PyObject suffix, PyObject startObj, PyObject endObj, int len) {
        int[] indices = translateIndices(s, startObj, endObj, len);
        String substr = s.toString().substring(indices[0], indices[1]);

        if (!(suffix instanceof PyTuple)) {
            // It ought to be PyUnicode or some kind of bytes with the buffer API.
            String suf = asUTF16StringOrError(suffix);
            // If s is non-BMP, and this is a PyBytes (bytes), result will correctly be false.
            return substr.endsWith(suf);

        } else {
            // Loop will return true if this slice ends with any suffix in the tuple
            for (PyObject suffixObj : ((PyTuple)suffix).getArray()) {
                // It ought to be PyUnicode or some kind of bytes with the buffer API.
                String suf = asUTF16StringOrError(suffixObj);
                // If s is non-BMP, and this is a PyBytes (bytes), result will correctly be false.
                if (substr.endsWith(suf)) {
                    return true;
                }
            }
            // None matched
            return false;
        }
    }

    public static PyLong atol(CharSequence s, int base) {
        if ((base != 0 && base < 2) || (base > 36)) {
            throw Py.ValueError("invalid base for int literal:" + base);
        }

        try {
            BigInteger bi = Encoding.asciiToBigInteger(s, base, true);
            return new PyLong(bi);
        } catch (NumberFormatException exc) {
            throw Py.ValueError("invalid literal for int() with base " + base + ": '"
                    + s + "'");
        } catch (StringIndexOutOfBoundsException exc) {
            throw Py.ValueError("invalid literal for int() with base " + base + ": '"
                    + s + "'");
        }
    }

    private static BigInteger asciiToBigInteger(CharSequence str, int base, boolean isLong) {
        int b = 0;
        int e = str.length();

        while (b < e && Character.isWhitespace(str.charAt(b))) {
            b++;
        }

        while (e > b && Character.isWhitespace(str.charAt(e - 1))) {
            e--;
        }

        char sign = 0;
        if (b < e) {
            sign = str.charAt(b);
            if (sign == '-' || sign == '+') {
                b++;
                while (b < e && Character.isWhitespace(str.charAt(b))) {
                    b++;
                }
            }

            if (base == 16) {
                if (str.charAt(b) == '0') {
                    if (b < e - 1 && Character.toUpperCase(str.charAt(b + 1)) == 'X') {
                        b += 2;
                    }
                }
            } else if (base == 0) {
                if (str.charAt(b) == '0') {
                    if (b < e - 1 && Character.toUpperCase(str.charAt(b + 1)) == 'X') {
                        base = 16;
                        b += 2;
                    } else if (b < e - 1 && Character.toUpperCase(str.charAt(b + 1)) == 'O') {
                        base = 8;
                        b += 2;
                    } else if (b < e - 1 && Character.toUpperCase(str.charAt(b + 1)) == 'B') {
                        base = 2;
                        b += 2;
                    } else {
                        base = 8;
                    }
                }
            } else if (base == 8) {
                if (b < e - 1 && Character.toUpperCase(str.charAt(b + 1)) == 'O') {
                    b += 2;
                }
            } else if (base == 2) {
                if (b < e - 1 && Character.toUpperCase(str.charAt(b + 1)) == 'B') {
                    b += 2;
                }
            }
        }

        if (base == 0) {
            base = 10;
        }

        // if the base >= 22, then an 'l' or 'L' is a digit!
        if (isLong && base < 22 && e > b && (str.charAt(e - 1) == 'L' || str.charAt(e - 1) == 'l')) {
            e--;
        }

        CharSequence s = str;
        if (b > 0 || e < str.length()) {
            s = str.subSequence(b, e);
        }

        BigInteger bi;
        if (sign == '-') {
            bi = new BigInteger("-" + s, base);
        } else {
            bi = new BigInteger(s.toString(), base);
        }
        return bi;
    }

    public static final int atoi(CharSequence s, int base) {
        if ((base != 0 && base < 2) || (base > 36)) {
            throw Py.ValueError("invalid base for atoi()");
        }

        try {
            BigInteger bi = asciiToBigInteger(s, base, false);
            if (bi.compareTo(PyInteger.MAX_INT) > 0 || bi.compareTo(PyInteger.MIN_INT) < 0) {
                throw Py.OverflowError("long int too large to convert to int");
            }
            return bi.intValue();
        } catch (NumberFormatException exc) {
            throw Py.ValueError("invalid literal for int() with base " + base + ": '" + s
                    + "'");
        } catch (StringIndexOutOfBoundsException exc) {
            throw Py.ValueError("invalid literal for int() with base " + base + ": '" + s
                    + "'");
        }
    }

    /**
     * Convert this PyBytes to a floating-point value according to Python rules.
     *
     * @return the value
     */
    public static double atof(CharSequence s) {
        double x = 0.0;
        Matcher m = getFloatPattern().matcher(s);
        boolean valid = m.matches();

        if (valid) {
            // Might be a valid float: trimmed of white space in group 1.
            String number = m.group(1);
            try {
                char lastChar = number.charAt(number.length() - 1);
                if (Character.isLetter(lastChar)) {
                    // It's something like "nan", "-Inf" or "+nifty"
                    x = atofSpecials(m.group(1));
                } else {
                    // A numeric part was present, try to convert the whole
                    x = Double.parseDouble(m.group(1));
                }
            } catch (NumberFormatException e) {
                valid = false;
            }
        }

        // At this point, valid will have been cleared if there was a problem.
        if (valid) {
            return x;
        } else {
            String fmt = "could not convert string to float: '%s'";
            throw Py.ValueError(String.format(fmt, s));
        }

    }

    /**
     * Return the (lazily) compiled regular expression that matches all valid a Python float()
     * arguments, in which Group 1 captures the number, stripped of white space. Various invalid
     * non-numerics are provisionally accepted (e.g. "+inanity" or "-faint").
     */
    private static synchronized Pattern getFloatPattern() {
        if (floatPattern == null) {
            floatPattern = Pattern.compile("\\s*([+-]?" + UF_RE + ")\\s*");
        }
        return floatPattern;
    }

    /** Access only through {@link #getFloatPattern()}. */
    private static Pattern floatPattern = null;

    private static boolean isWhitespace(char c) {
        switch(c) {
            case 0x09:
            case 0x0A:
            case 0x0B:
            case 0x0C:
            case 0x0D:
            case 0x20:
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * Regular expression for an unsigned Python float, accepting also any sequence of the letters
     * that belong to "NaN" or "Infinity" in whatever case. This is used within the regular
     * expression patterns that define a priori acceptable strings in the float and complex
     * constructors. The expression contributes no capture groups.
     */
    private static final String UF_RE =
            "(?:(?:(?:\\d+\\.?|\\.\\d)\\d*(?:[eE][+-]?\\d+)?)|[infatyINFATY]+)";

    /**
     * Return the (lazily) compiled regular expression for a Python complex number. This is used
     * within the regular expression patterns that define a priori acceptable strings in the complex
     * constructors. The expression contributes five named capture groups a, b, x, y and j. x and y
     * are the two floats encountered, and if j is present, one of them is the imaginary part.
     * a and b are the optional parentheses. They must either both be present or both omitted.
     */
    private static synchronized Pattern getComplexPattern() {
        if (complexPattern == null) {
            complexPattern = Pattern.compile("\\s*(?<a>\\(\\s*)?" // Parenthesis <a>
                    + "(?<x>[+-]?" + UF_RE + "?)" // <x>
                    + "(?<y>[+-]" + UF_RE + "?)?(?<j>[jJ])?" // + <y> <j>
                    + "\\s*(?<b>\\)\\s*)?"); // Parenthesis <b>
        }
        return complexPattern;
    }

    /** Access only through {@link #getComplexPattern()} */
    private static Pattern complexPattern = null;

    /**
     * Conversion for non-numeric floats, accepting signed or unsigned "inf" and "nan", in any case.
     *
     * @param s to convert
     * @return non-numeric result (if valid)
     * @throws NumberFormatException if not a valid non-numeric indicator
     */
    private static double atofSpecials(String s) throws NumberFormatException {
        switch (s.toLowerCase()) {
            case "nan":
            case "+nan":
            case "-nan":
                return Double.NaN;
            case "inf":
            case "+inf":
            case "infinity":
            case "+infinity":
                return Double.POSITIVE_INFINITY;
            case "-inf":
            case "-infinity":
                return Double.NEGATIVE_INFINITY;
            default:
                throw new NumberFormatException();
        }
    }

    /**
     * Convert this PyBytes to a complex value according to Python rules.
     *
     * @return the value
     */
    public static PyComplex atocx(CharSequence s) {
        double x = 0.0, y = 0.0;
        Matcher m = getComplexPattern().matcher(s);
        boolean valid = m.matches();

        if (valid) {
            // Passes a priori, but we have some checks to make. Brackets: both or neither.
            if ((m.group("a") != null) != (m.group("b") != null)) {
                valid = false;

            } else {
                try {
                    // Pick up the two numbers [+-]? <x> [+-] <y> j?
                    String xs = m.group("x"), ys = m.group("y");

                    if (m.group("j") != null) {
                        // There is a 'j', so there is an imaginary part.
                        if (ys != null) {
                            // There were two numbers, so the second is the imaginary part.
                            y = toComplexPart(ys);
                            // And the first is the real part
                            x = toComplexPart(xs);
                        } else if (xs != null) {
                            // There was only one number (and a 'j')so it is the imaginary part.
                            y = toComplexPart(xs);
                            // x = 0.0;
                        } else {
                            // There were no numbers, just the 'j'. (Impossible return?)
                            y = 1.0;
                            // x = 0.0;
                        }

                    } else {
                        // There is no 'j' so can only be one number, the real part.
                        x = Double.parseDouble(xs);
                        if (ys != null) {
                            // Something like "123 +" or "123 + 456" but no 'j'.
                            throw new NumberFormatException();
                        }
                    }

                } catch (NumberFormatException e) {
                    valid = false;
                }
            }
        }

        // At this point, valid will have been cleared if there was a problem.
        if (valid) {
            return new PyComplex(x, y);
        } else {
            String fmt = "complex() arg is a malformed string: %s";
            throw Py.ValueError(String.format(fmt, s));
        }

    }

    /**
     * Helper for interpreting each part (real and imaginary) of a complex number expressed as a
     * string in {@link #atocx(String)}. It deals with numbers, inf, nan and their variants, and
     * with the "implied one" in +j or 10-j.
     *
     * @param s to interpret
     * @return value of s
     * @throws NumberFormatException if the number is invalid
     */
    private static double toComplexPart(String s) throws NumberFormatException {
        if (s.length() == 0) {
            // Empty string (occurs only as 'j')
            return 1.0;
        } else {
            char lastChar = s.charAt(s.length() - 1);
            if (Character.isLetter(lastChar)) {
                // Possibly a sign, then letters that ought to be "nan" or "inf[inity]"
                return atofSpecials(s);
            } else if (lastChar == '+') {
                // Occurs only as "+j"
                return 1.0;
            } else if (lastChar == '-') {
                // Occurs only as "-j"
                return -1.0;
            } else {
                // Possibly a sign then an unsigned float
                return Double.parseDouble(s);
            }
        }
    }

    public static String encode_UnicodeEscapeAsASCII(String str) {
        int size = str.length();
        StringBuilder v = new StringBuilder(str.length());

        char quote = 0;

        quote = str.indexOf('\'') >= 0 && str.indexOf('"') == -1 ? '"' : '\'';
        v.append(quote);

        for (int i = 0; size-- > 0; ) {
            char ch = str.charAt(i++);
            /* Escape quotes */
            if (ch == quote || ch == '\\') {
                v.append('\\');
                v.append(ch);
                continue;
            }
            /* Map UTF-16 surrogate pairs to Unicode \UXXXXXXXX escapes */
            else if (size > 0 && Character.isHighSurrogate(ch)) {
                char ch2 = str.charAt(i++);
                size--;
                if (Character.isLowSurrogate(ch2)) {
                    int ucs = Character.toCodePoint(ch, ch2);
                    v.append('\\');
                    v.append('U');
                    v.append(hexdigit[(ucs >> 28) & 0xf]);
                    v.append(hexdigit[(ucs >> 24) & 0xf]);
                    v.append(hexdigit[(ucs >> 20) & 0xf]);
                    v.append(hexdigit[(ucs >> 16) & 0xf]);
                    v.append(hexdigit[(ucs >> 12) & 0xf]);
                    v.append(hexdigit[(ucs >> 8) & 0xf]);
                    v.append(hexdigit[(ucs >> 4) & 0xf]);
                    v.append(hexdigit[ucs & 0xf]);
                    continue;
                }
                /* Fall through: isolated surrogates are copied as-is */
                i--;
                size++;
            }
            /* Map 16-bit characters to '\\uxxxx' */
            if (ch >= 256) {
                v.append('\\');
                v.append('u');
                v.append(hexdigit[(ch >> 12) & 0xf]);
                v.append(hexdigit[(ch >> 8) & 0xf]);
                v.append(hexdigit[(ch >> 4) & 0xf]);
                v.append(hexdigit[ch & 15]);
            }
            /* Map special whitespace to '\t', \n', '\r' */
            else if (ch == '\t') {
                v.append("\\t");
            } else if (ch == '\n') {
                v.append("\\n");
            } else if (ch == '\r') {
                v.append("\\r");
            } else if (ch < ' ' || ch >= 127) {
                /* Map non-printable US ASCII to '\xNN' */
                v.append('\\');
                v.append('x');
                v.append(hexdigit[(ch >> 4) & 0xf]);
                v.append(hexdigit[ch & 0xf]);
            } else {/* Copy everything else as-is */
                v.append(ch);
            }
        }
        v.append(quote);
        return v.toString();
    }
}

