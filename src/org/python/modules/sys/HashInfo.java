package org.python.modules.sys;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedType;

@ExposedType(name = "sys.hash_info")
public class HashInfo extends PyObject {
    public static final PyType TYPE = PyType.fromClass(HashInfo.class);

    @ExposedGet(doc = "width of the type used for hashing, in bits")
    public int width;
    @ExposedGet(doc = "prime number giving the modulus on which the hash function is based")
    public long modulus;
    @ExposedGet(doc = "value to be used for hash of a positive infinity")
    public int inf;
    @ExposedGet(doc = "value to be used for hash of a nan")
    public int nan;
    @ExposedGet(doc = "multiplier used for the imaginary part of a complex number")
    public int imag;
    @ExposedGet(doc = "name of the algorithm for hashing of str, bytes and memoryview")
    public String algorithm;
    @ExposedGet(doc = "internal output size of hash algorithm")
    public int hash_bits;
    @ExposedGet(doc = "seed size of hash algorithm")
    public int seed_bits;
    @ExposedGet(doc = "small string optimization cutoff")
    public int cutoff;

    private HashFunction defaultHashFunc = Hashing.sha1();

    public HashInfo() {
        width = 32;
        modulus = (1 << 31) - 1;
        inf = 314159;
        nan = 0;
        imag = 0xf4243;
        algorithm = "SHA-1";
        hash_bits = defaultHashFunc.bits();
        seed_bits = 128; // xxx
        cutoff = 0;
    }
}
