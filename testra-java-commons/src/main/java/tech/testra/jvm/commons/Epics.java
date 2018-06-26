package tech.testra.jvm.commons;

import java.lang.annotation.*;

/**
 * Wrapper annotation for {@link Epic}.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Epics {

    Epic[] value();

}
