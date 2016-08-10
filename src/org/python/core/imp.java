// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.python.Version;
import org.python.compiler.Module;
import org.python.core.util.FileUtil;
import org.python.core.util.PlatformUtil;
import org.python.modules.Setup;

/**
 * Utility functions for "import" support.
 *
 * Note that this class tries to match the names of the corresponding functions from CPython's
 * Python/import.c. In these cases we use CPython's function naming style (underscores and all
 * lowercase) instead of Java's typical camelCase style so that it's easier to compare with
 * import.c.
 */
public class imp {
    private static final String importlib_filename = "_bootstrap.py";
    private static final String external_filename = "_bootstrap_external.py";
    private static final String remove_frames = "_call_with_frames_removed";
    public static final String CACHEDIR = "__pycache__";

    private static final String IMPORT_LOG = "import";

    private static final String UNKNOWN_SOURCEFILE = "<unknown>";

    private static final int APIVersion = 37;

    public static final int NO_MTIME = -1;

    public static final int DEFAULT_LEVEL = 0;

    public static class CodeData {

        private final byte[] bytes;
        private final long mtime;
        private final String filename;

        public CodeData(byte[] bytes, long mtime, String filename) {
            this.bytes = bytes;
            this.mtime = mtime;
            this.filename = filename;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public long getMTime() {
            return mtime;
        }

        public String getFilename() {
            return filename;
        }
    }

    public static enum CodeImport {
        source, compiled_only;
    }

    /** A non-empty fromlist for __import__'ing sub-modules. */
    private static final PyObject nonEmptyFromlist = new PyTuple(Py.newUnicode("__doc__"));

    public static ClassLoader getSyspathJavaLoader() {
        return Py.getSystemState().getSyspathJavaLoader();
    }

    /**
     * Selects the parent class loader for Jython, to be used for dynamically loaded classes and
     * resources. Chooses between the current and context classloader based on the following
     * criteria:
     *
     * <ul>
     * <li>If both are the same classloader, return that classloader.
     * <li>If either is null, then the non-null one is selected.
     * <li>If both are not null, and a parent/child relationship can be determined, then the child
     * is selected.
     * <li>If both are not null and not on a parent/child relationship, then the current class
     * loader is returned (since it is likely for the context class loader to <b>not</b> see the
     * Jython classes)
     * </ul>
     *
     * @return the parent class loader for Jython or null if both the current and context
     *         classloaders are null.
     */
    public static ClassLoader getParentClassLoader() {
        ClassLoader current = imp.class.getClassLoader();
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context == current) {
            return current;
        }
        if (context == null) {
            return current;
        }
        if (current == null) {
            return context;
        }
        if (isParentClassLoader(context, current)) {
            return current;
        }
        if (isParentClassLoader(current, context)) {
            return context;
        }
        return current;
    }

    private static boolean isParentClassLoader(ClassLoader suspectedParent, ClassLoader child) {
        try {
            ClassLoader parent = child.getParent();
            if (suspectedParent == parent) {
                return true;
            }
            if (parent == null || parent == child) {
                // We reached the boot class loader
                return false;
            }
            return isParentClassLoader(suspectedParent, parent);

        } catch (SecurityException e) {
            return false;
        }
    }

    private imp() {}

    /**
     * If the given name is found in sys.modules, the entry from there is returned. Otherwise a new
     * PyModule is created for the name and added to sys.modules
     */
    public static PyModule addModule(String name) {
        name = name.intern();
        PyObject modules = Py.getSystemState().modules;
        PyModule module = (PyModule)modules.__finditem__(name);
        if (module != null) {
            return module;
        }
        module = new PyModule(name, null);
        PyModule __builtin__ = (PyModule)modules.__finditem__("__builtin__");
        PyObject __dict__ = module.__getattr__("__dict__");
        __dict__.__setitem__("__builtins__", __builtin__.__getattr__("__dict__"));
        __dict__.__setitem__("__package__", Py.None);
        modules.__setitem__(name, module);
        return module;
    }

    /**
     * Remove name form sys.modules if it's there.
     *
     * @param name the module name
     */
    private static void removeModule(String name) {
        name = name.intern();
        PyObject modules = Py.getSystemState().modules;
        if (modules.__finditem__(name) != null) {
            try {
                modules.__delitem__(name);
            } catch (PyException pye) {
                // another thread may have deleted it
                if (!pye.match(Py.KeyError)) {
                    throw pye;
                }
            }
        }
    }

