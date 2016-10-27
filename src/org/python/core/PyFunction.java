// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

/**
 * A Python function.
 */
@ExposedType(name = "function", isBaseType = false, doc = BuiltinDocs.function_doc)
public class PyFunction extends PyObject implements InvocationHandler, Traverseproc {

    public static final PyType TYPE = PyType.fromClass(PyFunction.class);

    /** Annotations */
    @ExposedGet
    public PyDictionary __annotations__;

    /** The writable name */
    @ExposedGet
    public String __name__;

    /** The qualified name */
    @ExposedGet
    @ExposedSet
    public String __qualname__;

    /** The writable doc string. */
    @ExposedGet
    @ExposedSet
    public PyObject __doc__;

    /** The read only namespace; a dict (PyStringMap). */
    @ExposedGet
    public PyObject __globals__;

    /**
     * Default argument values for associated kwargs. Exposed as a
     * tuple to Python. Writable.
     */
    public PyObject[] __defaults__;

    /**
     * Default argument values for keyword-only arguments. Exposed as
     * a dict to Python.
     */
    @ExposedGet
    @ExposedSet
    public PyDictionary __kwdefaults__;

    /** The actual function's code, writable. */
    @ExposedGet
    public PyCode __code__;

    /**
     * A function's lazily created __dict__; allows arbitrary
     * attributes to be tacked on. Read only.
     */
    public PyObject __dict__;

    /** A read only closure tuple for nested scopes. */
    @ExposedGet
    public PyObject __closure__;

    /** Writable object describing what module this function belongs to. */
    @ExposedGet
    @ExposedSet
    public PyObject __module__;

    public PyFunction(PyObject globals, PyObject[] defaults, PyDictionary kw_defaults, PyDictionary annotations, PyCode code, PyObject doc,
                      String qualname) {
        this(globals, defaults, kw_defaults, annotations, code, doc, (PyObject[]) null);
        __qualname__ = qualname;
    }

    public PyFunction(PyObject globals, PyObject[] defaults, PyDictionary kw_defaults, PyDictionary annotations, PyCode code, PyObject doc) {
        this(globals, defaults, kw_defaults, annotations, code, doc, (PyObject[]) null);
    }

    public PyFunction(PyObject globals, PyObject[] defaults, PyDictionary kw_defaults, PyDictionary annotations, PyCode code, PyObject doc,
                      String qualname, PyObject[] closure_cells) {
         this(globals, defaults, kw_defaults, code, doc, closure_cells);
        __annotations__ = annotations;
        __qualname__ = qualname;
    }
    public PyFunction(PyObject globals, PyObject[] defaults, PyDictionary kw_defaults, PyDictionary annotations, PyCode code, PyObject doc,
                      PyObject[] closure_cells) {
        this(globals, defaults, kw_defaults, code, doc, closure_cells);
        __annotations__ = annotations;
    }

    public PyFunction(PyObject globals, PyObject[] defaults, PyDictionary kw_defaults, PyCode code, PyObject doc,
                      PyObject[] closure_cells) {
        super(TYPE);
        __globals__ = globals;
        __name__ = code.co_name;
        __doc__ = doc != null ? doc : Py.None;
        // XXX: workaround the compiler passing Py.EmptyObjects
        // instead of null for defaults, whereas we want __defaults__
        // to be None (null) in that situation
        __defaults__ = (defaults != null && defaults.length == 0) ? null : defaults;
        __kwdefaults__ = kw_defaults;
        __code__ = code;
        __closure__ = closure_cells != null ? new PyTuple(closure_cells) : null;
        PyObject moduleName = globals.__finditem__("__name__");
        __module__ = moduleName != null ? moduleName : Py.None;
    }

    public PyFunction(PyObject globals, PyObject[] defaults, PyDictionary kw_defaults, PyCode code, PyObject doc) {
        this(globals, defaults, kw_defaults, code, doc, null);
    }

    // used by visitLambda
    public PyFunction(PyObject globals, PyObject[] defaults, PyDictionary kw_defaults, PyCode code) {
        this(globals, defaults, kw_defaults, code, null, null);
    }

    public PyFunction(PyObject globals, PyObject[] defaults, PyDictionary kw_defaults, PyCode code,
                      PyObject[] closure_cells) {
        this(globals, defaults, kw_defaults, code, null, closure_cells);
    }

