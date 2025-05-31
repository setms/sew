package org.setms.sew.core.domain.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.sew.core.domain.model.dsm.ClusteringAlgorithm;
import org.setms.sew.core.domain.model.dsm.Clusters;
import org.setms.sew.core.domain.model.dsm.DesignStructureMatrix;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.UseCase;
import org.setms.sew.core.inbound.format.sew.SewFormat;

@SuppressWarnings("unchecked")
class GenerateContextMapFromUseCasesTest {

  private final ClusteringAlgorithm<Pointer> clusteringAlgorithm = mock(ClusteringAlgorithm.class);

  @Test
  void shouldGenerateContextMap() throws IOException {
    var useCase = loadUseCase();
    when(clusteringAlgorithm.apply(any(DesignStructureMatrix.class)))
        .thenAnswer(
            invocation -> {
              var dsm = invocation.getArgument(0, DesignStructureMatrix.class);
              return new Clusters<Pointer>(dsm.getElements()).all();
            });

    var actual = new GenerateContextMapFromUseCases(clusteringAlgorithm).apply(List.of(useCase));

    assertThat(actual.getPackage()).isEqualTo("valid");
    assertThat(actual.getName()).isEqualTo("Valid");
    assertThat(actual.getBoundedContexts())
        .hasSize(4)
        .allSatisfy(
            context -> {
              assertThat(context.getPackage()).isEqualTo("valid");
              assertThat(context.getContent()).isNotEmpty();
            });
  }

  private UseCase loadUseCase() throws IOException {
    try (var input =
        getClass().getResourceAsStream("/use-cases/valid/src/main/requirements/JustDoIt.useCase")) {
      return new SewFormat().newParser().parse(input, UseCase.class, false);
    }
  }
}
