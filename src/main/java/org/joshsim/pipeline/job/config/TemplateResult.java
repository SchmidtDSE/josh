/**
 * Result object containing processed template string and strategy indicators.
 *
 * <p>This class encapsulates the results of template string processing, including
 * the processed template string and boolean flags indicating which export-specific
 * templates were found. This enables strategy selection for export facade creation.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job.config;

/**
 * Contains the result of template processing with strategy indicators.
 *
 * <p>This class provides both the processed template string and metadata about
 * which export-specific templates were detected during processing. This information
 * is used to determine whether to use consolidated export (single file) or
 * parameterized export (multiple files) strategies.</p>
 *
 * <p>Example usage:
 * <pre>
 * TemplateResult result = renderer.renderTemplate(template);
 * if (result.requiresParameterizedOutput()) {
 *     // Use multi-file export strategy
 * } else {
 *     // Use consolidated export strategy
 * }
 * </pre>
 */
public class TemplateResult {

  private final String processedTemplate;
  private final boolean hasReplicateTemplate;
  private final boolean hasStepTemplate;
  private final boolean hasVariableTemplate;

  /**
   * Creates a new TemplateResult with the specified processed template and strategy indicators.
   *
   * @param processedTemplate The template string after job-specific template substitution
   * @param hasReplicateTemplate True if {replicate} template was found in original template
   * @param hasStepTemplate True if {step} template was found in original template
   * @param hasVariableTemplate True if {variable} template was found in original template
   */
  public TemplateResult(String processedTemplate, boolean hasReplicateTemplate,
                        boolean hasStepTemplate, boolean hasVariableTemplate) {
    this.processedTemplate = processedTemplate;
    this.hasReplicateTemplate = hasReplicateTemplate;
    this.hasStepTemplate = hasStepTemplate;
    this.hasVariableTemplate = hasVariableTemplate;
  }

  /**
   * Returns the processed template string with job-specific templates substituted.
   *
   * <p>This string contains the template after phase 1 processing (job-specific templates
   * like {example}, {other} replaced) but before phase 2 processing (export-specific
   * templates like {replicate}, {step}, {variable}).</p>
   *
   * @return The processed template string
   */
  public String getProcessedTemplate() {
    return processedTemplate;
  }

  /**
   * Indicates whether the original template contained a {replicate} template variable.
   *
   * @return True if {replicate} template was found in the original template
   */
  public boolean hasReplicateTemplate() {
    return hasReplicateTemplate;
  }

  /**
   * Indicates whether the original template contained a {step} template variable.
   *
   * @return True if {step} template was found in the original template
   */
  public boolean hasStepTemplate() {
    return hasStepTemplate;
  }

  /**
   * Indicates whether the original template contained a {variable} template variable.
   *
   * @return True if {variable} template was found in the original template
   */
  public boolean hasVariableTemplate() {
    return hasVariableTemplate;
  }

  /**
   * Determines whether this template requires parameterized output (multi-file) strategy.
   *
   * <p>Returns true if the template contains {replicate}, indicating that separate files
   * should be created per replicate. When false, consolidated output (single file) with
   * replicate column/dimension should be used.</p>
   *
   * @return True if parameterized/multi-file output is required
   */
  public boolean requiresParameterizedOutput() {
    return hasReplicateTemplate;
  }

  /**
   * Determines whether this template requires step parameterization.
   *
   * <p>Returns true if the template contains {step}, indicating that separate files
   * should be created per step value for formats that support it.</p>
   *
   * @return True if step parameterization is required
   */
  public boolean requiresStepParameterization() {
    return hasStepTemplate;
  }

  /**
   * Determines whether this template requires variable parameterization.
   *
   * <p>Returns true if the template contains {variable}, indicating that separate files
   * should be created per variable for formats that support it.</p>
   *
   * @return True if variable parameterization is required
   */
  public boolean requiresVariableParameterization() {
    return hasVariableTemplate;
  }

  @Override
  public String toString() {
    return "TemplateResult{"
        + "processedTemplate='" + processedTemplate + '\''
        + ", hasReplicateTemplate=" + hasReplicateTemplate
        + ", hasStepTemplate=" + hasStepTemplate
        + ", hasVariableTemplate=" + hasVariableTemplate
        + '}';
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    TemplateResult that = (TemplateResult) obj;
    return hasReplicateTemplate == that.hasReplicateTemplate
        && hasStepTemplate == that.hasStepTemplate
        && hasVariableTemplate == that.hasVariableTemplate
        && processedTemplate.equals(that.processedTemplate);
  }

  @Override
  public int hashCode() {
    int result = processedTemplate.hashCode();
    result = 31 * result + (hasReplicateTemplate ? 1 : 0);
    result = 31 * result + (hasStepTemplate ? 1 : 0);
    result = 31 * result + (hasVariableTemplate ? 1 : 0);
    return result;
  }
}
