# Josh Simulation Engine
Ecologist-centered tools for easily describing and running agent-based simulations, executable either locally with parallelization or out to distribution across hundreds of machines.

![Work in Progress](https://img.shields.io/badge/status-work_in_progress-blue)

## Purpose
Focused on vegitation, this JVM-based platform allows for fluent modeling of organisms, disturbances, and management interventions. This open source project supports stochastic mechanics and the use of external resources like outside geotiffs or COGs. Using a highly readable domain specific language crafted just for ecologists, Josh makes it easy to quickly describe ecological systems and run those simulations with highly performant computational machinery with minimal fuss in installation. This suite of tools also allows for running simulations in the browser, local performant parallelized execution to take advantage of a single machine's resources, and large scale distributed processing all without changing a single line of code.

## Usage
If you have a browser, you can use these tools without any installation required. When you are ready to scale up, this software can execute either directly on your machine, in a containerized environment, or across potentially hundreds of machines.

### Web-based usage
Simply send your browser to [editor.joshsim.org](https://editor.joshsim.org). There, you can build simulations using the [Josh Language](https://language.joshsim.org) all without installing anything new on your machine. Your work is completely private and your simulations never leave your computer unless you ask them to. They run right within your browser! However, when you are ready to push forward, you can either download your work and continue using your local computer or take advantage of our infrastructure to run your simulations across many machines all with the click of a button!

### Local usage
The easiest way to get started locally is simply [get yourself a copy of open source Java](https://adoptium.net) and [the latest release of Josh](https://language.joshsim.org/download.html). Then, fire up the command line. Simply write your code to `.josh` files and execute locally like so:

```
$ java -jar joshsim.jar run simulation.josh
```

Simply run the jar without any command specified to get further help documentation. You can also specify an output location

```
$ java -jar joshsim.jar run simulation.josh --dump-state state.avro
```

This will dump the state at each timestep in [Avro](https://avro.apache.org) though, if only the final timestep is required, add the `--final-only` flag.

### Local UI
You can run the local UI through [joshsim-server](https://language.joshsim.org/download.html). Simply execute:

```
$ java -jar joshsim-server.jar
```

This will start a local web server which makes the UI available via your browser where you can work in private.

### Containerized usage
Containerization through [Docker](https://www.docker.com) and [Development Containers](https://containers.dev) can help you move your work from one computer to the next with ease. Please see our `Dockerfile` and `devcontainer.json`.

### Distributed usage
Distributing workloads is easy. Simply deploy either our jar file or container to a serverless solution like [Labmda](https://aws.amazon.com/lambda/) / [CloudRun](https://cloud.google.com/run) or submit on your own cluster via [Kubernetes](https://kubernetes.io). You can send the Josh jar over the network to get your script and you can write to cloud storage. All you have to do is provide the command line arguments:

```
$ java -jar joshsim.jar run https://your-url.org/script.josh --http-basic-user USERNAME --http=-basic-pass PASSWORD --simulation TestSimulation --replicates 10 --output minio://your-s3-bucket/test_simulation.avro --minio-key ACCESS_KEY --minio-secret ACCESS_SECRET
```

More details to follow here.

## Developing
We have options available for different kinds of development workflows.

### Development container
We provide a standard `Dockerfile` and `devcontainer.json`. Replit files are also provided (`.replit` and `replit.nix`).

### Manual environment setup
In addition to a development container we provide instructions for general local setup to aid various environments.

 - Get yourself a copy of [open source Java](https://adoptium.net).
 - Install the [Gradle build system](https://gradle.org/install/).
 - Build the project with `gradle fatJar`.

This will produce your home-built copy of Josh at `build/libs/joshsim-fat.jar`. If you want to develop for the web interface, the following are also required:

 - [Install node and npm](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm)
 - Install node development dependencies with `cd web; npm install --dev`
 - Install vanilla JS production dependencies with `cd web; bash support/install_deps.sh`

Note that test runners do require an available copy of Chrome or Chromium.

### Development standards
For Josh itself, please use the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). We aim for approximately 80% test coverage and all non-test public members having [JavaDoc](https://www.baeldung.com/javadoc). 

For the web interface, please use the [Google JavaScript Style Guide](https://google.github.io/styleguide/jsguide.html) with all non-test public members having [JSDoc](https://jsdoc.app). Note that we use vanilla JavaScript which must be able to run directly in browser so code is not run through webpack or similar prior to deployment. Very limited production dependencies are simply included via minified JS. We do not consider production use of CDNs to be acceptable for privacy reasons.

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

To check the default examples, execute `bash examples/validate.sh` and `bash examples/test.sh`. Finally, front-end tests for the editor can run via grunt:

```
$ grunt
```

This requires a node setup.

## Deployment
Deployment instructions are provided both inside and outside of CI / CD.

### Automated deployment
To deploy a new version of Josh, simply merge to `main` to generate and deploy a new `joshsim.jar`, `joshsim-server.jar`, and web editor resources (including `joshsim.wasm`) via SFTP and [GitHub Actions](https://docs.github.com/en/actions).

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
- [ANTLR4](https://www.antlr.org/) for parsing the QubecTalk domain-specific language under [BSD-3](https://www.antlr.org/license.html).
- [Avro Java](https://avro.apache.org/docs/1.11.1/api/java/) under [Apache v2](https://www.apache.org/licenses/).
- [Chart.js](https://www.chartjs.org/) for rendering some charts and graphs under [MIT](https://github.com/chartjs/Chart.js/blob/master/LICENSE.md).
- [Checkstyle](https://checkstyle.sourceforge.io) under [LGPL](https://github.com/checkstyle/checkstyle/blob/master/LICENSE).
- [D3](https://d3js.org/) for data visualization under [ISC](https://github.com/d3/d3/blob/main/LICENSE).
- [ESLint](https://eslint.org/) for code style enforcement under [MIT](https://github.com/eslint/eslint/blob/main/LICENSE).
- [Gradle](https://gradle.org) under [Apache v2](https://github.com/gradle/gradle?tab=Apache-2.0-1-ov-file#readme).
- [HTTP Components](https://hc.apache.org/httpcomponents-client-5.4.x/index.html) under [Apache v2](https://www.apache.org/licenses/).
- [Jetty](https://jetty.org/index.html) under [Apache v2](https://jetty.org/docs/jetty/12/index.html).
- [JUnit](https://junit.org/junit5/) under [EPL v2](https://github.com/junit-team/junit5).
- [Minio Java SDK](https://min.io/docs/minio/linux/developers/java/minio-java.html) under [Apache v2](https://github.com/minio/minio-java?tab=Apache-2.0-1-ov-file#readme).
- [Public Sans](https://public-sans.digital.gov/) font under [OFL-1.1](https://github.com/uswds/public-sans/blob/master/LICENSE.md).
- [QUnit](https://qunitjs.com/) for unit front-end testing under [MIT](https://github.com/qunitjs/qunit/blob/main/LICENSE.txt).
- [SVG Spinners](https://github.com/n3r4zzurr0/svg-spinners?tab=readme-ov-file) under [MIT](https://github.com/n3r4zzurr0/svg-spinners?tab=readme-ov-file)
- [Tabby](https://github.com/cferdinandi/tabby) for tab interface management under [MIT](https://github.com/cferdinandi/tabby/blob/master/LICENSE.md).
- [Webpack](https://webpack.js.org/) for bundling JavaScript modules under [MIT](https://github.com/webpack/webpack/blob/main/LICENSE).

Also uses [Tumerin]().