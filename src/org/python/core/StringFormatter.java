package org.python.core;

import org.python.core.stringlib.FloatFormatter;
import org.python.core.stringlib.IntegerFormatter;
import org.python.core.stringlib.InternalFormat;
import org.python.core.stringlib.TextFormatter;

/**
 * Interpreter for %-format strings. (Note visible across the core package.)
 */
final class StringFormatter {

    /** Index into {@link #format} being interpreted. */
    int index;
    /** Format being interpreted. */
    String format;
    /** Where the output is built. */
    StringBuilder buffer;
    /**
     * Index into args of argument currently being worked, or special values indicating -1: a single
     * item that has not yet been used, -2: a single item that has already been used, -3: a mapping.
     */
    int argIndex;
    /** Arguments supplied to {@link #format(PyObject)} method. */
    PyObject args;
    /** Indicate a <code>PyUnicode</code> result is expected. */
    boolean needUnicode;

    final char pop() {
        try {
            return format.charAt(index++);
        } catch (StringIndexOutOfBoundsException e) {
            throw Py.ValueError("incomplete format");
        }
    }

    final char peek() {
        return format.charAt(index);
    }

    final void push() {
        index--;
    }

    /**
     * Initialise the interpreter with the given format string, ready for {@link #format(PyObject)}.
     *
     * @param format string to interpret
     */
    public StringFormatter(String format) {
        this(format, false);
    }

    /**
     * Initialise the interpreter with the given format string, ready for {@link #format(PyObject)}.
     *
     * @param format string to interpret
     * @param unicodeCoercion to indicate a <code>PyUnicode</code> result is expected
     */
    public StringFormatter(String format, boolean unicodeCoercion) {
        index = 0;
        this.format = format;
        this.needUnicode = unicodeCoercion;
        buffer = new StringBuilder(format.length() + 100);
    }

    /**
     * Read the next object from the argument list, taking special values of <code>argIndex</code>
     * into account.
     */
    PyObject getarg() {
        PyObject ret = null;
        switch (argIndex) {
            case -3: // special index indicating a mapping
                return args;
            case -2: // special index indicating a single item that has already been used
                break;
            case -1: // special index indicating a single item that has not yet been used
                argIndex = -2;
                return args;
            default:
                ret = args.__finditem__(argIndex++);
                break;
        }
        if (ret == null) {
            throw Py.TypeError("not enough arguments for format string");
        }
        return ret;
    }

    /**
     * Parse a number from the format, except if the next thing is "*", read it from the argument
     * list.
     */
    int getNumber() {
        char c = pop();
        if (c == '*') {
            PyObject o = getarg();
            if (o instanceof PyLong) {
                return ((PyLong)o).asInt();
            }
            throw Py.TypeError("* wants int");
        } else {
            if (Character.isDigit(c)) {
                int numStart = index - 1;
                while (Character.isDigit(c = pop())) {}
                index -= 1;
                Integer i = Integer.valueOf(format.substring(numStart, index));
                return i.intValue();
            }
            index -= 1;
            return 0;
        }
    }

    /**
     * Return the argument as either a {@link PyInteger} or a {@link PyLong} according to its
     * <code>__int__</code> method, or its <code>__int__</code> method. If the argument has neither
     * method, or both raise an exception, we return the argument itself. The caller must check the
     * return type.
     *
     * @param arg to convert
     * @return PyInteger or PyLong if possible
     */
    private PyObject asNumber(PyObject arg) {
        if (arg instanceof PyInteger || arg instanceof PyLong) {
            // arg is already acceptable
            return arg;

        } else {
            // use __int__ or __int__to get an int (or long)
            if (arg.getClass() == PyFloat.class) {
                // A common case where it is safe to return arg.__int__()
                return arg.__int__();

            } else {
                /*
                 * In general, we can't simply call arg.__int__() because PyBytes implements it
                 * without exposing it to python (str has no __int__). This would make str
                 * acceptacle to integer format specifiers, which is forbidden by CPython tests
                 * (test_format.py). PyBytes implements __int__ perhaps only to help the int
                 * constructor. Maybe that was a bad idea?
                 */
                try {
                    // Result is the result of arg.__int__() if that works
                    return arg.__getattr__("__int__").__call__();
                } catch (PyException e) {
                    // Swallow the exception
                }

                // Try again with arg.__int__()
                try {
                    // Result is the result of arg.__int__() if that works
                    return arg.__getattr__("__int__").__call__();
                } catch (PyException e) {
                    // No __int__ defined (at Python level)
                    return arg;
                }
            }
        }
    }

