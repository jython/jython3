/* Copyright (c) 2017 Jython Developers */
package org.python.modules.zipimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.python.core.PyBytes;
import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyUnicode;

/**
 * Java level tests for {@link PyZipImporter}. If this module does not meet its expected behaviours,
 * Jython pretty much won't start, so a test independent of the interpreter seems a good idea.
 *
 * Python exceptions are not handled correctly in this test because the Python classes involved are
 * not initialised. So where a PyException might have been raised, a Java NullPointerException tends
 * to result.
 */
public class ZipImportTest {

    /** Shorthand for platform-specific file separator character */
    static final char SEP = File.separatorChar;

    /** In the standard library is a ZIP file at this (Unix-style) location: */
    static final String ARCHIVE =
            platform("dist/Lib/test/test_importlib/namespace_pkgs/top_level_portion1.zip");

    // (Relative) paths of sub-directories to the ZIP file and files within them.
    static final String FOOKEY = platform("foo/");
    static final String ONEKEY = platform("foo/one.py");

    @Before
    public void setUp() throws Exception {}

    /** Swap '/' for platform file name separator if different. */
    private static String platform(String path) {
        return (SEP == '/') ? path : path.replace('/', SEP);
    }

    /** Swap platform file name separator for '/' if different. */
    private static String unplatform(String path) {
        return (SEP == '/') ? path : path.replace(SEP, '/');
    }

    @After
    public void tearDown() throws Exception {
        // Empty the cache for a clean start next time
        PyDictionary cache = ZipImportModule._zip_directory_cache;
        for (PyObject item : cache.asIterable()) {
            cache.__delitem__(item);
        }
    }

    /**
     * Test method for
     * {@link org.python.modules.zipimport.PyZipImporter#PyZipImporter(java.lang.String)} where the
     * archive path is relative to the working directory.
     */
    @Test
    public void testPyZipImporterRelative() {
        testPyZipImporterHelper(ARCHIVE);
    }

    /**
     * Test method for
     * {@link org.python.modules.zipimport.PyZipImporter#PyZipImporter(java.lang.String)}. where the
     * archive path is absolute.
     */
    @Test
    public void testPyZipImporterAbsolute() {
        // Make the zip file location absolute
        Path path = Paths.get(ARCHIVE);
        testPyZipImporterHelper(path.toAbsolutePath().toString());
    }

    /**
     * Test the constructor using a given path as the path to the base archive (just the zip).
     *
     * @param archive path to zip (which may be absolute or relative)
     */
    private void testPyZipImporterHelper(String archive) {

        // Test where the archive path is just the zip file location
        PyZipImporter za = testPyZipImporterHelper(archive, archive, "");

        // Test where the archive path is extended with a sub-directory within the zip
        String fooPath = Paths.get(archive, FOOKEY).toString();
        PyZipImporter zf = testPyZipImporterHelper(fooPath, archive, FOOKEY);
        assertEquals("cache entry not re-used", za.files, zf.files);

        // If the platform is not Unix-like, check that a Unix-like path has the same result
        if (SEP != '/') {
            // Turn platform-specific archive path to Unix-like
            String archiveAlt = unplatform(archive);
            // Expected result still uses platform-specific separators (in API though not in ZIP).
            PyZipImporter za2 = testPyZipImporterHelper(archiveAlt, archive, "");
            assertEquals("cache entry not re-used", za.files, za2.files);
            String fooPathAlt = unplatform(fooPath);
            PyZipImporter zf2 = testPyZipImporterHelper(fooPathAlt, archive, FOOKEY);
            assertEquals("cache entry not re-used", za.files, zf2.files);
        }

        // Check that an extra SEP on the end does not upset the constructor
        PyZipImporter zax = testPyZipImporterHelper(archive + SEP, archive, "");
        assertEquals("cache entry not re-used", za.files, zax.files);
        PyZipImporter zfx = testPyZipImporterHelper(fooPath + SEP, archive, FOOKEY);
        assertEquals("cache entry not re-used", za.files, zfx.files);

    }

    /**
     * Helper to test construction of a <code>zipimporter</code>.
     *
     * @param archivePath argument to <code>zipimporter</code> constructor
     * @param archive expected base archive name
     * @param prefix expected prefix (sub-directory within ZIP)
     * @return the zipimporter we created for further tests
     */
    private PyZipImporter testPyZipImporterHelper(String archivePath, String archive,
            String prefix) {

        PyZipImporter z = new PyZipImporter(archivePath);
        assertEquals(archive, z.archive);
        assertEquals(prefix, z.prefix);

        // The files attribute should enumerate the files and directories.
        Map<String, PyTuple> map = mapFromFiles(z.files);

        // Check in the map that we got what we should
        assertEquals(2, map.size());
        String fooPath = Paths.get(archive, FOOKEY).toString() + SEP; // subdir gets SEP
        assertEquals(fooPath, map.get(FOOKEY).__getitem__(0).toString());
        String onePath = Paths.get(archive, ONEKEY).toString(); // file gets no SEP
        assertEquals(onePath, map.get(ONEKEY).__getitem__(0).toString());

        return z;
    }

