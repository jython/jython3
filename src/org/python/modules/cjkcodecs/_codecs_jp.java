package org.python.modules.cjkcodecs;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;

import java.nio.charset.Charset;

/**
 * Created by isaiah on 8/15/16.
 */
@ExposedModule
public class _codecs_jp {
    private static Charset EUC_JP = Charset.forName("euc_jp");
    private static Charset SHIFT_JIS = Charset.forName("shift_jis");
    private static Charset MS932 = Charset.forName("MS932");

    @ExposedFunction
    public static PyObject getcodec(String name) {
        switch(name) {
            case "euc_jp":
                return new PyMultibyteCodec(EUC_JP);
            case "shift_jis":
                return new PyMultibyteCodec(SHIFT_JIS);
            case "ms932":
                return new PyMultibyteCodec(MS932);
            default:
                throw Py.LookupError("no such codec is supported");
        }
    }
}
