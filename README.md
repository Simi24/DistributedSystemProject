# Hide And Seek In Distributed System

An academic Java project demonstrating core distributed systems concepts using gRPC and Protocol Buffers.

This repository contains a small distributed multiplayer simulation built for a university course. It focuses on practical implementations of coordination primitives and distributed algorithms while using modern RPC tooling (gRPC + protobuf) for inter-process communication.

## Highlights

- Languages & tools: Java, Gradle, gRPC, Protocol Buffers
- Focus areas: leader election, mutual exclusion, service discovery/access control, graceful exit
- Components: Administrator server, Player nodes, gRPC services, sensor simulators (heart-rate), and data stores

## Implemented distributed algorithms & protocols

- Leader election
	- A dedicated `ElectionService` implements a leader election protocol used to choose a coordinator among players/servers.
- Mutual exclusion / access control
	- Mechanisms to serialize access to shared resources via `AccessBaseService` semantics (token/permission style coordination inside the simulated environment).
- Graceful exit / membership changes
	- `ExitGameService` implements a controlled leave protocol so nodes can exit without corrupting shared state.
- Additional services
	- `GreetingsService` and `TagPlayerService` implement game-specific interactions over gRPC.

These algorithms are implemented in the `gRPC` package and wired together with the `AdministratorServer` and `Player` modules.

## Project structure (important folders)

- `src/main/java` — core Java sources
	- `AdministratorServer/` — server-side logic, player/measurements data stores, and admin services
	- `gRPC/` — gRPC service implementations (Election, AccessBase, ExitGame, Greetings, TagPlayer)
	- `Player/` — player-side modules (network/topology, HR sensor simulator, measurement processing)
	- `Simulators/` — sensor simulators and buffers used to generate synthetic workloads (heart-rate measurements)
	- `proto/` — .proto files that define the RPC interfaces used across components

## Key files

- `Main.java` — entry point for launching (local) simulation / demo runs
- `proto/*.proto` — Protocol Buffer definitions; used to generate gRPC stubs
- `AdministratorServer/*` — server logic and in-memory data stores (heart-rate, players list)
- `Player/*` — client modules and simulation hooks


## Quick demo

- Start an `AdministratorServer` instance.
- Launch multiple `Player` instances (they'll register via gRPC).
- Observe leader election logs and mutual exclusion events as players attempt to access shared resources.
- Use the HR simulator (`Simulators/HRSimulator.java`) to feed measurements into the system and inspect `HeartRateDataStore` behavior.

## Testing

- Unit tests are under `src/main/java/Test/` (for example, `HeartRateDataStoreTest.java`).
- Use `./gradlew test` to run tests (if the Gradle test task is configured).

