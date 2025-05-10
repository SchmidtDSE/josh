/**
 * Logic to deserialize a virtual file system from an internal wire format.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.joshsim.lang.io.VirtualFile;


/**
 * Utility to deserialize a virutal file system from a wire serialization.
 *
 * <p>Utility to deserialize input data into the wire transfer format. In this format tabs within
 * file contents will have been converted into four spaces and binary data will be base64 while
 * encoded but includes a flag indicating if the file is binary. This is used to run the virtual
 * file system within the Josh server when operating in a sandboxed environment.</p>
 *
 * <p>In this format, each file is represented as a string with tab-separated values. The first
 * value is the name of the file (path in the virutal file system) followed by a non-spaces tab. The
 * second value is either a 1 if the file's contents are base64 encoded binary data or a 0 if plain
 * text followed by a non- spaces tab. Finally, the third value is the content of the file with tabs
 * replaced with spaces followed by a tab. After the third tab, the next file starts or the string
 * ends if no further files.
 */
public class VirutalFileSystemWireDeserializer {

  /**
   * Load a virutal file system from a wire serialization of that file system.
   *
   * @param serialized The string seialization of the file system to be loaded using the tab
   *     separation format.
   * @returns Mapping from the name of a file or its path to its contents as a VirutaalFile object
   *     where binary files 
   */
  public static Map<String, VirtualFile> load(String serialized) {
    Map<String, VirtualFile> virtualFiles = new HashMap<>();
    
    if (serialized == null || serialized.trim().isEmpty()) {
      return virtualFiles;
    }

    StringTokenizer tokenizer = new StringTokenizer(serialized, "\t");
    
    while (tokenizer.hasMoreTokens()) {
      String path = tokenizer.nextToken();
      boolean isBinary = "1".equals(tokenizer.nextToken());
      String content = tokenizer.nextToken();
      
      virtualFiles.put(path, new VirtualFile(path, content, isBinary));
    }
    
    return virtualFiles;
  }
  
}
