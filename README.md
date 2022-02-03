# scala-isabelle

[![Build status](https://img.shields.io/github/checks-status/dominique-unruh/scala-isabelle/master?label=build)](https://github.com/dominique-unruh/scala-isabelle/actions/workflows/test.yml?query=branch%3Amaster)
[![Scaladoc](https://javadoc.io/badge2/de.unruh/scala-isabelle_2.13/scaladoc.svg)](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/index.html)
[![Scaladoc snapshot](https://img.shields.io/badge/scaladoc-snapshot-brightgreen.svg)](https://oss.sonatype.org/service/local/repositories/snapshots/archive/de/unruh/scala-isabelle_2.13/master-SNAPSHOT/scala-isabelle_2.13-master-SNAPSHOT-javadoc.jar/!/de/unruh/isabelle/index.html)
[![Gitter chat](https://img.shields.io/badge/gitter-chat-brightgreen.svg)](https://gitter.im/dominique-unruh/scala-isabelle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## What is this library for?

This library allows to control an [Isabelle](https://isabelle.in.tum.de/) process
from a Scala application. It allows to interact with the Isabelle process, 
manipulate objects such as terms and theorems as if they were local Scala objects,
and execute ML code in the Isabelle process. The library serves a similar purpose
as the discontinued [libisabelle](https://github.com/larsrh/libisabelle).

## Further reading

For information how to setup the library, examples, and documentation, see the [website](https://dominique-unruh.github.io/scala-isabelle).

## Projects using scala-isabelle

* [qrhl-tool](https://github.com/dominique-unruh/qrhl-tool) – A theorem prover for post-quantum security.
* [PISA](https://github.com/albertqjiang/PISA) - A reinforcement learning environment for theorem proving in Isabelle.

## Acknowledgments

Development was supported by the Air Force Office of Scientific Research (AOARD Grant FA2386-17-1-4022),
by the [ERC consolidator grant CerQuS (819317)](https://www.ut.ee/~unruh/cerqus/), and by the PRG946 grant from the Estonian Research Council.
