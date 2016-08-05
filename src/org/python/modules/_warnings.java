package org.python.modules;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyFrame;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.core.imp;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;
import org.python.expose.ModuleInit;

@ExposedModule(doc = "_warnings provides basic warning filtering support.")
public class _warnings {
    private static final String importlibString = "importlib";
    private static final String bootstrapString = "_bootstrap";

    private static PyObject _onceRegistry;
    private static PyObject _filters;
    private static PyObject _defaultAction;
    private static long _filtersVersion;

    @ModuleInit
    public static void classDictInit(PyObject dict) {
        _onceRegistry = new PyDictionary();
        dict.__setitem__("_onceregistry", _onceRegistry);
        _defaultAction = new PyUnicode("default");
        dict.__setitem__("_defaultaction", _defaultAction);
        _filters = initFilters();
        dict.__setitem__("filters", _filters);
        _filtersVersion = 0;
    }

    @ExposedFunction
    public static PyObject warn(PyObject args[], String[] keywords) {
        ArgParser ap = new ArgParser("warn", args, keywords, "message", "category", "stacklevel", "source");
        PyObject message = ap.getPyObject(0);
        PyObject category = ap.getPyObject(1, Py.UserWarning);
        int stackLevel = ap.getIndex(2, 1);
        PyObject source = ap.getPyObject(3, null);
        PyObject filename, lineno, module, registry;
        PyFrame f = Py.getThreadState().frame;
        if (stackLevel <= 0 || isInternalFrame(f)) {
            while(--stackLevel > 0 && f != null) {
                f = f.f_back;
            }
        } else {
            while (--stackLevel > 0 && f != null) {
                f = nextExternalFrame(f);
            }
        }
        PyObject globals;
        if (f == null) {
            globals = Py.getSystemState().sysdict;
            lineno = Py.One;
        } else {
            globals = f.f_globals;
            lineno = new PyLong(f.f_lineno);
        }
        registry = globals.__finditem__("__warningregistry__");
        if (registry == null) {
            registry = new PyDictionary();
            globals.__setitem__("__warningregistry__", registry);
        }
        module = globals.__getitem__("__name__");
        if (module == null) {
            module = new PyUnicode("<string>");
        }
        if (f == null) {
            filename = globals.__getitem__("__file__");
        } else {
            filename = new PyUnicode(f.f_code.co_filename);
        }

        return warn_explicit_impl(category, message, filename, lineno, module, registry, null, source);
    }

    @ExposedFunction
    public static final PyObject warn_explicit(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("warn_explicit", args, keywords, "message", "category",
                "filename", "lineno", "module", "registry", "module_globals", "source");
        PyObject message = ap.getPyObject(0);
        PyObject category = ap.getPyObject(1);
        PyObject filename = ap.getPyObject(2);
        PyObject lineno = ap.getPyObject(3);
        PyObject module = ap.getPyObject(4, Py.None);
        PyObject registry = ap.getPyObject(5, Py.None);
        PyObject moduleGlobals = ap.getPyObject(6, Py.None);
        PyObject source = ap.getPyObject(7, Py.None);
        if (moduleGlobals == Py.None) {
            return warn_explicit_impl(category, message, filename, lineno, module, registry, null, source);
        }
        PyObject loader = moduleGlobals.__getitem__("__loader__");
        PyObject moduleName = moduleGlobals.__getitem__("__name__");
        if (loader == null || moduleName == null) {
            return warn_explicit_impl(category, message, filename, lineno, module, registry, null, source);
        }
        source = loader.invoke("get_source", moduleName);
        if (source == null) {
            return null;
        } else if (source == Py.None) {
            return warn_explicit_impl(category, message, filename, lineno, module, registry, null, source);
        }
        PyList sourceList = (PyList) source.invoke("splitlines");
        if (sourceList == null) return null;
        PyObject sourceLine = sourceList.pyget(lineno.asInt() - 1);
        if (sourceList == null) return null;
        return warn_explicit_impl(category, message, filename, lineno, module, registry, sourceLine, source);
    }

