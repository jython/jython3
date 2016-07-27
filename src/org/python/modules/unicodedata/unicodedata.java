package org.python.modules.unicodedata;

import org.python.core.PyObject;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;
import org.python.expose.ModuleInit;

import static org.python.modules.unicodedata.UCD.UNICODEDATA;

@ExposedModule
public class unicodedata {
    @ModuleInit
    public static void clinic(PyObject dict) {
        dict.__setitem__("ucd_3_2_0", UCD.UCD_3_2);
        dict.__setitem__("unidata_version",UNICODEDATA.unidata_version());
    }

    @ExposedFunction
    public static PyObject normalize(PyObject form, PyObject str) {
        return UNICODEDATA.UCD_normalize(form, str);
    }

    @ExposedFunction
    public static PyObject category(PyObject chr) {
        return UNICODEDATA.UCD_category(chr);
    }

    @ExposedFunction
    public static PyObject bidirectional(PyObject chr) {
        return UNICODEDATA.UCD_bidirectional(chr);
    }

    @ExposedFunction
    public static PyObject combining(PyObject chr) {
        return UNICODEDATA.UCD_combining(chr);
    }

    @ExposedFunction(defaults = {"null"})
    public static PyObject decimal(PyObject chr, PyObject defaults) {
        return UNICODEDATA.UCD_decimal(chr, defaults);
    }

    @ExposedFunction(defaults = {"null"})
    public static PyObject digit(PyObject chr, PyObject defaults) {
        return UNICODEDATA.UCD_digit(chr, defaults);
    }

    @ExposedFunction
    public static PyObject mirrored(PyObject chr) {
        return UNICODEDATA.UCD_mirrored(chr);
    }

    @ExposedFunction
    public static PyObject east_asian_width(PyObject chr) {
        return UNICODEDATA.UCD_east_asian_width(chr);
    }

    @ExposedFunction(defaults = {"null"})
    public static PyObject numeric(PyObject chr, PyObject defaults) {
        return UNICODEDATA.UCD_numeric(chr, defaults);
    }

    @ExposedFunction(defaults = {"null"})
    public static PyObject name(PyObject chr, PyObject defaults) {
        return UNICODEDATA.UCD_name(chr, defaults);
    }

    @ExposedFunction
    public static PyObject lookup(PyObject name) {
        return UNICODEDATA.UCD_lookup(name);
    }

    @ExposedFunction
    public static PyObject decomposition(PyObject chr) {
        return UNICODEDATA.UCD_decomposition(chr);
    }
}
