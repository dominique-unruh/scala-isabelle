import de.unruh.isabelle.control.PIDEWrapper

import java.nio.file.Path
import java.io.{BufferedReader, File}
import isabelle.{Bash, Build, Isabelle_System, Isabelle_Thread, ML_Process, Options, Sessions, System_Channel}

import java.util.concurrent.Callable
import java.util.function.Consumer

class PIDEWrapperImpl(val isabelleRoot: Path) extends PIDEWrapper {
  override type Process = Bash.Process

  override def killProcess(process: Bash.Process): Unit =
    process.terminate()

//  override def stdout(process: Process): BufferedReader = process.stdout
//  override def stderr(process: Process): BufferedReader = process.stdout

  override def startIsabelleProcess(cwd: File, mlCode: String, logic: String,
                                    sessionDirs: Array[String], build: Boolean): Process = {
    val channel = System_Channel()
    Isabelle_System.init(isabelle_root = isabelleRoot.toString)
    val options = Options.init()
    val channel_options = options.string.update("system_channel_address", channel.address).
      string.update("system_channel_password", channel.password)

    val sessionDirs2 = sessionDirs.map(isabelle.Path.explode).toList
    val sessions_structure = Sessions.load_structure(options = options, dirs = sessionDirs2)
    val store = Sessions.store(options)

    if (build) {
      // Build.build_logic requires to run on an Isabelle_Thread.
      // We make it a daemon thread, but we wait for it to terminate before we continue. Thus it will effectively be a daemon only if the current thread is.
      // TODO: Does "build_logic" scan all sessions again? Can we avoid that?
      Isabelle_Thread.fork(name = "Build Isabelle", daemon = true) {
        Build.build_logic(options = options, logic = logic, build_heap = true, dirs = sessionDirs2)
      }.join()
    }

    ML_Process(channel_options, sessions_structure, store, eval_main = mlCode, logic = logic, cwd = cwd)
  }

  override def waitForProcess(process: Process, progress_stdout: Consumer[String], progress_stderr: Consumer[String]): Unit = {
    process.result(progress_stderr = progress_stderr.accept, progress_stdout = progress_stdout.accept)
  }
}
