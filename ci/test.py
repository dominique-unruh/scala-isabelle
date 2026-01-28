#!/bin/python
import dataclasses
import os
import random
import shutil
import subprocess
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

ISABELLE_VERSION=2025
JAVA_VERSION=11

@dataclass(frozen=True, kw_only=True)
class TestConfig:
    isabelle: str
    java: int

    def description(self) -> str:
        return f"Isabelle{self.isabelle}, Java {self.java}"

    @staticmethod
    def random() -> TestConfig:
        isabelle = random.choice(["2025-2", "2025-1", "2025", "2024", "2023", "2022", "2021-1", "2021", "2020", "2019"])
        java = random.choice([11, 17, 21, 25])
        return TestConfig(isabelle=isabelle, java=java)


def rm_rf(dir: Path) -> None:
    if dir.exists():
        shutil.rmtree(dir)

def do_test(config: TestConfig, show_results: bool) -> None:
    ci_dir = Path(__file__).absolute().parent
    scala_isabelle_dir = ci_dir.parent

    print(f"Testing config: {config.description()}")
    subprocess.run(["rsync", scala_isabelle_dir.as_posix()+"/"] + "all-files -a --exclude /ci --exclude /target --exclude /project/target --delete --delete-excluded".split(),
                   check=True, cwd=ci_dir)
    docker_cmd: list[str] = "docker build --iidfile .image .".split()
    build_args = {"ISABELLE_VERSION": config.isabelle, "JAVA_VERSION": config.java}
    for k,v in build_args.items():
        docker_cmd.append("--build-arg")
        docker_cmd.append(f"{k}={v}")
    subprocess.run(docker_cmd, check=True, cwd=ci_dir)
    image_id = open(ci_dir.joinpath(".image")).read()
    subprocess.run("docker rm temp_container", shell=True, check=False)
    subprocess.run("docker create --name temp_container".split() + [image_id], check=True, cwd=ci_dir)
    rm_rf(scala_isabelle_dir / "target/test-reports")
    rm_rf(scala_isabelle_dir / "target/test-reports-html")
    (scala_isabelle_dir / "target").mkdir(exist_ok=True)
    subprocess.run(["docker", "cp", "temp_container:/home/user/scala-isabelle/target/test-reports-html",
                    (scala_isabelle_dir / "target/").as_posix()], check=True)
    subprocess.run(["docker", "cp", "temp_container:/home/user/scala-isabelle/target/test-reports",
                    (scala_isabelle_dir / "target/").as_posix()], check=True)
    subprocess.run("docker rm temp_container", shell=True, check=True)
    with open(scala_isabelle_dir / "target/test-reports-html/index.html", "rt") as f:
        html = f.read()
        html = html.replace("ScalaTest Results",
                            config.description() + " @ " + datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
    with open(scala_isabelle_dir / "target/test-reports-html/index.html", "wt") as f:
        f.write(html)
    if show_results:
        subprocess.run(["firefox", (scala_isabelle_dir / "target/test-reports-html/index.html").as_posix()], check=True)



config = TestConfig.random()
# config = TestConfig(isabelle="2025", java=17)
do_test(config, show_results=True)