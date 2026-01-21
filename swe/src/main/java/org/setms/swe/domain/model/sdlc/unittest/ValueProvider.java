package org.setms.swe.domain.model.sdlc.unittest;

import java.util.function.Supplier;

// TODO: Shouldn't be Supplier<Object> but the **description** of a Supplier<Object>.
// IOW, it's something that allows computation of the value at runtime of the unit test, not runtime
// of the class that describes the unit test (i.e. UnitTest).
public interface ValueProvider extends Supplier<Object> {}