    private static byte[] readBytes(InputStream fp) {
        try {
            return FileUtil.readBytes(fp);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        } finally {
            try {
                fp.close();
            } catch (IOException e) {
                throw Py.IOError(e);
            }
        }
    }

    private static InputStream makeStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    static PyObject createFromPyClass(String name, InputStream fp, boolean testing,
            String sourceName, String compiledName) {
        return createFromPyClass(name, fp, testing, sourceName, compiledName, NO_MTIME);

    }

    static PyObject createFromPyClass(String name, InputStream fp, boolean testing,
            String sourceName, String compiledName, long mtime) {
        return createFromPyClass(name, fp, testing, sourceName, compiledName, mtime,
                CodeImport.source);
    }

    static PyObject createFromPyClass(String name, InputStream fp, boolean testing,
            String sourceName, String compiledName, long mtime, CodeImport source) {
        CodeData data = null;
        try {
            data = readCodeData(compiledName, fp, testing, mtime);
        } catch (IOException ioe) {
            if (!testing) {
                throw Py.ImportError(ioe.getMessage() + "[name=" + name + ", source=" + sourceName
                        + ", compiled=" + compiledName + "]", name);
            }
        }
        if (testing && data == null) {
            return null;
        }
        PyCode code;
        try {
            code = BytecodeLoader.makeCode(name + Version.PY_CACHE_TAG, data.getBytes(), //
                    source == CodeImport.compiled_only ? data.getFilename() : sourceName);
        } catch (Throwable t) {
            if (testing) {
                return null;
            } else {
                throw Py.JavaError(t);
            }
        }

        Py.writeComment(IMPORT_LOG,
                String.format("import %s # precompiled from %s", name, compiledName));
        return createFromCode(name, code);
    }

    public static byte[] readCode(String name, InputStream fp, boolean testing) throws IOException {
        return readCode(name, fp, testing, NO_MTIME);
    }

    public static byte[] readCode(String name, InputStream fp, boolean testing, long mtime)
            throws IOException {
        CodeData data = readCodeData(name, fp, testing, mtime);
        if (data == null) {
            return null;
        } else {
            return data.getBytes();
        }
    }

    public static CodeData readCodeData(String name, InputStream fp, boolean testing)
            throws IOException {
        return readCodeData(name, fp, testing, NO_MTIME);
    }

    public static CodeData readCodeData(String name, InputStream fp, boolean testing, long mtime)
            throws IOException {
        byte[] data = readBytes(fp);
        int api;
        AnnotationReader ar = new AnnotationReader(data);
        api = ar.getVersion();
        if (api != APIVersion) {
            if (testing) {
                return null;
            } else {
                String fmt = "compiled unit contains version %d code (%d required): %.200s";
                throw Py.ImportError(String.format(fmt, api, APIVersion, name), name);
            }
        }
        if (testing && mtime != NO_MTIME) {
            long time = ar.getMTime();
            if (mtime != time) {
                return null;
            }
        }
        return new CodeData(data, mtime, ar.getFilename());
    }

    public static byte[] compileSource(String name, File file) {
        return compileSource(name, file, null);
    }

    public static byte[] compileSource(String name, File file, String sourceFilename) {
        return compileSource(name, file, sourceFilename, null);
    }

    public static byte[] compileSource(String name, File file, String sourceFilename,
            String compiledFilename) {
        if (sourceFilename == null) {
            sourceFilename = file.toString();
        }
        long mtime = file.lastModified();
        return compileSource(name, makeStream(file), sourceFilename, mtime);
    }

    public static String makeCompiledFilename(String filename) {
        Path source = Paths.get(filename);
        Path base = source.getParent();
        Path file = source.getFileName();
        String classPath = file.toString().substring(0, file.toString().length() - 3) + Version.PY_CACHE_TAG + ".class";
        return base.resolve(Paths.get(imp.CACHEDIR, classPath)).toString();
    }

