theory ConnectToScala
  imports Main  
begin

ML \<open>
fun system string = if OS.Process.system string |> OS.Process.isSuccess then () else
  error ("Command " ^ string ^ " returned non-zero error code")
val inputPipeName = "/tmp/input-pipe" ^ string_of_int (Random.random_range 0 100000000000)
val outputPipeName = "/tmp/output-pipe" ^ string_of_int (Random.random_range 0 100000000000)
val _ = system ("mkfifo " ^ inputPipeName)
val _ = system ("mkfifo " ^ outputPipeName)
val _ = system ("/home/unruh/svn/qrhl-tool/scala-isabelle/scripts/connect-to-running-isabelle.py "
          ^ inputPipeName ^ " " ^ outputPipeName)
\<close>

ML_file "../../main/resources/de/unruh/isabelle/control/control_isabelle.ml"

ML \<open>
val control_isabelle_struct =
  #allStruct ML_Env.name_space () |> find_first (fn (n,_) => n = "Control_Isabelle") |> Option.valOf |> snd
val params : Standard_Thread.params = {name="scala-isabelle protocol", stack_limit=NONE, interrupts=false}
val thread = Standard_Thread.fork params (fn () =>
  (#enterStruct ML_Env.name_space ("Control_Isabelle", control_isabelle_struct);
   Control_Isabelle.handleLines ()))
\<close>


end
