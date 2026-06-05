# Josh Welcom Mat
An interactive "scrolly-telling" experience which introduces Josh and runs a small simulation in a reduced complexity embedded editor via WASM.

<br>

## Objective
Josh already offers a [series of guides](https://www.joshsim.org/guide.html) at but this sequence can take some time to complete, requiring some up-front investment from users. Other projects which similarly seek to reduce barriers to entry for computational tools have an interactive instructional sequence which enable users to work with code but in a more streamlined environment before addressing the full complexities of environment management or an IDE. For example, Processing has its [hello application](https://hello.processing.org/).

All this in mind, this proposal describes a multi-step flow in which a Josh code snippet is built up over time. Then, when the full snippet is ready, the user is invited to modify code. Results are to be shown within an editor similar to https://editor.joshsim.org/ but with only a run button an the visualization will be fixed to just a single variable.

<br>

## Design
The user will be able to move between steps of the sequence using a forward and backward button on the webpage. There will be four phases of the introduction sequence.

### Welcome
This is what the user will first see when the page loads. It should have an `h1` saying "Hello from Josh" with a short description:

> Josh is a friendly programming language co-designed by research software engineers, ecologists, mathematicians, and land managers for vegetation-focused ecological modeling. This short tutorial introduces you to the basics of Josh. Here, you can try out a simulation on your computer using just your browser.

This will just have a next button. This div should fade out when the next button is clicked to fade in the build up sequence.

### Build-up
On the left side of the screen^[On narrow window / mobile, will be top.] will be a div with a slighly gray background showing code that fades in over time. On the right side^[on narrow window / mobile, will be on bottom.] will be a div with explanatory text. More specifically, this will build up the simulation at `paper/forevertree` and, specifically, `forevertree_wasm.josh`. In order to make this happen, there will be a next and previous button that moves through a series of sub-steps. At each sub-step, new code is faded in on the left side and the text changes on the right.

 1. Structure: Lay out simulation, patch, and organism but have them otherwise empty.
 2. Geography: Add grid to simulation
 3. Initalization: Create forever tree in patch
 4. External data: Add in parts of code in Forever Tree which use external data
 5. Forever Tree: Fill out the rest of the forever tree entity
 6. Units: Add unit conversions
 7. Export: Add all export-related code lines as well as steps and config.

Each substep should have an h2. When the user advances past substep 7, we should fade out the build-up and fade in the playground.

### Playground
This is just going to be an:

 - Left side (top responsive): Ace editor with the simulation code above a `details` tag with plain textarea with the Josh config, above run and next buttons.
 - Right side (bottom responsive): Visualization of results. 

This can have h2 of `Try it yourself` above a brief description like:

> Now that we have built up the simulation, give it a run yourself. This will execute on your computer in your browser. Make modifications to the code and then click the run button.

This can use the editor box and the heatmap visualization from `editor`. However, we do not need to support file exports, modification of external data, run configuration (will just be WASM single replicate), etc. We should still have the scrub visualization but no control over changing visualized variable. Instead, we should always display `meanHeight`. The run button should cause the simluation to re-run with a simple "Simulation running..." message faded in on the right side before fading out (and the visualization fading in) when the simulation finishes execution.

### Conclusion
This should fade out the playground and fade in a final message with an h2 saying `Next steps`. This should direct the user to continue the guide at https://www.joshsim.org/guide.html. At this stage, no next button should be shown but previous should be available.

<br>

## Implementation
Everything but the playground . Use vanilla JS and CSS animations where possible to accomplish goals.

### Narrative
We should use responsive CSS and CSS grid to cerate the layout. We can add classes to identify each of the steps and sub-steps. This can be run using a `NarrativePresenter` following the presenter pattern seen in `editor`. We should apply a CSS class using JS to show / hide steps. This can use CSS animations to fade in / out.

### Playground
This should be a very minimal version of `editor`. We should consider re-use of `wasm.js` and `wasm.worker.js` as well as `mode-joshlang.js`. Furthermore, a slimmed down version of `viz.js` should be used. Where possible, files should be copied. However, we do not need: run configuration, metric configuration, save / load file, OPFS, persistance across browser sessions, modification of data file or config. The external data should be pulled from the server but the user should not configure it. Instead, the external data should simply be provided automatically from `paper/forevertree/data`.

### Accessibility
We should be mindful of WCAG requirements. In particular, we need two methods of navigation. Presently, we have forward / back but we should also add a footer with a table of contents button. This should open an HTML5 dialog which allows skipping between steps. This can be added to the `NarrativePresenter`.

### Deployment
We have not previously deployed to demo.joshsim.org but the same SFTP username and password can be used where the existing GitHub Actions can be extended. We should use the wasm build from earlier pipeline steps. Specifically, we should make a `deployDemo` similar to `deployStatic` that has `deployStatic` as a dependency. Just use `./demo.joshsim.org`. It should only deploy on `main`.

<br>

## Execution
We should use a multi-agent workflow to complete this task. In this process, we may re-use the agents at `.claude/agents`.

### Flow
We should go one component at a time. For every component, we should:

 - Invoke the `component-planner` with a path to this file and the number of the component to work on with reminder to only research solution and modify this file just within the component assigned.
 - Pause and ask the user any clarifying questions if needed before implementation. If no questions, continue to implementation.
 - Invoke the `component-implementer` with a path to this file and the number of the component to work on with reminder to not worry about style.
 - Invoke the `component-validator` with a path to this file and the number of the component to work on.
 - Read this file again and do a `git diff` to check on implementation status. Determine if implemeneted successfully and safe to continue.
 - If not safe to continue, pause and ask the user for help.
 - If safe to continue, compact the description of the now completed component down to no more than 3 sentences and then git commit.
 - Pause for user feedback and user manual testing.

After completing a component, continue to next component but do not do components in parallel.

### Decisions (resolved with user)
 - Climate external data: commit the two preprocessed `.jshd` files into `paper/forevertree/data/` (generated via `josh preprocess`, see `paper/forevertree/test.sh`). The playground fetches them at runtime; CI copies them into the deploy. No runtime preprocessing.
 - Cadence: pause at milestones (A: 1+2, B: 3+4, C: 5+6+7, D: 8+9, E: 10), committing between every component.

After completing a component, continue to next component but do not do components in parallel.

### Components
This will be completed as a series of components with a git commit between each.

#### 1. Scaffolding  ✅ complete
Created the static shell `demo.joshsim.org/{index.html, style/style.css, third_party/install_deps.sh}`, mirroring the `editor/` app conventions (PublicSans `@font-face`, `?v=` cache-bust, no bundler). `index.html` holds a `<main id="narrative">` with four phase `<section>`s — only `#phase-welcome` (active in HTML, renders with no JS) carries the "Hello from Josh" `h1`, the verbatim description, and a no-op Next button — plus an empty `<footer id="toc-footer">`. CSS provides `.phase`/`.phase.active` show-hide, a `.phase-two-col` grid skeleton, and fade `@keyframes` with a `prefers-reduced-motion` guard; no JS/importmap yet (deferred to Component 3).

#### 2. Deployment
Build out the initial github actions required to deploy the scaffolding.

#### 3. Build-up start
Create the first substep of the build-up with the JS needed to go from the welcome mat to the build up first substep. This should establish the `NarrativePresenter`.

#### 4. Build-up complete
Finish the rest of the build up sequence and all of its substeps.

#### 5. Establish non-running editor
Create the editor section with the Ace editor and config editor. The buttons do not need to do anything at this stage. However, the config and code editors should have the example code loaded.

#### 6. Establish running editor
Pull down the external data and read from the text area / Ace editor to run the simulation. This should show the running indicator but not yet the visualization.

#### 7. Establish visualization
When the simulation is done, show results on height per patch in the heatmap with the scrub tool.

#### 8. Create conclusion
Build out the conclusion as described above.

#### 9. Add table of contents
Add support for table of contents and jumping between steps.

#### 10. Update deployment
Return to the github actions and ensure everything is hooked up for deployment to run the demo in its entirety using only static hosting.
