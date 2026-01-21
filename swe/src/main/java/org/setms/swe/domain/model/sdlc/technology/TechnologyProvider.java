package org.setms.swe.domain.model.sdlc.technology;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.code.ProgrammingLanguage;

/**
 * Provides technology-specific implementations based on architectural decisions.
 *
 * <h2>Purpose</h2>
 *
 * <p>This class is the indirection layer between architectural decisions (captured as {@link
 * Decision} artifacts) and concrete technology implementations (like {@link UnitTestGenerator}).
 *
 * <p>It separates two concerns:
 *
 * <ol>
 *   <li>Understanding decisions and selecting implementations (this class)
 *   <li>Actually performing technology-specific work (the implementations it returns)
 * </ol>
 *
 * <h2>Design Context</h2>
 *
 * <p>Decisions are ADR-style artifacts that document technology choices. Each decision has a topic
 * (e.g., "ProgrammingLanguage") and a choice (e.g., "Java"). Choosing one option may unlock
 * follow-up decisions — for example, choosing Java introduces topics for build tool, test
 * framework, assertion library, and test data library.
 *
 * <p>This class maps those decisions to implementations. For example, given decisions for
 * ProgrammingLanguage=Java, TestFramework=JUnit, AssertionLibrary=AssertJ, and
 * PropertyTesting=JQwik, it returns the appropriate {@link UnitTestGenerator}.
 *
 * <h2>Implementation Approach</h2>
 *
 * <p>The mapping from decisions to implementations is currently hardcoded. This was a deliberate
 * choice over alternatives like ServiceLoader-based discovery or registry patterns, because:
 *
 * <ul>
 *   <li>The number of implementations is expected to be small
 *   <li>Simplicity is preferred during proof-of-concept phase
 *   <li>The interface ({@code unitTestGenerator()}) is stable, so refactoring to a more
 *       sophisticated selection mechanism later is straightforward
 * </ul>
 *
 * <h2>Future Direction</h2>
 *
 * <p>This class will grow additional methods as more generators are needed, such as code generators
 * that produce implementation code to pass the unit tests. The same pattern applies: this class
 * handles selection based on decisions; the generators handle the actual work.
 *
 * <p>If the number of implementations grows significantly or if separate modules per language
 * become necessary, the hardcoded mapping can be refactored to a discovery-based approach.
 *
 * @see Decision
 * @see UnitTestGenerator
 */
@RequiredArgsConstructor
public class TechnologyProvider {

  private final Map<String, String> decisions;

  public TechnologyProvider(Collection<Decision> decisions) {
    this(
        decisions.stream()
            .collect(Collectors.toMap(Decision::getTopic, Decision::getChoice, (a, b) -> a)));
  }

  public Optional<UnitTestGenerator> unitTestGenerator() {
    if ("Java".equals(decisions.get(ProgrammingLanguage.TOPIC))) {
      // TODO: Add JavaJqwikUnitTestGenerator when implemented
      return Optional.empty();
    }
    return Optional.empty();
  }
}
