package org.python.modules.unicodedata;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Normalizer;
import org.python.core.Py;
import org.python.core.PyFloat;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

import java.util.HashMap;

import static com.ibm.icu.lang.UCharacter.EastAsianWidth.*;
import static com.ibm.icu.lang.UCharacterEnums.ECharacterCategory.*;
import static com.ibm.icu.lang.UCharacterEnums.ECharacterDirection.*;
import static com.ibm.icu.text.Normalizer.*;

@ExposedType(name = "unicodedata.UCD")
public class UCD extends PyObject {
    private static final HashMap<PyUnicode, Normalizer.Mode> _FORMS = new HashMap<>();
    public static PyType TYPE = PyType.fromClass(UCD.class);

    public static final UCD UNICODEDATA = new UCD();
    public static final UCD UCD_3_2 = new UCD(true);

    private boolean ucd_3_2;

    protected UCD(boolean ucd_3_2) {
        super(TYPE);
        this.ucd_3_2 = ucd_3_2;
    }

    protected UCD() {
        this(false);
    }

    static {
        _FORMS.put(new PyUnicode("NFC"), NFC);
        _FORMS.put(new PyUnicode("NFKC"), NFKC);
        _FORMS.put(new PyUnicode("NFD"), NFD);
        _FORMS.put(new PyUnicode("NFKD"), NFKD);
    }

    @ExposedMethod
    final PyObject UCD_normalize(PyObject form, PyObject str) {
        if (!_FORMS.containsKey(form)) {
            throw Py.ValueError("invalid normalization form");
        }
        if (ucd_3_2) {
            return new PyUnicode(Normalizer.normalize(str.toString(), _FORMS.get(form), UNICODE_3_2));
        }
        return new PyUnicode(Normalizer.normalize(str.toString(), _FORMS.get(form)));
    }

    @ExposedMethod
    final PyObject UCD_category(PyObject chr) {
        int type = UCharacter.getType(getCodePoint("category", chr));
        String t;
        switch(type) {
            case COMBINING_SPACING_MARK:
                t = "Mc";
                break;
            case CONNECTOR_PUNCTUATION:
                t = "Pc";
                break;
            case CONTROL:
                t = "Cc";
                break;
            case CURRENCY_SYMBOL:
                t = "Sc";
                break;
            case DASH_PUNCTUATION:
                t = "Pd";
                break;
            case DECIMAL_DIGIT_NUMBER:
                t = "Nd";
                break;
            case ENCLOSING_MARK:
                t = "Me";
                break;
            case END_PUNCTUATION:
                t = "Pe";
                break;
            case FINAL_PUNCTUATION:
                t = "Pf";
                break;
            case FORMAT:
                t = "Cf";
                break;
            case INITIAL_PUNCTUATION:
                t = "Pi";
                break;
            case LETTER_NUMBER:
                t = "Nl";
                break;
            case LINE_SEPARATOR:
                t = "Zl";
                break;
            case LOWERCASE_LETTER:
                t = "Ll";
                break;
            case MATH_SYMBOL:
                t = "Sm";
                break;
            case MODIFIER_LETTER:
                t = "Lm";
                break;
            case MODIFIER_SYMBOL:
                t = "Sk";
                break;
            case NON_SPACING_MARK:
                t = "Mn";
                break;
            case OTHER_LETTER:
                t = "Lo";
                break;
            case OTHER_NUMBER:
                t = "No";
                break;
            case OTHER_PUNCTUATION:
                t = "Po";
                break;
            case OTHER_SYMBOL:
                t = "So";
                break;
            case PARAGRAPH_SEPARATOR:
                t = "Zp";
                break;
            case PRIVATE_USE:
                t = "Co";
                break;
            case SPACE_SEPARATOR:
                t = "Zs";
                break;
            case START_PUNCTUATION:
                t = "Ps";
                break;
            case SURROGATE:
                t = "Cs";
                break;
            case TITLECASE_LETTER:
                t = "Lt";
                break;
            case UNASSIGNED:
                t = "Cn";
                break;
            case UPPERCASE_LETTER:
                t = "Lu";
                break;
            default:
                t = "";
        }
        return new PyUnicode(t);
    }

