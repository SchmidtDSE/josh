/**
 * Structure to describe strategies for different kinds of output streams.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.IOException;
import java.io.OutputStream;


/**
 * Interface defining a strategy for providing an output stream for data writing purposes.
 *
 * <p>Implementations of this interface specify how and where to open an OutputStream for data
 * writing, enabling flexibility in defining output destinations like for local file storage, cloud
 * storage, or other persistence mechanisms.</p>
 */
public interface OutputStreamStrategy {

  /**
   * Opens a new OutputStream for writing data.
   *
   * @return an OutputStream instance ready for writing data which must be closed from outside this
   *     strategy.
   * @throws IOException if the stream cannot be opened or other I/O errors occur.
   */
  OutputStream open() throws IOException;

}
