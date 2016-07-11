package org.python.expose;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a static method should be exposed as a function for a module
 *
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface ExposedFunction {

    /**
     * @return the names to expose this method as. Defaults to just actual name of the method.
     */
    String[] names() default {};

    /**
     * @return default arguments. Starts at the number of arguments - defaults.length.
     */
    String[] defaults() default {};

    /**
     * Returns the __doc__ String for this function.
     */
    String doc() default "";
}
