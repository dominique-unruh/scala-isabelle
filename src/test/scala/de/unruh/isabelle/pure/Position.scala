package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleTest.isabelle
import org.scalatest.funsuite.AnyFunSuite


class PositionTest extends AnyFunSuite {
  test ("position none") {
    val pos = Position.none
    assert(pos.line.isEmpty)
    assert(pos.offset.isEmpty)
    assert(pos.endOffset.isEmpty)
    assert(pos.file.isEmpty)
    assert(pos.id.isEmpty)
  }
  test ("position start") {
    val pos = Position.start
    assert(pos.line == Some(1))
    assert(pos.offset  == Some(1))
    assert(pos.endOffset.isEmpty)
    assert(pos.file.isEmpty)
    assert(pos.id.isEmpty)
  }
  test ("position startWithFile") {
    val fileName = "foo.bar"
    val pos = Position.startWithFileName(fileName)
    assert(pos.line == Some(1))
    assert(pos.offset  == Some(1))
    assert(pos.endOffset.isEmpty)
    assert(pos.file == Some(fileName))
    assert(pos.id.isEmpty)
  }
}
