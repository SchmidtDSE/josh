
/**
 * Enum describing how a declared entity relates to a prior same-named definition.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.prototype;

/**
 * How an entity stanza's opening keyword (start / replace / update) relates to a prior entity of
 * the same name.
 */
public enum EntityOverwriteBehavior {

  /** {@code start}: declares a new entity; a prior same-named entity is an error. */
  NOT_SPECIFIED,

  /** {@code replace}: fully replaces a prior same-named entity, which must already exist. */
  OVERWRITE,

  /**
   * {@code update}: merges onto a prior same-named entity, which must already exist. Handlers this
   * declaration redeclares override the prior entity's matching handler; every other handler and
   * attribute carries over unchanged.
   */
  UPDATE

}
