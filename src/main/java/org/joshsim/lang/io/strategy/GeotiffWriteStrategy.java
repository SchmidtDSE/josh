package org.joshsim.lang.io.strategy;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.geotiff.GeotiffWriter;


public class GeotiffWriteStrategy extends PendingRecordWriteStrategy {
    private final String variable;
    private final GeotiffDimensions dimensions;

    public GeotiffWriteStrategy(String variable, GeotiffDimensions dimensions) {
        this.variable = variable;
        this.dimensions = dimensions;
    }

    @Override
    protected void writeAll(List<Map<String, String>> records, OutputStream outputStream) {
        try {
            // Create temp file
            File tempFile = File.createTempFile("geotiff", ".tif");
            tempFile.deleteOnExit();

            // Create data array
            float[] data = new float[dimensions.getGridWidthPixels() * dimensions.getGridHeightPixels()];
            // Initialize with NaN
            for (int i = 0; i < data.length; i++) {
                data[i] = Float.NaN;
            }

            // Fill data array from records
            for (Map<String, String> record : records) {
                double longitude = Double.parseDouble(record.get("position.longitude"));
                double latitude = Double.parseDouble(record.get("position.latitude"));
                String valueStr = record.get(variable);
                float value = valueStr != null ? Float.parseFloat(valueStr) : Float.NaN;

                // Calculate grid position
                int x = (int) ((longitude - dimensions.getMinLon()) / 
                    (dimensions.getMaxLon() - dimensions.getMinLon()) * 
                    dimensions.getGridWidthPixels());
                int y = dimensions.getGridHeightPixels() - 1 - (int) ((latitude - dimensions.getMinLat()) / 
                    (dimensions.getMaxLat() - dimensions.getMinLat()) * 
                    dimensions.getGridHeightPixels());

                if (x >= 0 && x < dimensions.getGridWidthPixels() && 
                    y >= 0 && y < dimensions.getGridHeightPixels()) {
                    data[y * dimensions.getGridWidthPixels() + x] = value;
                }
            }

            // Create the GeoTIFF writer
            GeotiffWriter writer = new GeotiffWriter(tempFile.getAbsolutePath());

            // Convert data to Array
            Array dataArray = Array.factory(DataType.FLOAT, 
                new int[]{dimensions.getGridHeightPixels(), dimensions.getGridWidthPixels()}, 
                data);

            // Write the data
            writer.writeGrid(dimensions.getMinLat(), dimensions.getMinLon(),
                (dimensions.getMaxLat() - dimensions.getMinLat()) / dimensions.getGridHeightPixels(),
                (dimensions.getMaxLon() - dimensions.getMinLon()) / dimensions.getGridWidthPixels(),
                dataArray);

            writer.close();

            // Copy temp file to output stream
            try (OutputStream out = outputStream) {
                byte[] buffer = new byte[8192];
                java.io.FileInputStream fis = new java.io.FileInputStream(tempFile);
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                fis.close();
                out.flush();
                tempFile.delete();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write GeoTIFF: " + e);
        }
    }

    @Override
    protected List<String> getRequiredVariables() {
        return List.of("position.latitude", "position.longitude", variable);
    }
}