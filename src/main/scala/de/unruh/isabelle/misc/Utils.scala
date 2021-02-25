package de.unruh.isabelle.misc

import java.nio.file.Path
import org.apache.commons.lang3.SystemUtils

import scala.util.Random
import scalaz.syntax.id._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/** Contains miscellaneous utility functions */
object Utils {
  /** Destroys the process `process`. Unlike `process.destroy()`,
   * this also invokes `.destroy()` on all child processes. And one second later,
   * it invokes `.destroyForcibly()` in case they didn't terminate yet. */
  def destroyProcessThoroughly(process: Process): Unit = {
    // The "toList" in the end is in order to make a copy of the descendant list
    // in case the killing disrupts the iterator
    val processes = process.toHandle :: process.descendants().iterator().asScala.toList

    for (p <- processes) {
      try p.destroy()
      catch { case e : Exception => e.printStackTrace() }
    }

    Future {
      // Not good because it blocks one executor thread. But seems alternatives are complicated
      // or need additional dependencies.
      Thread.sleep(1000)
      for (p <- processes) {
        try p.destroyForcibly()
        catch { case e : Exception => e.printStackTrace() }
      }
    }
  }

  /** Generates a fresh name based on `name`.
   *
   * The name is guaranteed to:
   *  - Be unique (by containing a random substring)
   *  - Contain only ASCII letters, digits, and underscores
   *  - Start with a letter
   *
   * `name` will be a prefix of the fresh name as far as possible.
   *
   * @return the fresh name
   **/
  def freshName(name: String): String = {
    name
      .map { c => if (c<128 && c.isLetterOrDigit) c else '_' }
      .into { n => if (n.head.isLetter) n else "x"+n }
      .into { _ + '_' + randomString() }
  }

  // DOCUMENT
  def randomString(): String = Random.alphanumeric.take(12).mkString

  /** Converts `path` to a path understood by Cygwin.
   * (Only available when running under Windows.) */
  def cygwinPath(path: Path): String =
    if (path.isAbsolute) {
      assert(SystemUtils.IS_OS_WINDOWS)
      val root = path.getRoot.toString.stripSuffix(":\\")
      val parts = for (i <- 0 until path.getNameCount) yield path.getName(i)
      s"/cygdrive/$root/${parts.mkString("/")}"
    } else {
      assert(SystemUtils.IS_OS_WINDOWS)
      val parts = for (i <- 0 until path.getNameCount) yield path.getName(i)
      parts.mkString("/")
    }
}
