# Josh Simulation Engine
Ecologist-centered tools for easily describing and running agent-based simulations, executable either locally with parallelization or out to distribution across hundreds of machines.

![Work in Progress](https://img.shields.io/badge/status-work_in_progress-blue)

## Purpose
Focused on vegetation, this platform runs on JVM or WebAssembly (via TeaVM). It allows for fluent modeling of organisms, disturbances, and management interventions. This open source project also supports stochastic mechanics and the use of external resources like outside geotiffs or COGs. Using a highly readable domain specific language crafted just for ecologists, Josh makes it easy to quickly describe ecological systems and run those simulations with highly performant computational machinery with minimal fuss in installation. This suite of tools also allows for running simulations in the browser, local performant parallelized execution to take advantage of a single machine's resources, and large scale distributed processing all without changing a single line of code.

## Usage
If you have a browser, you can use these tools without any installation required. When you are ready to scale up, this software can execute either directly on your machine, in a containerized environment, across potentially many machines via JoshCloud, or on your own infrastructure.

### Web-based usage
Simply send your browser to [editor.joshsim.org](https://editor.joshsim.org). There, you can build simulations using the Josh Language all without installing anything new on your machine. Your work is completely private and your simulations never leave your computer unless you ask them to. Computation runs right within your browser using WebAssembly! However, when you are ready to push forward, you can either download your work and continue using your local computer or take advantage of our infrastructure to run your simulations across many machines all with the click of a button!

### JoshCloud usage
For distributed usage across many machines with absolutely zero setup, the project maintains JoshCloud which provides access via an API key. This service is currently provided to trusted partners in preview. Simply provide your API via the web-based editor running on your machine or at [editor.joshsim.org](https://editor.joshsim.org).

The ability to use local tools to submit simulations that will run across our infrastructure via the CLI are also coming soon.

### Local usage
The easiest way to get started locally is to simply [get yourself a copy of open source Java](https://adoptium.net) and [the latest release of Josh](https://language.joshsim.org/download.html). Then, fire up the command line. First, write your code to `.josh` files. Then, execute locally like so:

```
$ java -jar joshsim.jar run simulation.josh
```

While COGs, geotiffs, and netCDF files can be provided directly, the preferred approach is to provide a jshd file which preprocesses these geospatial inputs for speed:

```
$ java -jar joshsim.jar preprocess simulation.josh MySimulation data.nc variable units output.jshd
$ java -jar joshsim.jar run simulation.josh --data output.jshd
```

Available commands include:

- `validate` for checking syntax:
  ```
  $ java -jar joshsim.jar validate simulation.josh
  ```
- `run` for executing simulations:
  ```
  $ java -jar joshsim.jar run simulation.josh
  ```
- `server` for starting a local web interface:
  ```
  $ java -jar joshsim.jar server
  ```
- `preprocess` for creating optimized jshd files:
  ```
  $ java -jar joshsim.jar preprocess simulation.josh MySimulation data.nc variable units output.jshd
  ```

Run the jar without any command specified to get further help documentation.

### Local UI
You can run the local UI through [joshsim](https://language.joshsim.org/download.html). Execute:

```
$ java -jar joshsim.jar server
```

This will start a local web server which makes the UI available via your browser where you can work in private. The local server supports a sandbox mode that limits access to only the code and jshd files provided, with no network access otherwise.

### Containerized usage
Containerization through [Docker](https://www.docker.com) and [Development Containers](https://containers.dev) can help you move your work from one computer to the next with ease. Please see our `Dockerfile` and `devcontainer.json`.

### Distributed usage
Distributing workloads is easy. Simply deploy either our jar file or container to a serverless solution like [Lambda](https://aws.amazon.com/lambda/) / [CloudRun](https://cloud.google.com/run) or submit on your own cluster via [Kubernetes](https://kubernetes.io). 

Distribution follows a leader / worker node architecture. A leader node will make a request to worker nodes through a URL in which API keys are passed. You may use a load balancer to distribute requests across workers. Note that the server runs both the leader and worker endpoints. Therefore, if desired, nodes may operate in both capacities. In other words, requests to workers and leaders can be at the same URL.

The Josh server supports HTTP2 for efficient communication. You can send Josh code and jshd precomputed data over the network in the request body. Results are returned via HTTP2 streaming back to the user client.

Here's an example of running the server with distribution:

```
$ java -jar joshsim.jar server --worker-url your-server-url.com/runReplicate
```

See also our [example Dockerfile](https://github.com/SchmidtDSE/josh/blob/main/cloud-img/Dockerfile.prod).

## Security
When running in server mode, some additional security mechanisms are in place.

### Sandbox
Josh includes a sandbox that limits access to only code and jshd files (with no other network access) when running in server mode. This ensures security and privacy. Note that the JoshCloud community infrastructure offering also runs with the sandbox. If the sandbox is not desired, use local execution from the command line.

### API keys
The server command will look for API keys in the `JOSH_API_KEYS` environment variable. If it is empty or not specified, all requests are allowed. If a value is given, a comma separated list of valid API keys is expected. If this feature is in use, requests without a valid API key will be rejected.

## Programming
Josh uses a domain-specific language designed specifically for ecological modeling. Here's a basic hello world example to get you started:

```
start unit year
  alias years
  alias yr  
  alias yrs
end unit

start simulation Main
  grid.size = 1000 m
  grid.low = 33.7 degrees latitude, -115.4 degrees longitude
  grid.high = 34.0 degrees latitude, -116.4 degrees longitude
  grid.patch = "Default"
  
  steps.low = 0 count
  steps.high = 10 count
  
  exportFiles.patch = "memory://editor/patches"
end simulation

start patch Default
  ForeverTree.init = create 10 count of ForeverTree
  
  export.averageAge.step = mean(ForeverTree.age)
  export.averageHeight.step = mean(ForeverTree.height)
end patch

start organism ForeverTree
  age.init = 0 year
  age.step = prior.age + 1 year
  
  height.init = 0 meters
  height.step = prior.height + sample uniform from 0 meters to 1 meters
end organism
```

This example creates a simple simulation with a "ForeverTree" organism - trees that grow in height over time. Each tree starts at age 0 and height 0, then grows one year older and gains random height each timestep.

The `exportFiles.patch = "memory://editor/patches"` line configures exports to be sent to the IDE's memory for interactive visualization rather than being saved to disk files. This memory export target allows users to explore and visualize simulation results directly within the web-based editor without creating output files.

For more comprehensive tutorials and guides, visit [https://joshsim.org/guide.html](https://joshsim.org/guide.html).

The Python interface (joshpy) is coming soon but not yet released.

## Developing
We have options available for different kinds of development workflows.

### Development container
We provide a standard `Dockerfile` and `devcontainer.json`. Replit files are also provided (`.replit` and `replit.nix`).

### Manual environment setup
In addition to a development container we provide instructions for general local setup to aid various environments.

 - Get yourself a copy of [open source Java](https://adoptium.net).
 - Install the [Gradle build system](https://gradle.org/install/).
 - Build the project with `gradle fatJar`.

This will produce your home-built copy of Josh at `build/libs/joshsim-fat.jar`. If you want to develop for the web interface, also install vanilla JS production dependencies with `editor/third_party/install_deps.sh`. This also requires the WASM build via `bash editor/war/get_from_jar.sh` from the repo root. This can be packaged into the jar itself with `gradle fatJar` afterwards.

### Development standards
For Josh itself, please use the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). We aim for approximately 80% test coverage and all non-test public members having [JavaDoc](https://www.baeldung.com/javadoc). 

For the web interface, please use the [Google JavaScript Style Guide](https://google.github.io/styleguide/jsguide.html) with all public members having [JSDoc](https://jsdoc.app). Note that we use vanilla JavaScript which must be able to run directly in browser so code is not run through webpack or similar prior to deployment. Very limited production dependencies are simply included via minified JS. We do not consider production use of CDNs to be acceptable for privacy reasons.

We require that our automated tests and checks pass prior to merging commits.

### Testing
We offer tests at two levels. First, the Java source behind Josh can be tested via [JUnit](https://junit.org/junit5/) and [Gradle](https://gradle.org):

```
$ gradle test
```

Josh scripts can also be validated for syntax errors and tested if unit tests are provided:

```
$ java -jar joshsim.jar validate script.josh
$ java -jar joshsim.jar test script.josh
```

To check the default examples, execute `bash examples/validate.sh` and `bash examples/test.sh`.

## Deployment
Deployment instructions are provided both inside and outside of CI / CD.

### Automated deployment
To deploy a new version of Josh, simply merge to `main` to generate and deploy a new `joshsim.jar` and web editor resources (including `joshsim.wasm`) via SFTP and [GitHub Actions](https://docs.github.com/en/actions).

### Manual deployment
If deploying outside of our CI / CD systems, only the jar is required to run Josh.

```
$ gradle fatJar
```

Simply use the output found under `build/libs/joshsim-fat.jar`. If also deploying the web editor:

```
$ cd web; bash support/package.sh
```

Simply serve static files from `web/build` like so:

```
$ cd web/build; python -m http.server
```

Any static hosting solution such as [Nginx](https://nginx.org) or [Jetty](https://jetty.org/index.html) can be used.

## Open source
This is an open source project of the [Schmidt Center for Data Science and Environment at UC Berkeley](https://dse.berkeley.edu).

### Licensing
Released under the BSD-3-Clause License. See `LICENSE` for more information.

### Technologies used
We use the following open source technologies:

- [ACE Editor](https://ace.c9.io/) for the code editing interface under [BSD-3](https://github.com/ajaxorg/ace/blob/master/LICENSE).
- [ANTLR4](https://www.antlr.org/) for parsing the Josh domain-specific language under [BSD-3](https://www.antlr.org/license.html).
- [Apache Commons Collections](https://commons.apache.org/proper/commons-collections/) for enhanced collections under [Apache v2](https://www.apache.org/licenses/).
- [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/) for CSV handling under [Apache v2](https://www.apache.org/licenses/).
- [Apache SIS](https://sis.apache.org/) for coordinate system transformations and COG support under [Apache v2](https://www.apache.org/licenses/).
- [Checkstyle](https://checkstyle.sourceforge.io) under [LGPL](https://github.com/checkstyle/checkstyle/blob/master/LICENSE).
- [D3](https://d3js.org/) for data visualization under [ISC](https://github.com/d3/d3/blob/main/LICENSE).
- [GeoTools](https://geotools.org/) for geospatial data processing under [LGPL](https://github.com/geotools/geotools/blob/main/LICENSE.md).
- [Gradle](https://gradle.org) under [Apache v2](https://github.com/gradle/gradle?tab=Apache-2.0-1-ov-file#readme).
- [JTS Topology Suite](https://locationtech.github.io/jts/) for geometry handling under [EDL](https://www.eclipse.org/org/documents/edl-v10.php).
- [JUnit](https://junit.org/junit5/) under [EPL v2](https://github.com/junit-team/junit5).
- [Math.js](https://mathjs.org/) for mathematical expressions under [Apache v2](https://github.com/josdejong/mathjs/blob/develop/LICENSE).
- [Minio Java SDK](https://min.io/docs/minio/linux/developers/java/minio-java.html) under [Apache v2](https://github.com/minio/minio-java?tab=Apache-2.0-1-ov-file#readme).
- [Mockito](https://site.mockito.org/) for testing under [MIT](https://github.com/mockito/mockito/blob/main/LICENSE).
- [Picocli](https://picocli.info/) for command line parsing under [Apache v2](https://github.com/remkop/picocli/blob/main/LICENSE).
- [Popper.js](https://popper.js.org/) for tooltip positioning under [MIT](https://github.com/floating-ui/floating-ui/blob/master/LICENSE).
- [Public Sans](https://public-sans.digital.gov/) font under [OFL-1.1](https://github.com/uswds/public-sans/blob/master/LICENSE.md).
- [SLF4J](https://www.slf4j.org/) for logging under [MIT](https://github.com/qos-ch/slf4j/blob/master/LICENSE.txt).
- [Spotless](https://github.com/diffplug/spotless) for code formatting under [Apache v2](https://github.com/diffplug/spotless/blob/main/LICENSE.txt).
- [Tabby](https://github.com/cferdinandi/tabby) for tab interface management under [MIT](https://github.com/cferdinandi/tabby/blob/master/LICENSE.md).
- [TeaVM](https://teavm.org/) for WebAssembly compilation under [Apache v2](https://github.com/konsoletyper/teavm/blob/master/LICENSE).
- [Tippy.js](https://atomiks.github.io/tippyjs/) for tooltips under [MIT](https://github.com/atomiks/tippyjs/blob/master/LICENSE).
- [UCAR NetCDF](https://www.unidata.ucar.edu/software/netcdf-java/) for NetCDF support under [BSD-3](https://github.com/Unidata/netcdf-java/blob/master/LICENSE).
- [Undertow](https://undertow.io/) for the local web server under [Apache v2](https://github.com/undertow-io/undertow/blob/master/LICENSE.txt).

We recommend [Temurin](https://projects.eclipse.org/projects/adoptium.temurin).
