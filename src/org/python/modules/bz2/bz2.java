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

/**
 * Python bz2 module
 * 
 */
public class bz2 implements ClassDictInit {

    public static final PyBytes __doc__ = new PyBytes("bz2 module");

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("BZ2File", PyBZ2File.TYPE);
        dict.__setitem__("BZ2Compressor", PyBZ2Compressor.TYPE);
        dict.__setitem__("BZ2Decompressor", PyBZ2Decompressor.TYPE);

        dict.__setitem__("classDictInit", null);
    }

    public static PyBytes compress(PyBytes data) {
        return compress(data, 9);
    }

    public static PyBytes compress(PyBytes data, int compresslevel) {
        PyBytes returnData = null;

        try {
            ByteArrayOutputStream compressedArray = new ByteArrayOutputStream();
            BZip2CompressorOutputStream bzbuf = new BZip2CompressorOutputStream(
                    compressedArray);

            bzbuf.write(data.toBytes());
            bzbuf.finish();
            bzbuf.close();

            returnData = new PyBytes(compressedArray.toString("iso-8859-1"));
            compressedArray.close();
        } catch (IOException e) {
            throw Py.IOError(e.getMessage());
        }

        return returnData;
    }

    public static PyBytes decompress(PyBytes data) {
        PyBytes returnString = null;

        if (data.toString().equals("")) {
            return Py.EmptyByte;
        }
        try {
            ByteArrayInputStream inputArray = new ByteArrayInputStream(
                    data.toBytes());
            BZip2CompressorInputStream bzbuf = new BZip2CompressorInputStream(
                    inputArray);

            ByteArrayOutputStream outputArray = new ByteArrayOutputStream();

            final byte[] buffer = new byte[8192];
            int n = 0;
            while ((n = bzbuf.read(buffer)) != -1) {
                outputArray.write(buffer, 0, n);
            }

            returnString = new PyBytes(outputArray.toString("iso-8859-1"));

            outputArray.close();
            bzbuf.close();
            inputArray.close();
        } catch (IOException e) {
            throw Py.ValueError(e.getMessage());
        }

        return returnString;
    }
}
