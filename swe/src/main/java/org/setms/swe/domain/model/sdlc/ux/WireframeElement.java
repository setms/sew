package org.setms.swe.domain.model.sdlc.ux;

/** An element that can appear inside a wireframe container. */
public sealed interface WireframeElement
    permits Container, Affordance, InputField, View, Feedback {}
