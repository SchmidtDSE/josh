/**
 * Logic to write to local files as part of Josh export.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.export;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * Strategy which opens an OutputStream to a local file.
 */
public class LocalOutputStreamStrategy implements OutputStreamStrategy {

  private final String location;

  /**
   * Constructs a LocalOutputStreamStrategy with the specified file location.
   *
   * @param location The local file path where the OutputStream will write data.
   */
  public LocalOutputStreamStrategy(String location) {
    this.location = location;
  }

  @Override
  public OutputStream open() throws IOException {
    return new FileOutputStream(location);
  }

}
