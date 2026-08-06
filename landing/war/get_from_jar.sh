# Stage the WebAssembly engine into the landing site, so a library page can run the model it shows.
#
# The same shape as demo.joshsim.org/war/get_from_jar.sh and editor/war/get_from_jar.sh: the war is
# a build artifact, not a checked-in one, so each site that runs the engine in a browser takes its
# own copy at build time. Run ./gradlew war first.
#
# The layout here is not free to change. landing/js/wasm.worker.js is a byte-identical copy of the
# demo's, and it loads the engine with importScripts("../war/js/JoshSim.js") relative to its own
# location -- so the engine has to sit at war/js and war/wasm-gc beside the js directory.

mkdir /tmp/josh-stage-landing
cp ./build/libs/JoshSim.war /tmp/josh-stage-landing
cd /tmp/josh-stage-landing
jar xvf /tmp/josh-stage-landing/JoshSim.war
cd -
rm -rf ./landing/war/js
mkdir ./landing/war/js
rm -rf ./landing/war/wasm-gc
mkdir ./landing/war/wasm-gc
cp /tmp/josh-stage-landing/js/JoshSim.js ./landing/war/js/JoshSim.js
cp /tmp/josh-stage-landing/wasm-gc/JoshSim.wasm ./landing/war/wasm-gc/JoshSim.wasm
cp /tmp/josh-stage-landing/wasm-gc/JoshSim.wasm-runtime.js ./landing/war/wasm-gc/JoshSim.wasm-runtime.js

rm -r /tmp/josh-stage-landing/
