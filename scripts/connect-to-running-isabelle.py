#!/usr/bin/python3

import os, sys, subprocess

distribution_directory = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(distribution_directory)

inputPipe = sys.argv[1]
outputPipe = sys.argv[2]

# TODO use a logfile provided on the command line

log = open("/tmp/scala-isabelle.log", "wt")

process = subprocess.Popen(["sbt", f"runMain de.unruh.isabelle.control.ConnectToRunningIsabelle {inputPipe} {outputPipe}"],
                           stdin=subprocess.DEVNULL, stderr=subprocess.STDOUT, stdout=subprocess.PIPE, encoding='utf8')

line = process.stdout.readline()
while line:
    if "[STARTED]" in line: break
    log.write(line)
    log.flush()
    line = process.stdout.readline()

if not line:
    log.write(f"Isabelle failed to start. Return code: {process.wait(10)}\n")
    sys.exit(1)

log.write("Scala-isaballe started, forking.\n")
log.flush()

if os.fork():
    sys.exit()

log.write("Forked.\n")
log.flush()

line = process.stdout.readline()
while line:
    log.write(line)
    log.flush()
    line = process.stdout.readline()
