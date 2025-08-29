/**
 * Reusable Josh program fixtures for testing.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

/**
 * Utility class providing reusable Josh program templates for testing.
 *
 * <p>This class contains minimal but complete Josh programs that can be used
 * in integration tests to verify end-to-end functionality without complex
 * mocking setups.</p>
 */
public class JoshTestFixtures {

  /**
   * Minimal simulation with a single tree organism that grows over time.
   * Suitable for basic replication and command testing.
   */
  public static final String MINIMAL_SIMULATION = """
      start simulation TestSim
        grid.size = 100 m
        grid.low = 0 degrees latitude, 0 degrees longitude
        grid.high = 0.1 degrees latitude, 0.1 degrees longitude
        grid.patch = "Default"
        steps.low = 0 count
        steps.high = 3 count
        exportFiles.patch = "%s"
      end simulation
      
      start patch Default
        Tree.init = create 2 count of Tree
        export.treeCount.step = count(Tree)
        export.averageAge.step = mean(Tree.age)
      end patch
      
      start organism Tree
        age.init = 0 count
        age.step = prior.age + 1 count
      end organism
      """;

  /**
   * Simulation template that accepts a custom export path.
   * Use with String.format() to specify the output file path.
   *
   * @param exportPath the file path for patch exports
   * @return formatted Josh program string
   */
  public static String minimalSimulationWithExport(String exportPath) {
    // Ensure the path has proper URI scheme
    String uriPath = exportPath.startsWith("file://") ? exportPath : "file://" + exportPath;
    return String.format(MINIMAL_SIMULATION, uriPath);
  }

  /**
   * Simulation without export files for testing scenarios where
   * file output is not needed.
   */
  public static final String MINIMAL_SIMULATION_NO_EXPORT = """
      start simulation TestSim
        grid.size = 100 m
        grid.low = 0 degrees latitude, 0 degrees longitude
        grid.high = 0.1 degrees latitude, 0.1 degrees longitude
        grid.patch = "Default"
        steps.low = 0 count
        steps.high = 3 count
      end simulation
      
      start patch Default
        Tree.init = create 2 count of Tree
        export.treeCount.step = count(Tree)
        export.averageAge.step = mean(Tree.age)
      end patch
      
      start organism Tree
        age.init = 0 count
        age.step = prior.age + 1 count
      end organism
      """;

  /**
   * More complex simulation for testing advanced features.
   * Includes multiple organism types and interactions.
   */
  public static final String COMPLEX_SIMULATION = """
      start simulation ComplexSim
        grid.size = 200 m
        grid.low = 0 degrees latitude, 0 degrees longitude
        grid.high = 0.2 degrees latitude, 0.2 degrees longitude
        grid.patch = "Forest"
        steps.low = 0 count
        steps.high = 5 count
        exportFiles.patch = "%s"
      end simulation
      
      start patch Forest
        Tree.init = create 3 count of Tree
        Shrub.init = create 5 count of Shrub
        export.treeCount.step = count(Tree)
        export.shrubCount.step = count(Shrub)
        export.totalBiomass.step = mean(Tree.height) + mean(Shrub.height)
      end patch
      
      start organism Tree
        age.init = 0 count
        age.step = prior.age + 1 count
        height.init = 1 count
        height.step = prior.height + sample uniform from 0 count to 2 count
      end organism
      
      start organism Shrub
        age.init = 0 count
        age.step = prior.age + 1 count
        height.init = 0 count
        height.step = prior.height + sample uniform from 0 count to 1 count
      end organism
      """;

  /**
   * Complex simulation template that accepts a custom export path.
   * Use with String.format() to specify the output file path.
   *
   * @param exportPath the file path for patch exports
   * @return formatted Josh program string
   */
  public static String complexSimulationWithExport(String exportPath) {
    // Ensure the path has proper URI scheme
    String uriPath = exportPath.startsWith("file://") ? exportPath : "file://" + exportPath;
    return String.format(COMPLEX_SIMULATION, uriPath);
  }

  private JoshTestFixtures() {
    // Utility class - no instantiation
  }
}