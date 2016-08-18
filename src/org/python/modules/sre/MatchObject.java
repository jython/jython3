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

import org.python.core.ArgParser;
import org.python.core.BuiltinDocs;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PySequence;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.core.Traverseproc;
import org.python.core.Visitproc;
import org.python.core.imp;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "_sre.SRE_Match", doc = BuiltinDocs.SRE_Match_doc)
public class MatchObject extends PyObject implements Traverseproc {
    public static final PyType TYPE = PyType.fromClass(MatchObject.class);

    @ExposedGet
    public PyObject string; /* link to the target string */
    public PyObject regs; /* cached list of matching spans */
    @ExposedGet
    public PatternObject pattern; /* link to the regex (pattern) object */
    @ExposedGet
    public int pos;
    @ExposedGet
    public int endpos; /* current target slice */
    public int lastindex; /* last index marker seen by the engine (-1 if none) */
    int groups; /* number of groups (start/end marks) */
    int[] mark;

    public MatchObject() {
        super(TYPE);
    }

    @ExposedMethod(doc = BuiltinDocs.SRE_Match_expand_doc)
    public PyObject SRE_Match_expand(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("expand", args, keywords, "template");
        PyObject template = ap.getPyObject(0);
        PyObject mod = imp.importName("re", true);
        PyObject func = mod.__getattr__("_expand");
        return func.__call__(new PyObject[] {pattern, this, template});
    }

    @ExposedMethod(doc = BuiltinDocs.SRE_Match_group_doc)
    public PyObject SRE_Match_group(PyObject[] args, String[] keywords) {
        switch (args.length) {
        case 0:
            return getslice(Py.Zero, Py.None);
        case 1:
            return getslice(args[0], Py.None);
        default:
            PyObject[] result = new PyObject[args.length];
            for (int i = 0; i < args.length; i++)
                result[i] = getslice(args[i], Py.None);
            return new PyTuple(result);
        }
    }

    @ExposedMethod(doc = BuiltinDocs.SRE_Match_groups_doc)
    public PyObject SRE_Match_groups(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("groups", args, kws, "default");
        PyObject def = ap.getPyObject(0, Py.None);

        PyObject[] result = new PyObject[groups-1];
        for (int i = 1; i < groups; i++) {
            result[i-1] = getslice_by_index(i, def);
        }
        return new PyTuple(result);
    }

    @ExposedMethod(doc = BuiltinDocs.SRE_Match_groupdict_doc)
    public PyObject SRE_Match_groupdict(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("groupdict", args, kws, "default");
        PyObject def = ap.getPyObject(0, Py.None);

        PyObject result = new PyDictionary();

        if (pattern.groupindex == null)
            return result;

        PyObject keys = pattern.groupindex.invoke("keys");

        PyObject iter = keys.__iter__();
        for (PyObject key = iter.__next__(); key != null; ) {
            PyObject item = getslice(key, def);
            result.__setitem__(key, item);
            key = iter.__next__();
        }
        return result;
    }

    @ExposedMethod(defaults = {"0"}, doc = BuiltinDocs.SRE_Match_start_doc)
    public PyObject SRE_Match_start(int index) {
        if (index < 0 || index >= groups)
            throw Py.IndexError("no such group");

        return Py.newInteger(mark[index*2]);
    }

    @ExposedMethod(defaults = {"0"}, doc = BuiltinDocs.SRE_Match_end_doc)
    public PyObject SRE_Match_end(int index) {
        if (index < 0 || index >= groups)
            throw Py.IndexError("no such group");

        return Py.newInteger(mark[index*2+1]);
    }

    @ExposedMethod(defaults = {"0"}, doc = BuiltinDocs.SRE_Match_span_doc)
    public PyTuple SRE_Match_span(int index) {
        if (index < 0 || index >= groups)
            throw Py.IndexError("no such group");

        int start = mark[index*2];
        int end = mark[index*2+1];

        return _pair(start, end);
    }

    @ExposedGet(name = "regs")
    public PyObject regs() {

        PyObject[] regs = new PyObject[groups];

        for (int index = 0; index < groups; index++) {
            regs[index] = _pair(mark[index*2], mark[index*2+1]);
        }

        return new PyTuple(regs);
    }


    PyTuple _pair(int i1, int i2) {
        return new PyTuple(Py.newInteger(i1), Py.newInteger(i2));
    }

    private PyObject getslice(PyObject index, PyObject def) {
        return getslice_by_index(getindex(index), def);
    }

    private int getindex(PyObject index) {
        if (index instanceof PyLong)
            return ((PyLong) index).getValue().intValue();

        int i = -1;

        if (pattern.groupindex != null) {
            index = pattern.groupindex.__finditem__(index);
            if (index != null) {
                if (index instanceof PyInteger)
                    return ((PyInteger) index).getValue();
                if (index instanceof PyLong)
                    return ((PyLong) index).getValue().intValue();
            }
        }
        return i;
    }

    private PyObject getslice_by_index(int index, PyObject def) {
        if (index < 0 || index >= groups)
            throw Py.IndexError("no such group");

        index *= 2;
        int start = mark[index];
        int end = mark[index+1];

        //System.out.println("group:" + index + " " + start + " " +
        //                   end + " l:" + string.length());

        if (string == null || start < 0)
            return def;
        return PatternObject.getslice(string, start, end);
    }

    @ExposedGet(name = "lastindex")
    public PyObject getLastIndex() {
        return lastindex == -1 ? Py.None : Py.newLong(lastindex);
    }

    public PyObject __findattr_ex__(String key) {
        //System.out.println("__findattr__:" + key);
        if (key == "flags")
            return Py.newInteger(pattern.flags);
        if (key == "groupindex")
            return pattern.groupindex;
        if (key == "re")
            return pattern;
        if (key == "lastgroup"){
            if(pattern.indexgroup != null && lastindex >= 0)
                return pattern.indexgroup.__getitem__(lastindex);
            return Py.None;
        }
        return super.__findattr_ex__(key);
    }

    @Override
    public String toString() {
        return String.format("<%s object; span=%s, match='%s'>", getType().getName(), SRE_Match_span(0),
                SRE_Match_group(Py.EmptyObjects, Py.NoKeywords));
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retVal;
        if (pattern != null) {
            retVal = visit.visit(pattern, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (string != null) {
            retVal = visit.visit(string, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        return regs != null ? visit.visit(regs, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (ob == pattern || ob == string || ob == regs);
    }
}
