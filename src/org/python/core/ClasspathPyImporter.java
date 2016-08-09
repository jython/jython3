/* Copyright (c) Jython Developers */
package org.python.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.python.core.util.FileUtil;
import org.python.core.util.StringUtil;
import org.python.core.util.importer;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.util.Generic;

// TODO: This could be replaced by the frozen module machinery, implement _imp.is_frozen / exec_frozen properly
// NOTE: this class should be kept as a fallback to bootstrap the import machinery, before we find a way to freeze
// the importlib/_bootstrap(_external).py without a working import mechanism
@Untraversable
@ExposedType(name="ClasspathPyImporter")
public class ClasspathPyImporter extends importer {

    public static final String PYCLASSPATH_PREFIX = "__pyclasspath__/";
    public static final PyType TYPE = PyType.fromClass(ClasspathPyImporter.class);

    public ClasspathPyImporter(PyType subType) {
        super(subType);
    }

    public ClasspathPyImporter() {
        super(TYPE);
    }

    @ExposedNew
    @ExposedMethod
    final void ClasspathPyImporter___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("__init__", args, kwds, new String[] {"path"});
        String path = ap.getString(0);
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        this.path = path;
    }

    /**
     * Return the contents of the jarred file at the specified path
     * as bytes.
     *
     * @param path a String path name within the archive
     * @return a String of data in binary mode (no CRLF)
     */
    @Override
    public String get_data(String path) {
        return ClasspathPyImporter_get_data(path);
    }

    @ExposedMethod
    final String ClasspathPyImporter_get_data(String path) {
        // Strip any leading occurrence of the hook string
//        int len = PYCLASSPATH_PREFIX.length();
//        if (len < path.length() && path.startsWith(PYCLASSPATH_PREFIX)) {
//            path = path.substring(len);
//        }

        // Bundle wraps the stream together with a close operation
        try (Bundle bundle = makeBundle(makeEntry(path))) {
            byte[] data = FileUtil.readBytes(bundle.inputStream);
            return StringUtil.fromBytes(data);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    /**
     * Return the source code for the module as a string (using
     * newline characters for line endings)
     *
     * @param fullname the fully qualified name of the module
     * @return a String of the module's source code or null
     */
    public String get_source(String fullname) {
        return ClasspathPyImporter_get_source(fullname);
    }

    @ExposedMethod
    final String ClasspathPyImporter_get_source(String fullname) {
        ModuleInfo moduleInfo = getModuleInfo(fullname);
        if (moduleInfo.notFound()) {
            throw Py.ImportError(String.format("can't find module '%s'", fullname), fullname);
        }
        try (InputStream is = new FileInputStream(new File(moduleInfo.getSourcePath()))) {
            byte[] data = FileUtil.readBytes(is);
            return StringUtil.fromBytes(data);
        } catch (IOException e) {
            throw Py.ImportError(String.format("source of %s not found", fullname));
        }
    }

    /**
     * Find the module for the fully qualified name.
     *
     * @param fullname the fully qualified name of the module
     * @param path if not installed on the meta-path None or a module path
     * @return a loader instance if this importer can load the module, None
     *         otherwise
     */
    @ExposedMethod(defaults = "null")
    final PyObject ClasspathPyImporter_find_module(String fullname, String path) {
        return importer_find_module(fullname, path);
    }

    @ExposedMethod
    final PyObject ClasspathPyImporter_find_spec(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("find_spec", args, keywords, "fullname", "path", "target");
        String fullname = ap.getString(0);
        PyObject path = ap.getPyObject(1, Py.None);
        PyObject target = ap.getPyObject(2, Py.None);
        PyObject spec = importer_find_spec(fullname);
        if (spec == null) {
            return Py.None;
        }
        spec.__setattr__("has_location", Py.True);
        return spec;
    }

    /**
     * Determine whether a module is a package.
     *
     * @param fullname the fully qualified name of the module
     * @return whether the module is a package
     */
    @ExposedMethod
    final boolean ClasspathPyImporter_is_package(String fullname) {
        return importer_is_package(fullname);
    }

    /**
     * Return the code object associated with the module.
     *
     * @param fullname the fully qualified name of the module
     * @return the module's PyCode object or None
     */
    @ExposedMethod
    final PyObject ClasspathPyImporter_get_code(String fullname) {
        ModuleCodeData moduleCodeData = getModuleCode(fullname);
        if (moduleCodeData != null) {
            return moduleCodeData.code;
        }
        return Py.None;
    }

    /**
     * Load a module for the fully qualified name.
     *
     * @param fullname the fully qualified name of the module
     * @return a loaded PyModule
     */
    @ExposedMethod
    final PyObject ClasspathPyImporter_load_module(String fullname) {
        return importer_load_module(fullname);
    }

    @ExposedMethod
    final PyObject ClasspathPyImporter_create_module(PyObject spec) {
        String fullname = spec.__getattr__("name").asString();
        // the module *must* be in sys.modules before the loader executes the module code; the
        // module code may (directly or indirectly) import itself
        PyModule mod = imp.addModule(fullname);
        mod.__setattr__("__loader__", this);
        return mod;
    }

    @ExposedMethod
    final PyObject ClasspathPyImporter_exec_module(PyObject module) {
        PyModule mod = (PyModule) module;
        String fullname = mod.__findattr__("__name__").asString();
        ModuleCodeData moduleCodeData = getModuleCode(fullname);
        return imp.createFromCode(fullname, moduleCodeData.code);
    }

    @Override
    protected long getSourceMtime(String path) {
        // Can't determine this easily
        return -1;
    }

    @Override
    protected Bundle makeBundle(String entry) {
        InputStream is = entries.remove(entry);
        return new Bundle(is) {
            @Override
            public void close() {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw Py.JavaError(e);
                }
            }
        };
    }

    @Override
    protected String makeEntry(String filename) {
        if (entries.containsKey(filename)) {
            return filename;
        }
        InputStream is;
        File input = new File(filename);
        try {
            is = new FileInputStream(input);
        } catch (FileNotFoundException e) {
            return null;
        }
//        ClassLoader sysClassLoader = Py.getSystemState().getClassLoader();
//        if (sysClassLoader != null) {
//            is = tryClassLoader(filename, sysClassLoader, "sys");
//        } else {
//            is = tryClassLoader(filename, imp.getParentClassLoader(), "parent");
//        }
//        if (is == null) {
//            return null;
//        }

        entries.put(filename, is);
        return filename;
    }

    private InputStream tryClassLoader(String fullFilename, ClassLoader loader, String name) {
        if (loader != null) {
            Py.writeDebug("import", "trying " + fullFilename + " in " + name + " class loader");
            return loader.getResourceAsStream(fullFilename);
        }
        return null;
    }

    @Override
    protected String makeFilename(String fullname) {
        int dot = fullname.lastIndexOf('.');
        if (dot < 0) {
            return fullname;
        }
        return fullname.substring(dot + 1);
    }

    @Override
    protected String makeFilePath(String fullname) {
        int dot = fullname.lastIndexOf('.');
        if (dot < 0) {
            return path;
        }
        return path + fullname.substring(0, dot + 1).replace('.', File.separatorChar);
    }

    @Override
    protected String makePackagePath(String fullname) {
        return path;
    }

    private Map<String, InputStream> entries = new HashMap<>();

    private String path;
}
