package org.python.expose.generate;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.python.core.PyBuiltinMethod;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.expose.ExposedMethod;
import org.python.expose.MethodType;

/**
 * Generates a class to call a given method with the {@link ExposedMethod} annotation as a method on
 * a builtin Python type.
 */
public class InstanceMethodExposer extends MethodExposer {

    MethodType type;

    public InstanceMethodExposer(Type onType,
                                 int access,
                                 String methodName,
                                 String desc,
                                 String typeName) {
        this(onType,
             access,
             methodName,
             desc,
             typeName,
             new String[0],
             new String[0],
             MethodType.DEFAULT,
             "");
    }

    public InstanceMethodExposer(Type onType,
                                 int access,
                                 String methodName,
                                 String desc,
                                 String typeName,
                                 String[] asNames,
                                 String[] defaults,
                                 MethodType type,
                                 String doc) {
        super(onType,
              methodName,
              Type.getArgumentTypes(desc),
              Type.getReturnType(desc),
              typeName,
              asNames,
              defaults,
              isWide(desc) ? PyBuiltinMethod.class : PyBuiltinMethodNarrow.class,
              doc);
        if ((access & ACC_STATIC) != 0) {
            throwInvalid("@ExposedMethod can't be applied to static methods");
        }
        if (isWide(args)) {
            if (defaults.length > 0) {
                throwInvalid("Can't have defaults on a method that takes PyObject[], String[]");
            }
        }
        this.type = type;
    }

    protected void checkSelf() {
        mv.visitTypeInsn(CHECKCAST, onType.getInternalName());
    }

    protected void makeCall() {
        // Actually call the exposed method
        call(onType, methodName, returnType, args);
        if (type == MethodType.BINARY) {
            checkBinaryResult();
        }
    }

    /** Throw NotImplemented if a binary method returned null. */
    private void checkBinaryResult() {
        // If this is a binary method,
        mv.visitInsn(DUP);
        Label regularReturn = new Label();
        mv.visitJumpInsn(IFNONNULL, regularReturn);
        getStatic(PY, "NotImplemented", PYOBJ);
        mv.visitInsn(ARETURN);
        mv.visitLabel(regularReturn);
    }

    private static boolean isWide(String methDescriptor) {
        return isWide(Type.getArgumentTypes(methDescriptor));
    }
}
