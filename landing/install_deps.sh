[ ! -e publicsans ] && mkdir publicsans

[ ! -e publicsans/public-sans-v2.001.zip ] && wget https://github.com/uswds/public-sans/releases/download/v2.001/public-sans-v2.001.zip -O publicsans/public-sans-v2.001.zip

if [ ! -e publicsans/fonts/otf/PublicSans-Regular.otf ]; then
  cd publicsans
  unzip public-sans-v2.001.zip
  cd ..
fi
