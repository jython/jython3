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
 ther compatibility work.
 */


package org.python.modules.sre;

import java.util.*;

import com.google.common.base.Joiner;
import org.python.core.*;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

import static org.python.modules.sre.SRE_STATE.*;

@ExposedType(name = "_sre.SRE_Pattern", doc = BuiltinDocs.SRE_Pattern_doc)
public class PatternObject extends PyObject implements Traverseproc {
    long[] code; /* link to the code string object */
    @ExposedGet
    public PyObject pattern; /* link to the pattern source (or None) */
    @ExposedGet
    public int groups;
    @ExposedGet
    public PyObject groupindex;
    @ExposedGet
    public int flags;
    PyObject indexgroup;
    public int codesize;

    private boolean isBytes;


    public PatternObject(PyObject pattern, int flags, long[] code,
            int groups, PyObject groupindex, PyObject indexgroup) {

        if (pattern != null)
            this.pattern = pattern;
        this.isBytes = !(pattern instanceof PyUnicode);
        this.flags   = flags;
        this.code    = code;
        this.codesize = code.length;
        this.groups  = groups;
        this.groupindex = new PyDictProxy(groupindex);
        this.indexgroup = indexgroup;
        if (!isBytes && (flags & (SRE_FLAG_ASCII)) != SRE_FLAG_ASCII) {
            this.flags |= SRE_FLAG_UNICODE;
        }
    }

    @ExposedMethod(doc = BuiltinDocs.SRE_Pattern_match_doc)
    public PyObject SRE_Pattern_match(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("match", args, kws,
                                     "pattern", "pos", "endpos");
        PyObject string = ap.getPyObject(0);
        int start = ap.getInt(1, 0);
        int end = ap.getInt(2, string.__len__());
        SRE_STATE state = new SRE_STATE(string, start, end, flags);

        state.ptr = state.start;
        int status = state.SRE_MATCH(code, 0, 1);

        PyObject ret = _pattern_new_match(state, string, status);
        if (ret == null) return Py.None;
        return ret;
    }

    @ExposedMethod(doc = BuiltinDocs.SRE_Pattern_fullmatch_doc)
    public PyObject SRE_Pattern_fullmatch(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("match", args, kws,
                "pattern", "pos", "endpos");
        PyObject string = ap.getPyObject(0);
        int start = ap.getInt(1, 0);
        int end = ap.getInt(2, string.__len__());
        SRE_STATE state = new SRE_STATE(string, start, end, flags);

        state.ptr = state.start;
        int status = state.SRE_MATCH(code, 0, 1, true);
//        if (status > 0) {
//            if (state.endpos - state.pos < string.__len__()) {
//                return Py.None;
//            }
//        }

        PyObject ret = _pattern_new_match(state, string, status);
        if (ret == null) return Py.None;
        return ret;
    }

    @ExposedMethod(doc = BuiltinDocs.SRE_Pattern_search_doc)
    public PyObject SRE_Pattern_search(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("search", args, kws,
                                     "pattern", "pos", "endpos");
        PyObject string = ap.getPyObject(0);
        int start = ap.getInt(1, 0);
        int end = ap.getInt(2, string.__len__());

        SRE_STATE state = new SRE_STATE(string, start, end, flags);

        int status = state.SRE_SEARCH(code, 0);

        PyObject ret = _pattern_new_match(state, string, status);
        if (ret == null) return Py.None;
        return ret;
    }

    @ExposedMethod(doc = BuiltinDocs.SRE_Pattern_sub_doc)
    public PyObject SRE_Pattern_sub(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("sub", args, kws,
                                     "repl", "string", "count");
        PyObject template = ap.getPyObject(0);
        int count = ap.getInt(2, 0);

        return subx(template, ap.getPyObject(1), count, false);
    }

    @ExposedMethod(doc = BuiltinDocs.SRE_Pattern_subn_doc)
    public PyObject SRE_Pattern_subn(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("subn", args, kws,
                                     "repl", "string", "count");
        PyObject template = ap.getPyObject(0);
        int count = ap.getInt(2, 0);

        return subx(template, ap.getPyObject(1), count, true);
    }