    /**
     * Stores the bytes in compiledSource in compiledFilename.
     *
     * If compiledFilename is null, it's set to the results of makeCompiledFilename(sourcefileName).
     *
     * If sourceFilename is null or set to UNKNOWN_SOURCEFILE, then null is returned.
     *
     * @return the compiledFilename eventually used; or null if a compiledFilename couldn't be
     *         determined or if an error was thrown while writing to the cache file.
     */
    public static String cacheCompiledSource(String sourceFilename, String compiledFilename,
            byte[] compiledSource) {
        if (compiledFilename == null) {
            if (sourceFilename == null || sourceFilename.equals(UNKNOWN_SOURCEFILE)) {
                return null;
            }
            compiledFilename = makeCompiledFilename(sourceFilename);
        }
        FileOutputStream fop = null;
        try {
            SecurityManager man = System.getSecurityManager();
            if (man != null) {
                man.checkWrite(compiledFilename);
            }
            File compiledFile = new File(compiledFilename);
            File parent = compiledFile.getParentFile();
            parent.mkdirs(); // makesure __pycache__ exists
            fop = new FileOutputStream(compiledFile);
            fop.write(compiledSource);
            fop.close();
            return compiledFilename;
        } catch (IOException exc) {
            // If we can't write the cache file, just log and continue
            Py.writeDebug(IMPORT_LOG, "Unable to write to source cache file '" + compiledFilename
                    + "' due to " + exc);
            return null;
        } catch (SecurityException exc) {
            // If we can't write the cache file, just log and continue
            Py.writeDebug(IMPORT_LOG, "Unable to write to source cache file '" + compiledFilename
                    + "' due to " + exc);
            return null;
        } finally {
            if (fop != null) {
                try {
                    fop.close();
                } catch (IOException e) {
                    Py.writeDebug(IMPORT_LOG, "Unable to close source cache file '"
                            + compiledFilename + "' due to " + e);
                }
            }
        }
    }

    public static byte[] compileSource(String name, InputStream fp, String filename) {
        return compileSource(name, fp, filename, NO_MTIME);
    }

