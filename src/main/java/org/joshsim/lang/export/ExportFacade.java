package org.joshsim.lang.export;


import java.io.OutputStream;

public class ExportFacade {

  private final ExportSerializeStrategy<?> serializeStrategy;
  private final ExportWriteStrategy<?> writeStrategy;
  private final OutputStream outputStream;

  public ExportFacade(ExportTarget exportTarget) {
    if (!exportTarget.getFormat().equalsIgnoreCase("CSV")) {
      throw new IllegalArgumentException("ExportTarget must have CSV format.");
    }

    this.serializeStrategy = new MapSerializeStrategy();
    this.writeStrategy = new CsvWriteStrategy(); // Assuming CsvWriteStrategy is implemented.

    if (!exportTarget.isLocalPath()) {
      throw new IllegalArgumentException("ExportTarget must have a local path.");
    }

    try {
      this.outputStream = new FileOutputStream(exportTarget.getPath());
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to open file for writing: " + exportTarget.getPath(), e);
    }

  }

}
