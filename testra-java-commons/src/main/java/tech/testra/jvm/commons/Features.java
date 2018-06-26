package tech.testra.jvm.commons;

import java.lang.annotation.*;

/**
 * Wrapper annotation for {@link Feature}.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Features {

    Feature[] value();

}
