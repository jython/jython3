package org.python.modules.cjkcodecs;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;

import java.nio.charset.Charset;

@ExposedModule
public class _codecs_kr {
    private static Charset CP949 = Charset.forName("cp949");
    private static Charset JOHAB = Charset.forName("johab");
    private static Charset EUC_KR = Charset.forName("euc_kr");

    @ExposedFunction
    public static PyObject getcodec(String name) {
        switch(name) {
            case "cp949":
                return new PyMultibyteCodec(CP949);
            case "johab":
                return new PyMultibyteCodec(JOHAB);
            case "euc_kr":
                return new PyMultibyteCodec(EUC_KR);
            default:
                throw Py.LookupError("no such codec is supported");
        }
    }
}
