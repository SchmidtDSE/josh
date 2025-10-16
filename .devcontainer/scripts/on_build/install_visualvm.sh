#!/bin/bash

apt-get update && \
apt-get install -y wget unzip && \
wget -q https://github.com/oracle/visualvm/releases/download/2.2/visualvm_22.zip -O visualvm.zip && \
unzip -q visualvm.zip && \
mv visualvm_* /opt/visualvm && \
ln -s /opt/visualvm/bin/visualvm /usr/local/bin/visualvm && \
rm visualvm.zip && \
apt-get clean && rm -rf /var/lib/apt/lists/*
