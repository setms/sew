package org.setms.swe.inbound.tool;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.technology.CodePackager;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

class PackagerToolTest {

  @Test
  void shouldCallPackageCodeDuringValidation() {
    var codePackager = mock(CodePackager.class);
    var tool = givenPackagerToolWith(codePackager);
    var workspace = new InMemoryWorkspace();

    tool.validate(workspace.root(), null, new ResolvedInputs(), new ArrayList<>());

    verify(codePackager).packageCode(any(), any());
  }

  private PackagerTool givenPackagerToolWith(CodePackager codePackager) {
    var resolver = mock(TechnologyResolver.class);
    when(resolver.codePackager(any(), any())).thenReturn(Optional.of(codePackager));
    return new PackagerTool(resolver);
  }
}
