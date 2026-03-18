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
        .contains("import com.example.notifications.domain.services.NotificationsServiceImpl;")
        .contains("import static org.assertj.core.api.Assertions.assertThat;")
        .contains("@Test")
        .contains("class NotificationsAggregateTest")
        .contains("@InjectMocks private NotificationsServiceImpl service;")
        .contains("var notifyUser = someNotifyUser();")
        .contains("var expected = new UserNotified(notifyUser.message(), notifyUser.dueDate());")
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
        .contains("import com.example.notifications.domain.services.SendNotificationServiceImpl;")
        .contains("@InjectMocks private SendNotificationServiceImpl service;")
        .contains("var userNotified = someUserNotified();")
        .contains("var expected = new SendEmail(userNotified.message());")
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
        .contains("import com.example.notifications.domain.services.NotificationListServiceImpl;")
        .contains("@InjectMocks private NotificationListServiceImpl service;")
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
        .contains("Arbitraries.strings().alpha().ofMinLength(1)")
        .doesNotContain("Text::new")
        .doesNotContain("import org.setms.swe.Text");
  }

  @Test
  void shouldGenerateUnitTestWithInitAndState() {
    var acceptanceTest = givenAcceptanceTestWithInitAndState();

    var actual = generator.generate(acceptanceTest).getFirst();

    assertThatInitStateTestHasCorrectCode(actual);
  }

  private AcceptanceTest givenAcceptanceTestWithInitAndState() {
    var message = fieldVariable();
    var existingNotification =
        elementVariable(
            "existingNotification",
            "entity",
            "Notification",
            new FieldAssignment(fqn("a0"))
                .setFieldName("Message")
                .setValue(variableLink("message")));
    var notifyUser =
        elementVariable(
            "notifyUser",
            "command",
            "NotifyUser",
            new FieldAssignment(fqn("a1"))
                .setFieldName("Message")
                .setValue(variableLink("message")));
    var updatedNotification =
        elementVariable(
            "updatedNotification",
            "entity",
            "Notification",
            new FieldAssignment(fqn("a2"))
                .setFieldName("Message")
                .setValue(variableLink("message")));
    var scenario =
        new AggregateScenario(fqn("Reject duplicate notification"))
            .setInit(List.of(variableLink("existingNotification")))
            .setAccepts(variableLink("notifyUser"))
            .setState(List.of(variableLink("updatedNotification")));
    return new AcceptanceTest(fqn(ACCEPTANCE_TEST_PACKAGE, "NotificationsAggregate"))
        .setSut(new Link("aggregate", "Notifications"))
        .setVariables(List.of(message, existingNotification, notifyUser, updatedNotification))
        .setScenarios(List.of(scenario));
  }

  private void assertThatInitStateTestHasCorrectCode(CodeArtifact actual) {
    var code = actual.getCode();
    assertThat(code)
        .as("Generated test should set up existing entity in repository for init state")
        .contains("var existingNotification = someNotification();");
    assertThat(code)
        .as("Generated test should mock loadAll with init items")
        .contains("when(repository.loadAll()).thenReturn(List.of(existingNotification));");
    assertThat(code)
        .as("Generated test should construct expected entity state after command")
        .contains("var expectedUpdatedNotification = new Notification(notifyUser.message());");
    assertThat(code)
        .as("Generated test should verify repository update with expected entity state")
        .contains("verify(repository).update(expectedUpdatedNotification);");
  }

  @Test
  void shouldGenerateUnitTestWithStateButNoInit() {
    var acceptanceTest = givenAcceptanceTestWithStateButNoInit();

    var actual = generator.generate(acceptanceTest).getFirst();

    assertThatStateOnlyTestHasCorrectCode(actual);
  }

  private AcceptanceTest givenAcceptanceTestWithStateButNoInit() {
    var message = fieldVariable();
    var notifyUser =
        elementVariable(
            "notifyUser",
            "command",
            "NotifyUser",
            new FieldAssignment(fqn("a1"))
                .setFieldName("Message")
                .setValue(variableLink("message")));
    var newNotification =
        elementVariable(
            "newNotification",
            "entity",
            "Notification",
            new FieldAssignment(fqn("a2"))
                .setFieldName("Message")
                .setValue(variableLink("message")));
    var scenario =
        new AggregateScenario(fqn("Accept NotifyUser"))
            .setAccepts(variableLink("notifyUser"))
            .setState(List.of(variableLink("newNotification")));
    return new AcceptanceTest(fqn(ACCEPTANCE_TEST_PACKAGE, "NotificationsAggregate"))
        .setSut(new Link("aggregate", "Notifications"))
        .setVariables(List.of(message, notifyUser, newNotification))
        .setScenarios(List.of(scenario));
  }

  private void assertThatStateOnlyTestHasCorrectCode(CodeArtifact actual) {
    var code = actual.getCode();
    assertThat(code)
        .as("Generated test should not mock loadAll when init is empty")
        .doesNotContain("when(repository.loadAll())");
    assertThat(code)
        .as("Generated test should construct expected new entity state")
        .contains("var expectedNewNotification = new Notification(notifyUser.message());");
    assertThat(code)
        .as("Generated test should verify repository insert with expected new entity")
        .contains("verify(repository).insert(expectedNewNotification);");
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
