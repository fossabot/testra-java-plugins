package tech.testra.jvm.plugin.cucumberv3.test.steps;

import static java.util.Locale.ENGLISH;

import cucumber.api.TypeRegistry;
import cucumber.api.TypeRegistryConfigurer;
import io.cucumber.cucumberexpressions.ParameterType;
import io.cucumber.cucumberexpressions.Transformer;
import java.util.Locale;

public class BooleanTypeRegisteryConfigurer implements TypeRegistryConfigurer {
  @Override
  public void configureTypeRegistry(TypeRegistry typeRegistry) {
    typeRegistry.defineParameterType(new ParameterType<>(
        "booleanType",
        "\\w+",
        Boolean.class,
        ((Transformer) (arg) -> Boolean.valueOf(arg)),
        false,
        true
    ));
  }

  @Override
  public Locale locale() {
    return ENGLISH;
  }
}
