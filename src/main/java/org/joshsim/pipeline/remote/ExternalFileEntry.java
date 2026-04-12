/**
 * Record holding metadata for an external file to be streamed during remote execution.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote;

/**
 * Metadata for an external file to be streamed during remote execution.
 *
 * <p>Instead of reading and serializing all external file data into a single in-memory String,
 * this record captures the metadata needed to stream files from disk at request time. This avoids
 * exceeding Java's String length limit (~2.1GB) for large binary files like JSHD datasets.</p>
 *
 * @param filename The logical filename (e.g., "data.jshd")
 * @param isBinary Whether the file is binary (Base64 encoded) or text
 * @param filePath The absolute or relative path to the file on disk
 */
public record ExternalFileEntry(String filename, boolean isBinary, String filePath) {}
