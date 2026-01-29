#!/bin/python
import dataclasses
import os
import random
import shlex
import shutil
import subprocess
import sys
import tarfile
import tempfile
import time

import docker
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

import fabric
import paramiko
from paramiko.sftp_client import SFTPClient

docker_client = docker.DockerClient()

@dataclass(frozen=True, kw_only=True)
class SshHost:
    hostname: str = "localhost"
    port: int = 22
    user: str|None = None

    def hostport(self) -> str:
        if self.port == 22:
            return self.hostname
        else:
            return f"{self.hostname}:{self.port}"


@dataclass(frozen=True, kw_only=True)
class TestConfig:
    isabelle: str
    java: int
    os: str

    def description(self) -> str:
        return f"Isabelle{self.isabelle}, Java {self.java}, OS {self.os}"

    def dirname(self) -> str:
        return f"isa{self.isabelle}-java{self.java}-{self.os}"

    # TODO: move to context
    def ssh_host(self) -> SshHost:
        if self.os == "linux":
            return SshHost(hostname="localhost")
        elif self.os == "windows":
            return SshHost(hostname="localhost", port=2222, user="dominique unruh")
        else:
            raise RuntimeError(f"""No host known for os "{os}".""")

    # TODO: move to context
    def temp_directory(self) -> Path:
        if self.os == "linux":
            return Path("/tmp")
        elif self.os == "windows":
            return Path("/c/Windows/Temp")
        else:
            raise RuntimeError(f"""Standard temp dir for os "{os}" not known.""")

    @staticmethod
    def random(isabelle: str|None = None, java: int|None = None, os: str|None = None) -> TestConfig:
        if isabelle is None:
            isabelle = random.choice(["2025-2", "2025-1", "2025", "2024", "2023", "2022", "2021-1", "2021", "2020", "2019"])
        if java is None:
            java = random.choice([11, 17, 21, 25])
        if os is None:
            os = random.choice(["linux", "windows"])
        return TestConfig(isabelle=isabelle, java=java, os=os)

@dataclass(frozen=False, kw_only=True)
class TestContext:
    config: TestConfig
    ssh: fabric.Connection|None = None
    remote_directory: Path|None = None

    def path2os(self, path: Path) -> str:
        os = self.config.os
        if os == "linux":
            return path.as_posix()
        elif os == "windows":
            parts: list[str] = path.parts
            assert len(parts) >= 3, parts
            assert parts[0] == '/', parts
            assert parts[1].lower() == 'c', parts
            win_path = "c:\\" + "\\".join(parts[2:])
            print(win_path)
            return win_path
        else:
            raise RuntimeError(f"Unknown os {os}")

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

def ssh_connect(context: TestContext) -> None:
    ssh_host = context.config.ssh_host()
    print(f"Connecting to remote host {ssh_host.hostport()}")
    connection = fabric.Connection(host=ssh_host.hostname, port=ssh_host.port, user=ssh_host.user,
                                   forward_agent=False)
    context.ssh = connection

def ssh_run(context: TestContext, cmd: list[str], cwd: Path|None=None, check: bool=True, force_bash: bool=False) -> None:
    ssh = context.ssh
    if cwd is None:
        cwd = context.remote_directory
    assert(isinstance(cwd, Path))
    os = context.config.os
    if os == "linux":
        cmd = f"cd -- {shlex.quote(cwd.as_posix())} && {' '.join(shlex.quote(arg) for arg in cmd)}"
    elif os == "windows" and force_bash:
        cmd = f"cd -- {shlex.quote(cwd.as_posix())} && {' '.join(shlex.quote(arg) for arg in cmd)}"
        cmd = subprocess.list2cmdline([r'c:\tools\msys64\usr\bin\bash', '--login', '-c', cmd])
    elif os == "windows" and not force_bash:
        cmd = subprocess.list2cmdline(["c:", "&&", "cd", context.path2os(cwd), '&&'] + cmd)
    else:
        raise RuntimeError(f"Unknown os {os}")

    ssh.run(cmd, echo=True, in_stream=False, replace_env=True, warn=not check)



