// Copyright (c) Corporation for National Research Initiatives
package org.python.modules;

import org.python.modules.posix.PosixModule;

// This is sort of analogous to CPython's Modules/Setup file.  Use this to
// specify additional builtin modules.

public class Setup {
    // Each element of this array is a string naming a builtin module to
    // add to the system.  The string has the following allowable forms:
    //
    // name
    //     The module name is `name' and the class name is
    //     org.python.modules.name
    //
    // name:class
    //     The module name is `name' and the class name is `class' where
    //     class must be a fully qualified Java class name
    //
    // name:null
    //     The module `name' is removed from the list of builtin modules
    //
    // That isn't very useful here, but you can add additional builtin
    // modules by editing the Jython registry file.  See the property
    // python.modules.builtin for details.

    public static String[] builtinModules = {
            "_bytecodetools",
//            "_codecs",
            "_hashlib",
            "_io:org.python.modules._io._io",
            "_json:org.python.modules._json._json",
            "_jythonlib:org.python.modules._jythonlib._jythonlib",
            "_random:org.python.modules.random.RandomModule",
//            "_sre",
//            "_string",
            "_systemrestart",
//            "_types",
//            "_weakref:org.python.modules._weakref.WeakrefModule",
            "_pickle",
            "cmath",
            "errno",
            "fcntl",
            "gc",
            "jarray",
            "jffi:org.python.modules.jffi.jffi",
            "marshal",
            "math",
            "operator",
            "struct",
            "synchronize",
//            "_thread:org.python.modules.thread.thread",
//            PosixModule.getOSName() + ":org.python.modules.posix.PosixModule"
    };

    public static String[] newbuiltinModules = {
            "_ast:org.python.antlr.ast.AstModule",
            "_bz2:org.python.modules.bz2.bz2",
            "_codecs",
            "_codecs_cn:org.python.modules.cjkcodecs._codecs_cn",
            "_codecs_tw:org.python.modules.cjkcodecs._codecs_tw",
            "_codecs_hk:org.python.modules.cjkcodecs._codecs_hk",
            "_codecs_kr:org.python.modules.cjkcodecs._codecs_kr",
            "_codecs_jp:org.python.modules.cjkcodecs._codecs_jp",
            "_collections:org.python.modules._collections.Collections",
            "_csv:org.python.modules._csv._csv",
            "_datetime:org.python.modules._datetime.DatetimeModule",
            "_functools:org.python.modules._functools._functools",
            "_imp:org.python.modules._imp",
            "_multibytecodec:org.python.modules.cjkcodecs._multibytecodec",
            "_multiprocessing:org.python.modules._multiprocessing._multiprocessing",
            "_posixsubprocess",
            "_sre",
            "_string",
            "_types",
            "_thread:org.python.modules.thread._thread",
            "_warnings",
            "_weakref:org.python.modules._weakref.WeakrefModule",
            "array:org.python.modules.ArrayModule",
            "binascii",
            "faulthandler:org.python.modules.FaultHandler",
            "itertools:org.python.modules.itertools.itertools",
            "posix:org.python.modules.posix.PosixModule",
            "subprocess:org.python.modules.subprocess.SubprocessModule",
            "sys:org.python.modules.sys.SysModule",
            "time:org.python.modules.time.TimeModule",
            "unicodedata:org.python.modules.unicodedata.unicodedata",
            "zipimport:org.python.modules.zipimport.ZipImportModule",
            "zlib:org.python.modules.zlib.ZlibModule",
    };
}
