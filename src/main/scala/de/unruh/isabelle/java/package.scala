package de.unruh.isabelle

/** This package provides methods for making access to scala-isabelle possible from Java (or
 * other non-Scala JVM languages).
 *
 * It does not provide any new functionality but only various wrapper methods for cases
 * where some of the methods in this library are hard to access from Java. (E.g.,
 * when a method expects Scala collections as input.)
 *
 * For Scala methods that need but lack a wrapper, please
 * [[https://github.com/dominique-unruh/scala-isabelle/issues/new?labels=java file an issue]].
 **/
package object java

