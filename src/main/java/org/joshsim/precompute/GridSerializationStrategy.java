/**
 * Definition for a strategy which serializes or deserializes a PrecomputedGrid.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.io.InputStream;
import java.io.OutputStream;


/**
 * Strategy which can send a precomputed grid into an output stream or parse from an input stream.
 */
public interface GridSerializationStrategy {

  /**
   * Serialize a grid to an output stream.
   *
   * @param target grid to send to output stream
   * @param outputStream the target location for the grid
   */
  void serialize(PrecomputedGrid target, OutputStream outputStream);

  /**
   * Deserialize a grid from an output stream.
   *
   * @param inputStream grid from which to read the precomputed grid
   * @return the target from which to get the grid
   */
  PrecomputedGrid deserialize(InputStream inputStream);

}
