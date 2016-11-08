/*
 * Copyright 1998 Finn Bock.
 *
 * This program contains material copyrighted by:
 * Copyright (c) 1991-1995 by Stichting Mathematisch Centrum, Amsterdam,
 * The Netherlands.
 */

// cStringIO with StringBuilder semantics - don't use without using external
// synchronization. Java does provide other alternatives.

package org.python.modules;

import org.python.core.PyArray;
import org.python.core.PyType;
import org.python.modules._io.PyStringIO;

/**
 * This module implements a file-like class, StringIO, that reads and
 * writes a string buffer (also known as memory files).
 * See the description on file objects for operations.
 * @author Finn Bock, bckfnn@pipmail.dknet.dk
 * @version cStringIO.java,v 1.10 1999/05/20 18:03:20 fb Exp
 */
public class cStringIO {

    public static PyType InputType = PyType.fromClass(PyStringIO.class);
    public static PyType OutputType = PyType.fromClass(PyStringIO.class);

    public static PyStringIO StringIO() {
        return new PyStringIO();
    }

    /**
     * Create a StringIO object, initialized by the value.
     * @param buffer       The initial value.
     * @return          a new StringIO object.
     */
    public static PyStringIO StringIO(String buffer) {
        return new PyStringIO(buffer);
    }

    /**
     * Create a StringIO object, initialized by an array's byte stream.
     * @param array       The initial value, from an array.
     * @return          a new StringIO object.
     */
    public static PyStringIO StringIO(PyArray array) {
        return new PyStringIO(array);
    }


    private static String[] strings = new String[256];
    static String getString(char ch) {
        if (ch > 255) {
            return new String(new char[] { ch });
        }

      String s = strings[ch];

      if (s == null) {
          s = new String(new char[] { ch });
          strings[ch] = s;
      }
      return s;
   }

}
