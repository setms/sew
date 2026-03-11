package org.setms.swe.domain.model.sdlc;

import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;
import static org.setms.km.domain.model.format.Strings.toFriendlyName;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.orchestration.ProcessOrchestrator;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;

@Slf4j
public class SdlcOrchestrator extends ProcessOrchestrator {

  private static final String INDEX_PATH = ".km/sdlc/documentation.html";
  private static final String HTML_START =
      """
      <html>
        <head>
          <title>SDLC report</title>
        </head>
        <body>
      """;
  private static final String HTML_END =
      """
        </body>
      </html>
      """;

  private final Resource<?> index;

  public SdlcOrchestrator(Workspace<?> workspace) {
    super(workspace);
    workspace.registerArtifactChangedHandler(this::sdlcArtifactChanged);
    workspace.registerArtifactDeletedHandler(this::sdlcArtifactDeleted);
    index = workspace.root().select(INDEX_PATH);
  }

  private void sdlcArtifactChanged(String path, Artifact ignored) {
    sdlcArtifactDeleted(path);
  }

  private void sdlcArtifactDeleted(String path) {
    if (path.startsWith("/.km/reports/") && !path.equals(INDEX_PATH)) {
      updateSdlcDocumentation();
    }
  }

  private void updateSdlcDocumentation() {
    try {
      var root = getWorkspace().root();
      var reports = root.select(".km").matching("reports", "html");
      if (reports.isEmpty()) {
        index.delete();
        return;
      }
      createIndex(reports);
    } catch (IOException ignored) {
      log.error("Failed to create SDLC report");
    }
  }

  private void createIndex(List<? extends Resource<?>> reports) throws IOException {
    try (var writer = new PrintWriter(new OutputStreamWriter(index.writeTo()))) {
      writer.println(HTML_START);
      writeIndex(reports, writer);
      writer.println(HTML_END);
    }
  }

  private void writeIndex(Collection<? extends Resource<?>> reports, PrintWriter writer) {
    var sections = toSections(reports);
    writeToc(sections, writer);
    writeSections(sections, writer);
  }

  private void writeToc(Iterable<Section> sections, PrintWriter writer) {
    writer.printf("<strong>Table of Contents</strong>%n<ul>%n");
    sections.forEach(
        section ->
            writer.printf("<li><a href='#%s'>%s</a></li>%n", section.id(), section.heading()));
    writer.println("</ul>");
  }

  private void writeSections(Iterable<Section> sections, PrintWriter writer) {
    sections.forEach(
        section -> {
          writer.printf("<h1 id='%s'>%s</h1>%n<ul>", section.id(), section.heading());
          section
              .links()
              .forEach(
                  link ->
                      writer.printf(
                          "<li><a href='%s'>%s</a></li>%n", link.reference(), link.title()));
          writer.println("</ul>");
        });
  }

  private List<Section> toSections(Collection<? extends Resource<?>> reports) {
    var result = new ArrayList<Section>();
    toSection(reports, "Domain stories", ".domainStory").ifPresent(result::add);
    toSection(reports, "Use cases", ".useCase").ifPresent(result::add);
    toSection(reports, "Domains", ".domain").ifPresent(result::add);
    toSection(reports, "Acceptance tests", ".acceptance").ifPresent(result::add);
    toSection(reports, "Modules", ".modules").ifPresent(result::add);
    return result;
  }

  private Optional<Section> toSection(
      Collection<? extends Resource<?>> reports, String title, String extension) {
    return Optional.of(
            reports.stream()
                .filter(r -> r.path().contains(extension + '/'))
                .sorted(comparing(Resource::lastModifiedAt))
                .map(r -> new Link(nameOf(r), pathFromIndexTo(r)))
                .toList())
        .filter(not(Collection::isEmpty))
        .map(links -> new Section(title, links));
  }

  private String pathFromIndexTo(Resource<?> resource) {
    return "..%s".formatted(resource.path().substring("/.km".length()));
  }

  private String nameOf(Resource<?> report) {
    return toFriendlyName(report.name().substring(0, report.name().lastIndexOf('.')));
  }

  private static String idOf(String text) {
    return text.toLowerCase().replaceAll("[^a-z ]]", "").replaceAll("\\s+", "_");
  }

  private record Link(String title, String reference) {}

  private record Section(String heading, List<Link> links) {

    String id() {
      return idOf(heading);
    }
  }
}
