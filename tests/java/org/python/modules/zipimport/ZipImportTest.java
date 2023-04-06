/* Copyright (c) 2017 Jython Developers */
package org.python.modules.zipimport;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
    static final String ARCHIVE = platform("tmpdir_ZipImportTest/sub/test.zip");
    static final Path ARCHIVE_PATH = Paths.get(ARCHIVE);

    /** The file structure in the zip. Separators are '/', irrespective of platform. */
    // @formatter:off
    static final String[] STRUCTURE = {
            // Used in testZipimporter_get_data
            "sub/dir/foo/",             // FOO
            "sub/dir/foo/x/one.py",     // ONE
            "sub/dir/foo/two.py",       // TWO two is a module in sub/dir/foo
            // A library whose path is "...test.zip"
            "b/__init__.py",            // b is a package
            "b/three.py",               // three is a module in b
            "b/c/",
            "b/c/__init__.py",          // b.c is a package
            "b/c/__init__.pyc",         // should be preferred to __init__.py
            "b/c/four.py",
            "b/c/four.pyc",             // should be preferred to four.py
            "b/c/five.pyc",
            "b/d/__init__.pyc",         // b.d is a package (even though no __init__.py)
            // No "b/d/__init__.py",
            // A library whose path is "...test.zip/lib/a"
            "lib/a/b/__init__.py",      // b is a package
            "lib/a/b/three.py",         // three is a module in b
            "lib/a/b/c/",
            "lib/a/b/c/__init__.py",    // b.c is a package
            "lib/a/b/c/__init__.pyc",   // should be preferred to __init__.py
            "lib/a/b/c/four.py",
            "lib/a/b/c/four.pyc",       // should be preferred to four.py
            "lib/a/b/c/five.pyc",
            "lib/a/b/d/__init__.pyc",   // b.d is a package (even though no __init__.py)
            // No "lib/a/b/d/__init__.py",
    };

    static final int FOO = 0, ONE = 1, TWO = 2; // Where they are in STRUCTURE
    // @formatter:on

    /** (Relative) path of directory "foo" using platform separator. */
    static final String FOOKEY = platform(STRUCTURE[FOO]);
    /** (Relative) path of file "one.py" using platform separator. */
    static final String ONEKEY = platform(STRUCTURE[ONE]);

    static final Charset UTF8 = Charset.forName("UTF-8");

    /** A time later than any source file, used on <code>.class</code> entries. */
    static final long LATER = Instant.parse("2038-01-01T00:00:00Z").toEpochMilli();

    /** Map from file name to the binary contents in the zip entry. */
    private static final Map<String, byte[]> BINARY = new HashMap<>(STRUCTURE.length);

    /** Empty CPython module. Date 1 Jan 2038 to be later than the <code>.py</code> */
    static final byte[] EMPTY_MODULE = {0x16, 0xd, 0xd, 0xa, // magic number: CPython pythonrun.c
            -128, 23, -24, 127, 0, 0, 0, 0, -29, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 64,
            0, 0, 0, 115, 4, 0, 0, 0, 100, 0, 0, 83, 41, 1, 78, -87, 0, 114, 1, 0, 0, 0, 114, 1, 0,
            0, 0, 114, 1, 0, 0, 0, -6, 7, 60, 101, 109, 112, 116, 121, 62, -38, 8, 60, 109, 111,
            100, 117, 108, 101, 62, 1, 0, 0, 0, 115, 0, 0, 0, 0};

    /** Empty <code>.class</code> (don't know what to write and don't care in these tests). */
    static final byte[] EMPTY_CLASS = {1, 2, 3, 4};

    /** Number of zip entries created. */
    static int zipEntryCount = 0;

    @BeforeClass
    public static void createTestZip() throws IOException {

        // Ensure directories exist to the archive location
        if (ARCHIVE_PATH.getNameCount() > 1) {
            Files.createDirectories(ARCHIVE_PATH.getParent());
        }

        // Create (or overwrite) the raw archive file itself
        OutputStream out = new BufferedOutputStream(Files.newOutputStream(ARCHIVE_PATH));

        // Wrap it so there are two inputs: one for ZipEntry objects and one for text to encode
        try (ZipOutputStream zip = new ZipOutputStream(out)) {

            // Make an entry for each path mentioned in STRUCTURE
            for (int i = 0; i < STRUCTURE.length; i++) {

                // The structure table gives us names for the entries
                String name = STRUCTURE[i];

                if (name.endsWith("/")) {
                    // Directory entry: no content
                    zip.putNextEntry(new ZipEntry(name));
                    zip.closeEntry();

                } else if (name.endsWith(".pyc")) {
                    // Reduce to module name
                    createCompiledZipEntry(zip, name.substring(0, name.length() - 4));
                    zipEntryCount += 1;

                } else if (name.endsWith(".py")) {
                    // Content is Python source
                    createTestZipEntry(zip, name, String.format("# %s\n", name));

                } else {
                    // Any other kind of file (just in case)
                    createTestZipEntry(zip, name, String.format("Contents of %s\n", name));
                }

                zipEntryCount += 1;
            }
        }
    }

    /**
     * Helper to {@link #createTestZip()} creating two entries with binary content: one .
     *
     * @param zip file to write
     * @param name of relative zip entry (a '/'-path)
     * @throws IOException
     */
    private static void createCompiledZipEntry(ZipOutputStream zip, String name)
            throws IOException {
        // Ensure time stamp on the binary is a long time in the future.
        createTestZipEntry(zip, name + ".class", LATER, EMPTY_CLASS);
        // But also make a valid .pyc file (for behavioural comparison with CPython)
        createTestZipEntry(zip, name + ".pyc", LATER, EMPTY_MODULE);
    }

    /**
     * Helper to {@link #createTestZip()} creating one entry with binary content.
     *
     * @param zip file to write
     * @param name of relative zip entry (a '/'-path)
     * @param time in milliseconds since epoch for timestamp (use current time if &lt;0)
     * @param content to write in the entry
     * @throws IOException
     */
    private static void createTestZipEntry(ZipOutputStream zip, String name, long time,
            byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        if (time >= 0L) {
            entry.setTime(time);
        }
        zip.putNextEntry(entry);
        zip.write(content);
        BINARY.put(name, content);
        zip.closeEntry();
    }

    /**
     * Helper to {@link #createTestZip()} creating one entry with text content.
     *
     * @param zip file to write
     * @param name of relative zip entry (a '/'-path)
     * @param content to write in the entry
     * @throws IOException
     */
    private static void createTestZipEntry(ZipOutputStream zip, String name, String content)
            throws IOException {
        ByteBuffer buf = UTF8.encode(content);
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        long time = System.currentTimeMillis();
        createTestZipEntry(zip, name, time, bytes);
    }

    @AfterClass
    public static void deleteTestZip() {
        // Useful to look at from Python!
        // try {
        // Files.deleteIfExists(ARCHIVE_PATH);
        // } catch (IOException e) {
        // Meh!
        // }
    }

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {
        // Empty the cache for a clean start next time
        PyDictionary cache = ZipImportModule._zip_directory_cache;
        for (PyObject item : cache.asIterable()) {
            cache.__delitem__(item);
        }
    }

    /** Swap '/' for platform file name separator if different. */
    private static String platform(String path) {
        return (SEP == '/') ? path : path.replace('/', SEP);
    }

    /** Swap platform file name separator for '/' if different. */
    private static String unplatform(String path) {
        return (SEP == '/') ? path : path.replace(SEP, '/');
    }

    /**
     * Test method for {@link PyZipImporter#PyZipImporter(java.lang.String)} where the archive path
     * is relative to the working directory.
     */
    @Test
    public void testPyZipImporterRelative() {
        testPyZipImporterHelper(ARCHIVE);
    }

    /**
     * Test method for {@link PyZipImporter#PyZipImporter(java.lang.String)}. where the archive path
     * is absolute.
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

        // Test where the archive path is extended with a sub-directory "foo" within the zip
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
        assertEquals(zipEntryCount, map.size());
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
     * Test method for {@link PyZipImporter#zipimporter_get_data(java.lang.String)}.
     */
    @Test
    public void testZipimporter_get_data() {

        // Get the reference result
        PyBytes expected = new PyBytes(BINARY.get(STRUCTURE[ONE]));

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

    /** Class describing one entry in the ZIP file, parsed to various useful paths and elements. */
    static class EntrySplit {

        /** Path from root in the ZIP to the current module (Javanese: relative '/'-path). */
        final String parent;
        /** Name of the current module. */
        final String name;
        /** Extension (e.g. ".py") represented by the specification we split. */
        final String ext;
        /** Whether the current module is a package. */
        final boolean isPackage;
        /** Path that would be used by the import protocol to create the PyZipImporter. */
        final Path archivePath;
        /** File path to and through the ZIP to the current module. */
        final Path filePath;

        /** Parse the path string into the parts needed. */
        EntrySplit(String entry) {

            // Split the entry into a path in the archive, name and extension.
            int slash = entry.lastIndexOf('/');
            String parent = entry.substring(0, slash + 1); // ends with '/' or is "" = no '/'
            int dot = entry.lastIndexOf('.');
            String name, ext;
            if (dot < slash) {
                dot = entry.length();
            }
            name = entry.substring(slash + 1, dot); // e.g. "three" or "__init__"
            ext = entry.substring(dot); // e.g. ".py" or ".class" or "" = no dot

            isPackage = "__init__".equals(name);

            if (isPackage) {
                if (slash < 1) {
                    // Pretty sure it is not valid to have __init__ in the root. Blame the tools.
                    throw new IllegalArgumentException("Invalid entry: __init__ in root.");
                }
                /*
                 * Move everything up one in the hierarchy. The package name is at the end of the
                 * parent string and the zipimporter to use is the one constructed for the parent of
                 * that.
                 */
                slash = parent.lastIndexOf('/', slash - 1);
                name = parent.substring(slash + 1, parent.length() - 1);
                parent = parent.substring(0, slash + 1);
            }
            this.parent = parent;
            this.name = name;
            this.ext = ext;

            // Construct a path to the required place inside the archive
            StringBuilder path = new StringBuilder(ARCHIVE);
            path.append(SEP).append(platform(parent));
            this.archivePath = Paths.get(path.toString());

            // The file path is estimated (since a binary might exist) & depends on package-ness.
            path.append(name);
            if (isPackage) {
                path.append(SEP).append("__init__");
            }
            path.append(ext);
            this.filePath = Paths.get(path.toString());
        }
    }

    /** Split a filename at the last '.', as long as it is not before the last separator. */
    private static int dotIndex(String entry, char sep) {
        int slash = entry.lastIndexOf(sep);
        int dot = entry.lastIndexOf('.');
        if (dot <= slash) {
            dot = entry.length();
        }
        return dot;
    }

    /**
     * Test method for {@link PyZipImporter#zipimporter_get_filename(java.lang.String)}. Given this
     * list of entries in the archive:
     *
     * <pre>
     * >>> from zipimport import zipimporter as zi
     * >>> archive = r'tmpdir_ZipImportTest\sub\test.zip'
     * >>> z = zi(archive)
     * >>> for n in sorted(z._files.keys()): print(n, z._files[n][5:7]) # (time, date)
     * ...
     * b\__init__.py (44545, 19185)
     * b\c\ (44545, 19185)
     * b\c\__init__.class (0, 29729)
     * b\c\__init__.py (44545, 19185)
     * b\c\__init__.pyc (0, 29729)
     * b\c\five.class (0, 29729)
     * b\c\five.pyc (0, 29729)
     * b\c\four.class (0, 29729)
     * b\c\four.py (44545, 19185)
     * b\c\four.pyc (0, 29729)
     * b\d\__init__.class (0, 29729)
     * b\d\__init__.pyc (0, 29729)
     * b\three.py (44545, 19185)
     * </pre> CPython behaviour is: <pre>
     * >>> z.get_filename('b')
     * 'tmpdir_ZipImportTest\\sub\\test.zip\\b\\__init__.py'
     * >>> zb = zi(archive + '/b')
     * >>> zb.get_filename('c')
     * 'tmpdir_ZipImportTest\\sub\\test.zip\\b\\c\\__init__.py'
     * >>> zb.get_filename('d')
     * 'tmpdir_ZipImportTest\\sub\\test.zip\\b\\d\\__init__.pyc'
     * >>> zc = zi(archive + '/b/c')
     * >>> zc.get_filename('four')
     * 'tmpdir_ZipImportTest\\sub\\test.zip\\b\\c\\four.py'
     * >>> zc.get_filename('five')
     * 'tmpdir_ZipImportTest\\sub\\test.zip\\b\\c\\five.pyc'
     * </pre> That is, we report the <code>.py</code> file name if there is one and the
     * <code>.pyc</code> file name if the <code>.py</code> is missing (but in Jython, the
     * <code>.class</code> file should be expected).
     */
    @Test
    public void testZipimporter_get_filename() {
        // Test with each of the entries that is not just a folder
        for (String entry : STRUCTURE) {
            if (entry.endsWith(".py")) {
                // Parse the entry string into the parts we need and test
                EntrySplit split = new EntrySplit(entry);
                check_get_filename(split);
            } else if (entry.endsWith(".class")) {
                // In this case, if a .py exists, we should be given that instead
                String pyRelativePath = entry.substring(entry.length() - 6) + ".py";
                if (BINARY.containsKey(pyRelativePath)) {
                    entry = pyRelativePath;
                }
                // Parse the entry string into the parts we need and test
                EntrySplit split = new EntrySplit(entry);
                check_get_filename(split);
            }
        }
    }

    /**
     * When we ask for the module indicated by each the {@link #STRUCTURE} entry, we should get as a
     * file name a string consistent with the entry, allowing for the several ways to satisfy the
     * request, and for the use of the platform {@link File#separatorChar}.
     *
     * @param split derived from a non-directory entry in {@link #STRUCTURE}.
     */
    private static void check_get_filename(EntrySplit split) {
        // Make a PyZipImporter for this entry (parent of module or package)
        PyZipImporter z = new PyZipImporter(split.archivePath.toString());

        // The file path in the split tells us What we should have, except for the extension.
        String expected = split.filePath.toString();

        // Only the last element ought to be used.
        String target = "ignore.me." + split.name;
        PyObject filenameObject = z.zipimporter_get_filename(target);

        if (filenameObject instanceof PyUnicode) {
            // Compare with expected value and actual, but only up to the dot
            String filename = ((PyUnicode) filenameObject).getString();
            int dot = dotIndex(filename, SEP);
            assertThat("base file path", filename.substring(0, dot),
                    equalTo(expected.substring(0, dotIndex(expected, SEP))));
            assertThat("file path for " + target, filename, equalTo(expected));
        } else {
            fail("get_filename() result not a str object");
        }
    }

    /**
     * Test method for {@link PyZipImporter#zipimporter_load_module(java.lang.String)}.
     */
    // @Test
    public void testZipimporter_load_module() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link PyZipImporter#zipimporter_is_package(java.lang.String)}.
     */
    // @Test
    public void testZipimporter_is_package() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link PyZipImporter#zipimporter_get_source(java.lang.String)}.
     */
    // @Test
    public void testZipimporter_get_source() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link PyZipImporter#zipimporter_get_code(java.lang.String)}.
     */
    // @Test
    public void testZipimporter_get_code() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link PyZipImporter#zipimporter_find_spec(org.python.core.PyObject[], java.lang.String[])}.
     *
     * Disabled test. <code>find_spec</code> is not implemented in
     * <code>zipimport.zipimporter</code> in CPython 3.5 as far as we can tell, and by not having it
     * exposed, we should get fall-back behaviour depending on <code>find_module</code>.
     */
    // @Test
    public void testZipimporter_find_spec() {
        // fail("Not yet implemented");
    }

}
