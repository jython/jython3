package org.python.modules.bz2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyBytes;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;
import org.python.expose.ModuleInit;

/**
 * Python _bz2 module
 */
@ExposedModule
public class bz2 {
    @ModuleInit
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("BZ2File", PyBZ2File.TYPE);
        dict.__setitem__("BZ2Compressor", PyBZ2Compressor.TYPE);
        dict.__setitem__("BZ2Decompressor", PyBZ2Decompressor.TYPE);
    }

    @ExposedFunction
    public static PyObject compress(PyObject[] args, String[] keywords) {
        PyBytes returnData = null;

        try {
            ByteArrayOutputStream compressedArray = new ByteArrayOutputStream();
            BZip2CompressorOutputStream bzbuf = new BZip2CompressorOutputStream(
                    compressedArray);

            bzbuf.write(Py.unwrapBuffer(args[0]));
            bzbuf.finish();
            bzbuf.close();

            returnData = new PyBytes(compressedArray.toString("iso-8859-1"));
            compressedArray.close();
        } catch (IOException e) {
            throw Py.IOError(e.getMessage());
        }

        return returnData;
    }

    @ExposedFunction
    public static PyObject decompress(PyObject data) {
        if (data.toString().equals("")) {
            return Py.EmptyByte;
        }
        try {
            ByteArrayInputStream inputArray = new ByteArrayInputStream(Py.unwrapBuffer(data));
            BZip2CompressorInputStream bzbuf = new BZip2CompressorInputStream(
                    inputArray);

            ByteArrayOutputStream outputArray = new ByteArrayOutputStream();

            final byte[] buffer = new byte[8192];
            int n = 0;
            while ((n = bzbuf.read(buffer)) != -1) {
                outputArray.write(buffer, 0, n);
            }


            outputArray.close();
            bzbuf.close();
            inputArray.close();
            return new PyBytes(outputArray.toString("iso-8859-1"));
        } catch (IOException e) {
            throw Py.ValueError(e.getMessage());
        }
    }
}
