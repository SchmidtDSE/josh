/**
 * Strategy which reads and writes files using the jshd format.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.io.InputStream;
import java.io.OutputStream;


/**
 * Strategy which reads or writes a grid serialization with binary values.
 *
 * <p>Strategy which reads or writes a grid serialization through a binary format. This uses the
 * jshd binary format.</p>
 */
public class BinaryGridSerializationStrategy implements GridSerializationStrategy {

  @Override
  public void serialize(PrecomputedGrid target, OutputStream outputStream) {

  }

  @Override
  public PrecomputedGrid deserialize(InputStream inputStream) {
    return null;
  }

}
