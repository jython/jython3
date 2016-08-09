/* Copyright (c) Jython Developers */
package org.python.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;

import org.python.Version;
import org.python.core.BytecodeLoader;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyList;
import org.python.core.PyModule;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.core.imp;

/**
 * A base class for PEP-302 path hooks. Handles looking through source, compiled, package and module
 * items in the right order, and creating and filling in modules.
 */
public abstract class importer extends PyObject {

    enum EntryType {
        IS_SOURCE, IS_BYTECODE, IS_PACKAGE
    }
    /** SearchOrder defines how we search for a module. */
    final SearchOrderEntry[] searchOrder;

    public importer(PyType subType) {
        super(subType);
        searchOrder = makeSearchOrder();
    }

    public importer() {
        searchOrder = makeSearchOrder();
    }

    /**
     * Return the bytes for the data located at <code>path</code>.
     */
    public abstract String get_data(String path);

    /**
     * Returns the value to fill in __path__ on a module with the given full module name created by
     * this importer.
     */
    protected abstract String makePackagePath(String fullname);

    /**
     * Given a full module name, return the potential file path in the archive (without extension).
     */
    protected abstract String makeFilename(String fullname);

    /**
     * Given a full module name, return the potential file path including the archive (without
     * extension).
     */
    protected abstract String makeFilePath(String fullname);

    /**
     * Returns an entry for a filename from makeFilename with a potential suffix such that this
     * importer can make a bundle with it, or null if fullFilename doesn't exist in this importer.
     */
    protected abstract String makeEntry(String filenameAndSuffix);

    /**
     * Returns a Bundle for fullFilename and entry, the result from a makeEntry call for
     * fullFilename.
     */
    protected abstract Bundle makeBundle(String entry);

    private SearchOrderEntry[] makeSearchOrder(){
        return new SearchOrderEntry[] {
            new SearchOrderEntry(EntryType.IS_PACKAGE, EntryType.IS_BYTECODE),
            new SearchOrderEntry(EntryType.IS_PACKAGE, EntryType.IS_SOURCE),
            new SearchOrderEntry(EntryType.IS_BYTECODE),
            new SearchOrderEntry(EntryType.IS_SOURCE)};
    }

    protected final PyObject importer_find_module(String fullname, String path) {
        ModuleInfo moduleInfo = getModuleInfo(fullname);
        if (moduleInfo.notFound()) {
            return Py.None;
        }
        return this;
    }

    protected final PyObject importer_find_spec(String fullname) {
        PyObject moduleSpec = Py.getSystemState().importlib.__findattr__("ModuleSpec");
        PyObject spec = moduleSpec.__call__(new PyUnicode(fullname), this);
        ModuleInfo info = getModuleInfo(fullname);
        if (info.notFound()) {
            return null;
        }
        if (info.isPackage()) {
            PyList pkgpath = new PyList();
            pkgpath.add(makePackagePath(fullname));
            spec.__setattr__("submodule_search_locations", pkgpath);
            spec.__setattr__("is_package", Py.True);
        }
        spec.__setattr__("cached", new PyUnicode(info.getPath()));
        spec.__setattr__("origin", new PyUnicode(info.getSourcePath()));
        return spec;
    }

    protected final PyObject importer_load_module(String fullname) {
        ModuleCodeData moduleCodeData = getModuleCode(fullname);
        if (moduleCodeData == null) {
            return Py.None;
        }
        // the module *must* be in sys.modules before the loader executes the module code; the
        // module code may (directly or indirectly) import itself
        PyModule mod = imp.addModule(fullname);
        mod.__dict__.__setitem__("__loader__", this);
        if (moduleCodeData.isPackage) {
            // add __path__ to the module *before* the code gets executed
            PyList pkgpath = new PyList();
            pkgpath.add(makePackagePath(fullname));
            mod.__dict__.__setitem__("__path__", pkgpath);
        }
        imp.createFromCode(fullname, moduleCodeData.code);
        Py.writeDebug("import", "import " + fullname + " # loaded from " + moduleCodeData.path);
        return mod;
    }

    /**
     * @param fullname
     *            the fully qualified name of the module
     * @return whether the module is a package
     */
    protected final boolean importer_is_package(String fullname) {
        ModuleInfo info = getModuleInfo(fullname);
        if (info.notFound()) {
            throw Py.ImportError(String.format("module %s not found", fullname));
        }
        return info.isPackage();
    }

    /**
     * Bundle is an InputStream, bundled together with a method that can close the input stream and
     * whatever resources are associated with it when the resource is imported.
     */
    protected abstract static class Bundle implements AutoCloseable {
        public InputStream inputStream;

        public Bundle(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        /**
         * Close the underlying resource if necessary. Raises an IOError if a problem occurs.
         */
        @Override
        public abstract void close();
    }

