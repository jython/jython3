package org.python.modules;

import jnr.constants.platform.Fcntl;
import jnr.posix.POSIX;
import org.python.core.ArgParser;
import org.python.core.ClassDictInit;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyUnicode;
import org.python.modules.posix.PosixModule;

/**
 * Created by isaiah on 6/17/16.
 */
public class fcntl implements ClassDictInit {
    private static POSIX posix = PosixModule.getPOSIX();

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyUnicode("fcntl"));
        for (Fcntl val : Fcntl.values()) {
            dict.__setitem__(val.name(), new PyLong(val.longValue()));
        }

        // hide from Python
        dict.__setitem__("classDictInit", null);
    }

    public static int fcntl(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("fcntl", args, keywords, "fd", "cmd", "arg");
        PyObject fileDescriptor = ap.getPyObject(0);
        int cmd = ap.getInt(1);
        int fd = PosixModule.getFD(fileDescriptor).getIntFD();
        return posix.fcntl(fd, Fcntl.valueOf(cmd));
    }
}
