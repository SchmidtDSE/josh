/**
 * Entrypoint into parser machinery for the Josh DSL.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.parse;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.RecognitionException;
import org.joshsim.lang.antlr.JoshLangLexer;
import org.joshsim.lang.antlr.JoshLangParser;


/**
 * Entrypoint for the Josh DSL parser step.
 *
 * <p>Entry point to the parser machinery for the Josh DSL (Domain Specific Language). It
 * leverages ANTLR for, capturing any syntax errors encountered during parsing.</p>
 */
public class Parser {

  /**
   * Constructs a Compiler object. This constructor initializes any necessary
   * configuration or state for the compilation process of Josh DSL.
   */
  public ParseResult parse(String inputCode) {
    CharStream input = CharStreams.fromString(inputCode);
    JoshLangLexer lexer = new JoshLangLexer(input);
    // Remove default error listeners that print to console
    lexer.removeErrorListeners();
    
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    JoshLangParser parser = new JoshLangParser(tokens);
    parser.removeErrorListeners();

    List<ParseError> parseErrors = new ArrayList<>();
    BaseErrorListener listener = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                int charPositionInLine, String msg, RecognitionException e) {
          parseErrors.add(new ParseError(line, msg));
        }
    };

    // Add our error listener to both lexer and parser
    lexer.addErrorListener(listener);
    parser.addErrorListener(listener);

    JoshLangParser.ProgramContext program = parser.program();

    return parseErrors.isEmpty() ? new ParseResult(program) : new ParseResult(parseErrors);
  }

}