    @ExposedMethod
    final PyObject UCD_bidirectional(PyObject chr) {
        int dir = UCharacter.getDirection(getCodePoint("bidirectional", chr));
        String t;
        switch(dir) {
            case ARABIC_NUMBER:
                t = "An";
                break;
            case BLOCK_SEPARATOR:
                t = "B";
                break;
            case BOUNDARY_NEUTRAL:
                t = "BN";
                break;
            case COMMON_NUMBER_SEPARATOR:
                t = "CS";
                break;
            case DIR_NON_SPACING_MARK:
                t = "NSM";
                break;
            case EUROPEAN_NUMBER:
                t = "EN";
                break;
            case EUROPEAN_NUMBER_SEPARATOR:
                t = "ES";
                break;
            case EUROPEAN_NUMBER_TERMINATOR:
                t = "ET";
                break;
            case FIRST_STRONG_ISOLATE:
                t = "FSI";
                break;
            case LEFT_TO_RIGHT:
                t = "L";
                break;
            case LEFT_TO_RIGHT_EMBEDDING:
                t = "LRE";
                break;
            case LEFT_TO_RIGHT_ISOLATE:
                t = "LRI";
                break;
            case LEFT_TO_RIGHT_OVERRIDE:
                t = "LRO";
                break;
            case OTHER_NEUTRAL:
                t = "ON";
                break;
            case POP_DIRECTIONAL_FORMAT:
                t = "PDF";
                break;
            case POP_DIRECTIONAL_ISOLATE:
                t = "PDI";
                break;
            case RIGHT_TO_LEFT:
                t = "R";
                break;
            case RIGHT_TO_LEFT_ARABIC:
                t = "AL";
                break;
            case RIGHT_TO_LEFT_EMBEDDING:
                t = "RLE";
                break;
            case RIGHT_TO_LEFT_ISOLATE:
                t = "RLI";
                break;
            case RIGHT_TO_LEFT_OVERRIDE:
                t = "RLO";
                break;
            case SEGMENT_SEPARATOR:
                t = "S";
                break;
            case WHITE_SPACE_NEUTRAL:
                t = "WS";
                break;
            default:
                t = "";
        }
        return new PyUnicode(t);
    }

    @ExposedMethod
    final PyObject UCD_combining(PyObject chr) {
        return new PyLong(UCharacter.getCombiningClass(getCodePoint("combining", chr)));
    }

    @ExposedMethod(defaults = {"null"})
    final PyObject UCD_decimal(PyObject chr, PyObject defaults) {
        int d = UCharacter.getNumericValue(getCodePoint("decimal", chr));
        if (d < 9 && d > 0) {
            return new PyLong(d);
        }
        if (defaults != null) {
            return defaults;
        }
        throw Py.ValueError("not a decimal");
    }

    @ExposedMethod(defaults = {"null"})
    final PyObject UCD_digit(PyObject chr, PyObject defaults) {
        int d = UCharacter.digit(getCodePoint("digit", chr));
        if (d != -1) {
            return new PyLong(d);
        }
        if (defaults != null) {
            return defaults;
        }
        throw Py.ValueError("not a digit");

    }

    @ExposedMethod
    final PyObject UCD_mirrored(PyObject chr) {
        return Py.newBoolean(UCharacter.isMirrored(getCodePoint("mirrored", chr)));
    }

    @ExposedMethod
    final PyObject UCD_east_asian_width(PyObject chr) {
        int w = UCharacter.getIntPropertyValue(getCodePoint("east_asian_width", chr), UProperty.EAST_ASIAN_WIDTH);
        String t;
        switch (w) {
            case AMBIGUOUS:
                t = "A";
                break;
            case COUNT:
                t = "?";
                break;
            case FULLWIDTH:
                t = "F";
                break;
            case HALFWIDTH:
                t = "H";
                break;
            case NARROW:
                t = "Na";
                break;
            case NEUTRAL:
                t = "N";
                break;
            case WIDE:
                t = "W";
                break;
            default:
                t = "";
        }
        return new PyUnicode(t);
    }

    @ExposedMethod(defaults = {"null"})
    final PyObject UCD_numeric(PyObject chr, PyObject defaults) {
        double n = UCharacter.getUnicodeNumericValue(getCodePoint("nemuric", chr));
        if (n == UCharacter.NO_NUMERIC_VALUE) {
            if (defaults != null) return defaults;
            throw Py.ValueError("not a numeric");
        }
        return new PyFloat(n);
    }

    @ExposedMethod(defaults = {"null"})
    final PyObject UCD_name(PyObject chr, PyObject defaults) {
        String name = UCharacter.getName(getCodePoint("name", chr));
        if (name != null) {
            return new PyUnicode(name);
        }
        if (defaults != null)
            return defaults;
        throw Py.ValueError("no such name");
    }

    @ExposedMethod
    final PyObject UCD_lookup(PyObject name) {
        int codepoint = UCharacter.getCharFromName(name.toString());
        if (codepoint == -1) {
            codepoint = UCharacter.getCharFromNameAlias(name.toString());
        }
        if (codepoint == -1) {
            throw Py.KeyError(String.format("undefined character name '%s'", name));
        }
        return new PyUnicode(String.valueOf(Character.toChars(codepoint)));
    }

    @ExposedMethod
    final PyObject UCD_decomposition(PyObject chr) {
        validateUnichr("decomposition", chr);
        String s;
        if (ucd_3_2) {
            s = Normalizer.decompose(chr.toString(), true, UNICODE_3_2);
        } else {
            s = Normalizer.decompose(chr.toString(), true);
        }
        return new PyUnicode(s);
    }

    @ExposedGet
    final PyObject unidata_version() {
        if (ucd_3_2) {
            return new PyUnicode("3.2.0");
        }
        return new PyUnicode(UCharacter.getUnicodeVersion().toString());
    }

    private int getCodePoint(String methodName, PyObject chr) {
        validateUnichr(methodName, chr);
        return chr.toString().codePointAt(0);
    }

    private void validateUnichr(String methodName, PyObject chr) {
        if (!(chr instanceof PyUnicode) || chr.__len__() != 1) {
            throw Py.TypeError(String.format("%s() argument must be a unicode character, not %s", methodName, chr.getType().getName()));
        }
    }
}
