/**
 * Tests for the ToolSchemas classpath loader.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.mcp.tool.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Verifies that each tool's schema resource is present on the classpath and parses as valid JSON
 * with the structural shape the MCP SDK expects.
 *
 * <p>Acts as a regression net: if a future change deletes, renames, or breaks a schema file,
 * these tests fail at build time rather than at MCP server startup.</p>
 */
public class ToolSchemasTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Each tool name and the set of property keys its schema must declare.
   */
  private static final String[][] EXPECTED_SCHEMAS = {
      {"validate_simulation", "script"},
      {"discover_config", "script"},
      {"preprocess_data",
          "script", "simulation", "dataFile", "variable", "unitsStr", "outputFile"},
      {"run_simulation", "script", "simulation", "replicates", "serialPatches", "seed"}
  };

  @Test
  public void loadsAllToolSchemas() throws Exception {
    for (String[] entry : EXPECTED_SCHEMAS) {
      String toolName = entry[0];
      String raw = ToolSchemas.load(toolName);
      assertNotNull(raw, "schema for " + toolName + " must not be null");
      assertTrue(raw.length() > 0, "schema for " + toolName + " must not be empty");
      JsonNode node = MAPPER.readTree(raw);
      assertEquals("object", node.path("type").asText(),
          "schema for " + toolName + " must declare type 'object'");
      JsonNode properties = node.path("properties");
      assertTrue(properties.isObject(),
          "schema for " + toolName + " must declare a properties object");
      for (int i = 1; i < entry.length; i++) {
        String prop = entry[i];
        assertTrue(properties.has(prop),
            "schema for " + toolName + " must declare property '" + prop + "'");
      }
    }
  }

  @Test
  public void missingSchemaThrows() {
    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> ToolSchemas.load("definitely_does_not_exist"));
    assertTrue(ex.getMessage().contains("definitely_does_not_exist"),
        "exception message should reference the missing schema name");
  }

}
