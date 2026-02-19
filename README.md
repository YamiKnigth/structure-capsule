# Structure Capsule Mod

A Fabric mod for Minecraft 1.21.1 that allows admins to save structures as items and give them to players for easy placement.

## Features

- **Empty Capsule**: A tool for admins to define and save structures
  - Left-click a block to set Position 1
  - Right-click a block to set Position 2
  - Shift + Right-click in air to open the save GUI
  - Configure structure name and preview color

- **Filled Capsule**: Contains a saved structure
  - Hold to see a ghost preview of the structure at your crosshair
  - Right-click to place the structure
  - Automatically rotates based on your facing direction
  - Item is consumed on use

- **Async Structure Placement**: Structures are placed over multiple ticks to prevent server lag
  - Configurable blocks per tick (default: 500)
  - Maintains 20 TPS even with large structures

## Commands

- `/scapsule give <structure_name> [player]` - Give a filled capsule to yourself or another player
- `/scapsule list` - List all saved structures

## Configuration

Edit `config/structurecapsule.json`:

```json
{
  "maxStructureSize": 5000,
  "blocksPerTick": 500,
  "allowedDimensions": [
    "minecraft:overworld",
    "minecraft:the_nether",
    "minecraft:the_end"
  ]
}
```

## Structure Storage

Structures are saved as NBT files in `config/structurecapsule/saved_structures/`

## Building

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/`

## Technical Details

### Rendering
- Uses `WorldRenderEvents.LAST` for ghost preview rendering
- No entities are spawned - purely visual rendering
- Client-side structure caching to prevent disk reads every frame

### Placement
- Server-side async placement with chunk loading checks
- Queue-based system processes one structure at a time
- Configurable block placement rate to balance performance

### Networking
- Custom packet for structure save requests
- NBT storage in items references disk files (not inline data)
- Prevents packet size issues with large structures

## License

MIT License
