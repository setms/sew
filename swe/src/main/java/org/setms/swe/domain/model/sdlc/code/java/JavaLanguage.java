package org.setms.swe.domain.model.sdlc.code.java;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.architecture.TopicProvider;
import org.setms.swe.domain.model.sdlc.code.ProgrammingLanguage;
import org.setms.swe.domain.model.sdlc.code.ProgrammingLanguageConventions;

public class JavaLanguage implements TopicProvider, ProgrammingLanguageConventions {

  private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w.]+)\\s*;");
  private static final Pattern CLASS_PATTERN = Pattern.compile("class\\s+(\\w+)");

  @Override
  public Map<String, Set<String>> validChoices() {
    return Map.of(ProgrammingLanguage.TOPIC, Set.of("Java"));
  }

  @Override
  public String extension() {
    return "java";
  }

  @Override
  public String unitTestPath() {
    return "src/test/java";
  }

  @Override
  public FullyQualifiedName extractName(String code) {
    return new FullyQualifiedName(extractPackage(code), extractClassName(code));
  }

  private String extractPackage(String code) {
    var matcher = PACKAGE_PATTERN.matcher(code);
    return matcher.find() ? matcher.group(1) : "";
  }

  private String extractClassName(String code) {
    var matcher = CLASS_PATTERN.matcher(code);
    return matcher.find() ? matcher.group(1) : "";
  }
}
