# RSE Project 2020

In this file, we explain how to set up, run, and develop this project. The
project description is located in
[./project-description/project.md](./project-description/project.md).

## Frameworks

The skeleton leverages various frameworks that simplify and aid software
development. In the following, we provide a quick introduction, with a focus on
how these frameworks help you complete this project.

We provide instructions exclusively for Linux (in particular, for Ubuntu). We
strongly recommend you use Ubuntu to develop and run this project.

### Docker

The project is set up to use [docker](https://www.docker.com/) - if you are
curious, you can find a simple introduction
[here](https://docker-curriculum.com/). Docker essentially simulates a
lightweight virtual machine, which allows us work in a controlled environment
that comes with the necessary dependencies for this project (e.g., APRON, Soot,
...).

To install docker, you may follow
[these](https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-ubuntu-18-04)
instructions.

To run the docker image in interactive mode (i.e., in a terminal where you can
run any command), run

```bash
./run-docker.sh
[...]
root@a515c5af06d6:/project/analysis$ TYPE YOUR COMMAND HERE...
```

In the following, we assume you ran the previous command (and are inside your
docker image). If you are set up locally (discussed later), you may omit this
step and operate directly on your machine (instead of in docker).

### Maven

We manage the project's build, reporting and testing using
[Maven](https://maven.apache.org/). The most important maven commands are:

```bash
# delete automatically generated files
root@a515c5af06d6:/project/analysis$ mvn clean
[...]
# compile, run unit and integration tests
root@a515c5af06d6:/project/analysis$ mvn verify
[...]
# generate report (tests, code coverage by JaCoCo)
root@a515c5af06d6:/project/analysis$ mvn site
[...]
```

The latter command generates a report in
[./analysis/target/site/index.html](./analysis/target/site/index.html).

### Unit and Integration Testing

We have set up maven to run unit tests (detected by filename pattern
`*Test.java`) and integration tests (detected by filename pattern `*IT.java`),
which are located in directory
[./analysis/src/test/java](./analysis/src/test/java).

The most important maven commands regarding testing are

```bash
# run unit tests and create "surefire" report
root@a515c5af06d6:/project/analysis$ mvn test surefire-report:report
# run integration tests and create "failsafe" report
mvn verify -Dskip.surefire.tests site -Dskip.surefire.tests
```

The two reports are located in
[surefire-report.html](analysis/target/site/surefire-report.html) and
[failsafe-report.html](analysis/target/site/failsafe-report.html), respectively.

### Logging

We use [SLF4J](http://www.slf4j.org/) as a front-end for logging. Our logging
backend is [Logback](http://logback.qos.ch/) - feel free to adapt the [logging
configuration](./analysis/src/main/resources/logback.xml) if needed.

### GitLab CI/CD

We have set up the project to build and test the project on every push to the
code repository, as controlled by [.gitlab-ci.yml](.gitlab-ci.yml). When the
tests fail, you will be notified by e-mail (depending on your GitLab settings).

We recommend that you start to develop new functionality by writing
corresponding tests, i.e., that you adhere to [test-driven
development](https://en.wikipedia.org/wiki/Test-driven_development).

#### Common issues

- `ERROR: Job failed (system failure)`: Likely, running the job again will
  resolve this issue. To this end, in GitLab, navigate to CI/CD -> Jobs -> Click
  on the box "Failed" of the failed job -> Click "Retry" (top right)

### Java Security Manager

We use the [Java Security
Manager](https://docs.oracle.com/javase/tutorial/essential/environment/security.html)
to restrict access to sensitive resources. The security policy is controlled by
[./analysis/src/main/resources/java.policy](./analysis/src/main/resources/java.policy),
which you should not modify.

### JaCoCo

We use [JaCoCo](https://www.eclemma.org/jacoco/) to record and report the code
coverage achieved by all tests. When running `mvn verify`, the code coverage is
reported in
[analysis/target/site/jacoco-merged-test-coverage-report/index.html](analysis/target/site/jacoco-merged-test-coverage-report/index.html).
As discussed in the project description, we will award additional points for a
instruction coverage of `>=75%`.

## Sanity Check for Submission

<span style="color:red">**IMPORTANT NOTE:**</span> To ensure we will be able to run your submission, follow these rules:

1. Various files in this repository come with a note to `NOT MODIFY THIS FILE`.
   We will overwrite these files for grading, so not adhering to them may mean
   that we cannot compile your project.
2. Before submission, check that running the tests works in the docker image. We
   will use this workflow for testing your submission.

```bash
root@cd6589e9f6f5:/project/analysis$ mvn verify
[...]
```

## Local Setup (Optional but Recommended)

In many cases, it is convenient to run the project outside the docker container
(e.g., for debugging).

### Local Installation

To install the relevant prerequisites on your system,
follow the commands in the [Dockerfile](./docker/Dockerfile). To run the commands in
your terminal, note the following.

#### RUN

You can run commands starting with `RUN` in your terminal. For example, if the
Dockerfile states

```Docker
RUN apt-get update && apt-get install -y \
  build-essential \
  wget
```

you may run (we split the command at `&&`, removed `\` and new lines, and added
`sudo`):

```bash
sudo apt-get update
sudo apt-get install -y build-essential wget
```

Note: Do not use `sudo` for the `mvn install:install-file` command (as mentioned
in the Dockerfile).

#### ENV

You can run commands starting with `ENV` on your terminal. For example, if the
Dockerfile states

```Docker
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64
```

you may run (we removed `ENV` and added `export` and `=`):

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
```

#### Using Java 1.8

For some reason, Soot runs significantly faster in Java 1.8 than in Java 1.11.
If you figure out why, let us know :)

To ensure maven indeed uses Java 1.8, run

```bash
echo "export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64" > $HOME/.mavenrc
```

### Local Configurations

If you want to change configurations local to your machine (such as
`JAVA_HOME`), follow the instructions in
[analysis/settings_template.xml](analysis/settings_template.xml).

### Local Development

We recommend using eclipse for development, which you can install, e.g., via

```bash
sudo snap install --classic eclipse
```

To prepare eclipse for running and debugging the project, follow these steps:

1. Open eclipse
2. Import the project: File -> Import -> Existing Maven Projects -> Next ->
   Browse and select the path to the [analysis](./analysis) directory -> Select
   `/pom.xml` -> Finish
3. Run the project: Right click on `analysis` in "Project Explorer" -> Run As ->
   Run Configurations -> Double-click Junit -> Select "Run all tests in the
   selected project" -> Switch to tab "Environment" -> Add -> Name:
   "LD_LIBRARY_PATH", Value: "/usr/local/lib" -> Ok -> Switch to tab JRE ->
   Select JavaSE-1.8 (or another java-8 version) -> Run

When you modify the project files from outside eclipse (e.g., from the command
line using the `mvn` command), you may need to update the project in eclipse. To
this end:

1. Open eclipse
2. Right click on `analysis` in "Project Explorer" -> Maven -> Update Project ->
   OK
