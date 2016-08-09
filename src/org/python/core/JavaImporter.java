package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

/**
 * Load Java classes.
 */
@Untraversable
@ExposedType(name = "JavaImporter")
public class JavaImporter extends PyObject {
    public static final PyType TYPE = PyType.fromClass(JavaImporter.class);
    public static final String JAVA_IMPORT_PATH_ENTRY = "__classpath__";

    public JavaImporter(PyType objtype) {
        super(objtype);
    }

    public JavaImporter() {
        super(TYPE);
    }

    @ExposedNew
    @ExposedMethod
    final void JavaImporter___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("__init__", args, kwds, new String[]{"path"});
        String path = ap.getString(0);
        if(!path.endsWith(JAVA_IMPORT_PATH_ENTRY)){
            throw Py.ImportError("unable to handle");
        }
    }

    /**
     * Find the module for the fully qualified name.
     *
     * @param name the fully qualified name of the module
     * @param path if installed on the meta-path None or a module path
     * @return a loader instance if this importer can load the module, None
     *         otherwise
     */
    @ExposedMethod(defaults = {"null"})
    public PyObject JavaImporter_find_module(String name, PyObject path) {
        Py.writeDebug("import", "trying " + name
                + " in packagemanager for path " + path);
        PyObject ret = PySystemState.packageManager.lookupName(name.intern());
        if (ret != null) {
            Py.writeComment("import", "'" + name + "' as java package");
            return this;
        }
        return Py.None;
    }

    @ExposedMethod
    public PyObject JavaImporter_find_spec(String name, PyObject path) {
        PyObject ret = lookupName(name);
        if (ret != null) {
            Py.writeComment("import", "'" + name + "' as java package");
            PyObject moduleSpec = Py.getSystemState().importlib.__findattr__("ModuleSpec");
            PyObject spec = moduleSpec.__call__(new PyUnicode(name), this);
            spec.__setattr__("__loader__", this);
            return spec;
        }
        return Py.None;
    }

    @ExposedMethod
    public PyObject JavaImporter_create_module(PyObject spec) {
        return lookupName(spec.__findattr__("name").asString());
    }

    @ExposedMethod
    public PyObject JavaImporter_exec_module(PyObject module) {
        return module;
    }

    @ExposedMethod
    public PyObject JavaImporter_load_module(String name) {
        return lookupName(name);
    }

    public static final PyObject lookupName(String name) {
        PyObject ret = PySystemState.packageManager.lookupName(name.intern());
        if (ret == null && name.startsWith("java.")) {
            ret = PySystemState.packageManager.lookupName(name.replace("java.", "").intern());
        }
        return ret;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return String.format("<%s object>", getType().fastGetName());
    }
}
