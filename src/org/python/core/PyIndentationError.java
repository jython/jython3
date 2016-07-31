// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * A convenience class for creating Indentation errors. Note that the
 * syntax error is still taken from Py.IndentationError.
 * <p>
 * Generally subclassing from PyException is not the right way
 * of creating new exception classes.
 */

public class PyIndentationError extends PyException {
    int lineno, column;
    String text;
    String filename;

    public PyIndentationError(String s, int line, int column, String text,
                         String filename)
    {
        super(Py.IndentationError);
        PyObject[] tmp = new PyObject[] {
            new PyBytes(filename), new PyInteger(line),
            new PyInteger(column), new PyBytes(text)
        };

        this.value = new PyTuple(new PyBytes(s), new PyTuple(tmp));

        this.lineno = line;
        this.column = column;
        this.text = text;
        this.filename = filename;
    }
}
