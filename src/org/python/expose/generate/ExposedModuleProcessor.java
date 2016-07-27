package org.python.expose.generate;

import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExposedModuleProcessor implements Opcodes, PyTypes {
    private List<MethodExposer> methodExposers = new ArrayList<>();

    private Map<String, FieldNode> exposedConsts = new HashMap<>();

    private ModuleExposer moduleExposer;

    private ClassWriter cw;

    private String typeName;

    private String initializer;

    private Type onType;

    public ExposedModuleProcessor(InputStream in) {
        initializer = null;
        ClassReader cr = null;
        try {
            cr = new ClassReader(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        cr.accept(new ModuleProcessor(cw), 0);
    }

    /**
     * @return the name of the exposed type.
     */
    public String getName() {
        return typeName;
    }

    public List<MethodExposer> getMethodExposers() {
        return methodExposers;
    }

    /**
     * @return the processed bytecode
     */
    public byte[] getBytecode() {
        return cw.toByteArray();
    }

    public ModuleExposer getModuleExposer() {
        return moduleExposer;
    }

    private class ModuleProcessor extends ClassVisitor {
        private String doc;

        public ModuleProcessor(ClassVisitor cv) {
            super(ASM5, cv);
        }

        @Override
        public void visit(int version,
                          int access,
                          String name,
                          String signature,
                          String superName,
                          String[] interfaces) {
            onType = Type.getType("L" + name + ";");
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor visitor = super.visitAnnotation(desc, visible);
            if(desc.equals(EXPOSED_MODULE.getDescriptor())) {
                return new ExposedModuleVisitor(onType, visitor) {

                    @Override
                    public void handleResult(String typeName,
                                             String doc) {
                        ExposedModuleProcessor.this.typeName = typeName;
                        ModuleProcessor.this.doc = doc;
                    }
                };
            }
            return visitor;
        }

        @Override
        public void visitEnd() {
            // typeName is set by the ExposedTypeVisitor in visitAnnotation, if
            // the ExposedType annotation is found.
            if(typeName == null) {
                throwInvalid("A class to be exposed must have the ExposedModule annotation");
            }
            moduleExposer = new ModuleExposer(onType,
                    doc,
                    getName(),
                    initializer,
                    methodExposers,
                    exposedConsts);
            for (MethodExposer exposer : methodExposers) {
                addInnerClass(exposer.getGeneratedType());
            }
            addInnerClass(moduleExposer.getGeneratedType());
            super.visitEnd();
        }

        /** Adds an inner class reference to inner from the class being visited. */
        private void addInnerClass(Type inner) {
            super.visitInnerClass(inner.getInternalName(),
                                  onType.getInternalName(),
                                  inner.getClassName().substring(inner.getClassName()
                                          .lastIndexOf('$') + 1),
                                  ACC_PRIVATE | ACC_STATIC);
        }

        @Override
        public MethodVisitor visitMethod(int access,
                                         final String name,
                                         final String desc,
                                         String signature,
                                         String[] exceptions) {
            // Otherwise we check each method for exposed annotations.
            final MethodVisitor passthroughVisitor = super.visitMethod(access,
                    name,
                    desc,
                    signature,
                    exceptions);
            if(name.equals("<clinit>")) {
                return passthroughVisitor;
            }
            return new ExposedMethodFinder(getName(),
                    onType,
                    access,
                    name,
                    desc,
                    exceptions,
                    passthroughVisitor) {

                @Override
                public void handleResult(InstanceMethodExposer exposer) {
                    throwInvalid("module cannot have instance method");
                }

                @Override
                public void handleResult(ClassMethodExposer exposer) {
                    throwInvalid("module cannot have class method");
                }

                @Override
                public void handleResult(FunctionExposer exposer) {
                    methodExposers.add(exposer);
                }

                @Override
                public void handleInitializer(String init) {
                    initializer = init;
                }

                @Override
                public void handleNewExposer(Exposer exposer) {
                    throwInvalid("module cannot have __new__");
                }

                @Override
                public void exposeAsGetDescriptor(String descName, String doc) {
                    throwInvalid("module cannot have descriptor");
                }

                @Override
                public void exposeAsSetDescriptor(String descName) {
                    throwInvalid("module cannot have descriptor");
                }

                @Override
                public void exposeAsDeleteDescriptor(String descName) {
                    throwInvalid("module cannot have descriptor");
                }
            };
        }

        @Override
        public FieldVisitor visitField(final int access,
                                       final String fieldName,
                                       final String desc,
                                       final String signature,
                                       final Object value) {
            FieldVisitor passthroughVisitor = super.visitField(access,
                                                               fieldName,
                                                               desc,
                                                               signature,
                                                               value);

            return new ExposedFieldFinder(access, fieldName, passthroughVisitor) {
                @Override
                public void exposeAsGet(String name, String doc) {
                    throwInvalid("module cannot have descriptor");
                }

                @Override
                public void exposeAsSet(String name) {
                    throwInvalid("module cannot have descriptor");
                }

                @Override
                public void exposeAsConstant(String name) {
                    exposedConsts.put(name, new FieldNode(access, fieldName, desc, signature, value));
                }
            };
        }

        private void throwInvalid(String msg) {
            throw new InvalidExposingException(msg + "[class=" + onType.getClassName() + "]");
        }
    }
}
