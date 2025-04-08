package org.setms.sew.intellij;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.antlr.SewLexer;

public class SewLexerAdapter extends LexerBase {

  private CharSequence buffer;
  private int bufferEnd;
  private SewLexer antlrLexer;
  private Token currentToken;

  @Override
  public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
    this.buffer = buffer;
    this.bufferEnd = endOffset;
    CharStream input =
        CharStreams.fromString(buffer.subSequence(startOffset, endOffset).toString());
    antlrLexer = new SewLexer(input);
    advance();
  }

  @Override
  public int getState() {
    return 0;
  }

  @Override
  public IElementType getTokenType() {
    if (currentToken == null || currentToken.getType() == Token.EOF) return null;
    return SewTokenTypes.get(currentToken.getType());
  }

  @Override
  public int getTokenStart() {
    return currentToken != null ? currentToken.getStartIndex() : bufferEnd;
  }

  @Override
  public int getTokenEnd() {
    return currentToken != null ? currentToken.getStopIndex() + 1 : bufferEnd;
  }

  @Override
  public void advance() {
    currentToken = antlrLexer.nextToken();
  }

  @Override
  public @NotNull CharSequence getBufferSequence() {
    return buffer;
  }

  @Override
  public int getBufferEnd() {
    return bufferEnd;
  }
}
