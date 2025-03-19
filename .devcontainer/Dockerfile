# Use an official OpenJDK runtime as a parent image
FROM quay.io/lib/eclipse-temurin:21

# Set the working directory
WORKDIR /workspaces/josh

# Copy the repo contents
COPY . .

# Install the required dependencies (gradle)
RUN .devcontainer/scripts/on_build/install_gradle.sh

# Add josh_validator and other interactive scripts to PATH, to be able to run them from anywhere
ENV PATH="/workspaces/josh/.devcontainer/scripts/interactive:${PATH}"

# Keep the container running
CMD tail -f /dev/nu