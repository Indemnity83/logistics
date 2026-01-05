# Logistics

A modern Minecraft 1.21 Fabric mod for logistics and pipe systems, inspired by BuildCraft and Logistics Pipes.

## Features (Planned)

### Phase 1: Pipes + Items
- Thin pipe blocks with 6-way connections (BuildCraft-like geometry)
- Server-side traveling item simulation with continuous progress
- Client-side interpolation for smooth visuals
- Basic extraction/insertion into adjacent inventories
- Simple routing with filters and priorities

### Phase 2: Logistics Basics
- Request Table block with GUI
- Provider pipes that expose inventory contents
- Request flow: request → routing → provider → dispatch
- Autocrafting support via vanilla Crafter

### Future Phases
- Fluid pipes with Transfer API integration
- Power/cost system for logistics operations

## Development

### Prerequisites
- Java 21 or higher
- Gradle (wrapper included)

### Building
```bash
./gradlew build
```

### Running
```bash
./gradlew runClient
```

## License

MIT License - see LICENSE file for details
