package org.setms.swe.domain.model.sdlc.code.java;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.acceptancetest.AcceptanceTest;
import org.setms.swe.domain.model.sdlc.acceptancetest.AggregateScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.ElementVariable;
import org.setms.swe.domain.model.sdlc.acceptancetest.FieldAssignment;
import org.setms.swe.domain.model.sdlc.acceptancetest.FieldVariable;
import org.setms.swe.domain.model.sdlc.acceptancetest.PolicyScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.ReadModelScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.Variable;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.architecture.TopLevelPackage;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.FieldType;
import org.setms.swe.domain.model.sdlc.overview.Initiative;

class JavaUnitTestGeneratorTest {

  private static final String TOP_LEVEL_PACKAGE = "com.example";
  private static final String ACCEPTANCE_TEST_PACKAGE = "notifications";

  private final JavaUnitTestGenerator generator = new JavaUnitTestGenerator(TOP_LEVEL_PACKAGE);

  @Test
  void shouldCreateGeneratorWhenPrerequisitesAreMet() {
    var inputs = givenInputsWithPrerequisites();
    var diagnostics = new ArrayList<Diagnostic>();

    var actual = JavaUnitTestGenerator.from(inputs, diagnostics);

    assertThat(actual).as("Generator").isPresent();
    assertThat(diagnostics).as("Diagnostics").isEmpty();
  }

  private ResolvedInputs givenInputsWithPrerequisites() {
    var initiative =
        new Initiative(new FullyQualifiedName("overview", "Project"))
            .setOrganization("Example")
            .setTitle("Project");
    var topLevelPackage =
        new Decision(new FullyQualifiedName("technology", TopLevelPackage.TOPIC))
            .setTopic(TopLevelPackage.TOPIC)
            .setChoice("com.example");
    return new ResolvedInputs()
        .put("initiatives", List.of(initiative))
        .put("decisions", List.of(topLevelPackage));
  }

  @Test
  void shouldGenerateUnitTestFromAggregateScenario() {
    var acceptanceTest = givenAggregateAcceptanceTest();

    var actual = generator.generate(acceptanceTest).getFirst();

    assertThatAggregateTestFileHasCorrectMetadata(actual);
    assertThatAggregateTestFileHasCorrectCode(actual);
  }

  private void assertThatAggregateTestFileHasCorrectMetadata(CodeArtifact actual) {
    var expectedPackage = TOP_LEVEL_PACKAGE + "." + ACCEPTANCE_TEST_PACKAGE;
    assertThat(actual).isNotNull();
    assertThat(actual.getName()).isEqualTo("NotificationsAggregateTest");
    assertThat(actual.getPackage()).isEqualTo(expectedPackage);
    assertThat(actual.getCode()).isNotEmpty();
  }

  private void assertThatAggregateTestFileHasCorrectCode(CodeArtifact actual) {
    var expectedPackage = TOP_LEVEL_PACKAGE + "." + ACCEPTANCE_TEST_PACKAGE;
    assertThat(actual.getCode())
        .contains("package %s;".formatted(expectedPackage))
        .contains("import static com.example.notifications.TestDataBuilder.someNotifyUser;")
        .contains("import com.example.notifications.domain.model.UserNotified;")
        .contains("import com.example.notifications.domain.services.NotificationsService;")
        .contains("import com.example.notifications.domain.services.NotificationsServiceImpl;")
        .contains("import static org.assertj.core.api.Assertions.assertThat;")
        .contains("@Test")
        .contains("class NotificationsAggregateTest")
        .contains("private final NotificationsService service = new NotificationsServiceImpl();")
        .contains("var notifyUser = someNotifyUser();")
        .contains(
            "var expected = new UserNotified(notifyUser.getMessage(), notifyUser.getDueDate());")
        .contains("var actual = service.accept(notifyUser);")
        .contains("assertThat(actual).isEqualTo(expected);");
  }

  private AcceptanceTest givenAggregateAcceptanceTest() {
    return givenAggregateAcceptanceTest(ACCEPTANCE_TEST_PACKAGE);
  }

