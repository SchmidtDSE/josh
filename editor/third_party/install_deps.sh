#!/bin/bash
set -e

if [ ! -e publicsans ]; then
  mkdir publicsans
fi

if [ ! -e publicsans/public-sans-v2.001.zip ]; then
  wget https://github.com/uswds/public-sans/releases/download/v2.001/public-sans-v2.001.zip -O publicsans/public-sans-v2.001.zip
fi

if [ ! -e publicsans/fonts/otf/PublicSans-Regular.otf ]; then
  cd publicsans
  unzip public-sans-v2.001.zip
  cd ..
fi

if [ ! -e ace.min.js ]; then
  wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/ace.min.js -O ace.min.js
fi

if [ ! -e theme-textmate.js ]; then
  wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/theme-textmate.min.js -O theme-textmate.js
fi

if [ ! -e theme-textmate-css.js ]; then
  wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/theme-textmate-css.min.js -O theme-textmate-css.js
fi

if [ ! -e ext-searchbox.js ]; then
  wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/ext-searchbox.js -O ext-searchbox.js
fi

if [ ! -e ext-options.js ]; then
  wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/ext-options.js -O ext-options.js
fi

if [ ! -e ext-prompt.js ]; then
  wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/ext-prompt.js -O ext-prompt.js
fi

if [ ! -e ext-language_tools.js ]; then
  wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/ext-language_tools.js -O ext-language_tools.js
fi

if [ ! -e tabby-ui.min.css ]; then
  wget https://cdn.jsdelivr.net/gh/cferdinandi/tabby@12.0.3/dist/css/tabby-ui.min.css -O tabby-ui.min.css
fi

if [ ! -e tabby.min.js ]; then
  wget https://cdn.jsdelivr.net/gh/cferdinandi/tabby@12.0.3/dist/js/tabby.min.js -O tabby.min.js
fi

if [ ! -e d3.min.js ]; then
  wget https://cdnjs.cloudflare.com/ajax/libs/d3/7.9.0/d3.min.js -O d3.min.js
fi

if [ ! -e math.min.js ]; then
  wget https://cdnjs.cloudflare.com/ajax/libs/mathjs/14.2.1/math.min.js -O math.min.js
fi

if [ ! -e popper.min.js ]; then
  wget https://unpkg.com/@popperjs/core@2 -O popper.min.js
fi

if [ ! -e tippy.min.js ]; then
  wget https://unpkg.com/tippy.js@6 -O tippy.min.js
fi
