package org.setms.swe.domain.model.sdlc.code.java;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.swe.domain.model.sdlc.acceptancetest.AcceptanceTest;
import org.setms.swe.domain.model.sdlc.acceptancetest.AggregateScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.ElementVariable;
import org.setms.swe.domain.model.sdlc.acceptancetest.FieldAssignment;
import org.setms.swe.domain.model.sdlc.acceptancetest.FieldVariable;
import org.setms.swe.domain.model.sdlc.acceptancetest.PolicyScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.ReadModelScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.Variable;
import org.setms.swe.domain.model.sdlc.design.FieldType;

class JavaUnitTestGeneratorTest {

  private static final String TOP_LEVEL_PACKAGE = "com.example";
  private static final String ACCEPTANCE_TEST_PACKAGE = "notifications";

  private final JavaUnitTestGenerator generator = new JavaUnitTestGenerator(TOP_LEVEL_PACKAGE);

  @Test
  void shouldGenerateUnitTestFromAggregateScenario() {
    var acceptanceTest = aggregateAcceptanceTest();

    var actual = generator.generate(acceptanceTest).getFirst();

    var expectedPackage = TOP_LEVEL_PACKAGE + "." + ACCEPTANCE_TEST_PACKAGE;
    assertThat(actual).isNotNull();
    assertThat(actual.getName()).isEqualTo("NotificationsAggregateTest");
    assertThat(actual.getPackage()).isEqualTo(expectedPackage);
    assertThat(actual.getCode()).isNotEmpty();
    assertThat(actual.getCode())
        .contains("package %s;".formatted(expectedPackage))
        .contains("import static com.example.notifications.TestData.someNotifyUser;")
        .contains("import static com.example.notifications.domain.model.UserNotified;")
        .contains("import static com.example.notifications.domain.services.NotificationsService;")
        .contains("import static org.assertj.core.api.Assertions.assertThat;")
        .contains("@Test")
        .contains("class NotificationsAggregateTest")
        .contains("private final NotificationsService service = new NotificationsService();")
        .contains("var notifyUser = someNotifyUser();")
        .contains(
            "var expected =\n"
                + "        new UserNotified()"
                + ".setMessage(notifyUser.getMessage())"
                + ".setDueDate(notifyUser.getDueDate());")
        .contains("var actual = service.accept(notifyUser);")
        .contains("assertThat(actual).isEqualTo(expected);");
  }

  private AcceptanceTest aggregateAcceptanceTest() {
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
    return new AcceptanceTest(fqn("NotificationsAggregate"))
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
    return new FullyQualifiedName(ACCEPTANCE_TEST_PACKAGE, name);
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
  void shouldGenerateTestMethodPerScenario() {
    var acceptanceTest = aggregateAcceptanceTest();

    var actual = generator.generate(acceptanceTest).getFirst();

    assertThat(actual.getCode()).contains("acceptNotifyUserAndEmitUserNotified").contains("@Test");
  }

  @Test
  void shouldGenerateUnitTestFromPolicyScenario() {
    var acceptanceTest = policyAcceptanceTest();

    var actual = generator.generate(acceptanceTest).getFirst();

    assertThat(actual).isNotNull();
    assertThat(actual.getName()).isEqualTo("SendNotificationPolicyTest");
    assertThat(actual.getCode())
        .contains("class SendNotificationPolicyTest")
        .contains("private final SendNotificationService service = new SendNotificationService();")
        .contains("var userNotified = someUserNotified();")
        .contains("var expected = new SendEmail().setMessage(userNotified.getMessage());")
        .contains("var actual = service.handle(userNotified);")
        .contains("assertThat(actual).isEqualTo(expected);");
  }

  private AcceptanceTest policyAcceptanceTest() {
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
    var acceptanceTest = readModelAcceptanceTest();

    var actual = generator.generate(acceptanceTest).getFirst();

    assertThat(actual).isNotNull();
    assertThat(actual.getName()).isEqualTo("NotificationListReadModelTest");
    assertThat(actual.getCode())
        .contains("class NotificationListReadModelTest")
        .contains("private final NotificationListService service = new NotificationListService();")
        .contains("var userNotified = someUserNotified();")
        .contains("service.handle(userNotified);")
        .doesNotContain("assertThat");
  }

  private AcceptanceTest readModelAcceptanceTest() {
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
    var acceptanceTest =
        new AcceptanceTest(fqn("NotificationsAggregate"))
            .setSut(new Link("aggregate", "Notifications"))
            .setVariables(List.of(message, notifyUser, userNotified))
            .setScenarios(List.of(scenario1, scenario2));

    var actual = generator.generate(acceptanceTest).getFirst();

    assertThat(actual.getCode())
        .contains("acceptNotifyUserAndEmitUserNotified")
        .contains("rejectDuplicateNotification");
  }

  @Test
  void shouldGenerateTestData() {
    var acceptanceTest = aggregateAcceptanceTest();

    var actual = generator.generate(acceptanceTest);

    assertThat(actual).hasSize(2);
    assertThat(actual.get(1).getCode())
        .contains("class TestData")
        .contains("public static NotifyUser someNotifyUser()")
        .contains("Combinators.combine(")
        .contains("Arbitraries.strings().ofMinLength(1).map(Text::new)");
  }
}
