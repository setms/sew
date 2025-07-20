package org.setms.sew.core.domain.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.setms.km.domain.model.artifact.Link;
import org.setms.sew.core.domain.model.dsm.ClusteringAlgorithm;
import org.setms.sew.core.domain.model.dsm.Clusters;
import org.setms.sew.core.domain.model.dsm.DesignStructureMatrix;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;
import org.setms.sew.core.inbound.format.sal.SalFormat;

@SuppressWarnings("unchecked")
class DiscoverDomainsFromUseCasesTest {

  private final ClusteringAlgorithm<Link> clusteringAlgorithm = mock(ClusteringAlgorithm.class);

  @Test
  void shouldGenerateDomains() throws IOException {
    var useCase = loadUseCase();
    when(clusteringAlgorithm.apply(any(DesignStructureMatrix.class)))
        .thenAnswer(
            invocation -> {
              var dsm = invocation.getArgument(0, DesignStructureMatrix.class);
              return new Clusters<Link>(dsm.getElements()).all();
            });

    var actual = new DiscoverDomainFromUseCases(clusteringAlgorithm).apply(List.of(useCase));

    assertThat(actual.getSubdomains()).hasSize(1);
    var dsmCaptor = ArgumentCaptor.forClass(DesignStructureMatrix.class);
    verify(clusteringAlgorithm).apply(dsmCaptor.capture());
    var dsm = dsmCaptor.getValue();
    assertThat(dsm.getElements()).hasSize(4);
  }

  private UseCase loadUseCase() throws IOException {
    try (var input =
        getClass().getResourceAsStream("/useCase/valid/src/main/requirements/JustDoIt.useCase")) {
      return new SalFormat().newParser().parse(input, UseCase.class, false);
    }
  }
}
