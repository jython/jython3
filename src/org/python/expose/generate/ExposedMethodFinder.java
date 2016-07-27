package org.python.expose.generate;

import java.util.List;

import org.python.expose.MethodType;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.python.util.Generic;

/**
 * Visits a method passing all calls through to its delegate. If an ExposedNew or ExposedMethod
 * annotation is visited, calls handleResult with the exposer constructed with that annotation. Only
 * one of the handleResult methods will be called, if any.
 */
public abstract class ExposedMethodFinder extends MethodVisitor implements PyTypes, Opcodes {

    private Exposer newExp;

    private ExposedMethodVisitor methVisitor, classMethVisitor, functionVisitor;

    private Type onType;

    private String methodDesc, typeName, methodName;
    private boolean initializer;

    private String[] exceptions;

    private int access;

    public ExposedMethodFinder(String typeName,
                               Type onType,
                               int access,
                               String name,
                               String desc,
                               String[] exceptions,
                               MethodVisitor delegate) {
        super(Opcodes.ASM5, delegate);
        this.typeName = typeName;
        this.onType = onType;
        this.access = access;
        this.methodName = name;
        this.methodDesc = desc;
        this.exceptions = exceptions;
        this.initializer = false;
    }

    /**
     * @param exposer -
     *            the InstanceMethodExposer built as a result of visiting ExposeMethod
     */
    public abstract void handleResult(InstanceMethodExposer exposer);

    /**
     * @param exposer -
     *            the ClassMethodExposer built as a result of visiting ExposeClassMethod
     */
    public abstract void handleResult(ClassMethodExposer exposer);

    /**
     * @param exposer -
     *                the FunctionExposer built as a result of visiting ExposedFunction
     */
    public abstract void handleResult(FunctionExposer exposer);

    /**
     * @param exposer -
     *            the newExposer built as a result of visiting ExposeNew
     */
    public abstract void handleNewExposer(Exposer exposer);

    /**
     * initializer method for builtin module
     * @param init the name of the method
     */
    public abstract void handleInitializer(String init);

    public abstract void exposeAsGetDescriptor(String descName, String doc);

    public abstract void exposeAsSetDescriptor(String descName);

    public abstract void exposeAsDeleteDescriptor(String descName);

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if(desc.equals(EXPOSED_NEW.getDescriptor())) {
            if((access & ACC_STATIC) != 0) {
                newExp = new NewExposer(onType, access, methodName, methodDesc, exceptions);
            } else {
                newExp = new OverridableNewExposer(onType,
                                                   Type.getType("L" + onType.getInternalName()
                                                           + "Derived;"),
                                                   access,
                                                   methodName,
                                                   methodDesc,
                                                   exceptions);
            }
        } else if(desc.equals(EXPOSED_METHOD.getDescriptor())) {
            methVisitor = new ExposedMethodVisitor();
            return methVisitor;
        } else if(desc.equals(EXPOSED_CLASS_METHOD.getDescriptor())){
            classMethVisitor = new ExposedMethodVisitor();
            return classMethVisitor;
        } else if(desc.equals(EXPOSED_FUNCTION.getDescriptor())) {
            functionVisitor = new ExposedMethodVisitor();
            return functionVisitor;
        } else if (desc.equals(MODULE_INIT.getDescriptor())) {
            if ((access & ACC_STATIC) == 0) {
                throwInvalid("@ModuleInit can't be applied to non-static methods");
            } else if (!methodDesc.equals(INIT_DESCRIPTOR)) {
                throwInvalid("@ModuleInit method can only accept one PyObject argument and return void");
            }
            initializer = true;
        } else if(desc.equals(EXPOSED_GET.getDescriptor())) {
            return new DescriptorVisitor(methodName) {
                @Override
                public void handleResult(String name, String doc) {
                    exposeAsGetDescriptor(name, doc);
                }
            };
        } else if(desc.equals(EXPOSED_SET.getDescriptor())) {
            return new DescriptorVisitor(methodName) {
                @Override
                public void handleResult(String name, String doc) {
                    exposeAsSetDescriptor(name);
                }
            };
        } else if(desc.equals(EXPOSED_DELETE.getDescriptor())) {
            return new DescriptorVisitor(methodName) {

                @Override
                public void handleResult(String name, String doc) {
                    exposeAsDeleteDescriptor(name);
                }
            };
        }
        return super.visitAnnotation(desc, visible);
    }

    private abstract class StringArrayBuilder extends RestrictiveAnnotationVisitor {

        @Override
        public void visit(String name, Object value) {
            vals.add((String)value);
        }

        @Override
        public void visitEnd() {
            handleResult(vals.toArray(new String[vals.size()]));
        }

        public abstract void handleResult(String[] result);

        List<String> vals = Generic.list();
    }

    class ExposedMethodVisitor extends RestrictiveAnnotationVisitor {

        public ExposedMethodVisitor() {
            super();
        }

        @Override
        public void visit(String name, Object value) {
            if (name.equals("doc")) {
                doc = (String)value;
            } else {
                super.visit(name, value);
            }
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            if(name.equals("names")) {
                return new StringArrayBuilder() {

                    @Override
                    public void handleResult(String[] result) {
                        names = result;
                    }
                };
            } else if(name.equals("defaults")) {
                return new StringArrayBuilder() {

                    @Override
                    public void handleResult(String[] result) {
                        defaults = result;
                    }
                };
            } else {
                return super.visitArray(name);
            }
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            if(name.equals("type")) {
                type = MethodType.valueOf(value);
            } else {
                super.visitEnum(name, desc, value);
            }
        }

        private String[] names = new String[0];

        private String[] defaults = new String[0];

        private MethodType type = MethodType.DEFAULT;

        private String doc = "";
    }

    @Override
    public void visitEnd() {
        if(methVisitor != null) {
            handleResult(new InstanceMethodExposer(onType,
                                           access,
                                           methodName,
                                           methodDesc,
                                           typeName,
                                           methVisitor.names,
                                           methVisitor.defaults,
                                           methVisitor.type,
                                           methVisitor.doc));
        }
        if(newExp != null) {
            handleNewExposer(newExp);
        }
        if (classMethVisitor != null) {
            handleResult(new ClassMethodExposer(onType,
                                                access,
                                                methodName,
                                                methodDesc,
                                                typeName,
                                                classMethVisitor.names,
                                                classMethVisitor.defaults,
                                                classMethVisitor.doc));
        }
        if (functionVisitor != null) {
             handleResult(new FunctionExposer(onType,
                                                access,
                                                methodName,
                                                methodDesc,
                                                typeName,
                                                functionVisitor.names,
                                                functionVisitor.defaults,
                                                functionVisitor.doc));
        }

        if (initializer) {
            handleInitializer(methodName);
        }
        super.visitEnd();
    }

    protected void throwInvalid(String msg) {
        throw new InvalidExposingException(msg + "[method=" + onType.getClassName() + "."
                + methodName + "]");
    }

}