    /**
     * Given a path to a compiled file in the archive, return the modification time of the
     * matching .py file.
     *
     * @param path to the compiled file
     * @return long mtime of the .py, or -1 if no source is available
     */
    protected abstract long getSourceMtime(String path);

    /**
     * Return module information for the module with the fully qualified name.
     *
     * @param fullname
     *            the fully qualified name of the module
     * @return the module's information
     */
    protected final ModuleInfo getModuleInfo(String fullname) {
        String path = makeFilename(fullname);
        String filePath = makeFilePath(fullname);

        for (SearchOrderEntry entry : searchOrder) {
            String entryPath = filePath + entry.searchPath(path);
            String importEntry = makeEntry(entryPath);
            if (importEntry == null) {
                continue;
            }
            return new ModuleInfo(fullname, entryPath, filePath + entry.sourcePath(path), entry);
        }
        return ModuleInfo.NOT_FOUND;
    }

    /**
     * Return the code object and its associated data for the module with the fully qualified name.
     *
     * @param fullname
     *            the fully qualified name of the module
     * @return the module's ModuleCodeData object
     */
    protected final ModuleCodeData getModuleCode(String fullname) {
        ModuleInfo moduleInfo = getModuleInfo(fullname);
        if (moduleInfo.notFound()) {
            throw Py.ImportError(String.format("can't find module '%s'", fullname), fullname);
        }

        String searchPath = moduleInfo.getPath();
        long mtime = -1;
        if (moduleInfo.isBytecode()) {
            mtime = getSourceMtime(searchPath);
        }

        Bundle bundle = makeBundle(searchPath);
        byte[] codeBytes;
        try {
            if (moduleInfo.isBytecode()) {
                try {
                    codeBytes = imp.readCode(fullname, bundle.inputStream, true, mtime);
                } catch (IOException ioe) {
                    throw Py.ImportError(ioe.getMessage() + "[path=" + searchPath + "]");
                }
            } else {
                codeBytes = imp.compileSource(fullname, bundle.inputStream, searchPath);
            }
        } finally {
            bundle.close();
        }

        PyCode code = BytecodeLoader.makeCode(fullname + Version.PY_CACHE_TAG, codeBytes, moduleInfo.getSourcePath());
        return new ModuleCodeData(code, moduleInfo.isPackage(), searchPath);
    }

    /**
     * Container for PyModule code - whether or not it's a package - and its path.
     */
    protected class ModuleCodeData {
        public PyCode code;
        public boolean isPackage;
        public String path;

        public ModuleCodeData(PyCode code, boolean isPackage, String path) {
            this.code = code;
            this.isPackage = isPackage;
            this.path = path;
        }
    }

    /**
     * A step in the module search order: the file suffix and its entry type.
     */
    protected static class SearchOrderEntry {
        public EnumSet<EntryType> types;

        public SearchOrderEntry(EntryType type, EntryType... types) {
            this.types = EnumSet.of(type, types);
        }

        public String sourcePath(String name) {
            StringBuilder ret = new StringBuilder();
            ret.append(name);
            if (isPackage()) {
                ret.append(File.separatorChar);
                ret.append("__init__");
            }
            return ret.append(".py").toString();
        }

        public String searchPath(String name) {
            if (isSource()) return sourcePath(name);
            StringBuilder ret = new StringBuilder();
            if (isPackage()) {
                ret.append(name);
                ret.append(File.separatorChar);
                ret.append(imp.CACHEDIR);
                ret.append(File.separatorChar);
                ret.append("__init__");
            } else {
                ret.append(imp.CACHEDIR);
                ret.append(File.separatorChar);
                ret.append(name);
            }
            return ret.append(Version.PY_CACHE_TAG).append(".class").toString();
        }

        private boolean isPackage() {
            return types.contains(EntryType.IS_PACKAGE);
        }

        private boolean isBytecode() {
            return types.contains(EntryType.IS_BYTECODE);
        }

        private boolean isSource() {
            return !isBytecode();
        }
    }

    /**
     * ModuleInfo is the blueprint for the importlib.machinery.ModuleSpec from PEP-451
     */
    protected static class ModuleInfo {
        private String name;
        private String path;
        private String sourcePath;
        private SearchOrderEntry entry;

        public static final ModuleInfo NOT_FOUND = new ModuleInfo();

        private ModuleInfo() {
            name = null;
        }

        public ModuleInfo(String name, String path, String sourcePath, SearchOrderEntry searchEntry) {
            this.name = name;
            this.path = path;
            this.sourcePath = sourcePath;
            this.entry = searchEntry;
        }

        public boolean isPackage() {
            return entry.isPackage();
        }

        public boolean notFound() {
            return name == null;
        }

        public boolean isBytecode() {
            return entry.isBytecode();
        }

        public String getPath() {
            return path;
        }

        public String getSourcePath() {
            return sourcePath;
        }
    }
}
