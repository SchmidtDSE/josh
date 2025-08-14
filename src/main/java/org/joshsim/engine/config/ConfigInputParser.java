/**
 * Parser for configuration input files (.jshc format).
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Parses configuration input files in jshc format.
 *
 * <p>Implements a finite state automaton to parse jshc files containing variable definitions
 * in the format "variableName = value units". Supports comments starting with # and ignores
 * empty lines. Based on the parsing pattern from cdibase UploadParserAutomaton.</p>
 */
public class ConfigInputParser {

  /**
   * States for the parsing automaton.
   */
  private enum State {
    IDLE,           // Looking for start of variable name or comment
    IN_COMMENT,     // Processing comment (ignore until newline)
    IN_VARIABLE_NAME, // Parsing variable name
    IN_EQUALS_SECTION, // Processing equals sign and whitespace
    IN_VALUE        // Parsing value and units
  }

  private final EngineValueFactory factory;
  private State currentState;
  private StringBuilder varName;
  private StringBuilder value;
  private ConfigBuilder builder;
  private int lineNumber;
  private boolean equalsFound;

  /**
   * Creates a new ConfigInputParser.
   *
   * @param factory the EngineValueFactory to use for creating values
   */
  public ConfigInputParser(EngineValueFactory factory) {
    this.factory = factory;
    reset();
  }

  /**
   * Parses the input string and returns a Config.
   *
   * @param input the jshc file content to parse
   * @return the parsed Config
   * @throws IllegalArgumentException if the input is malformed
   */
  public Config parse(String input) {
    reset();

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);

      if (c == '\n') {
        lineNumber++;
      }

