/**
 * Options for preprocessing external data into jshd format.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.util.Objects;


/**
 * The optional inputs to {@link PreprocessUtil#preprocess}, corresponding to the {@code preprocess}
 * CLI flags.
 *
 * <p>Built through {@link #builder()}, which starts from the CLI's own defaults so an unset value
 * behaves exactly as an omitted flag does. Instances are immutable and shared by every surface that
 * preprocesses: the {@code preprocess} and {@code preprocessBatch} commands, the
 * {@code /preprocessBatch} handler, and the {@code preprocess_data} MCP tool.</p>
 */
public final class PreprocessOptions {

  private static final String DEFAULT_CRS_CODE = "EPSG:4326";
  private static final String DEFAULT_HORIZ_COORD_NAME = "lon";
  private static final String DEFAULT_VERT_COORD_NAME = "lat";
  private static final String DEFAULT_TIME_NAME = "calendar_year";

  private final String crsCode;
  private final String horizCoordName;
  private final String vertCoordName;
  private final String timeName;
  private final String timestep;
  private final String defaultValue;
  private final boolean parallel;
  private final boolean amend;
  private final TimeAxisParams timeAxis;

  private PreprocessOptions(Builder builder) {
    this.crsCode = builder.crsCode;
    this.horizCoordName = builder.horizCoordName;
    this.vertCoordName = builder.vertCoordName;
    this.timeName = builder.timeName;
    this.timestep = builder.timestep;
    this.defaultValue = builder.defaultValue;
    this.parallel = builder.parallel;
    this.amend = builder.amend;
    this.timeAxis = builder.timeAxis;
  }

  /**
   * Returns a builder preloaded with the {@code preprocess} CLI defaults.
   *
   * @return A new builder.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns the options a bare {@code preprocess} invocation would use.
   *
   * @return Options with every value at its CLI default.
   */
  public static PreprocessOptions defaults() {
    return builder().build();
  }

  public String getCrsCode() {
    return crsCode;
  }

  public String getHorizCoordName() {
    return horizCoordName;
  }

  public String getVertCoordName() {
    return vertCoordName;
  }

  /**
   * Returns the source's time dimension name, or the empty string for a timeless source.
   *
   * @return The dimension name, or {@code ""} if the source has no time dimension.
   */
  public String getTimeName() {
    return timeName;
  }

  /**
   * Returns the single timestep to process, or the empty string to process the whole step range.
   *
   * @return The timestep as a string, or {@code ""}.
   */
  public String getTimestep() {
    return timestep;
  }

  /**
   * Returns the fill value for grid cells the source does not cover.
   *
   * @return The default value, or null if none was set.
   */
  public String getDefaultValue() {
    return defaultValue;
  }

  public boolean isParallel() {
    return parallel;
  }

  public boolean isAmend() {
    return amend;
  }

  public TimeAxisParams getTimeAxis() {
    return timeAxis;
  }

  /**
   * Builder for {@link PreprocessOptions}.
   *
   * <p>Every setter is optional; an unset value keeps the CLI default. Setters reject null rather
   * than treating it as "use the default", because the two classes this replaced disagreed about
   * what a null time dimension meant - one read it as the default dimension name, the other as a
   * source with no time dimension at all. Leave a value unset for the default, and call
   * {@link #noTimeDim()} for a timeless source.</p>
   */
  public static final class Builder {
    private String crsCode = DEFAULT_CRS_CODE;
    private String horizCoordName = DEFAULT_HORIZ_COORD_NAME;
    private String vertCoordName = DEFAULT_VERT_COORD_NAME;
    private String timeName = DEFAULT_TIME_NAME;
    private String timestep = "";
    private String defaultValue;
    private boolean parallel;
    private boolean amend;
    private TimeAxisParams timeAxis = TimeAxisParams.EMPTY;

    private Builder() {
      // Use PreprocessOptions.builder()
    }

    /**
     * Sets the coordinate reference system code used to read the source.
     *
     * @param value CRS code such as {@code EPSG:4326}; must not be null.
     * @return This builder.
     */
    public Builder crsCode(String value) {
      this.crsCode = require(value, "crsCode");
      return this;
    }

    /**
     * Sets the name of the source's horizontal coordinate dimension.
     *
     * @param value Dimension name such as {@code lon}; must not be null.
     * @return This builder.
     */
    public Builder horizCoordName(String value) {
      this.horizCoordName = require(value, "horizCoordName");
      return this;
    }

    /**
     * Sets the name of the source's vertical coordinate dimension.
     *
     * @param value Dimension name such as {@code lat}; must not be null.
     * @return This builder.
     */
    public Builder vertCoordName(String value) {
      this.vertCoordName = require(value, "vertCoordName");
      return this;
    }

    /**
     * Sets the name of the source's time dimension.
     *
     * <p>The empty string declares a source with no time dimension, which
     * {@link #noTimeDim()} says more directly.</p>
     *
     * @param value Dimension name, or {@code ""} for a timeless source; must not be null.
     * @return This builder.
     */
    public Builder timeName(String value) {
      this.timeName = require(value, "timeName");
      return this;
    }

    /**
     * Declares that the source has no time dimension, as {@code --no-time-dim} does.
     *
     * @return This builder.
     */
    public Builder noTimeDim() {
      this.timeName = "";
      return this;
    }

    /**
     * Restricts processing to a single source timestep.
     *
     * @param value Timestep to process, or null/blank for the whole step range.
     * @return This builder.
     */
    public Builder timestep(String value) {
      this.timestep = value != null ? value : "";
      return this;
    }

    /**
     * Sets the fill value for grid cells the source does not cover.
     *
     * @param value Numeric fill value as a string, or null for none.
     * @return This builder.
     */
    public Builder defaultValue(String value) {
      this.defaultValue = value;
      return this;
    }

    /**
     * Enables parallel processing of patches within each timestep.
     *
     * @param value True to process patches in parallel.
     * @return This builder.
     */
    public Builder parallel(boolean value) {
      this.parallel = value;
      return this;
    }

    /**
     * Adds slices to an existing output file instead of overwriting it.
     *
     * @param value True to amend.
     * @return This builder.
     */
    public Builder amend(boolean value) {
      this.amend = value;
      return this;
    }

    /**
     * Sets the temporal metadata to record in the output.
     *
     * @param value Declared time axis, or {@link TimeAxisParams#EMPTY} for none; must not be null.
     * @return This builder.
     */
    public Builder timeAxis(TimeAxisParams value) {
      this.timeAxis = require(value, "timeAxis");
      return this;
    }

    /**
     * Builds the immutable options.
     *
     * @return The resolved options.
     */
    public PreprocessOptions build() {
      return new PreprocessOptions(this);
    }

    private static <T> T require(T value, String name) {
      return Objects.requireNonNull(
          value, name + " must not be null; leave it unset to keep the CLI default");
    }
  }
}
