package org.setms.swe.domain.model.sdlc.code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.format.DataString;
import org.setms.km.domain.model.format.Parser;
import org.setms.km.domain.model.format.RootObject;

class CodeParser implements Parser {

  private final ProgrammingLanguageConventions conventions;

  CodeParser(ProgrammingLanguageConventions conventions) {
    this.conventions = conventions;
  }

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
      var name = extractName(code);
      var result = type.getConstructor(FullyQualifiedName.class).newInstance(name);
      if (result instanceof CodeArtifact codeArtifact) {
        codeArtifact.setCode(code);
      }
      return result;
    } catch (ReflectiveOperationException e) {
      throw new IOException(e);
    }
  }

  private FullyQualifiedName extractName(String code) {
    return Optional.ofNullable(conventions)
        .map(c -> c.extractName(code))
        .orElseGet(() -> new FullyQualifiedName("", ""));
  }
}
