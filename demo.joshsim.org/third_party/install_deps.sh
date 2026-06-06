#!/bin/bash
set -e

[ ! -e publicsans ] && mkdir publicsans

[ ! -e publicsans/public-sans-v2.001.zip ] && wget https://github.com/uswds/public-sans/releases/download/v2.001/public-sans-v2.001.zip -O publicsans/public-sans-v2.001.zip

if [ ! -e publicsans/fonts/otf/PublicSans-Regular.otf ]; then
  cd publicsans
  unzip public-sans-v2.001.zip
  cd ..
fi

[ ! -e prism-core.min.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/prism/1.30.0/components/prism-core.min.js -O prism-core.min.js

[ ! -e d3.min.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/d3/7.9.0/d3.min.js -O d3.min.js

[ ! -e math.min.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/mathjs/14.2.1/math.min.js -O math.min.js

[ ! -e ace.min.js ] && wget https://editor.joshsim.org/third_party/ace.min.js -O ace.min.js

[ ! -e theme-textmate.js ] && wget https://editor.joshsim.org/third_party/theme-textmate.js -O theme-textmate.js

[ ! -e theme-textmate-css.js ] && wget https://editor.joshsim.org/third_party/theme-textmate-css.js -O theme-textmate-css.js

mkdir -p ../data
[ ! -e ../data/forevertree.jshc ] && cp ../../paper/forevertree/forevertree.jshc ../data/forevertree.jshc
[ ! -e ../data/temperature.jshd ] && cp ../../paper/forevertree/data/temperature.jshd ../data/temperature.jshd
[ ! -e ../data/precipitation.jshd ] && cp ../../paper/forevertree/data/precipitation.jshd ../data/precipitation.jshd

# Build-up External-Data figures (canonical source: paper/forevertree/figures)
mkdir -p ../img
cp ../../paper/forevertree/figures/eco_temp_spatial.png ../img/eco_temp_spatial.png
cp ../../paper/forevertree/figures/eco_temp_domain.png ../img/eco_temp_domain.png
cp ../../paper/forevertree/figures/eco_precip_spatial.png ../img/eco_precip_spatial.png
cp ../../paper/forevertree/figures/eco_precip_domain.png ../img/eco_precip_domain.png
