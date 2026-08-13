[ ! -e publicsans ] && mkdir publicsans

[ ! -e publicsans/public-sans-v2.001.zip ] && wget https://github.com/uswds/public-sans/releases/download/v2.001/public-sans-v2.001.zip -O publicsans/public-sans-v2.001.zip

if [ ! -e publicsans/fonts/otf/PublicSans-Regular.otf ]; then
  cd publicsans
  unzip public-sans-v2.001.zip
  cd ..
fi

[ ! -e d3.min.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/d3/7.9.0/d3.min.js -O d3.min.js

# mathjs, pinned to the version demo.joshsim.org uses: summarize.js and model.js reach for a
# `math` global rather than importing one, so a page running a model needs this script tag or it
# fails with "math is not defined" only after the simulation has finished.
[ ! -e math.min.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/mathjs/14.2.1/math.min.js -O math.min.js
[ ! -e prism-tomorrow.min.css ] && wget https://cdnjs.cloudflare.com/ajax/libs/prism/1.30.0/themes/prism-tomorrow.min.css -O prism-tomorrow.min.css
[ ! -e prism-core.min.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/prism/1.30.0/components/prism-core.min.js -O prism-core.min.js
[ ! -e prism-autoloader.min.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/prism/1.30.0/plugins/autoloader/prism-autoloader.min.js -O prism-autoloader.min.js
