#!/usr/bin/env bash
#
# Render the build-up narrative prose from Markdown to HTML fragments with pandoc.
#
# Each content/<id>.md is rendered to content/<id>.html (a bare fragment, no <html>
# wrapper) which narrative.js fetches and injects into the build-up text panel at
# runtime. The .html outputs are build artifacts (gitignored); regenerate them by
# running this script. CI runs it as part of the buildWeb job.
#
# Requires pandoc (already a build dependency, used elsewhere for the language spec).

set -euo pipefail

cd "$(dirname "$0")"

for md in *.md; do
  [ -e "$md" ] || continue
  html="${md%.md}.html"
  pandoc "$md" -f markdown -t html5 -o "$html"
  echo "rendered $md -> $html"
done
