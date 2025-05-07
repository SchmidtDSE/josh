## Dev Container Setup

This project includes a development container configuration (`devcontainer.json`) to streamline the setup process for Java development. The dev container is pre-configured to provide the following tools and features:

#### Getting Started
To start using the dev container:
1. Open the project in VS Code.
2. When prompted, reopen the project in the dev container.
3. Once the container is built, you can start coding, running tests, and building the project without additional setup.

#### Installed VS Code Extensions
The dev container automatically installs the following VS Code extensions to aid Java development:
- **[vscjava.vscode-java-pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)**: A comprehensive Java development pack that includes support for debugging, IntelliSense, and Maven/Gradle integration.
- **[redhat.java](https://marketplace.visualstudio.com/items?itemName=redhat.java)**: Provides Java language support, including syntax highlighting, code navigation, and refactoring tools.
- **[shengchen.vscode-checkstyle](https://marketplace.visualstudio.com/items?itemName=shengchen.vscode-checkstyle)**: Enables Checkstyle integration for enforcing Java code style guidelines.

## Validating `.josh` Model Configurations

### Example validation

In CI/CD, in order to ensure that our langauge parser is working correctly. Under the hood, this is accomplished by compiling the `java` codebase and validating that the `.josh` configurations are correctly parsed by the language parser. If we break something on the `java` side, the validation will fail, because we assume each of these examples are correct (and will update them if we change the language specification).

We provide a convenience script to validate `.josh` configurations. This script is located at `josh_validator.sh`, which is located in `.devcontainer/scripts/interactive` - however, this folder is added to `PATH` during the docker build process, so it can be run from anywhere.

```bash
$ josh_validator.sh examples/sample.josh 
Validating: examples/sample.josh
Validated Josh code at examples/sample.josh
Validation successful
```

Running the validator on a syntactically incorrect `.josh` file will result in an error message.

```bash
$ josh_validator.sh examples/error.josh 
Validating: examples/error.josh
Found errors in Josh code at examples/error.josh:
 - On line 1: no viable alternative at input 'start test'
Validation failed (exit code: 3)
```

##### Notes 

- The first run will be relatively slow, as under the hood, the script will compile the `java` codebase and start the gradle daemon. Subsequent runs will be faster!

- A future iteration of this approach could probably use the gradle extension within VSCode to validate `.josh` files, which may be a bit cleaner - this works for now.

##### Preprocess Scratch

```bash
java -jar build/libs/joshsim-fat.jar preprocess \
    examples/simulations/simple_external.josh \
    TestSimpleSimulationExternal \
    tmp/maxtemp_riverside_annual.nc \
    temperature \
    celsius \
    test_riverside_tiny.jshd \
    --crs "EPSG:4326" \
    --x-coord "lon" \
    --y-coord "lat" \
    --time-dim "calendar_year"
```