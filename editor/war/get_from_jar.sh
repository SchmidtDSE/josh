#./gradlew war

mkdir /tmp/josh-stage-web
cp ./build/libs/JoshSim.war /tmp/josh-stage-web
cd /tmp/josh-stage-web
jar xvf /tmp/josh-stage-web/JoshSim.war
cd -
rm -rf ./editor/war/js
mkdir ./editor/war/js
rm -rf ./editor/war/wasm-gc
mkdir ./editor/war/wasm-gc
cp /tmp/josh-stage-web/js/JoshSim.js ./editor/war/js/JoshSim.js
cp /tmp/josh-stage-web/wasm-gc/JoshSim.wasm ./editor/war/wasm-gc/JoshSim.wasm
cp /tmp/josh-stage-web/wasm-gc/JoshSim.wasm-runtime.js ./editor/war/wasm-gc/JoshSim.wasm-runtime.js

rm -r /tmp/josh-stage-web/
