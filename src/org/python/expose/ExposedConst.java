package org.python.expose;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Expose static field as constant
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface ExposedConst {
    /**
     * @return The name of the constant, default to the static field name
     */
    String name() default "";
}
