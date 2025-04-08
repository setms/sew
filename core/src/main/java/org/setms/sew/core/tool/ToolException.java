package org.setms.sew.core.tool;

public class ToolException extends RuntimeException {

  public ToolException(String message, Throwable cause) {
    super(message, cause);
  }

  public ToolException(String message) {
    super(message);
  }
}
