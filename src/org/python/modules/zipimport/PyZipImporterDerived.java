/* Generated file, do not modify.  See jython/src/templates/gderived.py. */
package org.python.modules.zipimport;

import java.io.Serializable;
import org.python.core.*;
import org.python.core.finalization.FinalizeTrigger;
import org.python.core.finalization.FinalizablePyObjectDerived;

public class PyZipImporterDerived extends PyZipImporter implements Slotted,FinalizablePyObjectDerived,TraverseprocDerived {

    public PyObject getSlot(int index) {
        return slots[index];
    }

    public void setSlot(int index,PyObject value) {
        slots[index]=value;
    }

    private PyObject[]slots;

    public void __del_derived__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__del__");
        if (impl!=null) {
            impl.__get__(this,self_type).__call__();
        }
    }

    public void __ensure_finalizer__() {
        FinalizeTrigger.ensureFinalizer(this);
    }

    /* TraverseprocDerived implementation */
    public int traverseDerived(Visitproc visit,Object arg) {
        int retVal;
        for(int i=0;i<slots.length;++i) {
            if (slots[i]!=null) {
                retVal=visit.visit(slots[i],arg);
                if (retVal!=0) {
                    return retVal;
                }
            }
        }
        retVal=visit.visit(objtype,arg);
        return retVal!=0?retVal:traverseDictIfAny(visit,arg);
    }

    /* end of TraverseprocDerived implementation */

    private PyObject dict;

    public PyObject fastGetDict() {
        return dict;
    }

    public PyObject getDict() {
        return dict;
    }

    public void setDict(PyObject newDict) {
        if (newDict instanceof PyStringMap||newDict instanceof PyDictionary) {
            dict=newDict;
            if (dict.__finditem__(PyUnicode.fromInterned("__del__"))!=null&&!JyAttribute.hasAttr(this,JyAttribute.FINALIZE_TRIGGER_ATTR)) {
                FinalizeTrigger.ensureFinalizer(this);
            }
        } else {
            throw Py.TypeError("__dict__ must be set to a Dictionary "+newDict.getClass().getName());
        }
    }

    public void delDict() {
        // deleting an object's instance dict makes it grow a new one
        dict=new PyStringMap();
    }

    public PyZipImporterDerived(PyType subtype,String archivePath) {
        super(subtype,archivePath);
        slots=new PyObject[subtype.getNumSlots()];
        dict=subtype.instDict();
        if (subtype.needsFinalizer()) {
            FinalizeTrigger.ensureFinalizer(this);
        }
    }

    public int traverseDictIfAny(Visitproc visit,Object arg) {
        return visit.visit(dict,arg);
    }

    public PyUnicode __str__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__str__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyUnicode)
                return(PyUnicode)res;
            throw Py.TypeError("__str__"+" returned non-"+"unicode"+" (type "+res.getType().fastGetName()+")");
        }
        return super.__str__();
    }

    public PyUnicode __repr__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__repr__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyUnicode)
                return(PyUnicode)res;
            throw Py.TypeError("__repr__"+" returned non-"+"unicode"+" (type "+res.getType().fastGetName()+")");
        }
        return super.__repr__();
    }

    public PyFloat __float__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__float__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyFloat)
                return(PyFloat)res;
            throw Py.TypeError("__float__"+" returned non-"+"float"+" (type "+res.getType().fastGetName()+")");
        }
        return super.__float__();
    }

    public PyComplex __complex__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__complex__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyComplex)
                return(PyComplex)res;
            throw Py.TypeError("__complex__"+" returned non-"+"complex"+" (type "+res.getType().fastGetName()+")");
        }
        return super.__complex__();
    }

    public PyObject __pos__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__pos__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__();
        return super.__pos__();
    }

    public PyObject __neg__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__neg__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__();
        return super.__neg__();
    }

    public PyObject __abs__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__abs__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__();
        return super.__abs__();
    }

    public PyObject __invert__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__invert__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__();
        return super.__invert__();
    }

    public PyObject __reduce__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__reduce__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__();
        return super.__reduce__();
    }

    public PyObject __dir__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__dir__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__();
        return super.__dir__();
    }

    public PyObject __add__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__add__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__add__(other);
    }

    public PyObject __radd__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__radd__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__radd__(other);
    }

    public PyObject __sub__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__sub__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__sub__(other);
    }

    public PyObject __rsub__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rsub__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rsub__(other);
    }

    public PyObject __mul__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__mul__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__mul__(other);
    }

    public PyObject __rmul__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rmul__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rmul__(other);
    }

    public PyObject __matmul__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__matmul__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__matmul__(other);
    }

    public PyObject __rmatmul__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rmatmul__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rmatmul__(other);
    }

    public PyObject __floordiv__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__floordiv__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__floordiv__(other);
    }

    public PyObject __rfloordiv__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rfloordiv__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rfloordiv__(other);
    }

    public PyObject __truediv__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__truediv__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__truediv__(other);
    }

    public PyObject __rtruediv__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rtruediv__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rtruediv__(other);
    }

    public PyObject __mod__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__mod__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__mod__(other);
    }

    public PyObject __rmod__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rmod__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rmod__(other);
    }

    public PyObject __divmod__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__divmod__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__divmod__(other);
    }

    public PyObject __rdivmod__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rdivmod__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rdivmod__(other);
    }

    public PyObject __rpow__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rpow__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rpow__(other);
    }

    public PyObject __lshift__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__lshift__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__lshift__(other);
    }

    public PyObject __rlshift__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rlshift__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rlshift__(other);
    }

    public PyObject __rshift__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rshift__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rshift__(other);
    }

    public PyObject __rrshift__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rrshift__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rrshift__(other);
    }

    public PyObject __and__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__and__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__and__(other);
    }

    public PyObject __rand__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rand__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rand__(other);
    }

    public PyObject __or__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__or__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__or__(other);
    }

    public PyObject __ror__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__ror__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__ror__(other);
    }

    public PyObject __xor__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__xor__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__xor__(other);
    }

    public PyObject __rxor__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rxor__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rxor__(other);
    }

    public PyObject __format__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__format__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__format__(other);
    }

    public PyObject __iadd__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__iadd__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__iadd__(other);
    }

    public PyObject __isub__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__isub__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__isub__(other);
    }

    public PyObject __imul__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__imul__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__imul__(other);
    }

    public PyObject __imatmul__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__imatmul__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__imatmul__(other);
    }

    public PyObject __idiv__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__idiv__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__idiv__(other);
    }

    public PyObject __ifloordiv__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__ifloordiv__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__ifloordiv__(other);
    }

    public PyObject __itruediv__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__itruediv__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__itruediv__(other);
    }

    public PyObject __imod__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__imod__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__imod__(other);
    }

    public PyObject __ipow__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__ipow__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__ipow__(other);
    }

    public PyObject __ilshift__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__ilshift__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__ilshift__(other);
    }

    public PyObject __irshift__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__irshift__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__irshift__(other);
    }

    public PyObject __iand__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__iand__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__iand__(other);
    }

    public PyObject __ior__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__ior__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__ior__(other);
    }

    public PyObject __ixor__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__ixor__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__ixor__(other);
    }

    public PyObject __int__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__int__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyLong||res instanceof PyInteger)
                return res;
            throw Py.TypeError("__int__"+" returned non-"+"long"+" (type "+res.getType().fastGetName()+")");
        }
        return super.__int__();
    }

    public int hashCode() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__hash__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyInteger) {
                return((PyInteger)res).getValue();
            } else
                if (res instanceof PyLong) {
                    return((PyLong)res).getValue().intValue();
                }
            throw Py.TypeError("__hash__ should return a int");
        }
        if (self_type.lookup("__eq__")!=null) {
            throw Py.TypeError(String.format("unhashable type: '%.200s'",getType().fastGetName()));
        }
        return super.hashCode();
    }

    public boolean __bool__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__bool__");
        if (impl==null) {
            impl=self_type.lookup("__len__");
            if (impl==null)
                return super.__bool__();
        }
        PyObject o=impl.__get__(this,self_type).__call__();
        Class c=o.getClass();
        if (c!=PyLong.class&&c!=PyBoolean.class) {
            throw Py.TypeError(String.format("__bool__ should return bool or int, returned %s",self_type.getName()));
        }
        return o.__bool__();
    }

    public boolean __contains__(PyObject o) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__contains__");
        if (impl==null)
            return super.__contains__(o);
        return impl.__get__(this,self_type).__call__(o).__bool__();
    }

    public int __len__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__len__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyInteger||res instanceof PyLong) {
                return res.asInt();
            }
            throw Py.TypeError(String.format("'%s' object cannot be interpreted as an integer",getType().fastGetName()));
        }
        return super.__len__();
    }

    public PyObject __iter__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__iter__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__();
        impl=self_type.lookup("__getitem__");
        if (impl==null)
            return super.__iter__();
        return new PySequenceIter(this);
    }

    public PyObject __next__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__next__");
        if (impl!=null) {
            return impl.__get__(this,self_type).__call__();
        }
        return super.__next__(); // ???
    }

    public PyObject __finditem__(PyObject key) { // ???
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__getitem__");
        if (impl!=null)
            try {
                return impl.__get__(this,self_type).__call__(key);
            } catch (PyException exc) {
                if (exc.match(Py.LookupError))
                    return null;
                throw exc;
            }
        return super.__finditem__(key);
    }

    public PyObject __finditem__(int key) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__getitem__");
        if (impl!=null)
            try {
                return impl.__get__(this,self_type).__call__(new PyInteger(key));
            } catch (PyException exc) {
                if (exc.match(Py.LookupError))
                    return null;
                throw exc;
            }
        return super.__finditem__(key);
    }

    public PyObject __getitem__(PyObject key) {
        // Same as __finditem__, without swallowing LookupErrors. This allows
        // __getitem__ implementations written in Python to raise custom
        // exceptions (such as subclasses of KeyError).
        //
        // We are forced to duplicate the code, instead of defining __finditem__
        // in terms of __getitem__. That's because PyObject defines __getitem__
        // in terms of __finditem__. Therefore, we would end with an infinite
        // loop when self_type.lookup("__getitem__") returns null:
        //
        //  __getitem__ -> super.__getitem__ -> __finditem__ -> __getitem__
        //
        // By duplicating the (short) lookup and call code, we are safe, because
        // the call chains will be:
        //
        // __finditem__ -> super.__finditem__
        //
        // __getitem__ -> super.__getitem__ -> __finditem__ -> super.__finditem__

        PyType self_type=getType();
        PyObject impl=self_type.lookup("__getitem__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__(key);
        return super.__getitem__(key);
    }

    public void __setitem__(PyObject key,PyObject value) { // ???
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__setitem__");
        if (impl!=null) {
            impl.__get__(this,self_type).__call__(key,value);
            return;
        }
        super.__setitem__(key,value);
    }

    public void __delitem__(PyObject key) { // ???
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__delitem__");
        if (impl!=null) {
            impl.__get__(this,self_type).__call__(key);
            return;
        }
        super.__delitem__(key);
    }

    public PyObject __call__(PyObject args[],String keywords[]) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__call__");
        if (impl!=null) {
            return impl.__get__(this,self_type).__call__(args,keywords);
        }
        return super.__call__(args,keywords);
    }

    public PyObject __findattr_ex__(String name) {
        return Deriveds.__findattr_ex__(this,name);
    }

    public void __setattr__(String name,PyObject value) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__setattr__");
        if (impl!=null) {
            impl.__get__(this,self_type).__call__(PyUnicode.fromInterned(name),value);
            //CPython does not support instance-acquired finalizers.
            //So we don't check for __del__ here.
            return;
        }
        super.__setattr__(name,value);
    }

    public void __delattr__(String name) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__delattr__");
        if (impl!=null) {
            impl.__get__(this,self_type).__call__(PyUnicode.fromInterned(name));
            return;
        }
        super.__delattr__(name);
    }

    public PyObject __get__(PyObject obj,PyObject type) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__get__");
        if (impl!=null) {
            if (obj==null)
                obj=Py.None;
            if (type==null)
                type=Py.None;
            return impl.__get__(this,self_type).__call__(obj,type);
        }
        return super.__get__(obj,type);
    }

    public void __set__(PyObject obj,PyObject value) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__set__");
        if (impl!=null) {
            impl.__get__(this,self_type).__call__(obj,value);
            return;
        }
        super.__set__(obj,value);
    }

    public void __delete__(PyObject obj) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__delete__");
        if (impl!=null) {
            impl.__get__(this,self_type).__call__(obj);
            return;
        }
        super.__delete__(obj);
    }

    public PyObject __pow__(PyObject other,PyObject modulo) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__pow__");
        if (impl!=null) {
            PyObject res;
            if (modulo==null) {
                res=impl.__get__(this,self_type).__call__(other);
            } else {
                res=impl.__get__(this,self_type).__call__(other,modulo);
            }
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__pow__(other,modulo);
    }

    public void dispatch__init__(PyObject[]args,String[]keywords) {
        Deriveds.dispatch__init__(this,args,keywords);
    }

    public PyObject richCompare(PyObject other,CompareOp op) {
        PyType type=getType();
        PyObject meth=type.lookup(op.meth());
        PyObject res=meth.__get__(this,type).__call__(other);
        if (res!=Py.NotImplemented) {
            return res;
        }
        return super.richCompare(other,op);
    }

    public PyObject __index__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__index__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyInteger||res instanceof PyLong) {
                return res;
            }
            throw Py.TypeError(String.format("__index__ returned non-(int,long) (type %s)",res.getType().fastGetName()));
        }
        return super.__index__();
    }

    public Object __tojava__(Class c) {
        // If we are not being asked by the "default" conversion to java, then
        // we can provide this as the result, as long as it is a instance of the
        // specified class. Without this, derived.__tojava__(PyObject.class)
        // would broke. (And that's not pure speculation: PyReflectedFunction's
        // ReflectedArgs asks for things like that).
        if ((c!=Object.class)&&(c!=Serializable.class)&&(c.isInstance(this))) {
            return this;
        }
        // Otherwise, we call the derived __tojava__, if it exists:
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__tojava__");
        if (impl!=null) {
            PyObject delegate=impl.__get__(this,self_type).__call__(Py.java2py(c));
            if (delegate!=this)
                return delegate.__tojava__(Object.class);
        }
        return super.__tojava__(c);
    }

    public Object __coerce_ex__(PyObject o) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__coerce__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(o);
            if (res==Py.NotImplemented)
                return Py.None;
            if (!(res instanceof PyTuple))
                throw Py.TypeError("__coerce__ didn't return a 2-tuple");
            return((PyTuple)res).getArray();
        }
        return super.__coerce_ex__(o);
    }

}
