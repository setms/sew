package org.setms.sew.tool;

public class ToolException extends RuntimeException {

  public ToolException(String message, Throwable cause) {
    super(message, cause);
  }

  public ToolException(String message) {
    super(message);
  }
}
