#!/bin/bash
# License: BSD-3-Clause
#
# Stage the shared browser client into every site that serves it.
#
# editor.joshsim.org, demo.joshsim.org, and joshsim.org each run the same engine client in the
# reader's browser: the WASM layer, the wire format, the model, the summarizer, and the charts.
# It lives once in web/js and is copied into each site here, rather than imported across origins,
# because the three sites deploy to three separate hosts and none of them may depend on another
# being up.
#
# The staged copies are gitignored. Editing one changes nothing that ships and leaves `git status`
# empty; edit web/js instead. Each site keeps its own files beside them -- editor/js/editor.js,
# demo.joshsim.org/js/playground.js, landing/js/runner.js -- which is what the shared client is
# driven by.
#
# Usage: bash web/stage_js.sh

set -e

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE="$ROOT/web/js"

# The unit of sharing is the directory, not the file: model.js imports ./debug.js, wasm.worker.js
# reaches for ./parse.js and ../war/js/JoshSim.js, and wasm.js resolves its worker relative to
# itself. All of that holds only while these land together, beside each site's war/.
for site in editor demo.joshsim.org landing; do
  destination="$ROOT/$site/js"
  mkdir -p "$destination"
  cp "$SOURCE"/*.js "$destination/"
  echo "staged $(ls "$SOURCE" | wc -l) files -> $site/js"
done
