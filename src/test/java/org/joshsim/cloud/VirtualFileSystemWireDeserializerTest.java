package org.joshsim.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.Map;
import org.joshsim.lang.io.VirtualFile;
import org.junit.jupiter.api.Test;


/**
 * Tests for deserializing the virtual file system from a wire encoding in a string.
 */
public class VirtualFileSystemWireDeserializerTest {
  
  @Test
  void testEmptyString() {
    Map<String, VirtualFile> result = VirtualFileSystemWireDeserializer.load("");
    assertTrue(result.isEmpty());
  }
  
  @Test
  void testSingleTextFile() {
    String serialized = "hello.txt\t0\tworld\t";
    Map<String, VirtualFile> result = VirtualFileSystemWireDeserializer.load(serialized);
    
    assertEquals(1, result.size());
    assertTrue(result.containsKey("hello.txt"));
    
    VirtualFile file = result.get("hello.txt");
    assertEquals("hello.txt", file.getPath());
    assertEquals("world", file.getContent());
    assertEquals(false, file.getIsBinary());
  }
  
  @Test
  void testSingleBinaryFile() {
    // Encode 123 as base64
    String base64Content = Base64.getEncoder().encodeToString(Long.toString(123).getBytes());
    String serialized = "hello.bin\t1\t" + base64Content + "\t";
    
    Map<String, VirtualFile> result = VirtualFileSystemWireDeserializer.load(serialized);
    
    assertEquals(1, result.size());
    assertTrue(result.containsKey("hello.bin"));
    
    VirtualFile file = result.get("hello.bin");
    assertEquals("hello.bin", file.getPath());
    assertEquals(base64Content, file.getContent());
    assertEquals(true, file.getIsBinary());
  }
  
  @Test
  void testMultipleFiles() {
    String base64Content = Base64.getEncoder().encodeToString(Long.toString(123).getBytes());
    String serialized = "hello.txt\t0\tworld\thello.bin\t1\t" + base64Content + "\t";
    
    Map<String, VirtualFile> result = VirtualFileSystemWireDeserializer.load(serialized);
    
    assertEquals(2, result.size());
    
    // Verify text file
    VirtualFile textFile = result.get("hello.txt");
    assertEquals("hello.txt", textFile.getPath());
    assertEquals("world", textFile.getContent());
    assertEquals(false, textFile.getIsBinary());
    
    // Verify binary file
    VirtualFile binaryFile = result.get("hello.bin");
    assertEquals("hello.bin", binaryFile.getPath());
    assertEquals(base64Content, binaryFile.getContent());
    assertEquals(true, binaryFile.getIsBinary());
  }
}
