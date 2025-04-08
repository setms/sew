package org.setms.sew.intellij;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import java.util.HashMap;
import java.util.Map;
import org.setms.sew.antlr.SewLexer;

public final class SewTokenTypes {

  public static final IElementType IDENTIFIER = new SewElementType("IDENTIFIER");
  public static final IElementType STRING = new SewElementType("STRING");
  public static final IElementType COMMENT = new SewElementType("COMMENT");
  public static final IElementType WS = new SewElementType("WS");

  public static final IElementType PACKAGE = new SewElementType("'package'");
  public static final IElementType DOT = new SewElementType("'.'");
  public static final IElementType EQ = new SewElementType("'='");
  public static final IElementType LBRACE = new SewElementType("'{'");
  public static final IElementType RBRACE = new SewElementType("'}'");
  public static final IElementType LBRACK = new SewElementType("'['");
  public static final IElementType RBRACK = new SewElementType("']'");
  public static final IElementType COMMA = new SewElementType("','");

  private static final Map<Integer, IElementType> tokenTypeMap = new HashMap<>();

  static {
    tokenTypeMap.put(SewLexer.IDENTIFIER, IDENTIFIER);
    tokenTypeMap.put(SewLexer.STRING, STRING);
    tokenTypeMap.put(SewLexer.COMMENT, COMMENT);
    tokenTypeMap.put(SewLexer.WS, WS);

    // Keywords and punctuation (by literal token types)
    tokenTypeMap.put(SewLexer.T__0, PACKAGE); // 'package'
    tokenTypeMap.put(SewLexer.T__1, DOT); // '.'
    tokenTypeMap.put(SewLexer.T__2, LBRACE); // '{'
    tokenTypeMap.put(SewLexer.T__3, RBRACE); // '}'
    tokenTypeMap.put(SewLexer.T__4, EQ); // '='
    tokenTypeMap.put(SewLexer.T__5, LBRACK); // '['
    tokenTypeMap.put(SewLexer.T__6, RBRACK); // ']'
    tokenTypeMap.put(SewLexer.T__7, COMMA); // ','
  }

  public static IElementType get(int antlrTokenType) {
    return tokenTypeMap.getOrDefault(antlrTokenType, TokenType.BAD_CHARACTER);
  }

  private SewTokenTypes() {}
}
