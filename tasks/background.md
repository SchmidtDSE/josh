# Josh Simulation Engine - Project Background

## Overview

Josh is an ecologist-centered platform for agent-based simulations focused on vegetation modeling. It uses a domain-specific language (DSL) designed for ecological researchers, policy makers, and scientists who may not have extensive programming backgrounds. The platform enables multi-occupancy patch-based ecological simulations where multiple species occupying grid cells can be modeled through individual behaviors with optional state changes.

## Key Features

### Multiple Execution Environments
- **Browser via WebAssembly**: Simulations run directly in web browsers using TeaVM compilation, requiring no installation
- **Local JVM**: High-performance execution with parallelization on local machines 
- **Distributed via JoshCloud**: Large-scale distributed processing across multiple machines with API key access

### Josh Language
A readable domain-specific language with:
- Stanza-based structure (simulation, patch, organism, disturbance, management)
- Strong unit system with custom units and conversions
- Stochastic modeling with distribution sampling
- Spatial queries and geospatial data support
- Configuration files (.jshc) for parameterization without code changes
- Preprocessed data files (.jshd) for optimized geospatial data handling

## Project Structure

### Core Components
- **Java Backend**: Main simulation engine built with Gradle
- **Web Editor**: Browser-based IDE at editor.joshsim.org
- **Command Line Interface**: Local execution via `java -jar joshsim.jar`
- **Python Interface (joshpy)**: Coming soon but not yet released

### Commands
- `validate`: Check Josh script syntax
- `run`: Execute simulations
- `server`: Start local web interface
- `preprocess`: Create optimized .jshd files from geospatial data
- `discoverConfig`: Find configuration variables in scripts
- `test`: Run Josh script unit tests

## Development Workflow

### Validation Commands
**IMPORTANT**: All work should ensure these validation commands pass or fail in an expected way:
- `./gradlew test` - Run Java unit tests
- `./gradlew checkstyleMain` - Check main code style compliance
- `./gradlew checkstyleTest` - Check test code style compliance

### Additional Quality Checks
- `./gradlew generateGrammarSource` - Verify ANTLR grammar generation
- `bash examples/validate.sh` - Validate example Josh scripts
- `bash examples/test.sh` - Run example Josh script tests

### Build Commands
- `./gradlew fatJar` - Build executable JAR with all dependencies
- `./gradlew war` - Build WebAssembly version for browser

### Notes

Some output is suppressed. If you are adding printf statements for debugging, please be sure to review `build.gradle`. That file should have its prior suppression settings restored when debugging is done. Please also be careful to remove unnecessary printf statements when done.

## Development Standards

### Java Code (Google Java Style Guide)
- ~80% test coverage target
- JavaDoc for all non-test public members
- Use Spotless for automatic formatting
- Checkstyle enforced via Gradle

### JavaScript Code (Google JavaScript Style Guide)
- JSDoc for all public members
- Vanilla JavaScript only (no webpack/bundlers)
- Must run directly in browser
- Limited production dependencies via minified JS

### Josh Scripts
- Follow ecological modeling best practices
- Use meaningful entity and variable names
- Include unit tests where applicable
- Document complex behaviors with comments

## CI/CD Pipeline

### GitHub Actions Workflow
The project uses comprehensive CI/CD via `.github/workflows/build.yaml`:

1. **Build Phase**:
   - Build language specification PDF from markdown
   - Build fat JAR for distribution
   - Build WebAssembly version for browser

2. **Static Checks**:
   - Grammar generation verification
   - Checkstyle for main and test code
   - Python linting with pyflakes

3. **Testing**:
   - Java unit tests via Gradle
   - Josh example validation and execution
   - Tutorial preprocessing tests
   - Python package installation and tests

4. **Deployment** (main/dev branches only):
   - Deploy to SFTP for static hosting
   - Deploy to Google Cloud Run for distributed execution
   - Separate production and development environments

## Security Features

### Sandbox Mode
- Limits file access to code, .jshd, and .jshc files only
- Blocks network access except for intended operations
- Enabled by default in server mode and JoshCloud

### API Key Management
- Optional API key validation via `JOSH_API_KEYS` environment variable
- Comma-separated list of valid keys
- All requests allowed if not configured

## Key Technologies

### Core Dependencies
- **ANTLR4**: DSL parsing and grammar
- **TeaVM**: WebAssembly compilation for browser execution
- **Apache SIS**: Coordinate systems and COG support
- **GeoTools**: Geospatial data processing
- **UCAR NetCDF**: NetCDF file support
- **Undertow**: Local web server

### Development Tools
- **Gradle**: Build system and dependency management
- **JUnit**: Java unit testing
- **Mockito**: Test mocking framework
- **Checkstyle**: Code style enforcement
- **Spotless**: Automatic code formatting

## Repository Structure
```
josh/
├── src/                    # Java source code
├── editor/                 # Web-based editor
├── examples/              # Example Josh scripts
├── landing/               # Landing page and documentation
├── cloud-img/             # Docker configurations
├── joshpy/                # Python interface (future)
├── tasks/                 # Task documentation
├── llms.txt              # LLM-specific context
└── build.gradle          # Build configuration
```

## Environment Requirements

### Development
- Java 21 (Temurin recommended)
- Gradle build system
- Optional: Docker for containerized development

### Production
- Java 21 runtime for local execution
- Modern web browser for WebAssembly version
- API key for JoshCloud access (trusted partners only)

## Getting Started

1. **Quick Start (Browser)**: Visit [editor.joshsim.org](https://editor.joshsim.org)
2. **Local Development**: 
   ```bash
   ./gradlew fatJar
   java -jar build/libs/joshsim-fat.jar server
   ```
3. **Run Tests**: 
   ```bash
   ./gradlew test
   ./gradlew checkstyleMain
   ./gradlew checkstyleTest
   ```

## Support and Documentation

- **Guide**: [joshsim.org/guide.html](https://joshsim.org/guide.html)
- **Language Specification**: Available in `llms.txt` and `LanguageSpecification.md`
- **Examples**: See `examples/` directory for sample simulations
- **License**: BSD-3-Clause (see LICENSE file)

## Recommended Reading

For developers and AI assistants working on this project, it is highly recommended to review:
- **README.md**: Complete project overview, usage instructions, and development setup
- **llms.txt**: Comprehensive Josh language specification and DSL documentation specifically formatted for LLM consumption

## Project Status

- **Status**: Work in Progress
- **Maintained by**: Schmidt Center for Data Science and Environment at UC Berkeley
- **Open Source**: Contributions welcome following development standards
