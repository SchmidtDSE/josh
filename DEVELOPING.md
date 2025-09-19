# Josh Development Guide

This guide provides common commands used during development of the Josh Simulation Engine, extracted from the CI/CD workflow.

## Prerequisites

- Java 21 (Temurin recommended)
- Gradle build system
- libnetcdf-dev (for netCDF support)
- Python 3.9+ (for joshpy development)

## Build Commands

### Core Build
```bash
# Build the main fat JAR
./gradlew fatJar
# Output: build/libs/joshsim-fat.jar

# Build the WAR for web deployment
./gradlew war
# Output: build/libs/JoshSim.war

# Generate ANTLR grammar source
./gradlew generateGrammarSource
```

## Code Quality & Style Checks

```bash
# Check main code style (Google Java Style Guide)
./gradlew checkstyleMain

# Check test code style
./gradlew checkstyleTest

# Run all style checks
./gradlew checkstyleMain checkstyleTest
```

## Testing

### Java Tests
```bash
# Run all Java unit tests
./gradlew test

# Validate Josh script examples
bash examples/validate.sh

# Run Josh script tests
bash examples/test.sh

# Test external data with --data option
bash examples/test_data_option.sh

# Test job configuration template functionality
bash examples/test_job_config.sh
```

### Preprocessing Tests
```bash
# Test basic preprocessing functionality
bash examples/test_basic_preprocess.sh

# Test spatial preprocessing functionality
bash examples/test_spatial_preprocess.sh

# Test temporal preprocessing and create tutorial data
bash landing/test_preprocess.sh
```

### Python Tests
```bash
# Install joshpy
cd joshpy && pip install .

# Install development dependencies
cd joshpy && pip install .[dev]

# Run Python linting
pyflakes joshpy/joshpy/*.py

# Run Python tests
cd joshpy && nose2
```

## Web Interface Development

```bash
# Install JavaScript dependencies for editor
cd editor/third_party && bash install_deps.sh

# Install landing page dependencies
cd landing && bash install_deps.sh

# Extract WASM from JAR for web editor
bash editor/war/get_from_jar.sh

# Package web editor for deployment
cd web && bash support/package.sh
```

## Josh CLI Commands

### Validation & Testing
```bash
# Validate Josh script syntax
java -jar build/libs/joshsim-fat.jar validate script.josh

# Run Josh script tests
java -jar build/libs/joshsim-fat.jar test script.josh

# Run a simulation
java -jar build/libs/joshsim-fat.jar run simulation.josh
```

### Preprocessing
```bash
# Create optimized jshd files from netCDF
java -jar build/libs/joshsim-fat.jar preprocess simulation.josh MySimulation data.nc variable units output.jshd
```

### Configuration Discovery
```bash
# Find configuration variables in scripts
java -jar build/libs/joshsim-fat.jar discoverConfig simulation.josh
```

### Local Server
```bash
# Start local web server for UI
java -jar build/libs/joshsim-fat.jar server

# Start server with worker URL for distributed execution
java -jar build/libs/joshsim-fat.jar server --worker-url your-server-url.com/runReplicate
```

## Full Build & Test Sequence

To replicate the full CI/CD build locally:

```bash
# 1. Build core JAR
./gradlew fatJar

# 2. Run style checks
./gradlew checkstyleMain checkstyleTest

# 3. Run Java tests
./gradlew test

# 4. Validate examples
bash examples/validate.sh

# 5. Run preprocessing tests
bash examples/test_basic_preprocess.sh
bash examples/test_spatial_preprocess.sh
bash landing/test_preprocess.sh

# 6. Run Josh examples
bash examples/test.sh
bash examples/test_data_option.sh
bash examples/test_job_config.sh

# 7. Build web components (if needed)
./gradlew war
cd editor/third_party && bash install_deps.sh
bash editor/war/get_from_jar.sh
./gradlew fatJar  # Rebuild with embedded web assets
```

## Troubleshooting Build Issues

If the build is broken, check these common issues:

1. **Java Version**: Ensure Java 21 is installed and active
   ```bash
   java -version
   ```

2. **Grammar Generation**: If ANTLR grammar issues occur
   ```bash
   ./gradlew clean generateGrammarSource
   ```

3. **Style Violations**: Fix code style issues
   ```bash
   ./gradlew checkstyleMain
   # Review reports at: build/reports/checkstyle/main.xml
   ```

4. **Test Failures**: Run tests with more detail
   ```bash
   ./gradlew test --info
   # Review reports at: build/reports/tests/test/index.html
   ```

5. **Missing Dependencies**: Ensure netCDF is installed
   ```bash
   # Ubuntu/Debian
   sudo apt-get update && sudo apt-get install -y libnetcdf-dev

   # macOS
   brew install netcdf
   ```

6. **Clean Build**: Start fresh if issues persist
   ```bash
   ./gradlew clean
   ./gradlew fatJar
   ```

## Environment Variables

- `JOSH_API_KEYS`: Comma-separated list of valid API keys for server mode (optional)

## Output Locations

- Fat JAR: `build/libs/joshsim-fat.jar`
- WAR file: `build/libs/JoshSim.war`
- Test reports: `build/reports/tests/test/index.html`
- Checkstyle reports: `build/reports/checkstyle/`
- Packaged web editor: `web/build/`