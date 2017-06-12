/* Copyright (c) 2017 Jython Developers */
package org.python.modules.zipimport;

import org.python.Version;
import org.python.core.ArgParser;
import org.python.core.BuiltinDocs;
import org.python.core.BytecodeLoader;
import org.python.core.Py;
import org.python.core.PyBytes;
import org.python.core.PyCode;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PyModule;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.core.imp;
import org.python.core.util.FileUtil;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@ExposedType(name = "zipimport.zipimporter", doc = BuiltinDocs.zipimport_zipimporter_doc)
public class PyZipImporter extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyZipImporter.class);

    /**
     * Path to the ZIP archive: "path/to/archive.zip" if constructed from
     * "path/to/archive.zip/a/sub/directory".
     */
    // XXX In CPython it is "decoded from the FS encoding" (by PyUnicode_FSDecoder, during __init__)
    @ExposedGet
    public final String archive;

    /**
     * File prefix: "a/sub/directory/", if constructed from "path/to/archive.zip/a/sub/directory".
     */
    // XXX here CPython says "encoded to the FS encoding" but that doesn't seem to be accurate
    @ExposedGet
    public final String prefix;

    /** Dictionary with information on each file in the ZIP <code>{path: tocEntry}</code> */
    @ExposedGet(name = "_files")
    public final PyObject files;

    /**
     * Construct a <code>PyZipImporter</code> for the given path, which may include sub-directories
     * within the ZIP file, for example <code>path/to/archive.zip/a/sub/directory</code>. Because
     * equivalence between ZIP files and sub-directories in Python import (see
     * <a href="https://www.python.org/dev/peps/pep-0273/#subdirectory-equivalence"> PEP 273</a>), a
     * <code>PyZipImporter</code> operates using the platform-specific file separator ("\" on
     * Windows) at all public interfaces, so on that platform a path like
     * <code>path\to\archive.zip\a\sub\directory</code> will normally be supplied. However, we
     * follow CPython in tolerating either "/" or the platform-specific file separator in the
     * <code>archivePath</code>, or indeed any mixture of the two.
     *
     * @param archivePath path to archive and optionally a sub-directory within it
     */
    public PyZipImporter(String archivePath) {
        this(TYPE, archivePath);
    }

    /**
     * Equivalent to {@link PyZipImporter#PyZipImporter(String)} when sub-class required.
     *
     * @param type of sub-class
     * @param archivePath path to archive and optionally a sub-directory within it
     */
    public PyZipImporter(PyType type, String archivePath) {
        super(type);

        if (archivePath == null || archivePath.length() == 0) {
            throw ZipImportModule.ZipImportError("archive path is empty");
        }
        archivePath = toPlatformSeparator(archivePath);

        /*
         * archivepath may be e.g. "path/to/archive.zip/a/sub/directory", meaning we must look for
         * modules in the lib directory inside the ZIP file path/to/archive.zip (provided that it
         * exists). We must separate the archive (ZIP file) proper from the starting path within it,
         * which is known as the "prefix".
         */
        Path fullPath = Paths.get(archivePath), archive = fullPath;
        int prefixEnd = archive.getNameCount();
        int prefixStart = prefixEnd;

        // Strip elements from end of path until empty, a file or a directory
        for (archive = fullPath; prefixStart > 1; archive = archive.getParent(), prefixStart--) {
            if (Files.isRegularFile(archive)) {
                break;
            } else if (Files.isDirectory(archive)) {
                // Stripping names got us to a directory: no ZIP file here
                archive = null;
                break;
            }
        }

        if (archive == null || archive.getNameCount() == 0) {
            throw ZipImportModule.ZipImportError(String.format("not a Zip file: %s", archivePath));
        }

        // Look up, or add if necessary, an entry in the cache for the files.
        this.archive = archive.toString();
        PyObject files = ZipImportModule._zip_directory_cache.__finditem__(this.archive);
        if (files == null) {
            /*
             * This is new. Make a cache entry that enumerates the files in the ZIP. This is also
             * where we throw if we can't read it as a ZIP.
             */
            files = readDirectory(archive);
            ZipImportModule._zip_directory_cache.__setitem__(this.archive, files);
        }
        this.files = files;

        // The prefix is that part of the original archivePath that is not in archive.
        if (prefixStart < prefixEnd) {
            // There was a prefix
            Path prefix = fullPath.subpath(prefixStart, prefixEnd);
            this.prefix = prefix.toString() + File.separator;
        } else {
            this.prefix = "";
        }
    }

    /**
     * __new__ method equivalent to {@link PyZipImporter#PyZipImporter(String)}.
     */
    @ExposedNew
    final static PyObject zipimporter_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("zipimporter", args, keywords, "archivepath");
        String archivePath = ap.getString(0);
        // XXX Should FS-decode args[0] here. CPython uses PyUnicode_FSDecoder, during __init__
        return new PyZipImporter(archivePath);
    }


    @Override
    public String toString() {
        Path archivePath = Paths.get(archive, prefix);
        return String.format("<zipimporter object \"%s\">",  archivePath);
    }

    @ExposedMethod
    public final PyObject zipimporter_exec_module(PyObject module) {
        PyModule mod = (PyModule) module;
        String fullname = mod.__findattr__("__name__").asString();
        return imp.createFromCode(fullname, (PyCode) zipimporter_get_code(fullname));
    }

    /**
     * CPython zipimport module is very outdated, it's not yet compliant with PEP-451, the specs are
     * checking the old behaviour this method and a few others that are deprecated a simply
     * implemented to satisfy the test suite
     *
     * @param fullname
     * @return a python module
     */
    @Deprecated
    @ExposedMethod
    public final PyObject zipimporter_load_module(String fullname) {
        return getEntry(fullname, (entry, inputStream) -> {
            PyModule mod = imp.addModule(fullname);
            imp.createFromCode(fullname, (PyCode) zipimporter_get_code(fullname));
            String folder = archive + File.separator + prefix;
            if (entry._package) {
                PyList pkgPath = new PyList();
                pkgPath.append(new PyUnicode(folder + entry.dir(fullname)));
                mod.__setattr__("__path__", pkgPath);
            }
            if (entry.binary) {
                mod.__setattr__("__cached__", new PyUnicode(folder + entry.path(fullname)));
            }
            mod.__setattr__("__file__", new PyUnicode(folder + entry.sourcePath(fullname)));
            return mod;
        });
    }

    @ExposedMethod
    public final PyObject zipimporter_is_package(String fullname) {
        return getEntry(fullname, (entry, input) -> Py.newBoolean(entry._package));
    }

    @ExposedMethod
    public final PyObject zipimporter_get_source(String fullname) {
        return getEntry(fullname, (entry, inputStream) -> {
            try {
                if (entry.binary) {
                    return Py.None;
                }
                return new PyUnicode(FileUtil.readBytes(inputStream));
            } catch (IOException e) {
                throw ZipImportModule.ZipImportError(e.getMessage());
            }
        });
    }

    @ExposedMethod
    public final PyObject zipimporter_get_data(String filename) {
        ZipFile zipFile = null;
        if (filename.startsWith(archive)) {
            filename = filename.substring(archive.length() + 1);
        }
        try {
            zipFile = new ZipFile(new File(archive));
            ZipEntry zipEntry = zipFile.getEntry(prefix + filename);
            if (zipEntry != null) {
                return new PyBytes(FileUtil.readBytes(zipFile.getInputStream(zipEntry)));
            }
            throw ZipImportModule.ZipImportError(filename);
        } catch (IOException e) {
            throw ZipImportModule.ZipImportError(e.getMessage());
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {}
            }
        }
    }

