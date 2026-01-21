package org.setms.swe.domain.model.sdlc.technology;

import org.setms.swe.domain.model.sdlc.acceptancetest.AcceptanceTest;

/**
 * Generates unit test code from an acceptance test.
 *
 * <p>Implementations are language and framework specific (e.g., Java/JUnit/AssertJ/JQwik). They
 * know nothing about architectural decisions — the selection of which implementation to use is
 * handled by {@link TechnologyProvider}.
 */
public interface UnitTestGenerator {

  GeneratedCode generate(AcceptanceTest acceptanceTest);
}