    private static final PyObject warn_explicit_impl(PyObject category, PyObject message,
                                               PyObject filename, PyObject lineno,
                                               PyObject module, PyObject registry, PyObject sourceline,
                                               PyObject source) {
        PyObject text;
        if (Py.isInstance(message, Py.Warning)) {
            text = message.__str__();
            category = message.getType();
        } else {
            if (!(category instanceof PyType) || !Py.isSubClass(category, Py.Warning)) {
                throw Py.TypeError(String.format("category must be a Warning subclass, not '%s'",
                        category.getType().getName()));
            }
            text = message;
            message = category.__call__(message);
        }
        PyTuple key = new PyTuple(text, category, lineno);
        if (registry != null && registry != Py.None) {
            if (alreadyWarned(registry, key, false)) {
                return Py.None;
            }
        }
        PyObject item = null;
        PyObject action = getFilter(category, text, lineno, module);
        if (action == null) {
            return Py.None;
        }
        if (action.toString().equals("error")) {
            throw new PyException(category, message);
        }
        if (!action.toString().equals("always")) {
            if (registry != null && registry != Py.None) {
                registry.__setitem__(key, Py.True);
            } else if (action.toString().equals("ignore")) {
                return Py.None;
            } else if (action.toString().equals("once")) {
                if (registry == null || registry == Py.None) {
                    registry = getOnceRegistry();
                }
                updateRegistry(registry, text, category, false);
            } else if (action.toString().equals("module")) {
                if (registry != null && registry != Py.None) {
                    updateRegistry(registry, text, category, false);
                }
            } else if (!action.toString().equals("default")) {
                throw Py.RuntimeError(String.format("Unrecognised action (%R) in warnings.filters: %R", action, item));
            }
        }
        callShowWarning(category, text, message, filename, lineno.asInt(),
                              lineno, sourceline, source);
        return Py.None;
    }

    @ExposedFunction
    public static final void _filters_mutated() {
        _filtersVersion++;
    }

    // CPython: call_show_warning
    private static final boolean callShowWarning(PyObject category, PyObject text, PyObject message,
                                               PyObject filename, int lineno, PyObject lineno_obj,
                                               PyObject sourceline, PyObject source) {
        PyObject showFunc = getWarningsAttr("_showwarnmsg", source != null);
        if (showFunc == null) {
            showWarning(filename, lineno_obj, text, category, sourceline);
        }
        if (!showFunc.isCallable()) {
            throw Py.TypeError("warnings._showwarnmsg() must be set to a callable");
        }
        PyObject warnmsgCls = getWarningsAttr("WarningMessage", false);
        if (warnmsgCls == null) {
            throw Py.RuntimeError("unsable to get warnings.WarningMessage");
        }
        PyObject msg = warnmsgCls.__call__(new PyObject[] {message, category,
                filename, lineno_obj, Py.None, Py.None}, Py.NoKeywords);
        showFunc.__call__(msg);
        return true;
    }

    private static final void showWarning(PyObject filename, PyObject lineno, PyObject text,
                                          PyObject category, PyObject sourceLine) {
        String linenoStr = String.format(":%d: ", lineno.asInt());
        PyObject name = category.__getattr__("__name__");
        PyObject fstderr = Py.getSystemState().sysdict.__getitem__("stderr");
        if (fstderr == null) {
            System.err.println("lost sys.stderr");
            return;
        }
        PyObject write = fstderr.__getattr__("write");
        write.__call__(filename);
        write.__call__(new PyUnicode(linenoStr));
        write.__call__(name);
        write.__call__(new PyUnicode(":"));
        write.__call__(text);
        write.__call__(new PyUnicode("\n"));
        // TODO print sourceline
    }

    // CPython: get_filter
    private static final PyObject getFilter(PyObject category, PyObject text, PyObject lineno, PyObject module) {
        PyObject warningsFilters = getWarningsAttr("filters", false);
        if (warningsFilters != null) {
            _filters = warningsFilters;
        }
        if (_filters == null || !(_filters instanceof PyList)) {
            throw Py.ValueError("_warnings.filters must be a list");
        }
        int i = -1;
        PyObject action;
        for (PyObject tmpItem : _filters.asIterable()) {
            i++;
            if (!(tmpItem instanceof PyTuple) || ((PyTuple) tmpItem).__len__() != 5) {
                throw Py.ValueError(String.format("warnings.filters item %d isn't a 5-tuple", i));
            }
            PyTuple tuple = (PyTuple) tmpItem;
            action = tuple.pyget(0);
            PyObject msg = tuple.pyget(1);
            PyObject cat = tuple.pyget(2);
            PyObject mod = tuple.pyget(3);
            PyObject line = tuple.pyget(4);
            boolean goodMsg = checkMatched(msg, text);
            if (!goodMsg) return null;
            boolean goodMod = checkMatched(mod, module);
            if (!goodMod) return null;
            boolean isSubclass = Py.isSubClass(category, cat);
            if (!isSubclass) return null;
            if (goodMsg && goodMod && isSubclass && (line.equals(Py.Zero) || line.equals(lineno))) {
                return action;
            }
        }

        action = getDefaultAction();
        if (action != null) {
            return action;
        }
        throw Py.ValueError("warnings.defaultaction not found");
    }

