<p align="center">
    <img src="assets/logo.png" alt="protop logo" width="400px"/>
</p>

# protop

protop is an open source protocol buffer package manager inspired by--and heavily modeled after--NPM. It is mainly motivated by the total lack of consistency in how protobufs are currently shared. For public APIs to embrace gRPC, or for an organization to develop with protobufs without a complex monorepo solution, a simple package manager is the obvious solution.

This tool works well for local projects as well as projects with external dependencies. For distributing projects, there is an implementation of a Nexus repository plugin being developed [here](https://github.com/protop-io/nexus-repository-protop), currently in a functional beta state as well.

Protop is welcome to contributions; if you are interested in the technology, please reach out here are to the maintainer at jeffery@protop.io.

_WARNING: This project is super fresh and relatively untested in the wild. If you have any thoughts about improvements or issues, please report them / let's chat!_

## Installation
We are working on making this as easy as possible starting with Homebrew.

### Install from source
In the cloned repository, run the install script in the root directory:
```bash
$ ./install.sh
```

This will prompt you to add `~/.protop/bin` to your PATH if it is not already there. Now you should be able to run `protop --help` to see a full list of commands/options.

## Usage

### Initialize a project
```bash
$ protop init
```
```bash
Organization name (required): awesome-labs
Project name (required): numbers
Initial version (default 1970.1.1.SNAPSHOT):
Entry point: (default "."):
Initialized new project.
```
This will generate a manifest file named `protop.json`.
```json
{
  "name" : "numbers",
  "version" : "1970.1.1.SNAPSHOT",
  "organization" : "awesome-labs",
  "include" : [ "." ]
}
```

### Sync dependencies
```bash
$ protop sync --use-links
```
```bash
Syncing linked projects.
Syncing registered dependencies.
Done syncing.
```

### Publish to a repository
```bash
$ protop publish
```

There is an implementation of a Nexus plugin for protop required for this to work. More details [here](https://github.com/protop-io/nexus-repository-protop). Coming soon, there will be better documentation on the API of the repository itself.

# Development

Protop is written in Java using the `picocli` CLI framework and Gradle for building. There are two main modules in this library, `protop-cli` which is the entry point for the CLI, and `protop-core` which contains all of the actual business logic.

To fully test with publishing, follow the [repository documentation](https://github.com/protop-io/nexus-repository-protop). The Nexus plugin is still in development and considered unstable for production, but it is stable enough for feature development of the CLI at this point.