    public static byte[] compileSource(String name, InputStream fp, String filename, long mtime) {
        ByteArrayOutputStream ofp = new ByteArrayOutputStream();
        ParserFacade.ExpectedEncodingBufferedReader bufReader = null;
        try {
            if (filename == null) {
                filename = UNKNOWN_SOURCEFILE;
            }
            org.python.antlr.base.mod node;
            CompilerFlags cflags = new CompilerFlags();
            bufReader = ParserFacade.prepBufReader(fp, cflags, filename, false);
            node = ParserFacade.parseOnly(bufReader, CompileMode.exec, filename, cflags);
            Module.compile(node, ofp, name + Version.PY_CACHE_TAG, filename, true, false, null, mtime);
            return ofp.toByteArray();
        } catch (Throwable t) {
            throw ParserFacade.fixParseError(bufReader, t, filename);
        } finally {
            try {
                bufReader.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static PyObject createFromSource(String name, InputStream fp, String filename) {
        return createFromSource(name, fp, filename, null, NO_MTIME);
    }

    public static PyObject createFromSource(String name, InputStream fp, String filename,
            String outFilename) {
        return createFromSource(name, fp, filename, outFilename, NO_MTIME);
    }

    public static PyObject createFromSource(String name, InputStream fp, String filename,
            String outFilename, long mtime) {
        byte[] bytes = compileSource(name, fp, filename, mtime);
        if (!Py.getSystemState().dont_write_bytecode) {
            outFilename = cacheCompiledSource(filename, outFilename, bytes);
        }

        Py.writeComment(IMPORT_LOG, "'" + name + "' as " + filename);

        PyCode code = BytecodeLoader.makeCode(name + Version.PY_CACHE_TAG, bytes, filename);
        return createFromCode(name, code);
    }

    /**
     * Returns a module with the given name whose contents are the results of running c. Sets
     * __file__ on the module to be moduleLocation unless moduleLocation is null. If c comes from a
     * local .py file or compiled ".{cache_tag}.class" class moduleLocation should be the result of running new
     * File(moduleLocation).getAbsolutePath(). If c comes from a remote file or is a jar
     * moduleLocation should be the full uri for c.
     */
    public static PyObject createFromCode(String name, PyCode c) {
        PyUnicode.checkEncoding(name);
        PyModule module = addModule(name);

        if (!(c instanceof PyTableCode)) {
            throw Py.TypeError(String.format("expected TableCode, got %s", c.getType().fastGetName()));
        }
        ReentrantLock importLock = Py.getSystemState().getImportLock();
        importLock.lock();
        try {
            PyFrame f = new PyFrame((PyTableCode) c, module.__dict__, module.__dict__, null);
            c.call(Py.getThreadState(), f);
            return module;
        } catch (Throwable t) {
            removeModule(name);
            throw t;
        } finally {
            if (importLock.isLocked())
                importLock.unlock();
        }
    }

    static PyObject createFromClass(String name, Class<?> c) {
        // Two choices. c implements PyRunnable or c is Java package
        if (PyRunnable.class.isAssignableFrom(c)) {
            try {
                return createFromCode(name, ((PyRunnable)c.newInstance()).getMain());
            } catch (InstantiationException e) {
                throw Py.JavaError(e);
            } catch (IllegalAccessException e) {
                throw Py.JavaError(e);
            }
        }
        return PyType.fromClass(c, false); // xxx?
    }

    public static PyObject getImporter(PyObject p) {
        PySystemState sys = Py.getSystemState();
        return getPathImporter(sys.path_importer_cache, sys.path_hooks, p);
    }

    static PyObject getPathImporter(PyObject cache, PyList hooks, PyObject p) {

        // attempt to get an importer for the path
        // use null as default value since Py.None is
        // a valid value in the cache for the default
        // importer
        PyObject importer = cache.__finditem__(p);
        if (importer != null) {
            return importer;
        }

        // nothing in the cache, so check all hooks
        PyObject iter = hooks.__iter__();
        for (PyObject hook; (hook = iter.__next__()) != null;) {
            try {
                importer = hook.__call__(p);
                break;
            } catch (PyException e) {
                if (!e.match(Py.ImportError)) {
                    throw e;
                }
            }
        }

        if (importer == null) {
            try {
                importer = new PyNullImporter(p);
            } catch (PyException e) {
                if (!e.match(Py.ImportError)) {
                    throw e;
                }
            }
        }

        if (importer != null) {
            cache.__setitem__(p, importer);
        } else {
            importer = Py.None;
        }

        return importer;
    }

    // FIXME: This is deprecated, the use way is use the importlib machinery, which include a BuiltinImporter
    // Once all builtin modules are properly annotated and exposed, we can remove this
    public static PyObject loadBuiltin(String name) {
        if (name == "__builtin__" || name == "builtins") {
            Py.writeComment(IMPORT_LOG, "'" + name + "' as __builtin__ in builtin modules");
            return new PyModule("__builtin__", Py.getSystemState().builtins);
        }
        String mod = PySystemState.getBuiltin(name);
        if (mod != null) {
            Class c = Py.findClassEx(mod, "builtin modules");
            if (c != null) {
                Py.writeComment(IMPORT_LOG, "'" + name + "' as " + mod + " in builtin modules");
                try {
                    if (PyObject.class.isAssignableFrom(c)) { // xxx ok?
                        return PyType.fromClass(c);
                    }
                    return createFromClass(name, c);
                } catch (NoClassDefFoundError e) {
                    throw Py.ImportError("Cannot import " + name + ", missing class " + c.getName(), name);
                }
            }
        }
        return null;
    }

    private final static PyTuple all = new PyTuple(Py.newUnicode('*'));

    /**
     * Called from jython generated code when a statement like "from spam.eggs import *" is
     * executed.
     */
    public static void importAll(String mod, PyFrame frame, int level) {
        PyObject module = importName(mod, false, frame.f_globals, all, level);
        importAll(module, frame);
    }

    public static void importAll(PyObject module, PyFrame frame) {
        PyObject names;
        boolean filter = true;
        if (module instanceof PyJavaPackage) {
            names = ((PyJavaPackage)module).fillDir();
        } else {
            PyObject __all__ = module.__findattr__("__all__");
            if (__all__ != null) {
                names = __all__;
                filter = false;
            } else {
                names = module.__dir__();
            }
        }

        loadNames(names, module, frame.getLocals(), filter);
    }

    /**
     * Called from jython generated code when a statement like "import spam" is executed.
     */
    public static PyObject importOne(String mod, PyFrame frame, int level) {
        return importName(mod, true, frame.f_globals, Py.None, level);
    }

    /**
     * Called from jython generated code when a statement like "import spam as foo" is executed.
     */
    public static PyObject importOneAs(String mod, PyFrame frame, int level) {
        return importName(mod, false, frame.f_globals, Py.None, level);
    }

    public static PyObject loadFromCompiled(String name, InputStream stream, String sourceName,
            String compiledName) {
        return createFromPyClass(name, stream, false, sourceName, compiledName);
    }

    public static boolean caseok(File file, String filename) {
        if (Options.caseok || !PlatformUtil.isCaseInsensitive()) {
            return true;
        }
        try {
            File canFile = new File(file.getCanonicalPath());
            boolean match = filename.regionMatches(0, canFile.getName(), 0, filename.length());
            if (!match) {
                // possibly a symlink. Get parent and look for exact match in listdir()
                // This is what CPython does in the case of Mac OS X and Cygwin.
                // XXX: This will be a performance hit, maybe jdk7 nio2 can give us a better
                // method?
                File parent = file.getParentFile();
                String[] children = parent.list();
                for (String c : children) {
                    if (c.equals(filename)) {
                        return true;
                    }
                }
            }
            return match;
        } catch (IOException exc) {
            return false;
        }
    }

    /**
     * Load the module by name. Upon loading the module it will be added to sys.modules.
     *
     * @param name the name of the module to load
     * @return the loaded module
     */
    public static PyObject load(String name) {
        PyUnicode.checkEncoding(name);
        ReentrantLock importLock = Py.getSystemState().getImportLock();
        importLock.lock();
        try {
            return import_first(name, new StringBuilder());
        } finally {
            if (importLock.isLocked())
                importLock.unlock();
        }
    }

    /**
     * Find the parent package name for a module.
     *
     * If __name__ does not exist in the module or if level is <code>0</code>, then the parent is
     * <code>null</code>. If __name__ does exist and is not a package name, the containing package
     * is located. If no such package exists and level is <code>-1</code>, the parent is
     * <code>null</code>. If level is <code>-1</code>, the parent is the current name. Otherwise,
     * <code>level-1</code> doted parts are stripped from the current name. For example, the
     * __name__ <code>"a.b.c"</code> and level <code>2</code> would return <code>"a.b"</code>, if
     * <code>c</code> is a package and would return <code>"a"</code>, if <code>c</code> is not a
     * package.
     *
     * @param dict the __dict__ of a loaded module
     * @param level used for relative and absolute imports. -1 means try both, 0 means absolute
     *            only, positive ints represent the level to look upward for a relative path (1
     *            means current package, 2 means one level up). See PEP 328 at
     *            http://www.python.org/dev/peps/pep-0328/
     *
     * @return the parent name for a module
     */
    private static String get_parent(PyObject dict, int level) {
        String modname;
        int orig_level = level;

        if ((dict == null && level == -1) || level == 0) {
            // try an absolute import
            return null;
        }

        PyObject tmp = dict.__finditem__("__package__");
        if (tmp != null && tmp != Py.None) {
            if (!Py.isInstance(tmp, PyUnicode.TYPE)) {
                throw Py.ValueError("__package__ set to non-string");
            }
            modname = ((PyUnicode)tmp).getString();
        } else {
            // __package__ not set, so figure it out and set it.

            tmp = dict.__finditem__("__name__");
            if (tmp == null) {
                return null;
            }
            modname = tmp.toString();

            // locate the current package
            tmp = dict.__finditem__("__path__");
            if (tmp instanceof PyList) {
                // __path__ is set, so modname is already the package name.
                dict.__setitem__("__package__", new PyUnicode(modname));
            } else {
                // __name__ is not a package name, try one level upwards.
                int dot = modname.lastIndexOf('.');
                if (dot == -1) {
                    if (level <= -1) {
                        // there is no package, perform an absolute search
                        dict.__setitem__("__package__", Py.None);
                        return null;
                    }
                    throw Py.ValueError("Attempted relative import in non-package");
                }
                // modname should be the package name.
                modname = modname.substring(0, dot);
                dict.__setitem__("__package__", new PyUnicode(modname));
            }
        }

        // walk upwards if required (level >= 2)
        while (level-- > 1) {
            int dot = modname.lastIndexOf('.');
            if (dot == -1) {
                throw Py.ValueError("Attempted relative import beyond toplevel package");
            }
            modname = modname.substring(0, dot);
        }

        if (Py.getSystemState().modules.__finditem__(modname) == null) {
            if (orig_level < 1) {
                if (modname.length() > 0) {
                    Py.warning(Py.RuntimeWarning, String.format("Parent module '%.200s' not found "
                            + "while handling absolute import", modname));
                }
            } else {
                throw Py.SystemError(String.format("Parent module '%.200s' not loaded, "
                        + "cannot perform relative import", modname));
            }
        }
        return modname.intern();
    }

    /**
     *
     * @param mod a previously loaded module
     * @param parentNameBuffer
     * @param name the name of the module to load
     * @return null or None
     */
    private static PyObject import_next(PyObject mod, StringBuilder parentNameBuffer, String name,
            String outerFullName, PyObject fromlist) {
        if (parentNameBuffer.length() > 0 && name != null && name.length() > 0) {
            parentNameBuffer.append('.');
        }
        parentNameBuffer.append(name);

        String fullName = parentNameBuffer.toString().intern();

        PySystemState sys = Py.getSystemState();
        PyObject modules = sys.modules;
        PyObject ret = modules.__finditem__(fullName);
        if (ret != null) {
            return ret;
        }
        try {
            return sys.importlib.invoke("_find_and_load", new PyUnicode(fullName), sys.builtins.__finditem__("__import__"));
        } catch (PyException pye) {
            /**
             * remove trackback that from '_bootstrap.py' or '_bootstrap_external.py' in case of ImportError
             * or marked with _call_with_frames_removed otherwise
             * FIXME: this works almost as good as CPython, but I (isaiah) am yet to find a way to remove the first frame,
             * as there is not pointer to pointer trick in java.
             */
            PyTraceback outer_link = null;
            PyTraceback base_tb = pye.traceback;
            PyTraceback tb = base_tb;
            PyTraceback prev_link = base_tb;
            boolean in_importlib = false;
            boolean always_trim = pye.match(Py.ImportError);
            while (tb != null) {
                PyTraceback next = (PyTraceback) tb.tb_next;
                PyBaseCode code = tb.tb_frame.f_code;
                boolean now_in_importlib = code.co_filename.equals(importlib_filename)
                        || code.co_filename.equals(external_filename);
                if (now_in_importlib && !in_importlib) {
                    outer_link = prev_link;
                }
                in_importlib = now_in_importlib;
                if (in_importlib && (always_trim || code.co_name.equals(remove_frames))) {
                    outer_link.tb_next = next;
                }
                prev_link = tb;
                tb = next;
            }
            throw pye;
        }
    }

    // never returns null or None
    private static PyObject import_first(String name, StringBuilder parentNameBuffer) {
        PyObject ret = import_next(null, parentNameBuffer, name, null, null);
        if (ret == null || ret == Py.None) {
            throw Py.ImportError("No module named " + name, name);
        }
        return ret;
    }

    private static PyObject import_first(String name, StringBuilder parentNameBuffer,
            String fullName, PyObject fromlist) {
        PyObject ret = import_next(null, parentNameBuffer, name, fullName, fromlist);
        if (ret == null || ret == Py.None) {
            if (JavaImportHelper.tryAddPackage(fullName, fromlist)) {
                ret = import_next(null, parentNameBuffer, name, fullName, fromlist);
            }
        }
        if (ret == null || ret == Py.None) {
            throw Py.ImportError("No module named " + name, name);
        }
        return ret;
    }

    // Hierarchy-recursively search for dotted name in mod;
    // never returns null or None
    // ??pending: check if result is really a module/jpkg/jclass?
    private static PyObject import_logic(PyObject mod, StringBuilder parentNameBuffer,
            String dottedName, String fullName, PyObject fromlist) {
        int dot = 0;
        int last_dot = 0;

        do {
            String name;
            dot = dottedName.indexOf('.', last_dot);
            if (dot == -1) {
                name = dottedName.substring(last_dot);
            } else {
                name = dottedName.substring(last_dot, dot);
            }
            PyJavaPackage jpkg = null;
            if (mod instanceof PyJavaPackage) {
                jpkg = (PyJavaPackage)mod;
            }

            mod = import_next(mod, parentNameBuffer, name, fullName, fromlist);
            if (jpkg != null && (mod == null || mod == Py.None)) {
                // try again -- under certain circumstances a PyJavaPackage may
                // have been added as a side effect of the last import_next
                // attempt. see Lib/test_classpathimport.py#test_bug1126
                mod = import_next(jpkg, parentNameBuffer, name, fullName, fromlist);
            }
            if (mod == null || mod == Py.None) {
                throw Py.ImportError("No module named " + name, name);
            }
            last_dot = dot + 1;
        } while (dot != -1);

        return mod;
    }

    // PyImport_ImportModuleLevelObject
    private static PyObject importModuleLevelObject(String name, boolean top, PyObject modDict,
            PyObject fromlist, int level) {
        PyObject pkg, spec;
        if (level < 0) {
            throw Py.ValueError("level must be >= 0");
        } else if (level > 0) {
            pkg = modDict.__findattr__("__package__");
            spec = modDict.__findattr__("__spec__");
            if (pkg != null && pkg != Py.None) {
                if (!(pkg instanceof PyUnicode)) {
                    throw Py.TypeError("package must be a string");
                } else if (spec != null && spec != Py.None) {
                    PyObject parent = spec.__getattr__("parent");
                    int eq = parent._cmp(pkg);
                    if (eq == 0) {
                        Py.ImportWarning("__package__ != __spec__.parent");
                    }
                }
            } else if (spec != null && spec != Py.None) {
                pkg = spec.__getattr__("parent");
            } else {
                Py.ImportWarning("can't resolve package from __spec or __package__, failing back on __name__ and __path__");
                pkg = modDict.__findattr__("__name__");
                if (pkg == null) {
                    throw Py.KeyError("'__name__' not in globals");
                }
                if ((pkg instanceof PyUnicode)) {
                    throw Py.TypeError("package must be a string");
                }
                if (modDict.__findattr__("__path__") == null) {
                    int dot;
                    //XXX
                }
            }
        }
        return null;
    }

    /**
     * @param name qualified name of module
     * @param top whether to return the top module
     * @param modDict Global namespace
     * @return a module
     */
    private static PyObject import_module_level(String name, boolean top, PyObject modDict,
            PyObject fromlist, int level) {
        if (name.length() == 0) {
            if (level == 0 || modDict == null) {
                throw Py.ValueError("Empty module name");
            } else {
                PyObject moduleName = modDict.__findattr__("__package__");
                if (moduleName != null && moduleName.toString().equals("__name__")) {
                    throw Py.ValueError("Attempted relative import in non-package");
                }
            }
        }
        if (name.indexOf(File.separatorChar) != -1) {
            throw Py.ImportError("Import by filename is not supported.");
        }
        PyObject modules = Py.getSystemState().modules;
        PyObject pkgMod = null;
        String pkgName = null;
        if (modDict != null && modDict.isMappingType()) {
            pkgName = get_parent(modDict, level);
            pkgMod = modules.__finditem__(pkgName);
            if (pkgMod != null && !(pkgMod instanceof PyModule)) {
                pkgMod = null;
            }
        }
        int dot = name.indexOf('.');
        String firstName;
        if (dot == -1) {
            firstName = name;
        } else {
            firstName = name.substring(0, dot);
        }
        StringBuilder parentNameBuffer = new StringBuilder(pkgMod != null ? pkgName : "");
        PyObject topMod = import_next(pkgMod, parentNameBuffer, firstName, name, fromlist);
        if (topMod == Py.None || topMod == null) {
            // Add None to sys.modules for submodule or subpackage names that aren't found, but
            // leave top-level entries out. This allows them to be tried again if another
            // import attempt is made after they've been added to sys.path.
            if (topMod == null && pkgMod != null) {
                modules.__setitem__(parentNameBuffer.toString().intern(), Py.None);
            }
            parentNameBuffer = new StringBuilder();
            // could throw ImportError
            if (level > 0) {
                topMod = import_first(pkgName + "." + firstName, parentNameBuffer, name, fromlist);
            } else {
                topMod = import_first(firstName, parentNameBuffer, name, fromlist);
            }
        }
        PyObject mod = topMod;
        if (dot != -1) {
            // could throw ImportError
            mod = import_logic(topMod, parentNameBuffer, name.substring(dot + 1), name, fromlist);
        }
        if (top) {
            return topMod;
        }

        if (fromlist != null && fromlist != Py.None) {
            ensureFromList(mod, fromlist, parentNameBuffer.toString());
        }
        return mod;
    }

    private static void ensureFromList(PyObject mod, PyObject fromlist, String name) {
        ensureFromList(mod, fromlist, name, false);
    }

    private static void ensureFromList(PyObject mod, PyObject fromlist, String name,
            boolean recursive) {
        if (mod.__findattr__("__path__") == null) {
            return;
        }

        // This can happen with imports like "from . import foo"
        if (name.length() == 0) {
            name = mod.__findattr__("__name__").toString();
        }

        StringBuilder modNameBuffer = new StringBuilder(name);
        for (PyObject item : fromlist.asIterable()) {
            if (!Py.isInstance(item, PyUnicode.TYPE)) {
                throw Py.TypeError("Item in ``from list'' not a string");
            }
            if (item.toString().equals("*")) {
                if (recursive) {
                    // Avoid endless recursion
                    continue;
                }
                PyObject all;
                if ((all = mod.__findattr__("__all__")) != null) {
                    ensureFromList(mod, all, name, true);
                }
            } else if (mod.__findattr__((PyUnicode)item) == null) {
                String fullName = modNameBuffer.append(".").append(item.toString()).toString();
                import_next(mod, new StringBuilder(name), item.toString(), fullName, null);
            }
        }
    }

    /**
     * Import a module by name.
     *
     * @param name the name of the package to import
     * @param top if true, return the top module in the name, otherwise the last
     * @return an imported module (Java or Python)
     */
    public static PyObject importName(String name, boolean top) {
        PyUnicode.checkEncoding(name);
        ReentrantLock importLock = Py.getSystemState().getImportLock();
        importLock.lock();
        try {
            return import_module_level(name, top, null, null, DEFAULT_LEVEL);
        } finally {
            if (importLock.isLocked())
                importLock.unlock();
        }
    }

    /**
     * Import a module by name. This is the default call for __builtin__.__import__.
     *
     * @param name the name of the package to import
     * @param top if true, return the top module in the name, otherwise the last
     * @param modDict the __dict__ of an already imported module
     * @return an imported module (Java or Python)
     */
    public static PyObject importName(String name, boolean top, PyObject modDict,
            PyObject fromlist, int level) {
        PyUnicode.checkEncoding(name);
        ReentrantLock importLock = Py.getSystemState().getImportLock();
        importLock.lock();
        try {
            return import_module_level(name, top, modDict, fromlist, level);
        } finally {
            if (importLock.isLocked())
                importLock.unlock();
        }
    }

    /**
     * replaced by importFrom with level param. Kept for backwards compatibility.
     *
     * @deprecated use importFrom with level param.
     */
    @Deprecated
    public static PyObject[] importFrom(String mod, String[] names, PyFrame frame) {
        return importFromAs(mod, names, null, frame, DEFAULT_LEVEL);
    }

    /**
     * Called from jython generated code when a statement like "from spam.eggs import foo, bar" is
     * executed.
     */
    public static PyObject[] importFrom(String mod, String[] names, PyFrame frame, int level) {
        return importFromAs(mod, names, null, frame, level);
    }

    /**
     * replaced by importFromAs with level param. Kept for backwards compatibility.
     *
     * @deprecated use importFromAs with level param.
     */
    @Deprecated
    public static PyObject[] importFromAs(String mod, String[] names, PyFrame frame) {
        return importFromAs(mod, names, null, frame, DEFAULT_LEVEL);
    }

    /**
     * Called from jython generated code when a statement like "from spam.eggs import foo as spam"
     * is executed.
     */
    public static PyObject[] importFromAs(String mod, String[] names, String[] asnames,
            PyFrame frame, int level) {
        PyObject module;
        ReentrantLock importLock = Py.getSystemState().getImportLock();
        importLock.lock();
        try {
            PyObject[] fromList = new PyObject[names.length];
            for (int i = 0; i < names.length; i++) {
                fromList[i] = new PyUnicode(names[i]);
            }
            module = import_module_level(mod, false, frame.f_globals, new PyTuple(fromList), level);
        } finally {
            if (importLock.isLocked())
                importLock.unlock();
        }
        PyObject[] submods = new PyObject[names.length];
        for (int i = 0; i < names.length; i++) {
            PyObject submod = module.__findattr__(names[i]);

            if (submod == null) {
                throw Py.ImportError("cannot import name " + names[i], names[i]);
            }
            submods[i] = submod;
        }
        return submods;
    }

    /**
     * From a module, load the attributes found in <code>names</code> into locals.
     *
     * @param filter if true, if the name starts with an underscore '_' do not add it to locals
     * @param locals the namespace into which names will be loaded
     * @param names the names to load from the module
     * @param module the fully imported module
     */
    private static void loadNames(PyObject names, PyObject module, PyObject locals, boolean filter) {
        for (PyObject name : names.asIterable()) {
            String sname = ((PyUnicode)name).internedString();
            if (filter && sname.startsWith("_")) {
                continue;
            } else {
                try {
                    PyObject value = module.__findattr__(sname);
                    if (value == null) {
                        PyObject nameObj = module.__findattr__("__name__");
                        if (nameObj != null) {
                            String submodName = nameObj.__str__().toString() + '.' + sname;
                            value =
                                    __builtin__
                                            .__import__(submodName, null, null, nonEmptyFromlist);
                        }
                    }
                    locals.__setitem__(sname, value);
                } catch (Exception exc) {
                    continue;
                }
            }
        }
    }

    public static int getAPIVersion() {
        return APIVersion;
    }
}
