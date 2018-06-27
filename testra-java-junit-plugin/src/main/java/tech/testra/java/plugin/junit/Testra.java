package tech.testra.java.plugin.junit;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Allure Junit5 annotation processor.
 */
public class Testra implements BeforeTestExecutionCallback {

  @Override
  public void beforeTestExecution(final ExtensionContext context) throws Exception {

  }


  private <T extends Annotation> Stream<T> getAnnotations(final AnnotatedElement annotatedElement,
      final Class<T> annotationClass) {
    final T annotation = annotatedElement.getAnnotation(annotationClass);
    return Stream.concat(
        extractRepeatable(annotatedElement, annotationClass).stream(),
        Objects.isNull(annotation) ? Stream.empty() : Stream.of(annotation)
    );
  }

  @SuppressWarnings("unchecked")
  private <T extends Annotation> List<T> extractRepeatable(final AnnotatedElement annotatedElement,
      final Class<T> annotationClass) {
    if (annotationClass.isAnnotationPresent(Repeatable.class)) {
      final Repeatable repeatable = annotationClass.getAnnotation(Repeatable.class);
      final Class<? extends Annotation> wrapper = repeatable.value();
      final Annotation annotation = annotatedElement.getAnnotation(wrapper);
      if (Objects.nonNull(annotation)) {
        try {
          final Method value = annotation.getClass().getMethod("value");
          final Object annotations = value.invoke(annotation);
          return Arrays.asList((T[]) annotations);
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      }
    }
    return Collections.emptyList();
  }


}
