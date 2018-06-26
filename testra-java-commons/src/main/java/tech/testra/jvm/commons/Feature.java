package tech.testra.jvm.commons;

import java.lang.annotation.*;

/**
 * Used to mark tests with feature label.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Repeatable(Features.class)
public @interface Feature {

    String value();

}
