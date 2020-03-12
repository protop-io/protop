<p align="center">
    <img src="assets/logo.png" alt="protop logo" width="400px"/>
</p>

# protop

protop is an open source protocol buffer package manager inspired by--and heavily modeled after--NPM. It is mainly motivated by the total lack of consistency in how protobufs are currently shared. For public APIs to embrace gRPC, or for an organization to develop with protobufs without a complex monorepo solution, a simple package manager is the obvious solution.

This tool works well for local projects as well as projects with external dependencies. For distributing projects, there is an implementation of a Nexus repository plugin being developed [here](https://github.com/protop-io/nexus-repository-protop), currently in a functional beta state as well.

Protop is welcome to contributions; if you are interested in the technology, please reach out here are to the maintainer at jeffery@protop.io.

_WARNING: This project is super fresh and relatively untested in the wild. If you have any thoughts about improvements or issues, please report them / let's chat!_

## Install

### From Homebrew
Protop is available from our tap. You'll only need to add the tap once:
```bash
brew tap protop-io/protop
```

Install protop with `brew`. It will automatically search our tap:
```bash
brew install protop
```

You can test that the installation worked:
```bash
protop
```

To install upgrades:
```bash
brew upgrade protop
```

### From the source
This is only necessary if you are actively developing or if you are not able to use Homebrew.

In the cloned repository, run the install script in the root directory:
```bash
$ ./install.sh
```

This will prompt you to add `~/.protop/bin` to your PATH if it is not already there. Now you should be able to run `protop help` to see a full list of commands/options.

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

The directory tree should now look like this:
```bash
.
├── protop
│   └── path
│       └── org_a
│           └── project_a -> ~/.protop/cache/other_org/other_project/1.2.1
├── protop.json
├── AwesomeProto.proto
└── out
```
Protop creates symbolic links to projects in a system-wide cache where all dependencies are stored whether they were `protop link`ed or retrieved from an external repository.

### Use with protoc
With dependencies synced, you can call `protoc` in a project that looks like the one above:


```bash
protoc --proto_path=protop/path \
       --java_out=out \
       AwesomeProto.proto
```

### Publish to a repository
```bash
$ protop publish -r=https://repository.example.com
```

There is an implementation of a Nexus plugin for protop required for this to work. More details [here](https://github.com/protop-io/nexus-repository-protop). Coming soon, there will be better documentation on the API of the repository itself.

### `.protoprc` configuration

Create a `.protoprc` file in the project directory to configure options that generally won't change, such as the repository URI. For example:
```properties
repository=https://repository.example.com
```

Currently, the following properties are recognized:
- `repository`: repository URI
- `publish.repository`: repository URI for publishing (prioritized over `repository`)
- `retrieve.repository`: repository URI for retrieving (prioritized over `repository`)

# Roadmap

There are a few technical debts that need to be resolved before any big features will be added (mostly better tests), but here are a few of the bigger items on the roadmap:

- Install via Homebrew (and others?)
- Production repository - At least initially, this will be implemented using the Nexus Repository Manager using the protop plugin.
- Interface for joining the repository and creating organizations (probably somewhere at _something.protop.io_)
- Full documentation at [protop.io](http://protop.io)
- Integration with Gradle and other development tools
- More contributors!

# Development

Protop is written in Java using the [Picocli](https://github.com/remkop/picocli) CLI framework. There are two main modules in this library, `protop-cli` which is the entry point for the CLI, and `protop-core` which contains all of the actual business logic.

To fully test with publishing, follow the [repository documentation](https://github.com/protop-io/nexus-repository-protop). The Nexus plugin is still in development and considered unstable for production, but it is stable enough for feature development of the CLI at this point.
