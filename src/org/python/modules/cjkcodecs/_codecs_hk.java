package org.python.modules.cjkcodecs;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;

import java.nio.charset.Charset;

@ExposedModule
public class _codecs_hk {
    private static Charset BIG5HKSCS = Charset.forName("big5hkscs");

    @ExposedFunction
    public static PyObject getcodec(String name) {
        switch(name) {
            case "big5hkscs":
                return new PyMultibyteCodec(BIG5HKSCS);
            default:
                throw Py.LookupError("no such codec is supported");
        }
    }
}
