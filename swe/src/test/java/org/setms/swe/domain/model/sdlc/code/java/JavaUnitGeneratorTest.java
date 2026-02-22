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
import org.setms.swe.domain.model.sdlc.unittest.UnitTest;

class JavaUnitGeneratorTest {

  private static final String PACKAGE = "com.example";

  private final JavaUnitGenerator generator = new JavaUnitGenerator();

  @Test
  void shouldGenerateUnitTestFromAggregateScenario() {
    var acceptanceTest = aggregateAcceptanceTest();

    var actual = generatedUnitTest(acceptanceTest);

    assertThat(actual.getPackage()).isEqualTo(PACKAGE);
    assertThatUnitTestHasName(actual, "NotificationsAggregateTest");
    assertThat(actual.getCode()).contains("package %s;".formatted(PACKAGE)).contains("@Test");
  }

  private UnitTest generatedUnitTest(AcceptanceTest acceptanceTest) {
    return (UnitTest) generator.generate(acceptanceTest).getFirst();
  }

  private AcceptanceTest aggregateAcceptanceTest() {
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
    var scenario =
        new AggregateScenario(fqn("Accept NotifyUser and emit UserNotified"))
            .setAccepts(variableLink("notifyUser"))
            .setEmitted(variableLink("userNotified"));
    return new AcceptanceTest(fqn("NotificationsAggregate"))
        .setSut(new Link("aggregate", "Notifications"))
        .setVariables(List.of(message, notifyUser, userNotified))
        .setScenarios(List.of(scenario));
  }

  private Variable<?, ?, ?> fieldVariable() {
    return new FieldVariable(fqn("message"))
        .setType(FieldType.TEXT)
        .setDefinitions(List.of("nonempty"));
  }

  private FullyQualifiedName fqn(String name) {
    return new FullyQualifiedName(PACKAGE, name);
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

    var actual = generatedUnitTest(acceptanceTest);

    assertThat(actual.getCode()).contains("acceptNotifyUserAndEmitUserNotified").contains("@Test");
  }

  @Test
  void shouldGenerateUnitTestFromPolicyScenario() {
    var acceptanceTest = policyAcceptanceTest();

    var actual = generatedUnitTest(acceptanceTest);

    assertThatUnitTestHasName(actual, "SendNotificationPolicyTest");
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

    var actual = generatedUnitTest(acceptanceTest);

    assertThatUnitTestHasName(actual, "NotificationListReadModelTest");
  }

  private void assertThatUnitTestHasName(UnitTest actual, String NotificationListReadModelTest) {
    assertThat(actual.getName()).isEqualTo(NotificationListReadModelTest);
    assertThat(actual.getCode()).contains("class " + NotificationListReadModelTest);
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

    var actual = generatedUnitTest(acceptanceTest);

    assertThat(actual.getCode())
        .contains("acceptNotifyUserAndEmitUserNotified")
        .contains("rejectDuplicateNotification");
  }
}
