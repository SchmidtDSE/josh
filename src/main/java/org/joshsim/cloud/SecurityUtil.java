/**
 * Security utilities for sanitizing error messages and secure logging.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;

import java.util.regex.Pattern;

/**
 * Utility class for handling security-related operations in the cloud API,
 * particularly around error message sanitization and secure logging.
 */
public class SecurityUtil {

  private static final Pattern API_KEY_PATTERN = Pattern.compile(
      "(?i)api[_\\s-]?key[_\\s-]?[:=]?\\s*[a-zA-Z0-9+/=]{10,}");
  private static final Pattern TOKEN_PATTERN = Pattern.compile(
      "(?i)token[_\\s-]?[:=]?\\s*[a-zA-Z0-9+/=]{10,}");
  private static final Pattern PASSWORD_PATTERN = Pattern.compile(
      "(?i)password[_\\s-]?[:=]?\\s*\\S{6,}");

  /**
   * Sanitizes an error message to remove potential credential information.
   *
   * @param originalMessage The original error message that may contain sensitive data
   * @param exception The original exception for context
   * @return A sanitized error message safe to return to users
   */
  public static String sanitizeErrorMessage(String originalMessage, Throwable exception) {
    if (originalMessage == null) {
      return getGenericErrorMessage(exception);
    }

    String sanitized = originalMessage;
    
    // Remove potential API keys, tokens, and passwords
    sanitized = API_KEY_PATTERN.matcher(sanitized).replaceAll("[REDACTED-API-KEY]");
    sanitized = TOKEN_PATTERN.matcher(sanitized).replaceAll("[REDACTED-TOKEN]");
    sanitized = PASSWORD_PATTERN.matcher(sanitized).replaceAll("[REDACTED-PASSWORD]");
    
    // Remove file paths that might leak system information
    sanitized = sanitized.replaceAll("/[^\\s]*", "[REDACTED-PATH]");
    sanitized = sanitized.replaceAll("\\\\[^\\s]*", "[REDACTED-PATH]");
    
    // If the sanitized message is too revealing, return a generic message
    if (containsSensitiveKeywords(sanitized)) {
      return getGenericErrorMessage(exception);
    }
    
    return sanitized;
  }

  /**
   * Creates a SimulationExecutionException with a sanitized user message.
   *
   * @param userFriendlyMessage A base user-friendly message
   * @param originalException The original exception that occurred
   * @return A new SimulationExecutionException with sanitized message
   */
  public static SimulationExecutionException createSafeException(String userFriendlyMessage, 
        Throwable originalException) {
    String sanitizedMessage = sanitizeErrorMessage(userFriendlyMessage, originalException);
    return new SimulationExecutionException(sanitizedMessage, originalException);
  }

  /**
   * Logs error details securely using the provided API data layer.
   *
   * @param apiDataLayer The API data layer for logging
   * @param apiKey The API key (will be hashed automatically)
   * @param operation The operation that failed
   * @param exception The exception that occurred
   * @param additionalContext Optional additional context
   */
  public static void logSecureError(CloudApiDataLayer apiDataLayer, String apiKey, 
        String operation, Throwable exception, String additionalContext) {
    
    try {
      // Use enhanced logging if available (EnvCloudApiDataLayer)
      if (apiDataLayer instanceof EnvCloudApiDataLayer) {
        ((EnvCloudApiDataLayer) apiDataLayer).logError(apiKey, operation, exception, 
            additionalContext);
      } else {
        // Fallback to standard logging for other implementations
        apiDataLayer.log(apiKey, "error_" + operation, 0);
        // Additional detailed logging to system err for debugging
        String errorDetails = String.format("Error in %s: %s", operation, exception.getMessage());
        if (additionalContext != null && !additionalContext.isEmpty()) {
          errorDetails += " | Context: " + additionalContext;
        }
        System.err.println("[josh cloud error] " + errorDetails + " | Exception: "
            + exception.getClass().getSimpleName());
      }
    } catch (Exception logException) {
      // If logging fails, at least try to log that logging failed
      System.err.println("[josh cloud error] Failed to log error: " + logException.getMessage());
    }
  }

  /**
   * Returns a generic error message based on exception type.
   *
   * @param exception The exception to classify
   * @return A generic but helpful error message
   */
  private static String getGenericErrorMessage(Throwable exception) {
    if (exception == null) {
      return "An error occurred during simulation execution";
    }
    
    String exceptionType = exception.getClass().getSimpleName();
    
    // Provide specific guidance for common exception types
    if (exceptionType.contains("Parse") || exceptionType.contains("Syntax")) {
      return "Invalid simulation code syntax";
    } else if (exceptionType.contains("Runtime") || exceptionType.contains("Execution")) {
      return "Error occurred during simulation execution";
    } else if (exceptionType.contains("IO") || exceptionType.contains("File")) {
      return "Error accessing simulation data";
    } else if (exceptionType.contains("Memory") || exceptionType.contains("OutOfMemory")) {
      return "Simulation requires too much memory";
    } else if (exceptionType.contains("Timeout")) {
      return "Simulation execution timed out";
    } else {
      return "An error occurred during simulation processing";
    }
  }

  /**
   * Checks if a message contains sensitive keywords that should not be exposed.
   *
   * @param message The message to check
   * @return true if the message contains sensitive information
   */
  private static boolean containsSensitiveKeywords(String message) {
    if (message == null) {
      return false;
    }
    
    String lowerMessage = message.toLowerCase();
    
    // Keywords that suggest internal system details
    String[] sensitiveKeywords = {
      "internal", "system", "database", "server", "host", "port", 
      "connection", "authentication", "credential", "secret", "private",
      "class ", "method ", "function ", "stack", "trace"
    };
    
    for (String keyword : sensitiveKeywords) {
      if (lowerMessage.contains(keyword)) {
        return true;
      }
    }
    
    return false;
  }
}