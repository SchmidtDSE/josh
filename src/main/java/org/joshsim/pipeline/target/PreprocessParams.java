/**
 * Immutable data class for preprocess-specific parameters.
 *
 * <p>Carries all parameters needed by {@link RemotePreprocessTarget} implementations
 * to dispatch a preprocessing job. Corresponds to the form fields of the
 * {@code /preprocessBatch} server endpoint and the CLI flags of
 * {@code preprocessBatch}.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import org.joshsim.command.PreprocessOptions;


/**
 * Parameters for a remote preprocessing job.
 *
 * <p>Holds what identifies the job - which file, which variable, where the result goes - alongside
 * the {@link PreprocessOptions} the remote end will preprocess with. The options are the same type
 * the local {@code preprocess} command uses, so a batch job and a local run are configured by one
 * object rather than by two hand-synchronized mirrors of it.</p>
 *
 * <p>Unlike the local command, the file names here are relative to the job's {@code workDir}: the
 * pod entrypoint and the {@code /preprocessBatch} handler each resolve them against the directory
 * they staged inputs into.</p>
 */
public class PreprocessParams {

  private final String dataFile;
  private final String variable;
  private final String units;
  private final String outputFile;
  private final PreprocessOptions options;

  /**
   * Constructs PreprocessParams.
   *
   * @param dataFile Filename of the data file within workDir.
   * @param variable Variable name or band number to extract.
   * @param units Units string for simulation use.
   * @param outputFile Filename for the output .jshd file, within workDir.
   * @param options Preprocessing options; null falls back to
   *     {@link PreprocessOptions#defaults()}.
   */
  public PreprocessParams(String dataFile, String variable, String units, String outputFile,
      PreprocessOptions options) {
    this.dataFile = dataFile;
    this.variable = variable;
    this.units = units;
    this.outputFile = outputFile;
    this.options = options != null ? options : PreprocessOptions.defaults();
  }

  public String getDataFile() {
    return dataFile;
  }

  public String getVariable() {
    return variable;
  }

  public String getUnits() {
    return units;
  }

  public String getOutputFile() {
    return outputFile;
  }

  public PreprocessOptions getOptions() {
    return options;
  }
}