  private AcceptanceTest givenAggregateAcceptanceTest(String packageName) {
    var message = fieldVariable();
    var dueDate =
        new FieldVariable(fqn("dueDate"))
            .setType(FieldType.TEXT)
            .setDefinitions(List.of("nonempty"));
    var notifyUser =
        elementVariable(
            "notifyUser",
            "command",
            "NotifyUser",
            new FieldAssignment(fqn("a1"))
                .setFieldName("Message")
                .setValue(variableLink("message")),
            new FieldAssignment(fqn("a3"))
                .setFieldName("DueDate")
                .setValue(variableLink("dueDate")));
    var userNotified =
        elementVariable(
            "userNotified",
            "event",
            "UserNotified",
            new FieldAssignment(fqn("a2"))
                .setFieldName("Message")
                .setValue(variableLink("message")),
            new FieldAssignment(fqn("a4"))
                .setFieldName("DueDate")
                .setValue(variableLink("dueDate")));
    var scenario =
        new AggregateScenario(fqn("Accept NotifyUser and emit UserNotified"))
            .setAccepts(variableLink("notifyUser"))
            .setEmitted(variableLink("userNotified"));
    return new AcceptanceTest(fqn(packageName, "NotificationsAggregate"))
        .setSut(new Link("aggregate", "Notifications"))
        .setVariables(List.of(message, dueDate, notifyUser, userNotified))
        .setScenarios(List.of(scenario));
  }

  private Variable<?, ?, ?> fieldVariable() {
    return new FieldVariable(fqn("message"))
        .setType(FieldType.TEXT)
        .setDefinitions(List.of("nonempty"));
  }

  private FullyQualifiedName fqn(String name) {
    return fqn(ACCEPTANCE_TEST_PACKAGE, name);
  }

  private FullyQualifiedName fqn(String packageName, String name) {
    return new FullyQualifiedName(packageName, name);
  }

  private Link variableLink(String name) {
    return new Link("variable", name);
  }

  private Variable<?, ?, ?> elementVariable(
      String name, String type, String id, FieldAssignment... assignments) {
    return new ElementVariable(fqn(name))
        .setType(new Link(type, id))
        .setDefinitions(List.of(assignments));
  }

  @Test
  void shouldGenerateUnitTestInCorrectPackage() {
    var acceptanceTest =
        givenAggregateAcceptanceTest(
            TOP_LEVEL_PACKAGE.substring(1 + TOP_LEVEL_PACKAGE.lastIndexOf('.')));

    var actual = generator.generate(acceptanceTest).getFirst();

    assertThat(actual.getPackage()).isEqualTo(TOP_LEVEL_PACKAGE);
  }

  @Test
  void shouldGenerateTestMethodPerScenario() {
    var acceptanceTest = givenAggregateAcceptanceTest();

    var actual = generator.generate(acceptanceTest).getFirst();

    assertThat(actual.getCode()).contains("acceptNotifyUserAndEmitUserNotified").contains("@Test");
  }

  @Test
  void shouldGenerateUnitTestFromPolicyScenario() {
    var acceptanceTest = givenPolicyAcceptanceTest();

    var actual = generator.generate(acceptanceTest).getFirst();

    assertThat(actual).isNotNull();
    assertThat(actual.getName()).isEqualTo("SendNotificationPolicyTest");
    assertThatPolicyTestFileHasCorrectCode(actual);
  }

  private void assertThatPolicyTestFileHasCorrectCode(CodeArtifact actual) {
    assertThat(actual.getCode())
        .contains("class SendNotificationPolicyTest")
        .contains("import com.example.notifications.domain.services.SendNotificationService;")
        .contains("import com.example.notifications.domain.services.SendNotificationServiceImpl;")
        .contains(
            "private final SendNotificationService service = new SendNotificationServiceImpl();")
        .contains("var userNotified = someUserNotified();")
        .contains("var expected = new SendEmail(userNotified.getMessage());")
        .contains("var actual = service.handle(userNotified);")
        .contains("assertThat(actual).isEqualTo(expected);");
  }

  private AcceptanceTest givenPolicyAcceptanceTest() {
    var message = fieldVariable();
    var userNotified =
        elementVariable(
            "userNotified",
            "event",
            "UserNotified",
            new FieldAssignment(fqn("a1"))
                .setFieldName("Message")
                .setValue(variableLink("message")));
    var sendEmail =
        elementVariable(
            "sendEmail",
            "command",
            "SendEmail",
            new FieldAssignment(fqn("a2"))
                .setFieldName("Message")
                .setValue(variableLink("message")));
    var scenario =
        new PolicyScenario(fqn("Handle UserNotified and issue SendEmail"))
            .setHandles(variableLink("userNotified"))
            .setIssued(variableLink("sendEmail"));
    return new AcceptanceTest(fqn("SendNotificationPolicy"))
        .setSut(new Link("policy", "SendNotification"))
        .setVariables(List.of(message, userNotified, sendEmail))
        .setScenarios(List.of(scenario));
  }

  @Test
  void shouldGenerateUnitTestFromReadModelScenario() {
    var acceptanceTest = givenReadModelAcceptanceTest();

    var actual = generator.generate(acceptanceTest).getFirst();

    assertThat(actual).isNotNull();
    assertThat(actual.getName()).isEqualTo("NotificationListReadModelTest");
    assertThatReadModelTestFileHasCorrectCode(actual);
  }

