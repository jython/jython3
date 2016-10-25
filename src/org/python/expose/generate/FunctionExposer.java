package org.python.expose.generate;

import org.objectweb.asm.Type;
import org.python.core.PyBuiltinClassMethodNarrow;
import org.python.core.PyBuiltinMethod;

public class FunctionExposer extends MethodExposer {
        public FunctionExposer(Type onType,
                                 int access,
                                 String methodName,
                                 String desc,
                                 String typeName,
                                 String[] asNames,
                                 String[] defaults,
                                 String doc) {
        super(onType,
              methodName,
              Type.getArgumentTypes(desc),
              Type.getReturnType(desc),
              typeName,
              asNames,
              defaults,
              isWide(desc) ? PyBuiltinMethod.class : PyBuiltinClassMethodNarrow.class,
              doc,
              true);
        if ((access & ACC_STATIC) == 0) {
            throwInvalid("@ExposedFunction can't be applied to non-static methods");
        }
        if (isWide(args)) {
            if (defaults.length > 0) {
                throwInvalid("Can't have defaults on a method that takes PyObject[], String[]");
            }
        }
    }

    @Override
    protected void checkSelf() {
        // noop
    }

    @Override
    protected void makeCall() {
        callStatic(onType, methodName, returnType, args);
    }

    @Override
    protected void loadSelfAndThreadState() {
        // noop
    }

    private static boolean isWide(String methDescriptor) {
        return isWide(Type.getArgumentTypes(methDescriptor));
    }
}
