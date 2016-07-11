package org.python.expose.generate;

/**
 * Created by isaiah on 7/6/16.
 */
public abstract class ConstantVisitor extends RestrictiveAnnotationVisitor {
    private String name;

    ConstantVisitor(String fieldName) {
        this.name = fieldName;
    }

    @Override
    public void visit(String name, Object value) {
        if (name.equals("name")) {
            this.name = (String)value;
        } else {
            super.visit(name, value);
        }
    }

    @Override
    public void visitEnd() {
        handleResult(name);
    }

    public abstract void handleResult(String name);
}
