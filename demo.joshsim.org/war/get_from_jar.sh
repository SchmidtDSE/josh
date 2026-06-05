#./gradlew war

mkdir /tmp/josh-stage-web
cp ./build/libs/JoshSim.war /tmp/josh-stage-web
cd /tmp/josh-stage-web
jar xvf /tmp/josh-stage-web/JoshSim.war
cd -
rm -rf ./demo.joshsim.org/war/js
mkdir ./demo.joshsim.org/war/js
rm -rf ./demo.joshsim.org/war/wasm-gc
mkdir ./demo.joshsim.org/war/wasm-gc
cp /tmp/josh-stage-web/js/JoshSim.js ./demo.joshsim.org/war/js/JoshSim.js
cp /tmp/josh-stage-web/wasm-gc/JoshSim.wasm ./demo.joshsim.org/war/wasm-gc/JoshSim.wasm
cp /tmp/josh-stage-web/wasm-gc/JoshSim.wasm-runtime.js ./demo.joshsim.org/war/wasm-gc/JoshSim.wasm-runtime.js

rm -r /tmp/josh-stage-web/
