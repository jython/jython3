
package org.python.modules;

import org.python.Version;
import org.python.core.PyCode;
import org.python.core.PyStringMap;
import org.python.core.__builtin__;
import org.python.core.Py;
import org.python.core.PyFile;
import org.python.core.PyList;
import org.python.core.PyModule;
import org.python.core.PyObject;
import org.python.core.PyUnicode;
import org.python.core.PySystemState;
import org.python.core.PyTuple;
import org.python.core.imp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;

/*
 * A bogus implementation of the CPython builtin module "imp".
 * Only the functions required by IDLE and PMW are implemented.
 * Luckily these function are also the only function that IMO can
 * be implemented under Jython.
 */

public class _imp {
    public static PyUnicode __doc__ = new PyUnicode(
        "This module provides the components needed to build your own\n"+
        "__import__ function.  Undocumented functions are obsolete.\n"
    );

    public static final int PY_SOURCE = 1;
    public static final int PY_COMPILED = 2;
    public static final int C_EXTENSION = 3;
    public static final int PKG_DIRECTORY = 5;
    public static final int C_BUILTIN = 6;
    public static final int PY_FROZEN = 7;
    public static final int IMP_HOOK = 9;

    private static class ModuleInfo {
        PyObject file;
        String filename;
        String suffix;
        String mode;
        int type;
        ModuleInfo(PyObject file, String filename, String suffix, String mode, int type) {
            this.file = file;
            this.filename = filename;
            this.suffix = suffix;
            this.mode = mode;
            this.type = type;
        }
    }

