package org.setms.km.domain.model.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.Artifact;

class ArtifactToolTest {

  @Test
  void shouldSupportSingleValidationTarget() {
    var input = new GlobInput<>("src/main", null, Artifact.class, "entity");
    var tool = new SingleTargetTool(input);

    var actual = tool.validates("src/main/User.entity");

    assertThat(actual).isTrue();
  }

  @Test
  void shouldSupportMultipleValidationTargets() {
    var input1 = new GlobInput<>("src/test/java", null, Artifact.class, "java");
    var input2 = new GlobInput<>("src/test/kotlin", null, Artifact.class, "kt");
    var tool = new MultiTargetTool(Set.of(input1, input2));

    var actual =
        tool.validates("src/test/java/UserTest.java")
            && tool.validates("src/test/kotlin/UserTest.kt");

    assertThat(actual).isTrue();
  }

  @Test
  void shouldIncludeAllValidationTargetsInAllInputs() {
    var input1 = new GlobInput<>("src/test/java", null, Artifact.class, "java");
    var input2 = new GlobInput<>("src/test/kotlin", null, Artifact.class, "kt");
    var tool = new MultiTargetTool(Set.of(input1, input2));

    var actual = tool.allInputs();

    assertThat(actual).containsExactlyInAnyOrder(input1, input2);
  }

  static class SingleTargetTool extends ArtifactTool<Artifact> {

    private final Input<Artifact> target;

    SingleTargetTool(Input<Artifact> target) {
      this.target = target;
    }

    @Override
    public Set<Input<? extends Artifact>> validationTargets() {
      return Set.of(target);
    }

    @Override
    public Set<Input<? extends Artifact>> validationContext() {
      return Set.of();
    }
  }

  static class MultiTargetTool extends ArtifactTool<Artifact> {

    private final Set<Input<? extends Artifact>> targets;

    MultiTargetTool(Set<Input<? extends Artifact>> targets) {
      this.targets = targets;
    }

    @Override
    public Set<Input<? extends Artifact>> validationTargets() {
      return targets;
    }

    @Override
    public Set<Input<? extends Artifact>> validationContext() {
      return Set.of();
    }
  }
}
