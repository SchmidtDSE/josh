# MinIO Integration Test Examples

This directory contains `.josh` files for testing MinIO direct streaming functionality in CI/CD.

## Test Files

- **`csv_export.josh`** - Tests CSV export to MinIO with multiple replicates
- **`netcdf_export.josh`** - Tests NetCDF export to MinIO with multiple replicates
- **`mixed_export.josh`** - Tests mixing MinIO and local file exports in same simulation
- **`backpressure.josh`** - Tests queue backpressure with small queue and large dataset

## Usage

These files are used by `.github/workflows/test-minio.yaml` for integration testing with a real MinIO server.

Example manual test:
```bash
./gradlew run --args "run examples/test/minio/csv_export.josh MinioCSVTest \
  --replicates=2 \
  --minio-endpoint=http://localhost:9000 \
  --minio-access-key=minioadmin \
  --minio-secret-key=minioadmin \
  --export-queue-size=10000 \
  --output-steps=5"
```
