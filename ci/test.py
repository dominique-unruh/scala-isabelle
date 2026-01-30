#!/bin/python

# PYTHON_ARGCOMPLETE_OK

import dataclasses
import glob
import html
import os
import random
import shutil
import subprocess
import sys
from typing import Iterable, Collection

import docker
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

from Cython.Utils import modification_time

docker_client = docker.DockerClient()

@dataclass(frozen=True, kw_only=True)
class Settings:
    ci_dir: Path
    main_dir: Path
    show_results: bool
    container_repo_dir: str = "scala-isabelle"
    isabelle_versions: Collection[str] = ("2025-2", "2025-1", "2025", "2024", "2023", "2022", "2021-1", "2021", "2020", "2019")
    java_versions: Collection[int] = (11, 17, 21, 25)

@dataclass(frozen=True, kw_only=True)
class TestConfig:
    isabelle: str|None
    java: int|None

    def description(self) -> str:
        return f"Isabelle{self.isabelle}, Java {self.java}"

    def dirname(self) -> str:
        return f"isa{self.isabelle}-java{self.java}"

    def random(self, settings: Settings) -> TestConfig:
        isabelle = self.isabelle
        if isabelle is None:
            isabelle = random.choice(settings.isabelle_versions)
        java = self.java
        if java is None:
            java = random.choice(settings.java_versions)
        return TestConfig(isabelle=isabelle, java=java)

@dataclass(frozen=True, kw_only=True)
class TestResult:
    success: bool
    results_dir: Path


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

def do_test(settings: Settings, config: TestConfig) -> TestResult:
    scala_isabelle_dir = settings.main_dir
    ci_dir = settings.ci_dir
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
    subprocess.run(["docker", "cp", f"temp_container:/home/user/{settings.container_repo_dir}/target/test-reports-html",
                    result_dir.as_posix()], check=True)
    subprocess.run(["docker", "cp", f"temp_container:/home/user/{settings.container_repo_dir}/target/test-reports",
                    result_dir.as_posix()], check=True)
    subprocess.run("docker rm temp_container", shell=True, check=True)
    with open(result_dir / "test-reports-html/index.html", "rt") as f:
        html = f.read()
        html = html.replace("ScalaTest Results",
                            config.description() + " @ " + datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
    with open(result_dir / "test-reports-html/index.html", "wt") as f:
        f.write(html)
    if settings.show_results:
        subprocess.run(["firefox", (result_dir / "test-reports-html/index.html").as_posix()], check=True)
    success = int(result_dir.joinpath("test-reports-html/return-code.txt").read_text()) == 0
    return TestResult(success=success, results_dir=result_dir)

def do_tests(settings: Settings, base_config: TestConfig, num_tests: int) -> bool:
    results: dict[TestConfig, TestResult] = {}
    for test_no in range(1,num_tests+1):
        config = base_config.random(settings)
        if config in results:
            print(f"Skipping test {test_no} due to identical test config.")
            continue
        print(f"Test {test_no}: {config.description()}")
        result = do_test(settings, config)
        results[config] = result

    with open(settings.main_dir / "target/test-results/index.html", 'wt') as index_file:
        index_file.write("<ul>\n")
        for file in glob.glob((settings.main_dir / "target/test-results/*/test-reports-html/index.html").as_posix()):
            file = Path(file)
            modification_time = datetime.fromtimestamp(file.stat().st_mtime).strftime('%Y-%m-%d %H:%M:%S')
            index_file.write(f"""<li><a href="{html.escape(file.as_uri())}">{html.escape(file.parent.parent.name)}</a> ({modification_time})</li>\n""")
        index_file.write("</ul>\n")

    for conf, result in results.items():
        print(f"{"✅" if result.success else "❌"} {conf.description()}")

    success = all(result.success for result in results.values())
    return success

def main():
    import argparse
    import argcomplete
    parser = argparse.ArgumentParser()
    parser.add_argument("--isabelle", type=str)
    parser.add_argument("--java", type=int)
    parser.add_argument("--num-tests", type=int, default=1)
    parser.add_argument("--show-results", action="store_true")
    parser.add_argument("--no-show-results", action="store_false", dest="show_results")
    argcomplete.autocomplete(parser)
    args = parser.parse_args()

    ci_dir = Path(__file__).absolute().parent
    scala_isabelle_dir = ci_dir.parent
    show_results = args.show_results or (args.num_tests <= 1)
    settings = Settings(ci_dir=ci_dir, main_dir=scala_isabelle_dir, show_results=show_results)

    config = TestConfig(isabelle=args.isabelle, java=args.java)
    success = do_tests(settings, config, args.num_tests)
    if not success:
        sys.exit(1)

if __name__ == '__main__':
    main()