    @ExposedNew
    static final PyObject function___new__(PyNewWrapper new_, boolean init, PyType subtype,
                                           PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("function", args, keywords,
                                     new String[] {"code", "globals", "name", "argdefs", "kwdefs",
                                                   "closure"}, 0);
        PyObject code = ap.getPyObject(0);
        PyObject globals = ap.getPyObject(1);
        PyObject name = ap.getPyObject(2, Py.None);
        PyObject defaults = ap.getPyObject(3, Py.None);
        PyObject kw_defaults = ap.getPyObject(4, Py.None);
        PyObject closure = ap.getPyObject(5, Py.None);

        if (!(code instanceof PyBaseCode)) {
            throw Py.TypeError("function() argument 1 must be code, not " +
                               code.getType().fastGetName());
        }
        if (name != Py.None && !Py.isInstance(name, PyBytes.TYPE)) {
            throw Py.TypeError("arg 3 (name) must be None or string");
        }
        if (defaults != Py.None && !(defaults instanceof PyTuple)) {
            throw Py.TypeError("arg 4 (defaults) must be None or tuple");
        }
        if (defaults != Py.None && !(defaults instanceof PyStringMap)) {
            throw Py.TypeError("arg 5 (kw_defaults) must be None or dict");
        }


        PyBaseCode tcode = (PyBaseCode)code;
        int nfree = tcode.co_freevars == null ? 0 : tcode.co_freevars.length;
        if (!(closure instanceof PyTuple)) {
            if (nfree > 0 && closure == Py.None) {
                throw Py.TypeError("arg 6 (closure) must be tuple");
            } else if (closure != Py.None) {
                throw Py.TypeError("arg 6 (closure) must be None or tuple");
            }
        }

        int nclosure = closure == Py.None ? 0 : closure.__len__();
        if (nfree != nclosure) {
            throw Py.ValueError(String.format("%s requires closure of length %d, not %d",
                                              tcode.co_name, nfree, nclosure));
        }
        if (nclosure > 0) {
            for (PyObject o : ((PyTuple)closure).asIterable()) {
                if (!(o instanceof PyCell)) {
                    throw Py.TypeError(String.format("arg 5 (closure) expected cell, found %s",
                                                     o.getType().fastGetName()));
                }
            }
        }

        PyFunction function = new PyFunction(globals,
                                             defaults == Py.None
                                             ? null : ((PyTuple)defaults).getArray(),
                                             kw_defaults == Py.None
                                             ? null : (PyDictionary) kw_defaults,
                                             tcode, null,
                                             closure == Py.None
                                             ? null : ((PyTuple)closure).getArray());
        if (name != Py.None) {
            function.__name__ = name.toString();
            function.__qualname__ = name.toString();
        }
        return function;
    }

    @ExposedSet(name = "__name__")
    public void setName(String func_name) {
        __name__ = func_name;
    }

    @ExposedDelete(name = "__name__")
    public void delName() {
        throw Py.TypeError("__name__ must be set to a string object");
    }

    @ExposedDelete(name = "__doc__")
    public void delDoc() {
        __doc__ = Py.None;
    }

    @ExposedGet(name = "__defaults__")
    public PyObject getDefaults() {
        if (__defaults__ == null) {
            return Py.None;
        }
        return new PyTuple(__defaults__);
    }

    @ExposedSet(name = "__defaults__")
    public void setDefaults(PyObject func_defaults) {
        if (func_defaults != Py.None && !(func_defaults instanceof PyTuple)) {
            throw Py.TypeError("func_defaults must be set to a tuple object");
        }
        this.__defaults__ = func_defaults == Py.None ? null : ((PyTuple)func_defaults).getArray();
    }

    @ExposedDelete(name = "__defaults__")
    public void delDefaults() {
        __defaults__ = null;
    }

    @ExposedSet(name = "__code__")
    public void setCode(PyCode code) {
        if (__code__ == null || !(code instanceof PyBaseCode)) {
            throw Py.TypeError("__code__ must be set to a code object");
        }
        PyBaseCode tcode = (PyBaseCode)code;
        int nfree = tcode.co_freevars == null ? 0 : tcode.co_freevars.length;
        int nclosure = __closure__ != null ? __closure__.__len__() : 0;
        if (nclosure != nfree) {
            throw Py.ValueError(String.format("%s() requires a code object with %d free vars,"
                                              + " not %d", __name__, nclosure, nfree));
        }
        this.__code__ = code;
    }

