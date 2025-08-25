/**
 * Template string processor for Josh export paths.
 *
 * <p>This class centralizes template string processing logic that was previously
 * scattered throughout JvmExportFacadeFactory. It handles both job-specific templates
 * (like {example}, {other}) and export-specific templates (like {replicate}, {step}, {variable})
 * using a two-phase processing approach.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobFileInfo;

/**
 * Renders template strings by replacing job-specific and export-specific template variables.
 *
 * <p>This class processes template strings in two phases:
 * <ol>
 *   <li>Phase 1: Replace job-specific templates using JoshJob file mappings</li>
 *   <li>Phase 2: Process export-specific templates based on file format</li>
 * </ol>
 *
 * <p>Example usage:
 * <pre>
 * TemplateStringRenderer renderer = new TemplateStringRenderer(job, 1);
 * String result = renderer.renderTemplate("file:///tmp/josh_{example}_{other}.csv");
 * // Result: "file:///tmp/josh_example_1_other_1.csv"
 * </pre>
 */
public class TemplateStringRenderer {

  private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{([^}]+)\\}");

  private final JoshJob job;
  private final int replicate;

  /**
   * Creates a new template string renderer.
   *
   * @param job The Josh job containing file mappings for template substitution
   * @param replicate The replicate number for export-specific templates
   * @throws IllegalArgumentException if job is null
   */
  public TemplateStringRenderer(JoshJob job, int replicate) {
    if (job == null) {
      throw new IllegalArgumentException("JoshJob cannot be null");
    }
    this.job = job;
    this.replicate = replicate;
  }

  /**
   * Renders a template string by replacing all template variables.
   *
   * <p>Processing occurs in two phases:
   * <ol>
   *   <li>Job-specific templates: {example}, {other} → file names from JoshJobFileInfo</li>
   *   <li>Export-specific templates: {replicate}, {step}, {variable} → format-specific
   *   processing</li>
   * </ol>
   *
   * @param template The template string containing variables in {name} format
   * @return The rendered string with all template variables replaced
   * @throws RuntimeException if unknown template variables are found
   */
  public String renderTemplate(String template) {
    if (template == null || template.isEmpty()) {
      return template;
    }

    // Phase 1: Replace job-specific templates
    String afterJobTemplates = processJobSpecificTemplates(template);

    // Phase 2: Process export-specific templates
    return processExportSpecificTemplates(afterJobTemplates);
  }

  /**
   * Renders a template string with strategy detection for export facade selection.
   *
   * <p>This method performs job-specific template processing and analyzes export-specific
   * templates for strategy detection. The processing of export-specific templates depends
   * on the file format and the presence of templates:</p>
   * 
   * <ul>
   *   <li>GeoTIFF: Always processes all templates (always uses separate files)</li>
   *   <li>CSV/NetCDF: Preserves {replicate} for strategy selection, processes others</li>
   * </ul>
   *
   * <p>Processing phases:
   * <ol>
   *   <li>Job-specific templates: Replace {example}, {other} with file names</li>
   *   <li>Export template detection: Analyze presence of {replicate}, {step}, {variable}</li>
   *   <li>Export template processing: Based on format and strategy requirements</li>
   * </ol>
   *
   * @param template The template string containing variables in {name} format
   * @return TemplateResult containing appropriately processed template and strategy indicators
   * @throws RuntimeException if unknown template variables are found
   */
  public TemplateResult renderTemplateWithStrategy(String template) {
    if (template == null || template.isEmpty()) {
      return new TemplateResult(template, false, false, false);
    }

    // Phase 1: Replace job-specific templates
    String afterJobTemplates = processJobSpecificTemplates(template);

    // Phase 2: Analyze export-specific templates in original template for strategy detection
    boolean hasReplicate = template.contains("{replicate}");
    boolean hasStep = template.contains("{step}");
    boolean hasVariable = template.contains("{variable}");

    // Phase 3: Process export-specific templates based on format and strategy
    String processedTemplate = processExportSpecificTemplatesForStrategy(
        afterJobTemplates, hasReplicate, hasStep, hasVariable);

    return new TemplateResult(processedTemplate, hasReplicate, hasStep, hasVariable);
  }

  /**
   * Processes job-specific template variables using file mappings from JoshJob.
   *
   * <p>Replaces templates like {example}, {other} with corresponding file names
   * from JoshJobFileInfo.getName(). For data like:
   * example.jshc=test_data/example_1.jshc;other.jshd=test_data/other_1.jshd
   * The template {example}_{other} becomes example_1_other_1</p>
   *
   * @param template The template string to process
   * @return String with job-specific templates replaced
   * @throws RuntimeException if unknown job templates are found
   */
  private String processJobSpecificTemplates(String template) {
    Map<String, JoshJobFileInfo> fileInfos = job.getFileInfos();
    Set<String> unknownTemplates = new HashSet<>();
    String result = template;

    // Find all template patterns in the string
    Matcher matcher = TEMPLATE_PATTERN.matcher(template);
    while (matcher.find()) {
      String templateVar = matcher.group(1);
      String templatePattern = "{" + templateVar + "}";

      // Check if this is a job-specific template (matches logical file name)
      String logicalFileName = findMatchingLogicalFileName(templateVar, fileInfos);
      if (logicalFileName != null) {
        JoshJobFileInfo fileInfo = fileInfos.get(logicalFileName);
        String replacement = fileInfo.getName();
        result = result.replace(templatePattern, replacement);
      } else if (isExportSpecificTemplate(templateVar)) {
        // This is an export-specific template, will be handled in phase 2
        continue;
      } else {
        // This is an unknown template
        unknownTemplates.add(templatePattern);
      }
    }

    // Report unknown templates with helpful error message
    if (!unknownTemplates.isEmpty()) {
      List<String> availableTemplates = new ArrayList<>();
      for (String logicalName : fileInfos.keySet()) {
        String baseName = extractBaseName(logicalName);
        availableTemplates.add("{" + baseName + "}");
      }
      availableTemplates.add("{replicate}");
      availableTemplates.add("{step}");
      availableTemplates.add("{variable}");

      String unknownList = String.join(", ", unknownTemplates);
      String availableList = String.join(", ", availableTemplates);
      throw new RuntimeException("Unknown template variables: " + unknownList
          + ". Available: " + availableList);
    }

    return result;
  }

  /**
   * Finds the logical file name that corresponds to a template variable.
   *
   * <p>Handles mapping from template variable to logical file name:
   * - Template "config.backup" → matches logical name "config.backup.jshc"
   * - Template "weather_data" → matches logical name "weather_data.jshd"</p>
   *
   * @param templateVar The template variable name (without braces)
   * @param fileInfos Map of logical file names to file info objects
   * @return The matching logical file name, or null if not found
   */
  private String findMatchingLogicalFileName(String templateVar,
      Map<String, JoshJobFileInfo> fileInfos) {
    for (String logicalName : fileInfos.keySet()) {
      String baseName = extractBaseName(logicalName);
      if (baseName.equals(templateVar)) {
        return logicalName;
      }
    }
    return null;
  }

  /**
   * Extracts the base name from a file name by removing the extension.
   *
   * <p>Examples:
   * "config.backup.jshc" → "config.backup"
   * "weather_data.jshd" → "weather_data"</p>
   *
   * @param fileName The file name
   * @return The base name without extension
   */
  private String extractBaseName(String fileName) {
    int lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex > 0) {
      return fileName.substring(0, lastDotIndex);
    }
    return fileName;
  }

  /**
   * Checks if a template variable is an export-specific template.
   *
   * @param templateVar The template variable name (without braces)
   * @return True if this is an export-specific template
   */
  private boolean isExportSpecificTemplate(String templateVar) {
    return "replicate".equals(templateVar)
           || "step".equals(templateVar)
           || "variable".equals(templateVar);
  }


  /**
   * Processes export-specific templates based on format and strategy requirements.
   *
   * <p>This method handles export-specific template processing for strategy-aware
   * template rendering. The processing depends on file format and strategy selection:</p>
   * 
   * <ul>
   *   <li>GeoTIFF: Always processes all templates (always separate files)</li>
   *   <li>CSV/NetCDF with {replicate}: Preserves {replicate} for parameterized strategy</li>
   *   <li>CSV/NetCDF without {replicate}: Removes {replicate} for consolidated strategy</li>
   * </ul>
   *
   * @param template The template string after job-specific processing
   * @param hasReplicate Whether the original template contained {replicate}
   * @param hasStep Whether the original template contained {step}
   * @param hasVariable Whether the original template contained {variable}
   * @return String with export-specific templates processed according to strategy
   */
  private String processExportSpecificTemplatesForStrategy(String template, 
                                                           boolean hasReplicate,
                                                           boolean hasStep, 
                                                           boolean hasVariable) {
    if (template.contains(".tif") || template.contains(".tiff")) {
      // For GeoTIFF: always process all templates (always uses separate files)
      return processExportSpecificTemplates(template);
    } else {
      // For CSV/NetCDF: preserve all export-specific templates based on strategy
      if (hasReplicate) {
        // Parameterized strategy: preserve all export templates for facade processing
        return template;
      } else {
        // Consolidated strategy: process step and variable, remove replicate 
        String withStep = hasStep ? template.replaceAll("\\{step\\}", "__step__") : template;
        String withVariable = hasVariable 
            ? withStep.replaceAll("\\{variable\\}", "__variable__") 
            : withStep;
        return withVariable.replaceAll("\\{replicate\\}", "");
      }
    }
  }

  /**
   * Processes export-specific template variables using existing format-specific logic.
   *
   * <p>This method replicates the logic from JvmExportFacadeFactory.getPath()
   * for handling {replicate}, {step}, and {variable} templates based on file format.</p>
   *
   * @param template The template string after job-specific processing
   * @return String with export-specific templates processed
   */
  private String processExportSpecificTemplates(String template) {
    // Determine file format for template processing strategy
    if (template.contains(".tif") || template.contains(".tiff")) {
      // For GeoTIFF: preserve replicate template behavior for separate files
      String replicateStr = Integer.toString(replicate);
      String withReplicate = template.replaceAll("\\{replicate\\}", replicateStr);
      String withStep = withReplicate.replaceAll("\\{step\\}", "__step__");
      String withVariable = withStep.replaceAll("\\{variable\\}", "__variable__");
      return withVariable;
    } else {
      // For tabular and NetCDF formats: remove replicate template (consolidated files)
      String withStep = template.replaceAll("\\{step\\}", "__step__");
      String withVariable = withStep.replaceAll("\\{variable\\}", "__variable__");
      return withVariable.replaceAll("\\{replicate\\}", "");
    }
  }
}
