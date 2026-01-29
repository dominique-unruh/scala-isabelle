#!/bin/python
import dataclasses
import os
import random
import shutil
import subprocess
import sys

import docker
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

docker_client = docker.DockerClient()


@dataclass(frozen=True, kw_only=True)
class TestConfig:
    isabelle: str
    java: int

    def description(self) -> str:
        return f"Isabelle{self.isabelle}, Java {self.java}"

    def dirname(self) -> str:
        return f"isa{self.isabelle}-java{self.java}"

    @staticmethod
    def random(isabelle: str|None = None, java: int|None = None) -> TestConfig:
        if isabelle is None:
            isabelle = random.choice(["2025-2", "2025-1", "2025", "2024", "2023", "2022", "2021-1", "2021", "2020", "2019"])
        if java is None:
            java = random.choice([11, 17, 21, 25])
        return TestConfig(isabelle=isabelle, java=java)


def rm_rf(dir: Path) -> None:
    if dir.exists():
        shutil.rmtree(dir)

def image_exists(name: str) -> bool:
    try:
        docker_client.images.get(name)
        return True
    except docker.errors.ImageNotFound:
        return False

def cache_image(name: str) -> None:
    """Pulls docker image `name:tag` and tags it as `name:tag-cached`.
    This has the effect that a docker file based on `name:tag-cached` will not automatically pull updates.
    `:tag` can be omitted and defaults to `:latest`.
    """
    if ":" not in name: name = name + ":latest"
    cache_name = name + "-cached"
    if not image_exists(cache_name):
        print(f"Tagging {name} as {cache_name}")
        img = docker_client.images.pull(name)
        img.tag(cache_name)
        docker_client.images.remove(name, noprune=True)

def do_test(config: TestConfig, show_results: bool) -> None:
    ci_dir = Path(__file__).absolute().parent
    scala_isabelle_dir = ci_dir.parent
    print(f"Testing config: {config.description()}")
    subprocess.run(["rsync", scala_isabelle_dir.as_posix()+"/"] +
                     "all-files -a --exclude /.idea --exclude /.run --exclude /ci --exclude /target --exclude /project/target --delete --delete-excluded".split(),
                   check=True, cwd=ci_dir)
    cache_image("archlinux:latest")
    docker_cmd: list[str] = "docker build --pull=false --iidfile .image .".split()
    build_args = {"ISABELLE_VERSION": config.isabelle, "JAVA_VERSION": config.java}
    for k,v in build_args.items():
        docker_cmd.append("--build-arg")
        docker_cmd.append(f"{k}={v}")
    subprocess.run(docker_cmd, check=True, cwd=ci_dir)
    image_id = open(ci_dir.joinpath(".image")).read()
    subprocess.run("docker rm temp_container", shell=True, check=False, stderr=subprocess.DEVNULL)
    subprocess.run("docker create --name temp_container".split() + [image_id], check=True, cwd=ci_dir)
    result_dir = scala_isabelle_dir / "target/test-results" / config.dirname()
    rm_rf(result_dir)
    result_dir.mkdir(exist_ok=True, parents=True)
    subprocess.run(["docker", "cp", "temp_container:/home/user/scala-isabelle/target/test-reports-html",
                    result_dir.as_posix()], check=True)
    subprocess.run(["docker", "cp", "temp_container:/home/user/scala-isabelle/target/test-reports",
                    result_dir.as_posix()], check=True)
    subprocess.run("docker rm temp_container", shell=True, check=True)
    with open(result_dir / "test-reports-html/index.html", "rt") as f:
        html = f.read()
        html = html.replace("ScalaTest Results",
                            config.description() + " @ " + datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
    with open(result_dir / "test-reports-html/index.html", "wt") as f:
        f.write(html)
    if show_results:
        subprocess.run(["firefox", (result_dir / "test-reports-html/index.html").as_posix()], check=True)


def main():
    config = TestConfig.random()
    # config = TestConfig(isabelle="2025", java=17)
    do_test(config, show_results=True)

if __name__ == '__main__':
    main()
