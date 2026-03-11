package org.setms.swe.domain.model.sdlc.packaging.docker;

import java.util.Collection;
import java.util.List;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.swe.domain.model.sdlc.architecture.Packaging;
import org.setms.swe.domain.model.sdlc.architecture.TopicProvider;
import org.setms.swe.domain.model.sdlc.packaging.Packager;

public class DockerPackager implements Packager, TopicProvider {

  @Override
  public boolean isValidChoice(String topic, String choice) {
    return Packaging.TOPIC.equals(topic) && "Docker".equals(choice);
  }

  @Override
  public Collection<Glob> packagingDescriptions() {
    return List.of(new Glob("/", "Dockerfile"), new Glob("/", "docker-compose.yml"));
  }

  @Override
  public FullyQualifiedName extractName(String code) {
    return new FullyQualifiedName(
        "root", code.startsWith("FROM ") ? "Dockerfile" : "DockerCompose");
  }
}
