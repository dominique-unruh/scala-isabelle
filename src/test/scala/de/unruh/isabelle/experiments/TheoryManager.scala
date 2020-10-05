package de.unruh.isabelle.experiments

import java.nio.file.Path

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.mlvalue.MLValue.compileFunction
import de.unruh.isabelle.pure.{Position, Theory, TheoryHeader}
import TheoryManager.{Ops, Source, Text}

import scala.concurrent.ExecutionContext

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._

class TheoryManager {
  // TODO split into getTheorySource and loadTheory
  def getTheory(name: String)(implicit isabelle: Isabelle, ec: ExecutionContext): Theory = Theory(name)
  def beginTheory(source: Source)(implicit isabelle: Isabelle, ec: ExecutionContext): Theory = {
    val header = getHeader(source)
    val masterDir = source.path.getParent
    Ops.begin_theory(masterDir, header, header.imports.map(getTheory)).retrieveNow
  }
  def getHeader(source: Source)(implicit isabelle: Isabelle, ec: ExecutionContext): TheoryHeader = source match {
    case Text(text, path, position) => Ops.header_read(text, position).retrieveNow
  }
}

object TheoryManager extends OperationCollection {

  trait Source { def path : Path }
//  case class Heap(name: String) extends Source
  case class File(path: Path) extends Source
  case class Text(text: String, path: Path, position: Position) extends Source
  object Text {
    // TODO: default position should come from path
    def apply(text: String, path: Path)(implicit isabelle: Isabelle, ec: ExecutionContext): Text = new Text(text, path, Position.none)
  }

  //noinspection TypeAnnotation
  protected final class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) {
    val header_read = compileFunction[String, Position, TheoryHeader]("fn (text,pos) => Thy_Header.read pos text")
    val begin_theory = compileFunction[Path, TheoryHeader, List[Theory], Theory](
      "fn (path, header, parents) => Resources.begin_theory path header parents")
  }

  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext) = new this.Ops
}