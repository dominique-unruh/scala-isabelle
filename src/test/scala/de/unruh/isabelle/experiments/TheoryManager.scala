package de.unruh.isabelle.experiments

import java.nio.file.Path

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.mlvalue.MLValue.compileFunction
import de.unruh.isabelle.pure.{Theory, TheoryHeader}

import scala.concurrent.ExecutionContext

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._

class TheoryManager {
  def getTheory(name: String)(implicit isabelle: Isabelle, ec: ExecutionContext): Theory = Theory(name)
}

object TheoryManager extends OperationCollection {
  def getHeader(source: Source)(implicit isabelle: Isabelle, ec: ExecutionContext): TheoryHeader = source match {
    case Text(text) => this.Ops.header_read(text).retrieveNow
  }

  private val global = null

  trait Source

  case class Heap(name: String) extends Source

  case class File(path: Path) extends Source

  case class Text(text: String) extends Source

  //noinspection TypeAnnotation
  protected final class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) {
    val header_read = compileFunction[String, TheoryHeader]("Thy_Header.read Position.none")
  }

  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext) = new this.Ops
}