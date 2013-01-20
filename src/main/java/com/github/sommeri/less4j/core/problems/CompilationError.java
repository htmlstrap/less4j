package com.github.sommeri.less4j.core.problems;

import java.net.URL;

import com.github.sommeri.less4j.LessCompiler.Problem;
import com.github.sommeri.less4j.core.ast.ASTCssNode;

public class CompilationError implements Problem {
  
  private final URL file;
  private final int line;
  private final int character;
  private final String message;

  public CompilationError(URL file, int line, int character, String message) {
    super();
    this.file = file;
    this.line = line;
    this.character = character;
    this.message = message;
  }

  public CompilationError(ASTCssNode offendingNode, String message) {
    super();
    this.file = offendingNode.getSourceFile();
    this.line = offendingNode.getSourceLine();
    this.character = offendingNode.getCharPositionInSourceLine();
    this.message = message;
  }

  @Override
  public Type getType() {
    return Type.ERROR;
  }

  public URL getFile() {
    return file;
  }

  @Override
  public int getLine() {
    return line;
  }

  @Override
  public int getCharacter() {
    return character;
  }

  @Override
  public String getMessage() {
    return message;
  }

}
