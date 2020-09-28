#!/usr/bin/python3

import os

distribution_directory = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(distribution_directory + "/src/test/isabelle")

os.system("/opt/Isabelle2020/bin/isabelle jedit -l HOL Scratch.thy &")