    // CPython: get_default_action
    private static final PyObject getDefaultAction() {
        PyObject defaultAction = getWarningsAttr("defaultaction", false);
        if (defaultAction == null) {
            return _defaultAction;
        }
        _defaultAction = defaultAction;
        return defaultAction;
    }

    // CPython: check_matched
    private static final boolean checkMatched(PyObject obj, PyObject arg) {
        if (obj == Py.None) {
            return true;
        }
        return obj.invoke("match", arg).__bool__();
    }

    // CPython: get_warnings_attr
    private static final PyObject getWarningsAttr(String attr, boolean tryImport) {
        PyObject warningsModule;
        if (tryImport) {
            warningsModule = imp.importName("warnings", true);
            if (warningsModule == null) return null;
        }
        warningsModule = Py.getSystemState().modules.__getitem__(new PyUnicode("warnings"));
        return warningsModule.__findattr__(attr);
    }

    // CPython: get_once_registry
    private static final PyObject getOnceRegistry() {
        PyObject registry = getWarningsAttr("onceregistry", false);
        if (registry == null) {
            return _onceRegistry;
        }
        _onceRegistry = registry;
        return registry;
    }

    private static final boolean updateRegistry(PyObject registry, PyObject text, PyObject category, boolean addZero) {
        PyObject altKey;
        if (addZero) {
            altKey = new PyTuple(text, category, Py.Zero);
        } else {
            altKey = new PyTuple(text, category);
        }
        return alreadyWarned(registry, altKey, true);
    }

    // CPython: already_warned
    private static final boolean alreadyWarned(PyObject registry, PyObject key, boolean shouldSet) {
        PyObject version, alreadyWarned;
        if (key == null) {
            return false;
        }

        version = registry.__finditem__("version");
        if (version == null || !(version instanceof PyLong) || (version.asLong() != _filtersVersion)) {
            ((PyDictionary) registry).clear();
            version = new PyLong(_filtersVersion);
            registry.__setitem__("version", version);
        } else {
            alreadyWarned = registry.__finditem__(key);
            if (alreadyWarned != null) {
                return true;
            }
        }
        if (shouldSet) {
            registry.__setitem__(key, Py.True);
        }
        return false;
    }

    // CPython: init_filters
    private static final PyObject initFilters() {
        PyList filters = new PyList();
        int pos = 0;
        filters.add(pos++, createFilter(Py.DeprecationWarning, "ignore"));
        filters.add(pos++, createFilter(Py.PendingDeprecationWarning, "ignore"));
        filters.add(pos++, createFilter(Py.ImportWarning, "ignore"));
        filters.add(pos++, createFilter(Py.BytesWarning, "ignore")); // XXX how to get the Py_BytesWarningFlag?
        filters.add(pos++, createFilter(Py.ResourceWarning, "ignore"));
        return filters;
    }

    // CPython: create_filter
    private static final PyObject createFilter(PyObject category, String action) {
        return new PyTuple(new PyUnicode(action), Py.None, category, Py.None, Py.Zero);
    }

    // CPython: is_internal_frame
    private static final boolean isInternalFrame(PyFrame frame) {
        if (frame == null || frame.f_code == null || frame.f_code.co_filename == null) {
            return false;
        }
        String filename = frame.f_code.co_filename;
        return filename.contains(importlibString) && filename.contains(bootstrapString);
    }

    // CPython: next_external_frame
    private static final PyFrame nextExternalFrame(PyFrame frame) {
        do {
            frame = frame.f_back;
        } while (frame != null && isInternalFrame(frame));
        return frame;
    }
}
