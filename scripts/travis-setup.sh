#!/bin/bash

set -ex

test -n "$ISA"

mkdir -p ~/install
if ! [ -e ~/install/sbt ]; then
  curl -Ls https://git.io/sbt > ~/install/sbt
fi
chmod +x ~/install/sbt

if ! [ -e /opt/Isabelle$ISA ]; then
  case "$TRAVIS_OS_NAME" in
    linux) curl https://isabelle.in.tum.de/website-Isabelle$ISA/dist/Isabelle${ISA}_linux.tar.gz | tar -x -z -C ~/install;;
    osx) curl https://isabelle.in.tum.de/website-Isabelle$ISA/dist/Isabelle${ISA}_macos.tar.gz | tar -x -z -C ~/install;;
    *) echo "Unsupported OS: $TRAVIS_OS_NAME"; exit 1;;
  esac
fi

case "$TRAVIS_OS_NAME" in
  linux) ISABELLE_HOME=~/install/Isabelle$ISA;;
  osx) ISABELLE_HOME=~/install/Isabelle$ISA.app/Isabelle;;
  *) echo "Unsupported OS: $TRAVIS_OS_NAME"; exit 1;;
esac

echo "$ISABELLE_HOME" > .isabelle-home

# "$ISABELLE_HOME/bin/isabelle" build -b -v HOL-Analysis
