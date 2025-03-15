package org.joshsim.lang.compiler;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.joshsim.lang.JoshLangLexer;
import org.joshsim.lang.JoshLangParser;


public class Compiler {

  public ParseResult parse(String inputCode) {
    CharStream input = CharStreams.fromString(inputCode);
    JoshLangLexer lexer = new JoshLangLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    JoshLangParser parser = new JoshLangParser(tokens);

    List<ParseError> parseErrors = new ArrayList<>();
    BaseErrorListener listener = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                int charPositionInLine, String msg, RecognitionException e) {
          parseErrors.add(new ParseError(line, msg));
        }
    }

    parser.addErrorListener(listener);

    JoshLangParser.ProgramContext program = parser.program();

    if (parseErrors.isEmpty()) {
      
    } else {
      
    }
  }

}