    private PyObject subx(PyObject template, PyObject string, int count, boolean subn) {
        PyObject filter = null;
        boolean filter_is_callable = false;
        if (template.isCallable()) {
            filter = template;
            filter_is_callable = true;
        } else {
            boolean literal = false;
            if (template instanceof PyUnicode) {
                literal = template.toString().indexOf('\\') < 0;
            }
            if (literal) {
                filter = template;
                filter_is_callable = false;
            } else {
                filter = call("re", "_subx", new PyObject[] {
                    this, template});
                filter_is_callable = filter.isCallable();
            }
        }

        SRE_STATE state = new SRE_STATE(string, 0, Integer.MAX_VALUE, flags);

        PyList list = new PyList();

        int n = 0;
        int i = 0;

        while (count == 0 || n < count) {
            state.state_reset();
            state.ptr = state.start;
            int status = state.SRE_SEARCH(code, 0);
            if (status <= 0) {
                if (status == 0)
                    break;
                _error(status);
            }
            int b = state.start;
            int e = state.ptr;

            if (i < b) {
                /* get segment before this match */
                list.append(getslice(string, i, b));
            }
            if (! (i == b && i == e && n > 0)) {
                PyObject item;
                if (filter_is_callable) {
                    /* pass match object through filter */
                    PyObject match = _pattern_new_match(state, string, 1);
                    if (match == null) {
                        match = Py.None;
                    }
                    item = filter.__call__(match);
                } else {
                    item = filter;
                }

                if (item != Py.None) {
                    list.append(item);
                }
                i = e;
                n++;
            }

            /* move on */
            if (state.ptr == state.start)
                state.start = state.ptr + 1;
            else
                state.start = state.ptr;
        }
        if (i < state.endpos) {
            list.append(getslice(string, i, state.endpos));
        }

        PyObject joiner;
        if (string instanceof PyUnicode) {
            joiner = Py.EmptyUnicode;
        } else {
            joiner = Py.EmptyByte;
        }
        PyObject outstring = join_list(list, joiner);
        if (subn) {
            return new PyTuple(outstring, Py.newLong(n));
        }
        return outstring;
    }

    private PyObject join_list(PyList list, PyObject string) {
        if (list.size() == 0) {
            return string;
        }
        return string.__getattr__("join").__call__(list);
    }

    @ExposedMethod(doc = BuiltinDocs.SRE_Pattern_split_doc)
    public PyObject SRE_Pattern_split(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("split", args, kws,
                                     "source", "maxsplit");
        PyObject string = ap.getPyObject(0);
        int maxsplit = ap.getInt(1, 0);

        if (code[0] != SRE_OP_INFO || code[3] == 0) {
            if (code[0] == SRE_OP_INFO && code[4] == 0) {
                throw Py.ValueError("split() requires a non-empty pattern match.");
            }
            Py.FutureWarning("split() requires a non-empty pattern match.");
        }
        SRE_STATE state = new SRE_STATE(string, 0, Integer.MAX_VALUE, flags);

        PyList list = new PyList();

        int n = 0;
        int last = state.start;
        while (maxsplit == 0 || n < maxsplit) {
            state.state_reset();
            state.ptr = state.start;
            int status = state.SRE_SEARCH(code, 0);
            if (status <= 0) {
                if (status == 0)
                    break;
                _error(status);
            }
            if (state.start == state.ptr) {
                if (last == state.end)
                    break;
                /* skip one character */
                state.start = state.ptr + 1;
                continue;
            }

            /* get segment before this match */
            PyObject item = getslice(string, last, state.start);
            list.append(item);

            for (int i = 0; i < groups; i++) {
                PyObject s = state.getslice(i+1, string, false);
                list.append(s);
            }
            n += 1;
            last = state.start = state.ptr;
        }

        list.append(getslice(string, last, state.endpos));
        return list;
    }

    protected static final PyObject getslice(PyObject string, int start, int end) {
        if (string instanceof PyUnicode || string instanceof PyBytes) {
            return ((PySequence) string).getslice(start, end);
        } else {
            try(PyBuffer buf = ((BufferProtocol) string).getBuffer(PyBUF.SIMPLE)) {
                byte[] bytes = new byte[end - start];
                buf.copyTo(start, bytes, 0, bytes.length);
                return new PyBytes(bytes);
            }
        }
    }

    private PyObject call(String module, String function, PyObject[] args) {
        PyObject sre = imp.importName(module, true);
        return sre.invoke(function, args);
    }

    @ExposedMethod(doc = BuiltinDocs.SRE_Pattern_findall_doc)
    public PyObject SRE_Pattern_findall(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("findall", args, kws,
                                     "source", "pos", "endpos");
        PyObject string = ap.getPyObject(0);
        int start = ap.getInt(1, 0);
        int end = ap.getInt(2, Integer.MAX_VALUE);

        SRE_STATE state = new SRE_STATE(string, start, end, flags);

        final List<PyObject> list = new ArrayList<PyObject>();

        while (state.start <= state.end) {
            state.state_reset();
            state.ptr = state.start;
            int status = state.SRE_SEARCH(code, 0);
            if (status > 0) {
                PyObject item;

                /* don't bother to build a match object */
                switch (groups) {
                case 0:
                    item = getslice(string, state.start, state.ptr);
                    break;
                case 1:
                    item = state.getslice(1, string, true);
                    break;
                default:
                    PyObject[] t = new PyObject[groups];
                    for (int i = 0; i < groups; i++)
                        t[i] = state.getslice(i+1, string, true);
                    item = new PyTuple(t);
                    break;
                }

                list.add(item);

                if (state.ptr == state.start)
                    state.start = state.ptr + 1;
                else
                    state.start = state.ptr;
            } else {

                if (status == 0)
                    break;

                _error(status);
            }
        }
        return new PyList(list);
    }

