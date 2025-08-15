
/**
 * Enumeration of different types of program fragments.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment;

/**
 * Defines the different types of fragments that can exist in a Josh program.
 *
 * <p>Each enum value represents a distinct type of program fragment that can be
 * processed during program interpretation.</p>
 */
public enum FragmentType {
  /** JoshFragment containing an event handler action. */
  ACTION,

  /** JoshFragment containing a compiled callable. */
  COMPILED_CALLABLE,

  /** JoshFragment containing a single unit conversion. */
  CONVERSION,

  /** JoshFragment containing multiple unit conversions. */
  CONVERSIONS,

  /** JoshFragment containing an entity prototype. */
  ENTITY,

  /** JoshFragment containing a group of event handlers. */
  EVENT_HANDLER_GROUP,

  /** JoshFragment containing a complete program. */
  PROGRAM,

  /** JoshFragment containing state information for an entity. */
  STATE,

  /** Fragment for configuration variable discovery. */
  CONFIG_DISCOVERABILITY,

  /** Fragment containing a set of discovered config variables. */
  CONFIG_VAR_SET,

  /** Fragment containing a single config value for discovery. */
  CONFIG_VALUE,

  /** Fragment containing configuration data. */
  CONFIG,

  /** Fragment containing a comment or empty line. */
  COMMENT
}
