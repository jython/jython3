// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.util.regex.Pattern;

/**
 * A convenience class for creating Syntax errors. Note that the
 * syntax error is still taken from Py.SyntaxError.
 * <p>
 * Generally subclassing from PyException is not the right way
 * of creating new exception classes.
 */

public class PySyntaxError extends PyException {
    private static Pattern INVALID_SYNTAX = Pattern.compile("no viable alternative at input.*");

    int lineno;
    int column;
    String text;
    String filename;


    public PySyntaxError(String s, int line, int column, String text,
                         String filename)
    {
        super(Py.SyntaxError);
        //XXX: null text causes Java error, though I bet I'm not supposed to
        //     get null text.
        if (text == null) {
            text = "";
        }
        if (INVALID_SYNTAX.matcher(s).matches()) {
            s = "invalid syntax";
        }
        PyObject[] tmp = new PyObject[] {
            new PyUnicode(filename), new PyLong(line),
            new PyLong(column), new PyUnicode(text)
        };

        this.value = new PyTuple(new PyUnicode(s), new PyTuple(tmp));

        this.lineno = line;
        this.column = column;
        this.text = text;
        this.filename = filename;
    }
}
