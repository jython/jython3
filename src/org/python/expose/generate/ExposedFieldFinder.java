package org.python.expose.generate;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

public abstract class ExposedFieldFinder extends FieldVisitor implements PyTypes {

    private int access;
    private String fieldName;
    private FieldVisitor delegate;

    public ExposedFieldFinder(int access, String name, FieldVisitor delegate) {
        super(Opcodes.ASM5);
        this.access = access;
        this.fieldName = name;
        this.delegate = delegate;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if(EXPOSED_GET.getDescriptor().equals(desc)) {
            return new DescriptorVisitor(fieldName) {

                @Override
                public void handleResult(String name, String doc) {
                    exposeAsGet(name, doc);
                }
            };
        } else if(EXPOSED_SET.getDescriptor().equals(desc)) {
            return new DescriptorVisitor(fieldName) {

                @Override
                public void handleResult(String name, String doc) {
                    exposeAsSet(name);
                }
            };
        } else if(EXPOSED_CONST.getDescriptor().equals(desc)) {
            if ((access & (ACC_STATIC | ACC_FINAL)) == 0) {
                throw new InvalidExposingException("Exposed constant must be static final");
            }
            return new ConstantVisitor(fieldName) {
                @Override
                public void handleResult(String name) {
                    exposeAsConstant(name);
                }
            };
        } else {
            return delegate.visitAnnotation(desc, visible);
        }
    }

    public abstract void exposeAsGet(String name, String doc);

    public abstract void exposeAsSet(String name);

    public abstract void exposeAsConstant(String name);

    public void visitAttribute(Attribute attr) {
        delegate.visitAttribute(attr);
    }

    public void visitEnd() {
        delegate.visitEnd();
    }
}