  private void assertThatReadModelTestFileHasCorrectCode(CodeArtifact actual) {
    assertThat(actual.getCode())
        .contains("class NotificationListReadModelTest")
        .contains("import com.example.notifications.domain.services.NotificationListService;")
        .contains("import com.example.notifications.domain.services.NotificationListServiceImpl;")
        .contains(
            "private final NotificationListService service = new NotificationListServiceImpl();")
        .contains("var userNotified = someUserNotified();")
        .contains("service.handle(userNotified);")
        .doesNotContain("assertThat");
  }

  private AcceptanceTest givenReadModelAcceptanceTest() {
    var message = fieldVariable();
    var userNotified =
        elementVariable(
            "userNotified",
            "event",
            "UserNotified",
            new FieldAssignment(fqn("a1"))
                .setFieldName("Message")
                .setValue(variableLink("message")));
    var scenario =
        new ReadModelScenario(fqn("Handle UserNotified")).setHandles(variableLink("userNotified"));
    return new AcceptanceTest(fqn("NotificationListReadModel"))
        .setSut(new Link("readModel", "NotificationList"))
        .setVariables(List.of(message, userNotified))
        .setScenarios(List.of(scenario));
  }

  @Test
  void shouldGenerateMultipleTestMethods() {
    var acceptanceTest = givenAcceptanceTestWithMultipleScenarios();

    var actual = generator.generate(acceptanceTest).getFirst();

    assertThat(actual.getCode())
        .contains("acceptNotifyUserAndEmitUserNotified")
        .contains("rejectDuplicateNotification");
  }

  private AcceptanceTest givenAcceptanceTestWithMultipleScenarios() {
    var message = fieldVariable();
    var notifyUser =
        elementVariable(
            "notifyUser",
            "command",
            "NotifyUser",
            new FieldAssignment(fqn("a1"))
                .setFieldName("Message")
                .setValue(variableLink("message")));
    var userNotified =
        elementVariable(
            "userNotified",
            "event",
            "UserNotified",
            new FieldAssignment(fqn("a2"))
                .setFieldName("Message")
                .setValue(variableLink("message")));
    var scenario1 =
        new AggregateScenario(fqn("Accept NotifyUser and emit UserNotified"))
            .setAccepts(variableLink("notifyUser"))
            .setEmitted(variableLink("userNotified"));
    var scenario2 =
        new AggregateScenario(fqn("Reject duplicate notification"))
            .setInit(List.of(variableLink("userNotified")))
            .setAccepts(variableLink("notifyUser"));
    return new AcceptanceTest(fqn("NotificationsAggregate"))
        .setSut(new Link("aggregate", "Notifications"))
        .setVariables(List.of(message, notifyUser, userNotified))
        .setScenarios(List.of(scenario1, scenario2));
  }

  @Test
  void shouldGenerateTestDataBuilder() {
    var acceptanceTest = givenAggregateAcceptanceTest();

    var actual = generator.generate(acceptanceTest);

    assertThat(actual).hasSize(2);
    assertThat(actual.get(1).getCode())
        .contains("class TestDataBuilder")
        .contains("public static NotifyUser someNotifyUser()")
        .contains("Combinators.combine(")
        .contains("Arbitraries.strings().ofMinLength(1)")
        .doesNotContain("Text::new")
        .doesNotContain("import org.setms.swe.Text");
  }

  @Test
  void shouldGenerateTestDataBuilderWithDateTimeField() {
    var acceptanceTest = givenAcceptanceTestWithDateTimeField();

    var actual = generator.generate(acceptanceTest);

    assertThat(actual.get(1).getCode())
        .contains("import java.time.LocalDateTime")
        .contains("Arbitraries.defaultFor(LocalDateTime.class)")
        .doesNotContain("OffsetDateTime");
  }

  private AcceptanceTest givenAcceptanceTestWithDateTimeField() {
    var createdAt = new FieldVariable(fqn("createdAt")).setType(FieldType.DATETIME);
    var notify =
        elementVariable(
            "notify",
            "command",
            "Notify",
            new FieldAssignment(fqn("a1"))
                .setFieldName("CreatedAt")
                .setValue(variableLink("createdAt")));
    var scenario = new AggregateScenario(fqn("Accept Notify")).setAccepts(variableLink("notify"));
    return new AcceptanceTest(fqn("NotificationsAggregate"))
        .setSut(new Link("aggregate", "Notifications"))
        .setVariables(List.of(createdAt, notify))
        .setScenarios(List.of(scenario));
  }
}