    @ExposedMethod(doc = BuiltinDocs.SRE_Pattern_finditer_doc)
    public PyObject SRE_Pattern_finditer(PyObject[] args, String[] kws) {
        ScannerObject scanner = SRE_Pattern_scanner(args, kws);
        PyObject search = scanner.__findattr__("search");
        return new PyCallIter(search, Py.None);
    }

    @ExposedMethod(doc = BuiltinDocs.SRE_Pattern_scanner_doc)
    public ScannerObject SRE_Pattern_scanner(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("scanner", args, kws,
                                     "pattern", "pos", "endpos");
        PyObject string = ap.getPyObject(0);

        ScannerObject self = new ScannerObject();
        self.state = new SRE_STATE(string,
                                   ap.getInt(1, 0),
                                   ap.getInt(2, Integer.MAX_VALUE),
                                   flags);
        self.pattern = this;
        self.string = string;
        return self;
    }



    private void _error(int status) {
        if (status == SRE_STATE.SRE_ERROR_RECURSION_LIMIT)
            throw Py.RuntimeError("maximum recursion limit exceeded");

        throw Py.RuntimeError("internal error in regular expression engine");
    }


    MatchObject _pattern_new_match(SRE_STATE state, PyObject string,
                                   int status)
    {
        /* create match object (from state object) */

        //System.out.println("status = " +  status + " " + string);

        if (status > 0) {
            /* create match object (with room for extra group marks) */
            MatchObject match = new MatchObject();
            match.pattern = this;
            match.string = string;
            match.regs = null;
            match.groups = groups+1;
            /* group zero */
            int base = state.beginning;

            match.mark = new int[match.groups*2];
            match.mark[0] = state.start - base;
            match.mark[1] = state.ptr - base;

            /* fill in the rest of the groups */
            int i, j;
            for (i = j = 0; i < groups; i++, j+=2) {
                if (j+1 <= state.lastmark && state.mark[j] != -1 &&
                                                    state.mark[j+1] != -1) {
                    match.mark[j+2] = state.mark[j] - base;
                    match.mark[j+3] = state.mark[j+1] - base;
                } else
                    match.mark[j+2] = match.mark[j+3] = -1;
            }
            match.pos = state.pos;
            match.endpos = state.endpos;
            match.lastindex = state.lastindex;

            return match;
        } else if (status == 0) {
            return null;
        }

        _error(status);
        return null;
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
        if (groupindex != null) {
            retVal = visit.visit(groupindex, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        return indexgroup != null ? visit.visit(indexgroup, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (ob == pattern || ob == groupindex || ob == indexgroup);
    }

    @Override
    public PyUnicode __repr__() {
        return SRE_Pattern___repr__();
    }

    @ExposedMethod
    public PyUnicode SRE_Pattern___repr__() {
        int flags = this.flags;
//        if (!isBytes && (flags & (SRE_FLAG_LOCALE|SRE_FLAG_UNICODE| SRE_FLAG_ASCII)) == SRE_FLAG_UNICODE) {
//            flags &= ~SRE_FLAG_UNICODE;
//        }
        List<String> flagItems = new ArrayList<>();
        for (FlagName flagName : flagNames) {
            if ((flags & flagName.value) > 0) {
                flagItems.add(flagName.name);
                flags &= ~flagName.value;
            }
        }

        if (flags > 0) {
            flagItems.add(String.format("0x%x", flags));
        }
        if (flagItems.size() > 0) {
            String flagsResult = Joiner.on("|").join(flagItems);
            return new PyUnicode(String.format("re.compile(%.200s, %s)", pattern.__repr__(), flagsResult));
        }
        return new PyUnicode(String.format("re.compile(%.200s)", pattern.__repr__()));
    }

    private static FlagName[] flagNames = {
            new FlagName("re.TEMPLATE", SRE_FLAG_TEMPLATE),
            new FlagName("re.IGNORECASE", SRE_FLAG_IGNORECASE),
            new FlagName("re.LOCALE", SRE_FLAG_LOCALE),
            new FlagName("re.MULTILINE", SRE_FLAG_MULTILINE),
            new FlagName("re.DOTALL", SRE_FLAG_DOTALL),
            new FlagName("re.UNICODE", SRE_FLAG_UNICODE),
            new FlagName("re.VERBOSE", SRE_FLAG_VERBOSE),
            new FlagName("re.DEBUG", SRE_FLAG_DEBUG),
            new FlagName("re.ASCII", SRE_FLAG_ASCII),
    };

    static class FlagName {
        String name;
        int value;

        FlagName(String n, int v) {
            name = n;
            value = v;
        }
    }
}