    @ExposedDelete(name = "__module__")
    public void delModule() {
        __module__ = Py.None;
    }

    @Override
    public PyObject fastGetDict() {
        return __dict__;
    }

    @ExposedGet(name = "__dict__")
    public PyObject getDict() {
        ensureDict();
        return __dict__;
    }

    @ExposedSet(name = "__dict__")
    public void setDict(PyObject value) {
        if (!(value instanceof PyDictionary) && !(value instanceof PyStringMap)) {
            throw Py.TypeError("setting function's dictionary to a non-dict");
        }
        __dict__ = value;
    }

    @ExposedDelete(name = "__dict__")
    public void delDict() {
        throw Py.TypeError("function's dictionary may not be deleted");
    }

    @ExposedSet(name = "__globals__")
    public void setGlobals(PyObject value) {
        throw Py.AttributeError("readonly attribute");
    }

    @ExposedDelete(name = "__globals__")
    public void delGlobals() {
        throw Py.AttributeError("readonly attribute");
    }


    @ExposedSet(name = "__closure__")
    public void setClosure(PyObject value) {
        throw Py.AttributeError("readonly attribute");
    }

    @ExposedDelete(name = "__closure__")
    public void delClosure() {
        throw Py.AttributeError("readonly attribute");
    }

    private void ensureDict() {
        if (__dict__ == null) {
            __dict__ = new PyStringMap();
        }
    }

    @Override
    public void __setattr__(String name, PyObject value) {
        function___setattr__(name, value);
    }

    @ExposedMethod(doc = BuiltinDocs.function___setattr___doc)
    final void function___setattr__(String name, PyObject value) {
        ensureDict();
        super.__setattr__(name, value);
    }

    @Override
    public PyObject __get__(PyObject obj, PyObject type) {
        return function___get__(obj, type);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.function___get___doc)
    final PyObject function___get__(PyObject obj, PyObject type) {
        return new PyMethod(this, obj, type);
    }

    @Override
    public PyObject __call__() {
        return __call__(Py.getThreadState());
    }

    @Override
    public PyObject __call__(ThreadState state) {
        return __code__.call(state, __globals__, __defaults__, __kwdefaults__, __closure__);
    }

    @Override
    public PyObject __call__(PyObject arg) {
        return __call__(Py.getThreadState(), arg);
    }

    @Override
    public PyObject __call__(ThreadState state, PyObject arg0) {
        return __code__.call(state, arg0, __globals__, __defaults__, __kwdefaults__, __closure__);
    }

    @Override
    public PyObject __call__(PyObject arg1, PyObject arg2) {
        return __call__(Py.getThreadState(), arg1, arg2);
    }

    @Override
    public PyObject __call__(ThreadState state, PyObject arg0, PyObject arg1) {
        return __code__.call(state, arg0, arg1, __globals__, __defaults__, __kwdefaults__, __closure__);
    }

