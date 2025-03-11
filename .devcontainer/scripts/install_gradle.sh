#/bin/bash

apt-get update && \
apt-get install -y wget unzip && \
wget https://services.gradle.org/distributions/gradle-7.2-bin.zip && \
unzip gradle-7.2-bin.zip && \
mv gradle-7.2 /opt/gradle && \
ln -s /opt/gradle/bin/gradle /usr/bin/gradle && \
rm gradle-7.2-bin.zip