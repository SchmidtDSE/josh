/**
 * Unit tests for JoshJobVariationVisitor class.
 *
 * <p>Tests the ANTLR visitor implementation for processing job variation
 * parse trees and configuring JoshJobBuilder instances.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.joshsim.lang.antlr.JoshJobVariationLexer;
import org.joshsim.lang.antlr.JoshJobVariationParser;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for JoshJobVariationVisitor functionality.
 *
 * <p>Validates visitor pattern implementation and integration
 * with ANTLR-generated parse trees.</p>
 */
public class JoshJobVariationVisitorTest {

  private JoshJobBuilder builder;

  /**
   * Set up test instances before each test.
   */
  @BeforeEach
  public void setUp() {
    builder = new JoshJobBuilder();
  }

  @Test
  public void testConstructorWithNullBuilder() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new JoshJobVariationVisitor(null)
    );
    
    assertEquals("JoshJobBuilder cannot be null", exception.getMessage());
  }

  @Test
  public void testVisitSingleFileSpec() {
    String specification = "example.jshc=test_data/example_1.jshc";
    
    JoshJobVariationLexer lexer = new JoshJobVariationLexer(CharStreams.fromString(specification));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    JoshJobVariationParser parser = new JoshJobVariationParser(tokens);
    
    ParseTree tree = parser.jobVariation();
    JoshJobVariationVisitor visitor = new JoshJobVariationVisitor(builder);
    
    JoshJobBuilder result = visitor.visit(tree);
    
    assertSame(builder, result);
    JoshJob job = result.build();
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertEquals(1, job.getFileNames().size());
  }

  @Test
  public void testVisitMultipleFileSpecs() {
    String specification = "example.jshc=test_data/example_1.jshc;"
        + "other.jshd=test_data/other_1.jshd";
    
    JoshJobVariationLexer lexer = new JoshJobVariationLexer(CharStreams.fromString(specification));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    JoshJobVariationParser parser = new JoshJobVariationParser(tokens);
    
    ParseTree tree = parser.jobVariation();
    JoshJobVariationVisitor visitor = new JoshJobVariationVisitor(builder);
    
    JoshJobBuilder result = visitor.visit(tree);
    
    assertSame(builder, result);
    JoshJob job = result.build();
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertEquals("test_data/other_1.jshd", job.getFilePath("other.jshd"));
    assertEquals(2, job.getFileNames().size());
  }

  @Test
  public void testVisitEmptySpecification() {
    String specification = "";  // Empty string - should parse to empty tree
    
    JoshJobVariationLexer lexer = new JoshJobVariationLexer(CharStreams.fromString(specification));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    JoshJobVariationParser parser = new JoshJobVariationParser(tokens);
    
    ParseTree tree = parser.jobVariation();
    JoshJobVariationVisitor visitor = new JoshJobVariationVisitor(builder);
    
    JoshJobBuilder result = visitor.visit(tree);
    
    assertSame(builder, result);
    JoshJob job = result.build();
    assertEquals(0, job.getFileNames().size());
  }

  @Test
  public void testVisitWithWhitespace() {
    String specification = "  example.jshc  =  test_data/example_1.jshc  ";
    
    JoshJobVariationLexer lexer = new JoshJobVariationLexer(CharStreams.fromString(specification));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    JoshJobVariationParser parser = new JoshJobVariationParser(tokens);
    
    ParseTree tree = parser.jobVariation();
    JoshJobVariationVisitor visitor = new JoshJobVariationVisitor(builder);
    
    JoshJobBuilder result = visitor.visit(tree);
    
    JoshJob job = result.build();
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
  }

  @Test
  public void testVisitComplexPaths() {
    String specification = "config.jshc=C:\\Users\\test\\config.jshc;"
        + "url.jshc=https://example.com:8080/config.jshc";
    
    JoshJobVariationLexer lexer = new JoshJobVariationLexer(CharStreams.fromString(specification));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    JoshJobVariationParser parser = new JoshJobVariationParser(tokens);
    
    ParseTree tree = parser.jobVariation();
    JoshJobVariationVisitor visitor = new JoshJobVariationVisitor(builder);
    
    JoshJobBuilder result = visitor.visit(tree);
    
    JoshJob job = result.build();
    assertEquals("C:\\Users\\test\\config.jshc", job.getFilePath("config.jshc"));
    assertEquals("https://example.com:8080/config.jshc", job.getFilePath("url.jshc"));
    assertEquals(2, job.getFileNames().size());
  }

  @Test
  public void testVisitIntegrationWithExistingBuilder() {
    // Pre-configure builder
    builder.setReplicates(5);
    
    String specification = "example.jshc=test_data/example_1.jshc";
    
    JoshJobVariationLexer lexer = new JoshJobVariationLexer(CharStreams.fromString(specification));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    JoshJobVariationParser parser = new JoshJobVariationParser(tokens);
    
    ParseTree tree = parser.jobVariation();
    JoshJobVariationVisitor visitor = new JoshJobVariationVisitor(builder);
    
    JoshJobBuilder result = visitor.visit(tree);
    
    JoshJob job = result.build();
    assertEquals(5, job.getReplicates());  // Should preserve existing configuration
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertEquals(1, job.getFileNames().size());
  }
}