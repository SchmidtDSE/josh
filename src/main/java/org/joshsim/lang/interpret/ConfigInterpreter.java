/**
 * ANTLR-based interpreter for Josh configuration files (.jshc format).
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.joshsim.engine.config.Config;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.lang.antlr.JoshConfigLexer;
import org.joshsim.lang.antlr.JoshConfigParser;
import org.joshsim.lang.interpret.fragment.jshc.JshcFragment;
import org.joshsim.lang.interpret.visitor.JoshConfigParserVisitor;

/**
 * Interprets Josh configuration files using ANTLR-generated parser.
 *
 * <p>This class replaces the custom automaton-based ConfigInputParser with
 * an ANTLR-based implementation for consistency with the rest of the Josh language processing.</p>
 */
public class ConfigInterpreter {

  /**
   * Custom error listener for ANTLR parsing that converts syntax errors to
   * IllegalArgumentException.
   */
  private static class ConfigErrorListener extends org.antlr.v4.runtime.BaseErrorListener {
    @Override
    public void syntaxError(org.antlr.v4.runtime.Recognizer<?, ?> recognizer,
                           Object offendingSymbol, int line, int charPositionInLine,
                           String msg, org.antlr.v4.runtime.RecognitionException e) {
      String errorMsg = "Parse error at line " + line + ":" + charPositionInLine + " " + msg;
      throw new IllegalArgumentException(errorMsg);
    }
  }

  /**
   * Interprets a Josh configuration file content and returns a Config.
   *
   * @param configContent The content of the .jshc file as a string
   * @param valueFactory The EngineValueFactory to use for creating values
   * @return A Config instance containing the parsed configuration values
   * @throws IllegalArgumentException if parsing fails due to syntax errors or invalid content
   */
  public Config interpret(String configContent, EngineValueFactory valueFactory) {
    try {
      // Create lexer and parser
      JoshConfigLexer lexer = new JoshConfigLexer(CharStreams.fromString(configContent));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      JoshConfigParser parser = new JoshConfigParser(tokens);

      // Add error handling
      parser.removeErrorListeners(); // Remove default console error listener
      parser.addErrorListener(new ConfigErrorListener());

      // Parse the config content
      ParseTree tree = parser.config();

      // Create visitor and visit the parse tree
      JoshConfigParserVisitor visitor = new JoshConfigParserVisitor(valueFactory);
      JshcFragment fragment = visitor.visit(tree);

      // Extract Config from the resulting fragment
      return fragment.getConfigBuilder().build();
    } catch (IllegalArgumentException e) {
      // Re-throw parse errors with consistent messaging
      if (e.getMessage().startsWith("Parse error at line")) {
        String errorMsg = "Failed to parse configuration file: '" + configContent + "'";
        throw new IllegalArgumentException(errorMsg, e);
      }
      throw e;
    }
  }
}
