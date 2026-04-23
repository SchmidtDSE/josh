/**
 * Composite external resource getter that supports both jshd and jshdz formats.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import org.joshsim.lang.bridge.ExternalResourceGetter;


/**
 * Composite {@link ExternalResourceGetter} that routes requests by file extension.
 *
 * <p>Supports {@code .jshd} files via {@link JshdExternalGetter} and {@code .jshdz} (XZ-compressed)
 * files via {@link JshdzExternalGetter}. This class is JVM-only and must not be referenced from
 * any code path compiled by TeaVM.</p>
 */
public class MultiFormatExternalGetter implements ExternalResourceGetter {

  private final JshdExternalGetter jshdGetter;
  private final JshdzExternalGetter jshdzGetter;

  /**
   * Construct a multi-format getter backed by the two format-specific getters.
   *
   * @param jshdGetter Getter for uncompressed {@code .jshd} files.
   * @param jshdzGetter Getter for XZ-compressed {@code .jshdz} files.
   */
  public MultiFormatExternalGetter(JshdExternalGetter jshdGetter, JshdzExternalGetter jshdzGetter) {
    this.jshdGetter = jshdGetter;
    this.jshdzGetter = jshdzGetter;
  }

  @Override
  public DataGridLayer getResource(String name) {
    if (name.endsWith(".jshdz")) {
      return jshdzGetter.getResource(name);
    } else if (name.endsWith(".jshd")) {
      return jshdGetter.getResource(name);
    } else {
      throw new IllegalArgumentException(
          "Expected a .jshd or .jshdz file name. Got: " + name
      );
    }
  }

}
