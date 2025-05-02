[ ! -e publicsans ] && mkdir publicsans

[ ! -e publicsans/public-sans-v2.001.zip ] && wget https://github.com/uswds/public-sans/releases/download/v2.001/public-sans-v2.001.zip -O publicsans/public-sans-v2.001.zip

if [ ! -e publicsans/fonts/otf/PublicSans-Regular.otf ]; then
  cd publicsans
  unzip public-sans-v2.001.zip
  cd ..
fi

[ ! -e ace.min.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/ace.min.js -O ace.min.js
[ ! -e theme-textmate.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/theme-textmate.min.js -O theme-textmate.js
[ ! -e theme-textmate-css.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/theme-textmate-css.min.js -O theme-textmate-css.js
[ ! -e ext-searchbox.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/ext-searchbox.js -O ext-searchbox.js
[ ! -e ext-options.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/ext-options.js -O ext-options.js
[ ! -e ext-prompt.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/ext-prompt.js -O ext-prompt.js
[ ! -e ext-language_tools.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/ext-language_tools.js -O ext-language_tools.js
[ ! -e tabby-ui.min.css ] && wget https://cdn.jsdelivr.net/gh/cferdinandi/tabby@12.0.3/dist/css/tabby-ui.min.css -O tabby-ui.min.css
[ ! -e tabby.min.js ] && wget https://cdn.jsdelivr.net/gh/cferdinandi/tabby@12.0.3/dist/js/tabby.min.js -O tabby.min.js
[ ! -e d3.min.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/d3/7.9.0/d3.min.js -O d3.min.js
[ ! -e math.min.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/mathjs/14.2.1/math.min.js -O math.min.js
[ ! -e popper.min.js ] && wget https://unpkg.com/@popperjs/core@2 -O popper.min.js
[ ! -e tippy.min.js ] && wget https://unpkg.com/tippy.js@6 -O tippy.min.js
