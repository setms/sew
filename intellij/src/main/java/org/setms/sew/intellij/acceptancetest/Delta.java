package org.setms.sew.intellij.acceptancetest;

public record Delta(int start, int end, String text, int newCaret) {}