      currentState = switch (currentState) {
        case IDLE -> processIdle(c, i);
        case IN_COMMENT -> processComment(c);
        case IN_VARIABLE_NAME -> processVariableName(c, i);
        case IN_EQUALS_SECTION -> processEqualsSection(c, i);
        case IN_VALUE -> processValue(c);
      };
    }

    // Handle case where file ends while parsing a value
    if (currentState == State.IN_VALUE) {
      finalizeVariable();
    }

    return builder.build();
  }

  /**
   * Resets the parser state for a new parse operation.
   *
   * <p>Initializes all state variables to their default values.</p>
   */
  private void reset() {
    currentState = State.IDLE;
    varName = new StringBuilder();
    value = new StringBuilder();
    builder = new ConfigBuilder();
    lineNumber = 1;
    equalsFound = false;
  }

  /**
   * Processes characters in the IDLE state.
   *
   * <p>Handles the start of new lines, looking for comments or variable names.</p>
   *
   * @param c the character to process
   * @param position the position in the input string
   * @return the next state to transition to
   * @throws IllegalArgumentException if an invalid character is encountered
   */
  private State processIdle(char c, int position) {
    return switch (c) {
      case '#' -> State.IN_COMMENT;
      case ' ', '\t', '\n', '\r' -> State.IDLE; // Skip whitespace
      default -> {
        if (isValidVariableNameStart(c)) {
          varName.setLength(0);
          varName.append(c);
          yield State.IN_VARIABLE_NAME;
        } else {
          throw new IllegalArgumentException(
              "Invalid character '" + c + "' at position " + position
              + " (line " + lineNumber + "). Expected variable name or comment.");
        }
      }
    };
  }

  /**
   * Processes characters in the IN_COMMENT state.
   *
   * <p>Ignores all characters until a newline is encountered.</p>
   *
   * @param c the character to process
   * @return the next state to transition to
   */
  private State processComment(char c) {
    return switch (c) {
      case '\n', '\r' -> State.IDLE;
      default -> State.IN_COMMENT; // Continue ignoring until newline
    };
  }

  /**
   * Processes characters in the IN_VARIABLE_NAME state.
   *
   * <p>Builds up the variable name character by character until an equals sign or
   * whitespace is encountered.</p>
   *
   * @param c the character to process
   * @param position the position in the input string
   * @return the next state to transition to
   * @throws IllegalArgumentException if an invalid character is encountered
   */
  private State processVariableName(char c, int position) {
    return switch (c) {
      case '=' -> {
        if (varName.length() == 0) {
          throw new IllegalArgumentException(
              "Empty variable name at position " + position
              + " (line " + lineNumber + ")");
        }
        equalsFound = true;
        yield State.IN_EQUALS_SECTION;
      }
      case ' ', '\t' -> State.IN_EQUALS_SECTION;
      case '\n', '\r' -> {
        throw new IllegalArgumentException(
            "Missing equals sign after variable name '" + varName.toString()
            + "' at position " + position + " (line " + lineNumber + ")");
      }
      default -> {
        if (isValidVariableNameChar(c)) {
          varName.append(c);
          yield State.IN_VARIABLE_NAME;
        } else {
          throw new IllegalArgumentException(
              "Invalid character '" + c + "' in variable name at position " + position
              + " (line " + lineNumber + ")");
        }
      }
    };
  }

  /**
   * Processes characters in the IN_EQUALS_SECTION state.
   *
   * <p>Handles the area around the equals sign, allowing optional whitespace.</p>
   *
   * @param c the character to process
   * @param position the position in the input string
   * @return the next state to transition to
   * @throws IllegalArgumentException if missing equals or value
   */
  private State processEqualsSection(char c, int position) {
    return switch (c) {
      case '=' -> {
        if (!equalsFound) {
          equalsFound = true;
        }
        yield State.IN_EQUALS_SECTION; // Continue looking for value
      }
      case ' ', '\t' -> State.IN_EQUALS_SECTION; // Skip whitespace around equals
      default -> {
        if (c == '\n' || c == '\r') {
          if (equalsFound) {
            throw new IllegalArgumentException(
                "Missing value after equals sign at position " + position
                + " (line " + lineNumber + ")");
          } else {
            throw new IllegalArgumentException(
                "Missing equals sign after variable name '" + varName.toString()
                + "' at position " + position + " (line " + lineNumber + ")");
          }
        }
        if (!equalsFound) {
          throw new IllegalArgumentException(
              "Missing equals sign after variable name '" + varName.toString()
              + "' at position " + position + " (line " + lineNumber + ")");
        }
        value.setLength(0);
        value.append(c);
        yield State.IN_VALUE;
      }
    };
  }

  /**
   * Processes characters in the IN_VALUE state.
   *
   * <p>Accumulates the value string until a newline or comment is encountered.</p>
   *
   * @param c the character to process
   * @return the next state to transition to
   */
  private State processValue(char c) {
    return switch (c) {
      case '\n', '\r' -> {
        finalizeVariable();
        yield State.IDLE;
      }
      case '#' -> {
        finalizeVariable();
        yield State.IN_COMMENT;
      }
      default -> {
        value.append(c);
        yield State.IN_VALUE;
      }
    };
  }

  /**
   * Finalizes the current variable by parsing its value and adding it to the builder.
   *
   * <p>Validates that the variable name and value are non-empty before adding to config.</p>
   *
   * @throws IllegalArgumentException if the value is empty
   */
  private void finalizeVariable() {
    if (varName.length() == 0) {
      return; // No variable to finalize
    }

    String variableName = varName.toString().trim();
    String valueString = value.toString().trim();

    if (valueString.isEmpty()) {
      throw new IllegalArgumentException(
          "Empty value for variable '" + variableName + "' (line " + lineNumber + ")");
    }

    // Parse value and units
    EngineValue engineValue = parseValueWithUnits(valueString, variableName);
    builder.addValue(variableName, engineValue);

    // Reset for next variable
    varName.setLength(0);
    value.setLength(0);
    equalsFound = false;
  }

  /**
   * Parses a value string that may contain units.
   *
   * <p>Parses values in the format supported by Josh: optional sign, digits with optional
   * decimal point, followed by optional units. No exponential notation is supported.</p>
   *
   * @param valueString the string containing a number and optional units
   * @param variableName the name of the variable being parsed (for error messages)
   * @return the parsed EngineValue with appropriate units
   * @throws IllegalArgumentException if the value format is invalid
   */
  private EngineValue parseValueWithUnits(String valueString, String variableName) {
    String trimmed = valueString.trim();

    // Use regex to match Josh number format and units in one pass
    // Pattern: optional +/-, digits, optional decimal point and more digits, then optional units
    String valuePattern = "^([+-]?\\d+(?:\\.\\d+)?)\\s*(.*)$";
    Pattern pattern = Pattern.compile(valuePattern);
    Matcher matcher = pattern.matcher(trimmed);

    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          "Invalid value format '" + trimmed + "' for variable '" + variableName
          + "' (line " + lineNumber + ")");
    }

    String numberPart = matcher.group(1);
    String unitsPart = matcher.group(2).trim();

    try {
      // Parse based on whether units are present
      if (unitsPart.isEmpty()) {
        return factory.parseNumber(numberPart, Units.EMPTY);
      } else {
        Units units = Units.of(unitsPart);
        return factory.parseNumber(numberPart, units);
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Invalid number format '" + numberPart + "' for variable '" + variableName
          + "' (line " + lineNumber + ")", e);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid units '" + unitsPart + "' for variable '" + variableName
          + "' (line " + lineNumber + ")", e);
    }
  }

  /**
   * Checks if a character is valid for starting a variable name.
   *
   * <p>According to Josh grammar: IDENTIFIER_: [A-Za-z][A-Za-z0-9]*</p>
   *
   * @param c the character to check
   * @return true if the character is a letter, false otherwise
   */
  private boolean isValidVariableNameStart(char c) {
    return Character.isLetter(c);
  }

  /**
   * Checks if a character is valid within a variable name.
   *
   * <p>According to Josh grammar: IDENTIFIER_: [A-Za-z][A-Za-z0-9]*</p>
   *
   * @param c the character to check
   * @return true if the character is a letter or digit, false otherwise
   */
  private boolean isValidVariableNameChar(char c) {
    return Character.isLetterOrDigit(c);
  }
}
