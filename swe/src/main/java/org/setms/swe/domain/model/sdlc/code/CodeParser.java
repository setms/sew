package org.setms.swe.domain.model.sdlc.code;

import static org.setms.km.domain.model.file.Files.readAllAsString;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.format.DataString;
import org.setms.km.domain.model.format.Parser;
import org.setms.km.domain.model.format.RootObject;
import org.setms.swe.domain.model.sdlc.technology.NameExtractor;

@RequiredArgsConstructor
class CodeParser implements Parser {

  private final NameExtractor nameExtractor;

  @Override
  public RootObject parse(InputStream input) throws IOException {
    return new RootObject(null, null, null).set("code", new DataString(readAllAsString(input)));
  }

  @Override
  public <T extends Artifact> T parse(InputStream input, Class<T> type, boolean validate)
      throws IOException {
    var code = readAllAsString(input);
    var name = extractName(code);
    try {
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
    return Optional.ofNullable(nameExtractor)
        .map(c -> c.extractName(code))
        .orElseGet(() -> new FullyQualifiedName("", ""));
  }
}
