# Use an official OpenJDK runtime as a parent image
FROM quay.io/lib/eclipse-temurin:21

# Set the working directory
WORKDIR /workspaces/josh-specification

# Copy the repo contents
COPY . .

# Install the required dependencies (gradle)
RUN .devcontainer/scripts/install_gradle.sh

# Keep the container running
CMD tail -f /dev/null