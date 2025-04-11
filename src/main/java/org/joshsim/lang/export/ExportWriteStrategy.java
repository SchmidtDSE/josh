package org.joshsim.lang.export;

import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;


/**
 *
 * @param <T>
 */
public interface ExportWriteStrategy<T> {

  void write(Stream<T> records, OutputStream outputStream) throws IOException;

}
