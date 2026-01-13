/**
 * Strategy to write to standard output (stdout).
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.OutputStream;

/**
 * OutputStreamStrategy implementation that writes to standard output.
 *
 * <p>This strategy returns System.out as the OutputStream. Note that the caller
 * should NOT close the returned stream, as closing System.out would prevent
 * further console output.</p>
 */
public class StdoutOutputStreamStrategy implements OutputStreamStrategy {

  @Override
  public OutputStream open() {
    return System.out;
  }

}
