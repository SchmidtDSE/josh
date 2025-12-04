# Use an official OpenJDK runtime as a parent image
FROM quay.io/lib/eclipse-temurin:21

# Set the working directory
WORKDIR /workspaces/josh

# Copy the repo contents
COPY . .

# Install X11 libraries for GeoTIFF/AWT support and NetCDF C library
RUN apt-get update && apt-get install -y \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libnetcdf-dev \
    && rm -rf /var/lib/apt/lists/*

# Install the required dependencies (gradle)
RUN .devcontainer/scripts/on_build/install_gradle.sh

# Install VisualVM for profiling
RUN .devcontainer/scripts/on_build/install_visualvm.sh

# Install third-party dependencies for the local UI server
RUN cd editor/third_party && bash install_deps.sh

# Build WASM and fat JAR so the server is ready to run immediately
RUN ./gradlew war && bash editor/war/get_from_jar.sh && ./gradlew fatJar

# Add josh_validator and other interactive scripts to PATH, to be able to run them from anywhere
ENV PATH="/workspaces/josh/.devcontainer/scripts/interactive:${PATH}"

# Keep the container running
CMD tail -f /dev/null