#/bin/bash

apt-get update && \
apt-get install -y wget unzip && \
wget https://services.gradle.org/distributions/gradle-8.5-bin.zip && \
unzip gradle-8.5-bin.zip && \
mv gradle-8.5 /opt/gradle && \
ln -s /opt/gradle/bin/gradle /usr/bin/gradle && \
rm gradle-8.5-bin.zip