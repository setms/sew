package org.setms.km.domain.model.kmsystem;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.*;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.*;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.km.test.MainArtifact;
import org.setms.km.test.MainTool;
import org.setms.km.test.OtherTool;
import org.setms.km.test.TestFormat;

class KmSystemTest {

  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private KmSystem kmSystem;

  private final Workspace<?> workspace = new InMemoryWorkspace();
  private final MainTool mainTool = new MainTool();
  private final OtherTool otherTool = new OtherTool();
  private final ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void init() {
    Tools.reload();
    Tools.add(mainTool);
    Tools.add(otherTool);
  }

  @Test
  void shouldValidateChangedArtifactButNotBuildItWhenInvalid() throws IOException {
    createKmSystem();
    mainTool.init(new Diagnostic(ERROR, "message"), null);
    otherTool.init();

    storeNewMainArtifact();

    assertThat(mainTool.validated).as("main validated").isTrue();
    assertThat(mainTool.built).as("main built").isFalse();
    assertThat(otherTool.validated).as("other validated").isFalse();
    assertThat(otherTool.built).as("other built").isFalse();
  }

  private void createKmSystem() {
    kmSystem = new KmSystem((workspace));
  }

  private String storeNewMainArtifact() throws IOException {
    var glob = mainTool.getMainInput().glob();
    var resource =
        workspace
            .root()
            .select(glob.path())
            .select("Bear." + glob.pattern().substring(1 + glob.pattern().lastIndexOf('.')));
    try (var output = resource.writeTo()) {
      new TestFormat()
          .newBuilder()
          .build(new MainArtifact(new FullyQualifiedName("ape.Bear")), output);
    }
    return resource.path();
  }

  @Test
  void shouldBuildValidChangedArtifactAndDependents() throws IOException {
    createKmSystem();
    mainTool.init();
    otherTool.init();

    var path = storeNewMainArtifact();

    assertThat(mainTool.validated).as("main validated").isTrue();
    assertThat(mainTool.built).as("main built").isTrue();
    assertThat(otherTool.validated).as("other validated").isFalse();
    assertThat(otherTool.built).as("other built").isTrue();
    assertThat(kmSystem.mainReportFor(path).name()).isEqualTo("report1");
  }

  @Test
  void shouldUpdateCachedGlobsWhenMatchingArtifactCreated() throws IOException {
    createKmSystem();

    var path = storeNewMainArtifact();

    assertThat(globsForMainTool()).hasSize(1).containsExactlyInAnyOrder(path);
  }

  private List<String> globsForMainTool() throws IOException {
    var globPaths =
        workspace.root().select(".km/globs/%s.glob".formatted(mainTool.getMainInput().glob()));
    try (var reader = new BufferedReader(new InputStreamReader(globPaths.readFrom()))) {
      return reader.lines().toList();
    }
  }

  @Test
  void shouldUpdateCachedGlobsWhenMatchingArtifactDeleted() throws IOException {
    var path = storeNewMainArtifact();
    createKmSystem();
    assertThat(globsForMainTool()).hasSize(1);

    workspace.root().select(path).delete();

    assertThat(globsForMainTool()).isEmpty();
  }

  @Test
  void shouldStoreValidationDiagnostics() throws IOException {
    createKmSystem();
    var mainValidationDiagnostic =
        new Diagnostic(
            ERROR, "Main message", new Location("ape", "bear"), new Suggestion("cheetah", "dingo"));
    mainTool.init(mainValidationDiagnostic, null);
    otherTool.init(new Diagnostic(WARN, "Other message"), null);

    var path = storeNewMainArtifact();

    assertThat(kmSystem.diagnosticsFor(path)).isEqualTo(Set.of(mainValidationDiagnostic));
  }

  private Resource<?> diagnosticsResourceFor(BaseTool<?> tool, Resource<?> diagnosticsRoot) {
    return diagnosticsRoot.select("%s.json".formatted(tool.getClass().getName()));
  }

  @Test
  void shouldStoreBuildDiagnostics() throws IOException {
    createKmSystem();
    var mainValidationDiagnostic = new Diagnostic(INFO, "Validation message");
    var mainBuildDiagnostic = new Diagnostic(ERROR, "Build message");
    mainTool.init(mainValidationDiagnostic, mainBuildDiagnostic);
    var otherBuildDiagnostic = new Diagnostic(WARN, "Other message");
    otherTool.init(null, otherBuildDiagnostic);

    var path = storeNewMainArtifact();

    assertThat(kmSystem.diagnosticsFor(path))
        .isEqualTo(Set.of(mainValidationDiagnostic, mainBuildDiagnostic, otherBuildDiagnostic));
  }

  @Test
  void shouldClearPreviouslyStoredDiagnosticsBeforeValidation() throws IOException {
    var path = "/main/Bear.mainArtifact";
    var diagnosticsRoot = workspace.root().select(".km/diagnostics%s".formatted(path));
    createDiagnostic(diagnosticsRoot, mainTool);
    createDiagnostic(diagnosticsRoot, otherTool);
    createKmSystem();
    mainTool.init();
    otherTool.init();

    var artifactPath = storeNewMainArtifact();

    assertThat(path).as("Test got path wrong").isEqualTo(artifactPath);
    assertThat(kmSystem.diagnosticsFor(artifactPath)).isEmpty();
  }

  private void createDiagnostic(Resource<?> diagnosticsRoot, BaseTool<?> tool) throws IOException {
    var diagnosticsResource = diagnosticsResourceFor(tool, diagnosticsRoot);
    try (var output = diagnosticsResource.writeTo()) {
      mapper.writeValue(output, Map.of("diagnostics", emptyList()));
    }
  }

  @Test
  void shouldClearPreviouslyStoredReportsBeforeBuilding() throws IOException {
    var path = "/main/Bear.mainArtifact";
    var report =
        workspace
            .root()
            .select(".km/reports%s/%s/old-report".formatted(path, mainTool.getClass().getName()));
    createReport(report);
    createKmSystem();
    mainTool.init();
    otherTool.init();

    var artifactPath = storeNewMainArtifact();

    assertThat(path).as("Test got path wrong").isEqualTo(artifactPath);
    assertThatIsEmpty(report);
  }

  private void createReport(Resource<?> report) throws IOException {
    try (var writer = new PrintWriter(report.writeTo())) {
      writer.println("Awesome report");
    }
  }

  private void assertThatIsEmpty(Resource<?> resource) throws IOException {
    try (var input = resource.readFrom()) {
      assertThat(input.available()).as("Size of old report").isZero();
    }
  }
}
