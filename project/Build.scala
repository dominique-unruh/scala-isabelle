import sbt._
import Keys._
import org.apache.commons.lang3.SystemUtils

import java.io.{File, FileNotFoundException, PrintWriter}
import scala.sys.process.Process

object Build {
  def copyFileInto(source: File, targetDir: File) =
    IO.copyFile(source, targetDir / source.name)

  /** Copies the whole classpath to target. Any previous content of target is deleted.
   *
   * Excluded are: the scala-library, anything locate inside an Isabelle distribution
   * */
  def copyClasspath(classpath: Classpath, target: File): Unit = {
    IO.delete(target)
    for (entry <- classpath;
         jar = entry.data
         if jar.isFile
         if !jar.toString.contains("Isabelle")
         if !jar.name.startsWith("scala-library-")
         )
      copyFileInto(jar, target)
  }

  def makeGitrevision(baseDirectory: File, file: File): File = {
    file.getParentFile.mkdirs()
    if (SystemUtils.IS_OS_WINDOWS) {
      val pr = new PrintWriter(file)
      pr.println("Built under windows, not adding gitrevision.txt") // On my machine, Windows doesn't have enough tools installed.
      pr.close()
    } else if ((baseDirectory / ".git").exists())
      Process(List("bash","-c",s"( date && git describe --tags --long --always --dirty --broken && git describe --always --all ) > $file")).!!
    else {
      val pr = new PrintWriter(file)
      pr.println("Not built from a GIT worktree.")
      pr.close()
    }
    file
  }
}

/** Class that holds a search path for Isabelle home directories */
case class IsabelleHomeDirectories(directories: File*) {
  /** Finds the Isabelle installation directory for version `version` */
  def findIsabelleRoot(version: String): File = {
    val candidates =
      for (dir <- directories;
           root <- List(dir / s"Isabelle$version", dir / s"Isabelle$version.app")
           if root.isDirectory)
      yield root
    val isabelleHome = candidates.headOption.getOrElse {
      throw new FileNotFoundException(s"No Isabelle root director found for Isabelle$version, searched $directories")
    }
    assert(isabelleHome.canRead, isabelleHome)
    isabelleHome
  }

  /** Constructs a classpath containing all jars from Isabelle version `version` */
  def getClasspath(version: String): Classpath = {
    val isabelleHome = findIsabelleRoot(version)
    val cp = ((isabelleHome / "lib" / "classes" +++ isabelleHome / "contrib") ** "*.jar").classpath
    assert(cp.nonEmpty)
    cp
  }
}
