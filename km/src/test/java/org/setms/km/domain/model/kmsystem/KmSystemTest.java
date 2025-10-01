package org.setms.km.domain.model.kmsystem;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.setms.km.domain.model.validation.Level.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.*;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.*;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.km.test.MainArtifact;
import org.setms.km.test.MainTool;
import org.setms.km.test.OtherArtifact;
import org.setms.km.test.OtherTool;

class KmSystemTest {

  public static final Duration MAX_BACKGROUND_VALIDATION_TIME = Duration.ofSeconds(1);

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
    return storeNewArtifact(mainTool, MainArtifact::new);
  }

  private String storeNewArtifact(
      ArtifactTool tool, Function<FullyQualifiedName, Artifact> artifactCreator)
      throws IOException {
    var input = tool.validationTarget();
    var resource = workspace.root().select(input.path()).select("Bear." + input.extension());
    try (var output = resource.writeTo()) {
      input
          .format()
          .newBuilder()
          .build(artifactCreator.apply(new FullyQualifiedName("ape.Bear")), output);
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
    assertThat(otherTool.built).as("other built").isFalse();
    assertThat(kmSystem.mainReportFor(path).name()).isEqualTo("report1");
  }

  @Test
  void shouldUpdateCachedGlobsWhenMatchingArtifactCreated() throws IOException {
    createKmSystem();

    var path = storeNewMainArtifact();

    assertThat(inputsForMainTool()).hasSize(1).containsExactlyInAnyOrder(path);
  }

  private List<String> inputsForMainTool() throws IOException {
    var input = mainTool.validationTarget();
    var globPaths =
        workspace
            .root()
            .select(".km/inputs/%s/%s.paths".formatted(input.path(), input.extension()));
    try (var reader = new BufferedReader(new InputStreamReader(globPaths.readFrom()))) {
      return reader.lines().toList();
    }
  }

  @Test
  void shouldUpdateCachedGlobsWhenMatchingArtifactDeleted() throws IOException {
    var path = storeNewMainArtifact();
    createKmSystem();
    assertThat(inputsForMainTool()).hasSize(1);

    workspace.root().select(path).delete();

    assertThat(inputsForMainTool()).isEmpty();
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

  private Resource<?> diagnosticsResourceFor(ArtifactTool tool, Resource<?> diagnosticsRoot) {
    return diagnosticsRoot.select("%s.json".formatted(tool.getClass().getName()));
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

  private void createDiagnostic(Resource<?> diagnosticsRoot, ArtifactTool tool) throws IOException {
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

  @Test
  void shouldValidateExistingArtifact() throws IOException {
    storeNewMainArtifact();

    createKmSystem();

    await()
        .atMost(MAX_BACKGROUND_VALIDATION_TIME)
        .untilAsserted(
            () -> assertThat(mainTool.validated).as("Existing artifact validated").isTrue());
  }

  @Test
  void shouldNotValidateExistingArtifactIfItHasntChanged()
      throws IOException, InterruptedException {
    var path = storeNewMainArtifact();
    var diagnosticsResource =
        workspace
            .root()
            .select(".km/diagnostics%s/%s.json".formatted(path, MainTool.class.getName()));
    try (var output = diagnosticsResource.writeTo()) {
      output.write(1);
    }
    assertThat(workspace.root().select(path).lastModifiedAt())
        .isBefore(diagnosticsResource.lastModifiedAt());

    createKmSystem();

    Thread.sleep(MAX_BACKGROUND_VALIDATION_TIME);
    assertThat(mainTool.validated).as("Existing artifact re-validated").isFalse();
  }

  @Test
  void shouldRevalidateWhenInputDeleted() throws IOException {
    var path = storeNewMainArtifact();
    storeNewOtherArtifact();
    createKmSystem();
    otherTool.init();

    workspace.root().select(path).delete();

    assertThat(otherTool.validated).as("Existing artifact re-validated").isTrue();
  }

  private void storeNewOtherArtifact() throws IOException {
    storeNewArtifact(otherTool, OtherArtifact::new);
  }
}