    /** Copy the _files attribute into a Java map for testing. */
    private Map<String, PyTuple> mapFromFiles(PyObject files) {
        Map<String, PyTuple> map = new HashMap<>();
        for (PyObject item : files.asIterable()) {
            String key = ((PyUnicode) item).getString();
            PyTuple value = ((PyTuple) files.__finditem__(item));
            map.put(key, value);
        }
        return map;
    }

    /**
     * Test method for {@link java.lang.Object#toString()}.
     */
    @Test
    public void test__repr__() {

        // Test relative path
        PyZipImporter z = new PyZipImporter(ARCHIVE);
        String expected = String.format("<zipimporter object \"%s\">", ARCHIVE);
        PyUnicode actual = z.__repr__();
        assertEquals(expected, actual.getString());

        // Test absolute path
        String abspath = Paths.get(ARCHIVE).toAbsolutePath().toString();
        z = new PyZipImporter(abspath);
        expected = String.format("<zipimporter object \"%s\">", abspath);
        actual = z.__repr__();
        assertEquals(expected, actual.getString());

        // Test path with subfolder
        String fooPath = Paths.get(ARCHIVE, FOOKEY).toString(); // Strips trailing SEP in FOOKEY
        z = new PyZipImporter(fooPath);
        expected = String.format("<zipimporter object \"%s\">", fooPath);
        actual = z.__repr__();
        assertEquals(expected, actual.getString());
    }

    /**
     * Test method for
     * {@link org.python.modules.zipimport.PyZipImporter#zipimporter_get_data(java.lang.String)}.
     */
    @Test
    public void testZipimporter_get_data() {

        // Compose a reference result (long-windedly: PyBytes(ByteBuffer) required!)
        String ONE_TEXT = "attr = 'portion1 foo one'\n";
        ByteBuffer buf = Charset.forName("UTF-8").encode(ONE_TEXT);
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        PyBytes expected = new PyBytes(bytes);

        // Test with construction from base archive and access by path within
        check_get_data(ARCHIVE, ONEKEY, expected);

        // Test with construction from base archive and access by path including archive
        String onePath = Paths.get(ARCHIVE).resolve(Paths.get(ONEKEY)).toString();
        check_get_data(ARCHIVE, onePath, expected);

        // Test with construction from sub-directory archive and access by path within
        String fooPath = Paths.get(ARCHIVE, FOOKEY).toString();
        check_get_data(fooPath, ONEKEY, expected);

        // Test with construction from sub-directory archive and access by path including archive
        check_get_data(fooPath, onePath, expected);

        if (SEP != '/') {
            // Test again, but with '/' in place of SEP in the path presented to get_data
            check_get_data(ARCHIVE, unplatform(onePath), expected);
            check_get_data(fooPath, unplatform(ONEKEY), expected);
        }
    }

    /** Helper for {@link #testZipimporter_get_data()} **/
    private void check_get_data(String archivePath, String name, PyBytes expected) {
        PyZipImporter z = new PyZipImporter(archivePath);
        PyBytes actual = (PyBytes) z.zipimporter_get_data(name);
        assertEquals(expected, actual);
    }

    /**
     * Test method for
     * {@link org.python.modules.zipimport.PyZipImporter#zipimporter_load_module(java.lang.String)}.
     */
    // @Test
    public void testZipimporter_load_module() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.python.modules.zipimport.PyZipImporter#zipimporter_is_package(java.lang.String)}.
     */
    // @Test
    public void testZipimporter_is_package() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.python.modules.zipimport.PyZipImporter#zipimporter_get_source(java.lang.String)}.
     */
    // @Test
    public void testZipimporter_get_source() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.python.modules.zipimport.PyZipImporter#zipimporter_get_filename(java.lang.String)}.
     */
    // @Test
    public void testZipimporter_get_filename() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.python.modules.zipimport.PyZipImporter#zipimporter_get_code(java.lang.String)}.
     */
    // @Test
    public void testZipimporter_get_code() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.python.modules.zipimport.PyZipImporter#zipimporter_find_spec(org.python.core.PyObject[], java.lang.String[])}.
     *
     * Disabled test. <code>find_spec</code> is not implemented in <code>zipimport.zipimporter</code> in CPython 3.5 as far as we can tell,
     * and by not having it exposed, we should get fall-back behaviour depending on <code>find_module</code>.
     */
    // @Test
    public void testZipimporter_find_spec() {
        //fail("Not yet implemented");
    }

}
