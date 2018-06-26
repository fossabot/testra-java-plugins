package tech.testra.jvm.commons;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Attachment {

  String value() default "";

  String type() default "";

}