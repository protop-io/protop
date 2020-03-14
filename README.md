<p align="center">
    <img src="assets/logo.png" alt="protop logo" width="400px"/>
</p>

# protop

protop is an open source protocol buffer package manager. It is mainly motivated by the lack of consistency in how protobufs are currently shared. For public APIs to embrace gRPC, or for an organization to develop with protobufs without a complex monorepo solution, a simple package manager is the obvious solution.

This tool works well for local projects as well as projects with external dependencies. For distributing projects, there is an implementation of a Nexus repository plugin being developed [here](https://github.com/protop-io/nexus-repository-protop), currently in a functional beta state as well.

Protop is welcome to contributions; if you are interested in the technology, please reach out here are to the maintainer at jeffery@protop.io. If you have any thoughts about improvements/features or issues, let's chat!

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

## Publish ("link") locally
This will make the project accessible to all other projects that run `sync` with links enabled.
```bash
$ protop link
```

To unlink a project, in the project directory:
```bash
$ protop unlink
```

To unlink all projects system-wide:
```bash
$ protop links clean
```

### Publish to a repository*
```bash
$ protop publish -r=https://repository.example.com
```

*There is an implementation of a Nexus plugin for protop required for this to work. More details [here](https://github.com/protop-io/nexus-repository-protop). Coming soon, there will be better documentation on the API of the repository itself.

### Sync local/external dependencies
Run the following with `-l` or `--use-links` to include local/linked dependencies, or run without it to only include dependencies from the network.
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
├── .protop
│   └── path
│       └── org_a
│           └── project_a -> ~/.protop/cache/other_org/other_project/1.2.1
├── protop.json
├── AwesomeProto.proto
└── out
```
As you can see, protop creates symbolic links to projects in a system-wide cache where all dependencies are stored whether they were `protop link`ed or retrieved from an external repository.

To clean the system-wide cache (not generally recommended/necessary):
```bash
$ protop cache clean
```

## Use with Gradle or other build tools

### Use with Gradle
There isn't a custom Gradle plugin for protop (yet). Even so, the implementation is quite simple. Assuming you have an existing `build.gradle` setup for a protobuf project, add the following task to the root project:
```groovy
task protop(type: Exec) {
    workingDir "."
    commandLine "protop", "sync"
}
```
This task will simply run `protop sync`. To invoke it upon `gradle build` and ensure that it is run before the protos are generated, alter the `protobuf` block (or add it now):
```groovy
protobuf {
    // ...
    generateProtoTasks {
        // ...
        all().each { task -> task.dependsOn protop }
    }
}
```

Finally, make sure the compiler will find all the synced protos:
```groovy
sourceSets {
    main {
        // ...
        proto {
            srcDir ".protop/path"
        }
    }
}
```

### Use with protoc directly
With dependencies already synced, you can call `protoc` in a project that looks like the one above:

```bash
protoc --proto_path=.protop/path \
       --java_out=out \
       AwesomeProto.proto
```

### Use with other build tools
Please let us know other tools you'd like to see an integration with besides the ones above. If you have an examples you'd like to add or have issues with the examples here, please open an issue or submit a PR.

## `.protoprc` configuration

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

- Production repository - At least initially, this will be implemented using the Nexus Repository Manager using the protop plugin.
- Interface for joining the repository and creating organizations (probably somewhere at _something.protop.io_)
- Full documentation at [protop.io](http://protop.io)
- Integration with Gradle and other development tools
- More contributors!

# Development

Protop is written in Java using the [Picocli](https://github.com/remkop/picocli) CLI framework. There are two main modules in this library, `protop-cli` which is the entry point for the CLI, and `protop-core` which contains all of the actual business logic.

To fully test with publishing, follow the [repository documentation](https://github.com/protop-io/nexus-repository-protop). The Nexus plugin is still in development and considered unstable for production, but it is stable enough for feature development of the CLI at this point.
