package de.unruh.isabelle.misc

import java.nio.file.{Files, Path}
import java.text.DateFormat
import java.time.LocalDate
import java.util.{Date, GregorianCalendar}
import scala.io.{Codec, Source}

class AFP(val afpHome : Path) {
  assert(Files.isDirectory(afpHome), s"$afpHome must be a directory")
  lazy val metadataMetadataPath: Path = {
    val file = afpHome.resolve("metadata").resolve("metadata")
    assert(Files.exists(file), s"$file does not exist")
    assert(Files.isRegularFile(file), s"$file must be a regular file")
    file
  }

  lazy val newestSubmissionDate: LocalDate = {
    val source = Source.fromFile(metadataMetadataPath.toFile)(Codec.UTF8)
//    val format = new java.text.SimpleDateFormat("yyyy-MM-dd")
    var newest : LocalDate = null
    try {
      for (line <- source.getLines();
           if line.startsWith("date = ")) {
        val dateString = line.substring("date = ".length)
//        println(s"[$dateString]")
        val date = LocalDate.parse(dateString)
        if (newest == null || newest.isBefore(date))
          newest = date
      }
      newest
    } finally {
      source.close()
    }
  }
}
