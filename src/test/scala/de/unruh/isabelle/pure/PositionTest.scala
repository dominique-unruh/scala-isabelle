package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleTest.isabelle
import org.scalatest.funsuite.AnyFunSuite
import de.unruh.isabelle.mlvalue.MLValue.compileFunction
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.mlvalue.Version

class PositionTest extends AnyFunSuite {
  // Explode a string into tokens, and return a list of (token, position) pairs.
  lazy val explode = compileFunction[String, List[(String, Position)]](
    """fn s => Token.explode Thy_Header.bootstrap_keywords Position.start s
               |> map (fn t => (Token.unparse t, Token.pos_of t))"""
  )
  val exampleString = "lemma \\<begin> foo \\<zzz>\n\\<sup^2> \\<unicode706B> 火 (Span {ket 0})»⟦C1⟧"

  test ("position none") {
    val pos = Position.none
    assert(pos.line.isEmpty)
    assert(pos.offset.isEmpty)
    assert(pos.endOffset.isEmpty)
    assert(pos.file.isEmpty)
    if (Version.from2021)
      assert(pos.id.isEmpty)
  }

  test ("position start") {
    val pos = Position.start
    assert(pos.line == Some(1))
    assert(pos.offset  == Some(1))
    assert(pos.endOffset.isEmpty)
    assert(pos.file.isEmpty)
    if (Version.from2021)
      assert(pos.id.isEmpty)
  }

  test ("position startWithFile") {
    val fileName = "foo.bar"
    val pos = Position.startWithFileName(fileName)
    assert(pos.line == Some(1))
    assert(pos.offset  == Some(1))
    assert(pos.endOffset.isEmpty)
    assert(pos.file == Some(fileName))
    if (Version.from2021)
      assert(pos.id.isEmpty)
  }

  test ("position basic") {
    val tokens = explode("lemma foo\nbar").retrieveNow
    assert(tokens.map(_._1) == List("lemma", " ", "foo", "\n", "bar"))
    assert(tokens.map(_._2).map(p => (p.line.get, p.offset.get, p.endOffset.get))
      == List((1,1,6), (1,6,7), (1,7,10), (1,10,11), (2,11,14)))
  }

  test ("position extract") {
    val tokens = explode(exampleString).retrieveNow
    for ((s, pos) <- tokens)
      assert(pos.extract(exampleString) == s)
  }

  test ("position extractUntil") {
    val tokens = explode(exampleString).retrieveNow
    tokens.sliding(2).map{
      case List((s, pos), (next_s, next_pos)) => assert(pos.extractUntil(next_pos, exampleString) == s)
      case _ => assert(false)
    }.toList
    // Check the last token, extracting until Position.none.
    assert(tokens.last._2.extractUntil(Position.none, exampleString) == tokens.last._1)
  }
}
