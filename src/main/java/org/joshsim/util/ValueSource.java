package org.joshsim.util;

/**
 * Enum representing the different sources of configuration values.
 */
public enum ValueSource {
    DIRECT,
    CONFIG_FILE,
    ENVIRONMENT,
    DEFAULT,
    NOT_FOUND
}
