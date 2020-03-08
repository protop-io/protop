# The protop CLI

_WARNING: This project is super fresh and relatively untested in the wild. If you have any thoughts about improvements or issues, please report them / let's chat!_

protop is an open source protocol buffer package manager inspired by--and heavily modeled after--NPM. It is mainly motivated by the total lack of consistency in how protobufs are currently shared. For public APIs to embrace gRPC, or for an organization to develop with protobufs without a complex monorepo solution, a simple package manager is the obvious solution.

This tool works well for local projects as well as projects with external dependencies. For distributing projects, there is an implementation of a Nexus repository plugin being developed [here](https://github.com/protop-io/nexus-repository-protop), currently in a functional beta state as well.

Protop is welcome to contributions; if you are interested in the technology, please reach out here are to the maintainer at jeffery@protop.io.

## Installation
We are working on making this as easy as possible starting with Homebrew.

## Usage

### Initialize a project
```bash
$ protop init

Organization name (required): awesome-labs
Project name (required): numbers
Initial version (default 1970.1.1.SNAPSHOT):
Entry point: (default "."):
Initialized new project.
```

### View the generated manifest
```bash
$ cat protop.json

{
  "name" : "numbers",
  "version" : "1970.1.1.SNAPSHOT",
  "organization" : "awesome-labs",
  "include" : [ "." ]
}
```

### Sync dependencies
```bash
$ protop sync

Syncing linked projects.
Syncing registered dependencies.
Done syncing.
```

### Publish to a repository*
```bash
$ protop publish
```

*There is an implementation of a Nexus plugin for protop required for this to work. More details [here](https://github.com/protop-io/nexus-repository-protop).

# Development

Protop is written in Java using the `picocli` CLI framework and Gradle for building.

To manually test local changes to the project, run the developer install script in the root directory:
```bash
$ ./install.sh
```

This will prompt you to add `~/.protop/dev/bin` to your PATH if it is not already there.
