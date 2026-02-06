package org.setms.swe.domain.model.sdlc.code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.format.DataString;
import org.setms.km.domain.model.format.Parser;
import org.setms.km.domain.model.format.RootObject;

class CodeParser implements Parser {

  private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w.]+)\\s*;");
  private static final Pattern CLASS_PATTERN = Pattern.compile("class\\s+(\\w+)");

  @Override
  public RootObject parse(InputStream input) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
      return new RootObject(null, null, null).set("code", new DataString(reader.readAllAsString()));
    }
  }

  @Override
  public <T extends Artifact> T parse(InputStream input, Class<T> type, boolean validate)
      throws IOException {
    try (var reader = new BufferedReader(new InputStreamReader(input))) {
      var code = reader.readAllAsString();
      var result =
          type.getConstructor(FullyQualifiedName.class)
              .newInstance(new FullyQualifiedName(extractPackage(code), extractClassName(code)));
      if (result instanceof CodeArtifact codeArtifact) {
        codeArtifact.setCode(code);
      }
      return result;
    } catch (ReflectiveOperationException e) {
      throw new IOException(e);
    }
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
