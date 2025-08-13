/**
 * Parser for configuration input files (.jshc format).
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.config;

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
   */
  private State processComment(char c) {
    return switch (c) {
      case '\n', '\r' -> State.IDLE;
      default -> State.IN_COMMENT; // Continue ignoring until newline
    };
  }

  /**
   * Processes characters in the IN_VARIABLE_NAME state.
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
   */
  private EngineValue parseValueWithUnits(String valueString, String variableName) {
    String trimmed = valueString.trim();
    
    // First try to split on whitespace
    String[] parts = trimmed.split("\\s+");
    
    if (parts.length == 1) {
      // Check if this is a number followed immediately by units (like "10m")
      String part = parts[0];
      
      // Try to find where the number ends and units begin
      int numberEnd = 0;
      boolean foundDecimalPoint = false;
      boolean foundExponent = false;
      
      // Handle negative numbers
      if (numberEnd < part.length()
          && (part.charAt(numberEnd) == '-' || part.charAt(numberEnd) == '+')) {
        numberEnd++;
      }
      
      // Find the end of the numeric part
      while (numberEnd < part.length()) {
        char c = part.charAt(numberEnd);
        if (Character.isDigit(c)) {
          numberEnd++;
        } else if (c == '.' && !foundDecimalPoint) {
          foundDecimalPoint = true;
          numberEnd++;
        } else if ((c == 'e' || c == 'E') && !foundExponent && numberEnd > 0) {
          foundExponent = true;
          numberEnd++;
          // Check for optional sign after 'e'
          if (numberEnd < part.length()
              && (part.charAt(numberEnd) == '-' || part.charAt(numberEnd) == '+')) {
            numberEnd++;
          }
        } else {
          break; // End of numeric part
        }
      }
      
      if (numberEnd == 0) {
        throw new IllegalArgumentException(
            "Invalid number format '" + part + "' for variable '" + variableName 
            + "' (line " + lineNumber + ")");
      }
      
      if (numberEnd == part.length()) {
        // Just a number, no units
        try {
          return factory.parseNumber(part, Units.EMPTY);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(
              "Invalid number format '" + part + "' for variable '" + variableName 
              + "' (line " + lineNumber + ")", e);
        }
      } else {
        // Number followed by units without space (like "10m")
        String numberPart = part.substring(0, numberEnd);
        String unitsPart = part.substring(numberEnd);
        
        try {
          Units units = Units.of(unitsPart);
          return factory.parseNumber(numberPart, units);
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
    } else if (parts.length >= 2) {
      // Number with units separated by spaces - reconstruct units from all parts after the first
      String numberPart = parts[0];
      StringBuilder unitsBuilder = new StringBuilder();
      for (int i = 1; i < parts.length; i++) {
        if (i > 1) {
          unitsBuilder.append(" ");
        }
        unitsBuilder.append(parts[i]);
      }
      String unitsPart = unitsBuilder.toString();
      
      try {
        Units units = Units.of(unitsPart);
        return factory.parseNumber(numberPart, units);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "Invalid number format '" + numberPart + "' for variable '" + variableName 
            + "' (line " + lineNumber + ")", e);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Invalid units '" + unitsPart + "' for variable '" + variableName 
            + "' (line " + lineNumber + ")", e);
      }
    } else {
      throw new IllegalArgumentException(
          "Invalid value format '" + valueString + "' for variable '" + variableName 
          + "' (line " + lineNumber + ")");
    }
  }

  /**
   * Checks if a character is valid for starting a variable name.
   */
  private boolean isValidVariableNameStart(char c) {
    return Character.isLetter(c) || c == '_';
  }

  /**
   * Checks if a character is valid within a variable name.
   */
  private boolean isValidVariableNameChar(char c) {
    return Character.isLetterOrDigit(c) || c == '_';
  }
}