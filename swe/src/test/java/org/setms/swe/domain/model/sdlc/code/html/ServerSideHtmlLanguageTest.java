package org.setms.swe.domain.model.sdlc.code.html;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.swe.domain.model.sdlc.code.ProgrammingLanguageConventions.Type.FRONTEND;

import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import org.setms.swe.domain.model.sdlc.architecture.UserInterface;
import org.setms.swe.domain.model.sdlc.code.ProgrammingLanguageConventions;

class ServerSideHtmlLanguageTest {

  private final ServerSideHtmlLanguage language = new ServerSideHtmlLanguage();

  @Test
  void shouldBeAFrontendLanguage() {
    var actual = language.type();

    assertThat(actual)
        .as("Server-side HTML is a frontend programming language")
        .isEqualTo(FRONTEND);
  }

  @Test
  void shouldUseHtmlExtension() {
    var actual = language.extension();

    assertThat(actual)
        .as("ServerSideHtmlLanguage should use 'html' as its file extension")
        .isEqualTo("html");
  }

  @Test
  void shouldUseTemplatesAsCodePath() {
    var actual = language.codePath();

    assertThat(actual)
        .as("ServerSideHtmlLanguage should store templates under src/main/resources/templates")
        .isEqualTo("src/main/resources/templates");
  }

  @Test
  void shouldExtractNameFromHtmlTitle() {
    var actual = language.extractName("<html><head><title>MyPage</title></head></html>");

    assertThat(actual.getName()).as("Name extracted from HTML <title> tag").isEqualTo("MyPage");
  }

  @Test
  void shouldValidateServerSideAsValidUserInterfaceChoice() {
    var actual = language.isValidChoice(UserInterface.TOPIC, "ServerSide");

    assertThat(actual)
        .as("ServerSide should be a valid choice for the UserInterface topic")
        .isTrue();
  }

  @Test
  void shouldBeRegisteredAsProgrammingLanguageConventions() {
    var actual =
        ServiceLoader.load(ProgrammingLanguageConventions.class).stream()
            .map(ServiceLoader.Provider::get)
            .toList();

    assertThat(actual)
        .as("ServiceLoader should provide a ServerSideHtmlLanguage instance")
        .anyMatch(l -> l instanceof ServerSideHtmlLanguage);
  }
}