    private static PyObject newFile(File file) {
        try {
            return new PyFile(new FileInputStream(file));
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    private static boolean caseok(File file, String filename) {
        return org.python.core.imp.caseok(file, filename);
    }

    /**
     * This needs to be consolidated with the code in (@see org.python.core.imp).
     *
     * @param name module name
     * @param entry a path String
     * @param findingPackage if looking for a package only try to locate __init__
     * @return null if no module found otherwise module information
     */
    static ModuleInfo findFromSource(String name, String entry, boolean findingPackage,
                                     boolean preferSource) {
        String sourceName = "__init__.py";
        String compiledName = "__init__" + Version.PY_CACHE_TAG + ".class";
        String directoryName = PySystemState.getPathLazy(entry);
        // displayDirName is for identification purposes: when null it
        // forces java.io.File to be a relative path (e.g. foo/bar.py
        // instead of /tmp/foo/bar.py)
        String displayDirName = entry.equals("") ? null : entry;

        // First check for packages
        Path dir = findingPackage ? Paths.get(directoryName) : Paths.get(directoryName, name);
        File sourceFile = Paths.get(dir.toString(), sourceName).toFile();
        File compiledFile = Paths.get(dir.toString(), imp.CACHEDIR, compiledName).toFile();

        boolean pkg = dir.toFile().isDirectory() && caseok(dir.toFile(), name) && (sourceFile.isFile()
                                                                 || compiledFile.isFile());

        if(!findingPackage) {
            if(pkg) {
                return new ModuleInfo(Py.None, Paths.get(displayDirName, name).toString(),
                                      "", "", PKG_DIRECTORY);
            } else {
                Py.writeDebug("import", "trying source " + dir.toString());
                sourceName = name + ".py";
                compiledName = name + Version.PY_CACHE_TAG + ".class";
                sourceFile = new File(directoryName, sourceName);
                compiledFile = Paths.get(directoryName, imp.CACHEDIR, compiledName).toFile();
            }
        }

        if (sourceFile.isFile() && caseok(sourceFile, sourceName)) {
            if (!preferSource && compiledFile.isFile() && caseok(compiledFile, compiledName)) {
                Py.writeDebug("import", "trying precompiled " + compiledFile.getPath());
                long pyTime = sourceFile.lastModified();
                long classTime = compiledFile.lastModified();
                if (classTime >= pyTime) {
                    return new ModuleInfo(newFile(compiledFile),
                                          new File(displayDirName, compiledName).getPath(),
                                          ".class", "rb", PY_COMPILED);
                }
            }
            return new ModuleInfo(newFile(sourceFile),
                                  new File(displayDirName, sourceName).getPath(),
                                  ".py", "r", PY_SOURCE);
        }

        // If no source, try loading precompiled
        Py.writeDebug("import", "trying " + compiledFile.getPath());
        if (compiledFile.isFile() && caseok(compiledFile, compiledName)) {
            return new ModuleInfo(newFile(compiledFile),
                    new File(displayDirName, compiledName).getPath(),
                                  ".class", "rb", PY_COMPILED);
        }
        return null;
    }

    public static PyObject load_dynamic(String name, String pathname) {
        return load_dynamic(name, pathname, null);
    }

    public static PyObject load_dynamic(String name, String pathname, PyObject file) {
        throw Py.ImportError("No module named " + name, name);
    }

    public static PyObject load_source(String modname, String filename) {
        return load_source(modname, filename, null);
    }

    public static PyObject load_source(String modname, String filename, PyObject file) {
        PyObject mod = Py.None;
        if (file == null) {
            // XXX: This should load the accompanying byte code file instead, if it exists
            file = new PyFile(filename, "r", 1024);
        }
        Object o = file.__tojava__(InputStream.class);
        if (o == Py.NoConversion) {
            throw Py.TypeError("must be a file-like object");
        }
        PySystemState sys = Py.getSystemState();
        String compiledFilename =
                org.python.core.imp.makeCompiledFilename(sys.getPath(filename));
        mod = org.python.core.imp.createFromSource(modname.intern(), (InputStream)o,
                                                   filename, compiledFilename);
        PyObject modules = sys.modules;
        modules.__setitem__(modname.intern(), mod);
        return mod;
    }

    public static PyObject load_compiled(String name, String pathname) {
        return load_compiled(name, pathname, new PyFile(pathname, "rb", -1));
    }

    public static PyObject reload(PyObject module) {
        return __builtin__.reload(module);
    }

    public static PyObject load_compiled(String name, String pathname, PyObject file) {
        InputStream stream = (InputStream) file.__tojava__(InputStream.class);
        if (stream == Py.NoConversion) {
            throw Py.TypeError("must be a file-like object");
        }

        // XXX: Ideally we wouldn't care about sourceName here (see
        // http://bugs.jython.org/issue1605847 msg3805)
        String sourceName = pathname;
        if (sourceName.endsWith(Version.PY_CACHE_TAG +".class")) {
            sourceName = sourceName.substring(0, sourceName.length() - 6 - Version.PY_CACHE_TAG.length()) + ".py";
        }
        return org.python.core.imp.loadFromCompiled(name.intern(), stream, sourceName, pathname);
    }

    public static PyObject find_module(String name) {
        return find_module(name, Py.None);
    }

    public static PyObject find_module(String name, PyObject path) {
        if (path == Py.None && PySystemState.getBuiltin(name) != null) {
            return new PyTuple(Py.None, Py.newString(name),
                               new PyTuple(Py.EmptyString, Py.EmptyString,
                                           Py.newInteger(C_BUILTIN)));
        }

        if (path == Py.None) {
            path = Py.getSystemState().path;
        }
        for (PyObject p : path.asIterable()) {
            ModuleInfo mi = findFromSource(name, p.toString(), false, true);
            if(mi == null) {
                continue;
            }
            return new PyTuple(mi.file,
                               new PyUnicode(mi.filename),
                               new PyTuple(new PyUnicode(mi.suffix),
                                           new PyUnicode(mi.mode),
                                           Py.newLong(mi.type)));
        }
        throw Py.ImportError("No module named " + name, name);
    }

    public static PyObject load_module(String name, PyObject file, PyObject filename, PyTuple data) {
        PyObject mod = Py.None;
        PySystemState sys = Py.getSystemState();
        int type = data.__getitem__(2).asInt();
        while(mod == Py.None) {
            String compiledName;
            switch (type) {
                case PY_SOURCE:
                    Object o = file.__tojava__(InputStream.class);
                    if (o == Py.NoConversion) {
                        throw Py.TypeError("must be a file-like object");
                    }

                    // XXX: This should load the accompanying byte code file instead, if it exists
                    String resolvedFilename = sys.getPath(filename.toString());
                    compiledName = org.python.core.imp.makeCompiledFilename(resolvedFilename);
                    if (name.endsWith(".__init__")) {
                        name = name.substring(0, name.length() - ".__init__".length());
                    } else if (name.equals("__init__")) {
                        name = new File(sys.getCurrentWorkingDir()).getName();
                    }

                    File fp = new File(resolvedFilename);
                    long mtime = -1;
                    if (fp.isFile()) {
                        mtime = fp.lastModified();
                    }

                    mod = org.python.core.imp.createFromSource(name.intern(),
                                                               (InputStream)o,
                                                               filename.toString(),
                                                               compiledName,
                                                               mtime);
                    break;
                case PY_COMPILED:
                    mod = load_compiled(name, filename.toString(), file);
                    break;
                case PKG_DIRECTORY:
                    PyModule m = org.python.core.imp.addModule(name);
                    m.__dict__.__setitem__("__path__", new PyList(new PyObject[] {filename}));
                    m.__dict__.__setitem__("__file__", filename);
                    ModuleInfo mi = findFromSource(name, filename.toString(), true, true);
                    type = mi.type;
                    file = mi.file;
                    filename = new PyUnicode(mi.filename);
                    break;
                default:
                    throw Py.ImportError("No module named " + name, name);
            }
        }
        PyObject modules = sys.modules;
        modules.__setitem__(name.intern(), mod);
        return mod;
    }

    public static PyObject create_builtin(PyObject spec) {
        PyObject name = spec.__getattr__("name");
        String modName = name.toString();
        for (String newmodule : Setup.newbuiltinModules) {
            if (modName.equals(newmodule.split(":")[0]))
                return new PyModule(modName, new PyStringMap());
        }
        return imp.loadBuiltin(modName);
    }

    public static int exec_builtin(PyObject mod) {
        String name = mod.__findattr__("__name__").toString();
        String classname = null;
        for (String newmodule : Setup.newbuiltinModules) {
            if (name.equals(newmodule.split(":")[0])) {
                classname = className(newmodule);
                break;
            }
        }
        if (classname == null) {
            return 0;
        }
        Class c = Py.findClassEx(classname, "builtin modules");
        if (c != null) {
            try {
                Method clinit = c.getMethod("clinic", PyModule.class);
                clinit.invoke(null, (PyModule) mod);
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }
        return 0;
    }

    private static String className(String name) {
        String classname;
        String modname;

        int colon = name.indexOf(':');
        if (colon != -1) {
            // name:fqclassname
            modname = name.substring(0, colon).trim();
            classname = name.substring(colon + 1, name.length()).trim();
            if (classname.equals("null")) {
                // name:null, i.e. remove it
                classname = null;
            }
        } else {
            modname = name.trim();
            classname = "org.python.modules." + modname;
        }
        return classname + "$PyExposer";
    }

    private static PyObject addModuleObject(PyObject name) {
        return imp.load(name.asString());
    }

    public static PyObject get_magic() {
	return new PyUnicode("\u0003\u00f3\r\n");
    }
    
    public static PyObject get_suffixes() {
        return new PyList(new PyObject[] {new PyTuple(new PyUnicode(".py"),
                                                      new PyUnicode("r"),
                                                      Py.newLong(PY_SOURCE)),
                                          new PyTuple(new PyUnicode(Version.PY_CACHE_TAG + ".class"),
                                                      new PyUnicode("rb"),
                                                      Py.newLong(PY_COMPILED)),});
    }

    public static PyModule new_module(String name) {
        return new PyModule(name, null);
    }

    public static boolean is_builtin(String name) {
        if (name.equals("hello")) return true;
        return PySystemState.getBuiltin(name) != null;
    }

    public static PyObject _fix_co_filename(PyCode code, PyObject path) {
        return Py.None;
    }

    public static PyObject get_frozen_object(String name) {
        return null;
    }

    public static int init_frozen(String name) {
        return -1;
    }

    public static boolean is_frozen_package(String name) {
        return false;
    }

    public static boolean is_frozen(String name) {
        return false;
    }

    /**
     * Acquires the interpreter's import lock for the current thread.
     *
     * This lock should be used by import hooks to ensure
     * thread-safety when importing modules.
     *
     */
    public static void acquire_lock() {
        Py.getSystemState().getImportLock().lock();
    }

    /**
     * Release the interpreter's import lock.
     *
     */
    public static void release_lock() {
        try{
            Py.getSystemState().getImportLock().unlock();
        }catch(IllegalMonitorStateException e){
            throw Py.RuntimeError("not holding the import lock");
        }
    }

    /**
     * Return true if the import lock is currently held, else false.
     *
     * @return true if the import lock is currently held, else false.
     */
    public static boolean lock_held() {
        return Py.getSystemState().getImportLock().isHeldByCurrentThread();
    }

    /**
     * Returns the list of file suffixes used to identify extension modules.
     */
    public static PyObject extension_suffixes() {
        return new PyList();
    }
}
