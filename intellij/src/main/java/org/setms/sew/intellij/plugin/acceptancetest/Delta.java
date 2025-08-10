package org.setms.sew.intellij.plugin.acceptancetest;

public record Delta(int start, int end, String text, int newCaret) {}
