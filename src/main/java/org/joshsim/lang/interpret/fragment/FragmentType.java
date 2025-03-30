
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
  /** Fragment containing an event handler action. */
  ACTION,
  
  /** Fragment containing a compiled callable. */
  COMPILED_CALLABLE,
  
  /** Fragment containing a single unit conversion. */
  CONVERSION,
  
  /** Fragment containing multiple unit conversions. */
  CONVERSIONS,
  
  /** Fragment containing an entity prototype. */
  ENTITY,
  
  /** Fragment containing a group of event handlers. */
  EVENT_HANDLER_GROUP,
  
  /** Fragment containing a complete program. */
  PROGRAM,
  
  /** Fragment containing state information for an entity. */
  STATE
}
