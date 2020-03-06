# The `protop` CLI

WARNING: This project is super un-finished (until this message disappears).

`protop` is a protocol buffer package manager inspired by--and heavily modeled after--NPM. It is mainly motivated by the total lack of consistency in how protobufs are currently shared. For public APIs to embrace gRPC, or for an organization to develop with protobufs without a complex monorepo solution, a simple package manager is the obvious solution.

More documentation about the CLI and project as a whole will follow. For now, here's a look at the CLI-to-be:


Initialize a project:
```bash
awesome-labs/numbers> protop init
Organization name (required): awesome-labs
Project name (required): numbers
Initial version (default 1970.1.1.SNAPSHOT):
Entry point: (default "."):
Initialized new project.
```

View the generated aggregatedManifest:
```bash
awesome-labs/numbers> cat protop.json
{
  "name" : "numbers",
  "version" : "1970.1.1.SNAPSHOT",
  "organization" : "awesome-labs",
  "include" : [ "." ]
}
```

Sync dependencies:
```bash
awesome-labs/numbers> protop sync
Syncing linked projects.
Syncing registered dependencies.
Done syncing.
```

Publish:
```bash
awesome-labs/numbers> protop publish
```
