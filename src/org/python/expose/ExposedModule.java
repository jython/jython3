package org.python.expose;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by isaiah on 7/5/16.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ExposedModule {
    /**
     * @return Name of the module, default to actual name of the class
     */
    String name() default "";

    /**
     * @return the __doc__ string for this module
     */
    String doc() default "";
}
