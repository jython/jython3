/*
 * Copyright 2000 Finn Bock
 *
 * This program contains material copyrighted by:
 * Copyright (c) 1997-2000 by Secret Labs AB.  All rights reserved.
 *
 * This version of the SRE library can be redistributed under CNRI's
 * Python 1.6 license.  For any other use, please contact Secret Labs
 * AB (info@pythonware.com).
 *
 * Portions of this engine have been developed in cooperation with
 * CNRI.  Hewlett-Packard provided funding for 1.6 integration and
 * other compatibility work.
 */

package org.python.modules.sre;

import org.python.core.*;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "_sre.SRE_Scanner", doc = BuiltinDocs.SRE_Scanner_doc)
public class ScannerObject extends PyObject implements Traverseproc {
    @ExposedGet
    public PatternObject pattern;
    PyObject string;
    SRE_STATE state;

    @ExposedMethod(doc = BuiltinDocs.SRE_Scanner_match_doc)
    public PyObject SRE_Scanner_match() {
        state.state_reset();
        state.ptr = state.start;

        int status = state.SRE_MATCH(pattern.code, 0, 1);
        if (status <= 0) {
            return Py.None;
        }
        MatchObject match = pattern._pattern_new_match(state, string, status);

        if (status == 0 || state.ptr == state.start)
            state.start = state.ptr + 1;
        else
            state.start = state.ptr;

        return match;
    }


    @ExposedMethod(doc = BuiltinDocs.SRE_Scanner_search_doc)
    public MatchObject SRE_Scanner_search() {
        state.state_reset();
        state.ptr = state.start;

        int status = state.SRE_SEARCH(pattern.code, 0);
        MatchObject match = pattern._pattern_new_match(state, string, status);

        if (status == 0 || state.ptr == state.start)
            state.start = state.ptr + 1;
        else
            state.start = state.ptr;

        return match;
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        if (pattern != null) {
            int retVal = visit.visit(pattern, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        return string != null ? visit.visit(string, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (ob == pattern || ob == string);
    }
}