// @ExposedMethod
// public final PyObject zipimporter_get_data(String fullname) {
// throw ZipImportModule.ZipImportError(fullname);
// }

    @ExposedMethod
    public final PyObject zipimporter_get_filename(String fullname) {
        return getEntry(fullname, (entry, inputStream) -> {
            return new PyUnicode(archive + File.separator + prefix + entry.sourcePath(fullname));
        });
    }

    @ExposedMethod
    public final PyObject zipimporter_get_code(String fullname) {
        try {
            long mtime = Files.getLastModifiedTime(new File(archive).toPath()).toMillis();
            return getEntry(fullname, (entry, inputStream) -> {
                byte[] codeBytes;
                if (entry.binary) {
                    try {
                        codeBytes = imp.readCode(fullname, inputStream, false, mtime);
                    } catch (IOException ioe) {
                        throw Py.ImportError(
                                ioe.getMessage() + "[path=" + entry.path(fullname) + "]");
                    }
                } else {
                    try {
                        byte[] bytes = FileUtil.readBytes(inputStream);
                        codeBytes = imp.compileSource(fullname, new ByteArrayInputStream(bytes),
                                entry.path(fullname));
                    } catch (IOException e) {
                        throw ZipImportModule.ZipImportError(e.getMessage());
                    }
                }
                return BytecodeLoader.makeCode(fullname + Version.PY_CACHE_TAG, codeBytes,
                        entry.path(fullname));
            });
        } catch (IOException e) {
            throw ZipImportModule.ZipImportError(e.getMessage());
        }
    }

    @ExposedMethod
    final PyObject zipimporter_find_spec(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("find_spec", args, keywords, "fullname", "path", "target");
        String fullname = ap.getString(0);
        PyObject path = ap.getPyObject(1, Py.None);
        PyObject target = ap.getPyObject(2, Py.None);
        PyObject moduleSpec = Py.getSystemState().importlib.__findattr__("ModuleSpec");
        PyObject spec = moduleSpec.__call__(new PyUnicode(fullname), this);
        return getEntry(fullname, (entry, inputStream) -> {
            String folder = archive + File.separatorChar + prefix;
            if (entry._package) {
                PyList pkgpath = new PyList();
                pkgpath.add(new PyUnicode(folder + entry.dir(fullname)));
                spec.__setattr__("submodule_search_locations", pkgpath);
                spec.__setattr__("is_package", Py.True);
            }
            if (entry.binary) {
                spec.__setattr__("cached", new PyUnicode(folder + entry.path(fullname)));
            }
            spec.__setattr__("origin", new PyUnicode(folder + entry.sourcePath(fullname)));
            spec.__setattr__("has_location", Py.True);
            return spec;
        });
    }

    /**
     * Return path with every `\`character replaced by {@link File#pathSeparatorChar}, if that's
     * different.
     */
    private static String toPlatformSeparator(String path) {
        if (File.separatorChar == '/') {
            return path;
        } else {
            return path.replace('/', File.separatorChar);
        }
    }

    /**
     * Return path with every {@link File#pathSeparatorChar} character replaced by `\`, if that's
     * different. We need this because Python treats paths into zip files as equivalent to paths in
     * the file system, hence localises the separator to the platform, while zip files themselves
     * use '/' consistently internally.
     */
    private static String fromPlatformSeparator(String path) {
        if (File.separatorChar == '/') {
            return path;
        } else {
            return path.replace(File.separatorChar, '/');
        }
    }

    private <T> T getEntry(String fullname, BiFunction<ModuleEntry, InputStream, T> func) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(new File(archive));
            for (ModuleEntry entry : entries()) {
                ZipEntry zipEntry = zipFile.getEntry(prefix + entry.path(fullname));
                if (zipEntry != null) {
                    return func.apply(entry, zipFile.getInputStream(zipEntry));
                }
            }
            throw ZipImportModule.ZipImportError(fullname);
        } catch (IOException e) {
            throw ZipImportModule.ZipImportError(e.getMessage());
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {}
            }
        }
    }

    private ModuleEntry[] entries() {
        boolean[] options = {true, false};
        ModuleEntry[] res = new ModuleEntry[4];
        int i = 0;
        for (boolean pack : options) {
            for (boolean bin : options) {
                res[i++] = new ModuleEntry(pack, bin);
            }
        }

        return res;
    }

    private static PyObject readDirectory(Path archive) {

        if (!Files.isReadable(archive)) {
            throw ZipImportModule
                    .ZipImportError(String.format("can't open Zip file: '%s'", archive));
        }

        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            PyObject files = new PyDictionary();
            readZipFile(zipFile, files);
            return files;
        } catch (IOException ioe) {
            throw ZipImportModule
                    .ZipImportError(String.format("can't read Zip file: '%s'", archive));
        }
    }

    /**
     * Read ZipFile metadata into a dict of toc entries.
     *
     * A tocEntry is a tuple:
     *
     * <pre>
     *     (__file__,     # value to use for __file__, available for all files
     *     compress,      # compression kind; 0 for uncompressed
     *     data_size,     # size of compressed data on disk
     *     file_size,     # size of decompressed data
     *     file_offset,   # offset of file header from start of archive (or -1 in Jython)
     *     time,          # mod time of file (in dos format)
     *     date,          # mod data of file (in dos format)
     *     crc,           # crc checksum of the data
     *     )
     *</pre>
     *
     * Directories can be recognized by the trailing <code>os.sep</code> in the name,
     * <code>data_size</code> and <code>file_offset</code> are 0.
     *
     * @param zipFile ZipFile to read
     * @param files a dict-like PyObject
     */
    private static void readZipFile(ZipFile zipFile, PyObject files) {
        // Iterate over the entries and build an informational tuple for each
        final String zipNameAndSep = zipFile.getName() + File.separator;
        for (Enumeration<? extends ZipEntry> zipEntries = zipFile.entries(); zipEntries
                .hasMoreElements();) {
            // Oh for Java 9 and Enumeration.asIterator()
            ZipEntry zipEntry = zipEntries.nextElement();
            String name = toPlatformSeparator(zipEntry.getName());
            // XXX: Java zip file uses UTF-8 internally. Is there an encoding issue here?
            PyObject file = new PyUnicode(zipNameAndSep + name);

            PyObject compress = new PyLong(zipEntry.getMethod());
            PyObject data_size = new PyLong(zipEntry.getCompressedSize());
            PyObject file_size = new PyLong(zipEntry.getSize());
            /*
             * file_offset is a CPython optimization; it's used to seek directly to the file when
             * reading it later. Jython doesn't do this nor is the offset available
             */
            PyObject file_offset = new PyLong(-1);
            PyObject time = new PyLong(zipEntry.getTime());
            PyObject date = new PyLong(zipEntry.getTime());
            PyObject crc = new PyLong(zipEntry.getCrc());

            PyTuple entry =
                    new PyTuple(file, compress, data_size, file_size, file_offset, time, date, crc);

            files.__setitem__(new PyUnicode(name), entry);
        }
    }

    class ModuleEntry {

        private boolean _package;
        private boolean binary;

        ModuleEntry(boolean pack, boolean bin) {
            _package = pack;
            binary = bin;
        }

        String path(String name) {
            StringBuilder res = new StringBuilder();
            res.append(name.substring(name.lastIndexOf('.') + 1));
            if (_package) {
                res.append(File.separatorChar + "__init__");
            }
            if (binary) {
                res.append(".class");
            } else {
                res.append(".py");
            }
            return res.toString();
        }

        String dir(String name) {
            return path(name).replaceFirst("/__init__\\.(py|class)$", "");
        }

        String sourcePath(String name) {
            return new ModuleEntry(_package, false).path(name);
        }
    }
}
