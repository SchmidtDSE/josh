/**
 * OutputStreamStrategy that writes to standard output.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Strategy that provides System.out as an OutputStream.
 *
 * <p>This strategy is used for stdout:// destinations in debug output. It simply returns
 * System.out which allows debug messages to be written directly to the console.</p>
 *
 * <p>Note: The returned stream should not be closed as it would close System.out, which
 * would affect all subsequent output to stdout.</p>
 */
public class StdoutOutputStreamStrategy implements OutputStreamStrategy {

  @Override
  public OutputStream open() throws IOException {
    return System.out;
  }
}