    /**
     * Return the argument as a {@link PyFloat} according to its <code>__float__</code> method. If
     * the argument has no such method, or it raises an exception, we return the argument itself.
     * The caller must check the return type.
     *
     * @param arg to convert
     * @return PyFloat if possible
     */
    private PyObject asFloat(PyObject arg) {

        if (arg instanceof PyFloat) {
            // arg is already acceptable
            return arg;

        } else {
            // use __float__ to get a float.
            if (arg.getClass() == PyFloat.class) {
                // A common case where it is safe to return arg.__float__()
                return arg.__float__();

            } else {
                /*
                 * In general, we can't simply call arg.__float__() because PyBytes implements it
                 * without exposing it to python (str has no __float__). This would make str
                 * acceptacle to float format specifiers, which is forbidden by CPython tests
                 * (test_format.py). PyBytes implements __float__ perhaps only to help the float
                 * constructor. Maybe that was a bad idea?
                 */
                try {
                    // Result is the result of arg.__float__() if that works
                    return arg.__getattr__("__float__").__call__();
                } catch (PyException e) {
                    // No __float__ defined (at Python level)
                    return arg;
                }
            }
        }
    }

    /**
     * Main service of this class: format one or more arguments with the format string supplied at
     * construction.
     *
     * @param args tuple or map containing objects, or a single object, to convert
     * @return result of formatting
     */
    @SuppressWarnings("fallthrough")
    public PyObject format(PyObject args) {
        PyObject dict = null;
        this.args = args;

        if (args instanceof PyTuple) {
            // We will simply work through the tuple elements
            argIndex = 0;
        } else {
            // Not a tuple, but possibly still some kind of container: use special argIndex values.
            argIndex = -1;
            if (args instanceof PyDictionary || args instanceof PyStringMap
                    || (!(args instanceof PySequence) && args.__findattr__("__getitem__") != null)) {
                dict = args;
                argIndex = -3;
            }
        }

        while (index < format.length()) {

            // Read one character from the format string
            char c = pop();
            if (c != '%') {
                buffer.append(c);
                continue;
            }

            // It's a %, so the beginning of a conversion specifier. Parse it.

            // Attributes to be parsed from the next format specifier
            boolean altFlag = false;
            char sign = InternalFormat.Spec.NONE;
            char fill = ' ';
            char align = '>';
            int width = InternalFormat.Spec.UNSPECIFIED;
            int precision = InternalFormat.Spec.UNSPECIFIED;

            // A conversion specifier contains the following components, in this order:
            // + The '%' character, which marks the start of the specifier.
            // + Mapping key (optional), consisting of a parenthesised sequence of characters.
            // + Conversion flags (optional), which affect the result of some conversion types.
            // + Minimum field width (optional), or an '*' (asterisk).
            // + Precision (optional), given as a '.' (dot) followed by the precision or '*'.
            // + Length modifier (optional).
            // + Conversion type.

            c = pop();
            if (c == '(') {
                // Mapping key, consisting of a parenthesised sequence of characters.
                if (dict == null) {
                    throw Py.TypeError("format requires a mapping");
                }
                // Scan along until a matching close parenthesis is found
                int parens = 1;
                int keyStart = index;
                while (parens > 0) {
                    c = pop();
                    if (c == ')') {
                        parens--;
                    } else if (c == '(') {
                        parens++;
                    }
                }
                // Last c=pop() is the closing ')' while indexKey is just after the opening '('
                String tmp = format.substring(keyStart, index - 1);
                // Look it up using this extent as the (right type of) key.
                this.args = dict.__getitem__(needUnicode ? new PyUnicode(tmp) : new PyBytes(tmp));
            } else {
                // Not a mapping key: next clause will re-read c.
                push();
            }

            // Conversion flags (optional) that affect the result of some conversion types.
            while (true) {
                switch (c = pop()) {
                    case '-':
                        align = '<';
                        continue;
                    case '+':
                        sign = '+';
                        continue;
                    case ' ':
                        if (!InternalFormat.Spec.specified(sign)) {
                            // Blank sign only wins if '+' not specified.
                            sign = ' ';
                        }
                        continue;
                    case '#':
                        altFlag = true;
                        continue;
                    case '0':
                        fill = '0';
                        continue;
                }
                break;
            }
            // Push back c as next clause will re-read c.
            push();

            /*
             * Minimum field width (optional). If specified as an '*' (asterisk), the actual width
             * is read from the next element of the tuple in values, and the object to convert comes
             * after the minimum field width and optional precision. A custom getNumber() takes care
             * of the '*' case.
             */
            width = getNumber();
            if (width < 0) {
                width = -width;
                align = '<';
            }

            /*
             * Precision (optional), given as a '.' (dot) followed by the precision. If specified as
             * '*' (an asterisk), the actual precision is read from the next element of the tuple in
             * values, and the value to convert comes after the precision. A custom getNumber()
             * takes care of the '*' case.
             */
            c = pop();
            if (c == '.') {
                precision = getNumber();
                if (precision < -1) {
                    precision = 0;
                }
                c = pop();
            }

            // Length modifier (optional). (Compatibility feature?) It has no effect.
            if (c == 'h' || c == 'l' || c == 'L') {
                c = pop();
            }

            /*
             * As a function of the conversion type (currently in c) override some of the formatting
             * flags we read from the format specification.
             */
            switch (c) {
                case 's':
                case 'r':
                case 'c':
                case '%':
                    // These have string-like results: fill, if needed, is always blank.
                    fill = ' ';
                    break;

                default:
                    if (fill == '0' && align == '>') {
                        // Zero-fill comes after the sign in right-justification.
                        align = '=';
                    } else {
                        // If left-justifying, the fill is always blank.
                        fill = ' ';
                    }
            }

            /*
             * Encode as an InternalFormat.Spec. The values in the constructor always have specified
             * values, except for sign, width and precision.
             */
            InternalFormat.Spec spec = new InternalFormat.Spec(fill, align, sign, altFlag, width, false, precision, c);

            /*
             * Process argument according to format specification decoded from the string. It is
             * important we don't read the argument from the list until this point because of the
             * possibility that width and precision were specified via the argument list.
             */

            // Depending on the type of conversion, we use one of these formatters:
            FloatFormatter ff;
            IntegerFormatter fi;
            TextFormatter ft;
            InternalFormat.Formatter f; // = ff, fi or ft, whichever we actually use.

            switch (spec.type) {
                case 'b':
                    PyObject arg = getarg();
                    f = ft = new TextFormatter(buffer, spec);
                    ft.setBytes(true);
                    PyObject __bytes__;
                    if (arg instanceof PyBytes) {
                        ft.format(((PyBytes) arg).getString());
                    } else if ((__bytes__ = arg.__findattr__("__bytes__")) != null) {
                        ft.format(((PyBytes) __bytes__.__call__(arg)).getString());
                    } else {
                        throw Py.TypeError(String.format(
                                " %b requires bytes, or an object that implements __bytes__, not '%s'",
                                arg.getType().fastGetName()));
                    }
                    break;
                case 's': // String: converts any object using __str__(), __unicode__() ...
                case 'r': // ... or repr().
                    arg = getarg();

                    // Get hold of the actual object to display (may set needUnicode)
                    String argAsString = spec.type == 's' ? arg.__str__().getString() : arg.__repr__().toString();
                    // Format the str/unicode form of the argument using this Spec.
                    f = ft = new TextFormatter(buffer, spec);
                    ft.setBytes(!needUnicode);
                    ft.format(argAsString);
                    break;

                case 'd': // All integer formats (+case for X).
                case 'o':
                case 'x':
                case 'X':
                case 'c': // Single character (accepts integer or single character string).
                case 'u': // Obsolete type identical to 'd'.
                case 'i': // Compatibility with scanf().

                    // Format the argument using this Spec.
                    f = fi = new IntegerFormatter.Traditional(buffer, spec);
                    // If not producing PyUnicode, disallow codes >255.
                    fi.setBytes(!needUnicode);

                    arg = getarg();

                    if (arg instanceof PyUnicode && spec.type == 'c') {
                        if (arg.__len__() != 1) {
                            throw Py.TypeError("%c requires int or char");
                        } else {
                            if (!needUnicode && arg instanceof PyUnicode) {
                                // Change of mind forced by encountering unicode object.
                                needUnicode = true;
                                fi.setBytes(false);
                            }
                            fi.format(((PyUnicode)arg).getString().codePointAt(0));
                        }

                    } else {
                        // Note various types accepted here as long as they have an __int__ method.
                        PyObject argAsNumber = asNumber(arg);

                        // We have to check what we got back.
                        if (argAsNumber instanceof PyInteger) {
                            fi.format(((PyInteger)argAsNumber).getValue());
                        } else if (argAsNumber instanceof PyLong) {
                            fi.format(((PyLong)argAsNumber).getValue());
                        } else {
                            // It couldn't be converted, raise the error here
                            throw Py.TypeError("%" + spec.type
                                    + " format: a number is required, not "
                                    + arg.getType().fastGetName());
                        }
                    }

                    break;

                case 'e': // All floating point formats (+case).
                case 'E':
                case 'f':
                case 'F':
                case 'g':
                case 'G':

                    // Format using this Spec the double form of the argument.
                    f = ff = new FloatFormatter(buffer, spec);
                    ff.setBytes(!needUnicode);

                    // Note various types accepted here as long as they have a __float__ method.
                    arg = getarg();
                    PyObject argAsFloat = asFloat(arg);

                    // We have to check what we got back..
                    if (argAsFloat instanceof PyFloat) {
                        ff.format(((PyFloat)argAsFloat).getValue());
                    } else {
                        // It couldn't be converted, raise the error here
                        throw Py.TypeError("float argument required, not "
                                + arg.getType().fastGetName());
                    }

                    break;

                case '%': // Percent symbol, but surprisingly, padded.

                    // We use an integer formatter.
                    f = fi = new IntegerFormatter.Traditional(buffer, spec);
                    fi.setBytes(!needUnicode);
                    fi.format('%');
                    break;

                default:
                    throw Py.ValueError("unsupported format character '"
                            + codecs.encode(Py.newUnicode(spec.type), null, "replace") + "' (0x"
                            + Integer.toHexString(spec.type) + ") at index " + (index - 1));
            }

            // Pad the result as specified (in-place, in the buffer).
            f.pad();
        }

        /*
         * All fields in the format string have been used to convert arguments (or used the argument
         * as a width, etc.). This had better not leave any arguments unused. Note argIndex is an
         * index into args or has a special value. If args is a 'proper' index, It should now be out
         * of range; if a special value, it would be wrong if it were -1, indicating a single item
         * that has not yet been used.
         */
        if (argIndex == -1 || (argIndex >= 0 && args.__finditem__(argIndex) != null)) {
            throw Py.TypeError("not all arguments converted during string formatting");
        }

        // Return the final buffer contents as a str or unicode as appropriate.
        return needUnicode ? new PyUnicode(buffer) : new PyBytes(buffer);
    }

}
