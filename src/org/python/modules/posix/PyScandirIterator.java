package org.python.modules.posix;

import org.python.core.Py;
import org.python.core.PyIterator;
import org.python.core.PyObject;
import org.python.expose.ExposedType;

import java.nio.file.Path;
import java.util.Iterator;

/**
 * Created by isaiah on 7/16/16.
 */
@ExposedType(name = "posix.ScandirIterator")
public class PyScandirIterator extends PyIterator {
    Iterator<Path> iter;
    public PyScandirIterator(Iterator<Path> iter) {
        this.iter = iter;
    }

    @Override
    public PyObject __next__() {
        if (!iter.hasNext()) throw Py.StopIteration();
        return new PyDirEntry(iter.next());
    }
}
