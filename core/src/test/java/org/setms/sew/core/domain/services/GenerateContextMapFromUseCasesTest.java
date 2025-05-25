package org.setms.sew.core.domain.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.sew.core.domain.model.sdlc.UseCase;
import org.setms.sew.core.inbound.format.sew.SewFormat;

class GenerateContextMapFromUseCasesTest {

  @Test
  void shouldGenerateContextMap() throws IOException {
    var useCase = loadUseCase();

    var actual = new GenerateContextMapFromUseCases().apply(List.of(useCase));

    assertThat(actual.getPackage()).isEqualTo("valid");
    assertThat(actual.getName()).isEqualTo("valid");
    assertThat(actual.getContexts()).hasSize(1);
    var context = actual.getContexts().getFirst();
    assertThat(context.getPackage()).isEqualTo("valid");
    assertThat(context.getName()).isEqualTo("System");
    assertThat(context.getContent()).hasSize(8);
  }

  private UseCase loadUseCase() throws IOException {
    try (var input =
        getClass().getResourceAsStream("/use-cases/valid/src/main/requirements/JustDoIt.useCase")) {
      return new SewFormat().newParser().parse(input, UseCase.class, false);
    }
  }
}