def do_test(config: TestConfig, show_results: bool) -> None:
    context = TestContext(config=config)
    print(f"Testing config: {config.description()}")
    time.sleep(1)
    ssh_connect(context)
    ci_dir = Path(__file__).absolute().parent
    scala_isabelle_dir = ci_dir.parent
    sftp: SFTPClient = context.ssh.sftp()
    remote_directory = config.temp_directory().joinpath(f"scala-isabelle-test-{random.randint(0, 1000000000)}")
    context.remote_directory = remote_directory

    try:
        print(f"Creating temporary directory {remote_directory}")
        sftp.mkdir(context.path2os(remote_directory))
        with tempfile.NamedTemporaryFile(suffix='.tar.gz', delete=True, delete_on_close=False) as tmp:
            tmp.close()
            print("Tarring data")
            # TODO: remote --exclude ci
            subprocess.run("tar -c -z --exclude ci --exclude .git --anchored --exclude ./target --exclude ./project/target --exclude ./project/project/target -f".split() + [tmp.name, "."], check=True,
                           cwd=scala_isabelle_dir)
            print("Sending data")
            sftp.mkdir(context.path2os(remote_directory / "all-files"))
            remote_tarfile = remote_directory / "data.tgz"
            sftp.put(tmp.name, context.path2os(remote_tarfile))
        print("Untarring data")
        ssh_run(context, 'tar -x -z -f'.split() + [remote_tarfile.as_posix()], cwd=remote_directory / "all-files", force_bash=True)
        sftp.remove(context.path2os(remote_tarfile))
        print("Sending Dockerfile")
        sftp.put(ci_dir.joinpath("Dockerfile").as_posix(), context.path2os(remote_directory / "Dockerfile"))
        # context.ssh.put(ci_dir.joinpath("Dockerfile").as_posix(), context.path2os(remote_directory / "Dockerfile"))
        # subprocess.run(["rsync", scala_isabelle_dir.as_posix()+"/"] + "all-files -a --exclude /ci --exclude /target --exclude /project/target --delete --delete-excluded".split(),
        #                check=True, cwd=ci_dir)
        # TODO:
        # cache_image("archlinux:latest")
        docker_cmd: list[str] = "docker build --pull=false --iidfile .image .".split()
        build_args = {"ISABELLE_VERSION": config.isabelle, "JAVA_VERSION": config.java}
        for k,v in build_args.items():
            docker_cmd.append("--build-arg")
            docker_cmd.append(f"{k}={v}")
        print("Building docker image")
        ssh_run(context, docker_cmd, cwd=remote_directory)
        image_id = sftp.open(context.path2os(remote_directory / ".image")).read().decode('ascii').strip()
        # image_id = open(ci_dir.joinpath(".image")).read()
        print("Removing old temp_container")
        ssh_run(context,"docker rm temp_container".split(), cwd=remote_directory, check=False)  # TODO check=False,  stderr=subprocess.DEVNULL
        print("Creating temp_container")
        ssh_run(context,"docker create --name temp_container".split() + [image_id], cwd=remote_directory)
        print("Copying data from temp_container")
        sftp.mkdir(context.path2os(remote_directory / "results"))
        ssh_run(context,["docker", "cp", "temp_container:/home/user/scala-isabelle/target/test-reports-html",
                        (remote_directory / "results").as_posix()], cwd=remote_directory)
        ssh_run(context, ["docker", "cp", "temp_container:/home/user/scala-isabelle/target/test-reports",
                        (remote_directory / "results").as_posix()], cwd=remote_directory)
        print("Tarring result data")
        ssh_run(context, "tar -c -z -f results.tgz results".split(), cwd=remote_directory)
        print("Removing temp_container")
        ssh_run(context, "docker rm temp_container".split(), cwd=remote_directory)
        local_result_dir = scala_isabelle_dir / "target/test-results" / config.dirname()
        with tempfile.NamedTemporaryFile(suffix='.tar.gz', delete=True, delete_on_close=False) as tmp:
            tmp.close()
            print("Transferring results")
            context.ssh.get(context.path2os(remote_directory / "results.tgz"), tmp.name)
            print(f"Untarring results (in {local_result_dir}")
            rm_rf(local_result_dir)
            local_result_dir.mkdir(exist_ok=True, parents=True)
            subprocess.run("tar -x --strip-components=2 -f".split() + [tmp.name], cwd=local_result_dir)

        with open(local_result_dir / "index.html", "rt") as f:
            html = f.read()
            html = html.replace("ScalaTest Results",
                                config.description() + " @ " + datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
        with open(local_result_dir / "index.html", "wt") as f:
            f.write(html)
        if show_results:
            subprocess.run(["firefox", (local_result_dir / "index.html").as_posix()], check=True)

    finally:
        # print("Removing temporary directory.")
        # ssh_run(context, "rm -rf".split() + [remote_directory.as_posix()], cwd=context.config.temp_directory())
        pass

def main() -> None:
    config = TestConfig.random(os="windows")
    # config = TestConfig(isabelle="2025", java=17)
    do_test(config, show_results=True)
    
if __name__ == '__main__':
    main()