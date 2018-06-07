package com.williamhill.whgtf.testra.jvm.plugin.utils;

import cucumber.runtime.CucumberException;
import cucumber.runtime.StepDefinitionMatch;
import gherkin.formatter.model.Step;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Plugin utils.
 */
public final class Utils {
  private static final String MD_5 = "md5";
  private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
  private Utils() {
  }

  public static String md5(final String source) {
    final byte[] bytes = getMessageDigest().digest(source.getBytes(UTF_8));
    return new BigInteger(1, bytes).toString(16);
  }

  private static MessageDigest getMessageDigest() {
    try {
      return MessageDigest.getInstance(MD_5);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Could not find md5 hashing algorithm", e);
    }
  }

  public static Step extractStep(final StepDefinitionMatch match) {
    try {
      final Field step = match.getClass().getDeclaredField("step");
      step.setAccessible(true);
      return (Step) step.get(match);
    } catch (ReflectiveOperationException e) {
      //shouldn't ever happen
      LOGGER.error(e.getMessage(), e);
      throw new CucumberException(e);
    }
  }

}