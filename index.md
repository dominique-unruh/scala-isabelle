# scala-isabelle – a Scala library for controlling Isabelle/HOL 

scala-isabelle is a Scala library to control an [Isabelle](https://isabelle.in.tum.de/) process
from a Scala application. It allows to interact with the Isabelle process,
manipulate objects such as terms and theorems as if they were local Scala objects,
and execute ML code in the Isabelle process. The library serves a similar purpose
as the discontinued [libisabelle](https://github.com/larsrh/libisabelle).
It can also be used from Java and other JVM languages.

## Setup

For information on how to setup the library, see [here](setup.md).

##  Example

A full example how to use Isabelle to parse terms and then operate on them in Scala is given [here](example.md).

## Documentation

Most information is in the
[API documentation](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/index.html).
For an introduction to the most important concepts, start with the API doc for the classes
[Isabelle](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/control/Isabelle.html),
[MLValue](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/mlvalue/MLValue.html),
and [Term](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/Term.html).

## Projects using scala-isabelle

* [qrhl-tool](https://github.com/dominique-unruh/qrhl-tool) – A theorem prover for post-quantum security.
* [PISA](https://github.com/albertqjiang/PISA) - A reinforcement learning environment for theorem proving in Isabelle.

## Acknowledgments

Development was supported by the Air Force Office of Scientific Research (AOARD Grant FA2386-17-1-4022),
by the [ERC consolidator grant CerQuS (819317)](https://www.ut.ee/~unruh/cerqus/), and by the PRG946 grant from the Estonian Research Council.
