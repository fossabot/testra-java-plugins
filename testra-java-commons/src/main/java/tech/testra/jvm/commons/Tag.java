package tech.testra.jvm.commons;

import java.lang.annotation.*;

/**
 * Used to mark tests with epic label.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Repeatable(Tags.class)
public @interface Tag {

    String value();

}
