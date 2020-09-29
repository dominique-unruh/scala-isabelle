#!/bin/bash
set -ex

PACKAGE=de.unruh.isabelle.experiments
PY="python3 scripts/generate-java-patterns.py --package $PACKAGE --tuple-size 10"
DIR=src/test/scala/de/unruh/isabelle/experiments

$PY --interfaces >$DIR/Pattern.java
$PY --unapply JavaPatterns.unapply_Pair --matcher Pair --unapply-typeparams T1 T2 --unapply-outputs T1 T2 \
  --unapply-input 'Tuple2<T1,T2>' --matcher-class PairPattern >$DIR/PairPattern.java
$PY --unapply JavaPatterns.unapply_Triple --matcher Pair --unapply-typeparams T1 T2 T3 --unapply-outputs T1 T2 T3 \
  --unapply-input 'Tuple3<T1,T2,T3>' --matcher-class TriplePattern >$DIR/TriplePattern.java
#$PY --unapply JavaPatterns.unapply_Tuple4 --matcher Tuple4 --unapply-typeparams T1 T2 T3 T4 --unapply-outputs T1 T2 T3 T4 \
#  --unapply-input 'Tuple3<T1,T2,T3,T4>' --matcher-class Tuple4Pattern >$DIR/Tuple4Pattern.java