    @Override
    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
        return __call__(Py.getThreadState(), arg1, arg2, arg3);
    }

    @Override
    public PyObject __call__(ThreadState state, PyObject arg0, PyObject arg1,
            PyObject arg2) {
        return __code__.call(state, arg0, arg1, arg2, __globals__, __defaults__, __kwdefaults__, __closure__);
    }

    @Override
    public PyObject __call__(PyObject arg0, PyObject arg1, PyObject arg2,
            PyObject arg3) {
        return __call__(Py.getThreadState(), arg0, arg1, arg2, arg3);
    }

    @Override
    public PyObject __call__(ThreadState state, PyObject arg0, PyObject arg1,
            PyObject arg2, PyObject arg3) {
        return __code__.call(state, arg0, arg1, arg2, arg3, __globals__, __defaults__, __kwdefaults__, __closure__);
    }

    @Override
    public PyObject __call__(PyObject[] args) {
        return __call__(Py.getThreadState(), args);
    }

    @Override
    public PyObject __call__(ThreadState state, PyObject[] args) {
        return __call__(state, args, Py.NoKeywords);
    }

    @Override
    public PyObject __call__(PyObject[] args, String[] keywords) {
        return __call__(Py.getThreadState(), args, keywords);
    }

    @Override
    public PyObject __call__(ThreadState state, PyObject[] args, String[] keywords) {
        return function___call__(state, args, keywords);
    }

    @ExposedMethod(doc = BuiltinDocs.function___call___doc)
    final PyObject function___call__(ThreadState state, PyObject[] args, String[] keywords) {
        return __code__.call(state, args, keywords, __globals__, __defaults__,  __kwdefaults__, __closure__);
    }

    @Override
    public PyObject __call__(PyObject arg1, PyObject[] args, String[] keywords) {
        return __call__(Py.getThreadState(), arg1, args, keywords);
    }

    @Override
    public PyObject __call__(ThreadState state, PyObject arg1, PyObject[] args,
                             String[] keywords) {
        return __code__.call(state, arg1, args, keywords, __globals__, __defaults__, __kwdefaults__, __closure__);
    }

    @Override
    public String toString() {
        return String.format("<function %s at %s>", __qualname__, Py.idstr(this));
    }

    @ExposedDelete(name = "__qualname__")
    public void deleteQualname() {
        throw Py.TypeError("__qualname__ must be set to a string object");
    }

    @Override
    public Object __tojava__(Class<?> c) {
        // Automatically coerce to single method interfaces
        if (c.isInstance(this) && c != InvocationHandler.class) {
            // for base types, conversion is simple - so don't wrap!
            // InvocationHandler is special, since it's a single method interface
            // that we implement, but if we coerce to it we want the arguments
            return c.cast( this );
        } else if (c.isInterface()) {
            if (c.getDeclaredMethods().length == 1 && c.getInterfaces().length == 0) {
                // Proper single method interface
                return proxy(c);
            } else {
                // Try coerce to interface with multiple overloaded versions of
                // the same method (name)
                String name = null;
                for (Method method : c.getMethods()) {
                    if (method.getDeclaringClass() != Object.class) {
                        if (name == null || name.equals(method.getName())) {
                            name = method.getName();
                        } else {
                            name = null;
                            break;
                        }
                    }
                }
                if (name != null) { // single unique method name
                    return proxy(c);
                }
            }
        }
        return super.__tojava__(c);
    }

    private Object proxy( Class<?> c ) {
        return Proxy.newProxyInstance(c.getClassLoader(), new Class[]{c}, this);
    }

    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
        // Handle invocation when invoked through Proxy (as coerced to single method interface)
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke( this, args );
        } else if (args == null || args.length == 0) {
            return __call__().__tojava__(method.getReturnType());
        } else {
            return __call__(Py.javas2pys(args)).__tojava__(method.getReturnType());
        }
    }

    @Override
    public boolean isMappingType() { return false; }

    @Override
    public boolean isNumberType() { return false; }

    @Override
    public boolean isSequenceType() { return false; }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        //globals cannot be null
        int retVal = visit.visit(__globals__, arg);
        if (retVal != 0) {
            return retVal;
        }
        if (__code__ != null) {
            retVal = visit.visit(__code__, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        //__module__ cannot be null
        retVal = visit.visit(__module__, arg);
        if (retVal != 0) {
            return retVal;
        }
        if (__defaults__ != null) {
            for (PyObject ob: __defaults__) {
                if (ob != null) {
                    retVal = visit.visit(ob, arg);
                    if (retVal != 0) {
                        return retVal;
                    }
                }
            }
        }
        //__doc__ cannot be null
        retVal = visit.visit(__doc__, arg);
        if (retVal != 0) {
            return retVal;
        }
        
//      CPython also traverses the name, which is not stored
//      as a PyObject in Jython:
//      Py_VISIT(f->func_name);

        if (__dict__ != null) {
            retVal = visit.visit(__dict__, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        return __closure__ != null ? visit.visit(__closure__, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        if (ob == null) {
            return false;
        }
        if (__defaults__ != null) {
            for (PyObject obj: __defaults__) {
                if (obj == ob) {
                    return true;
                }
            }
        }
        return ob == __doc__ || ob == __globals__ || ob == __code__
            || ob == __dict__ || ob == __closure__ || ob == __module__;
    }
}
