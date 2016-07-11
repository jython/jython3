package org.python.expose;

import org.python.core.PyObject;

/**
 * Created by isaiah on 7/6/16.
 */
public abstract class BaseModuleBuilder {
    private String name;

    private String doc;

    public String getDoc() {
        return doc;
    }

    public void setDoc(String doc) {
        this.doc = doc;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract void classDictInit(PyObject module);
}
