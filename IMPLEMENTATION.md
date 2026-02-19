# StructureCapsule Mod - Implementation Complete

## Project Structure Overview

```
src/main/
├── java/com/yamiknight/structurecapsule/
│   ├── StructureCapsuleMod.java          # Main mod initializer
│   ├── StructureCapsuleClient.java        # Client initializer
│   ├── command/
│   │   └── CapsuleCommand.java            # /scapsule commands
│   ├── config/
│   │   └── ModConfig.java                 # Configuration system
│   ├── items/
│   │   ├── ModItems.java                  # Item registry
│   │   ├── EmptyCapsuleItem.java          # Tool for defining structures
│   │   └── FilledCapsuleItem.java         # Consumable with structure data
│   ├── network/
│   │   └── StructureSavePacket.java       # Client->Server save packet
│   ├── placement/
│   │   └── AsyncStructurePlacer.java      # Async block placement engine
│   ├── renderer/
│   │   └── StructureGhostRenderer.java    # Client-side ghost preview
│   ├── screen/
│   │   └── StructureSaveScreen.java       # GUI for saving structures
│   └── structure/
│       ├── StructureData.java             # Structure data format
│       └── StructureManager.java          # Disk I/O for structures
└── resources/
    ├── fabric.mod.json
    ├── structurecapsule.mixins.json
    └── assets/structurecapsule/
        ├── lang/en_us.json
        └── models/item/
            ├── empty_capsule.json
            └── filled_capsule.json
```

## Key Implementation Details

### 1. Empty Capsule (Position Selection & Saving)
- **Left-click block**: Sets Position 1 (stored in item NBT)
- **Right-click block**: Sets Position 2 (stored in item NBT)
- **Shift + Right-click air**: Opens GUI to name structure and set preview color
- **On save**: Server scans region, saves to `.nbt` file, replaces item with filled capsule

### 2. Filled Capsule (Ghost Preview & Placement)
- **Holding item**: Client-side ghost renderer shows translucent preview at crosshair
- **Right-click**: Queues async placement, consumes item
- **NBT Data**: Stores only reference to structure name + metadata (not full block data)

### 3. Async Structure Placement (Performance Critical)
**Problem**: Large structures (1000+ blocks) would freeze server if placed instantly.

**Solution**: Queue-based time-sliced placement
- Each structure is added to a `ConcurrentLinkedQueue`
- Every server tick, place up to `config.blocksPerTick` blocks (default 500)
- Spreads large placements over multiple ticks
- Ensures 20 TPS maintained even with 5000-block structures

**Code Flow**:
```
Player right-clicks -> AsyncStructurePlacer.queuePlacement()
                    -> Creates PlacementTask with block list
                    -> Added to queue
                    
Every server tick -> AsyncStructurePlacer.tick()
                  -> Process current task
                  -> Place N blocks
                  -> Check chunk loading
                  -> Apply rotation
```

### 4. Ghost Rendering (No Entity Spawning)
**Critical**: Does NOT spawn entities or real blocks during preview.

**Implementation**:
- Hooked into `WorldRenderEvents.LAST`
- Checks if player holding `FilledCapsuleItem`
- Loads structure from disk (cached client-side)
- Renders wireframe boxes using `VertexConsumer`
- Translucent colored lines based on item NBT color
- Rotates preview based on player facing direction

### 5. Structure Storage Format
**Problem**: Can't store 5000 block states in item NBT (would exceed packet size).

**Solution**: Disk-based storage with NBT references
- Structures saved as `.nbt` files in `config/structurecapsule/saved_structures/`
- Item NBT stores only: `structureName`, `color`, `sizeX/Y/Z`, `blockCount`
- Structure loaded from disk when needed (placed or previewed)
- Standard Mojang NBT format for potential compatibility

### 6. Rotation System
Structures rotate in 90° increments based on player facing:
- **NORTH**: No rotation (0°)
- **EAST**: 90° clockwise
- **SOUTH**: 180°
- **WEST**: 90° counter-clockwise

Applied to both:
- Block positions (coordinate transformation)
- Block states (using `BlockState.rotate()`)

### 7. Configuration
JSON-based config in `config/structurecapsule.json`:
```json
{
  "maxStructureSize": 5000,      // Max blocks in structure
  "blocksPerTick": 500,           // Placement speed
  "allowedDimensions": [...]      // Where structures can be placed
}
```

## Commands
```
/scapsule give <structure_name> [player]
  - Requires OP level 2
  - Auto-completes structure names
  - Creates filled capsule with default green preview

/scapsule list
  - Shows all saved structures on disk
```

## Network Protocol
**StructureSavePacket** (Client → Server):
- Triggered when player clicks "Save" in GUI
- Sends: pos1, pos2, structureName, color
- Server validates, scans region, saves to disk
- Server replaces empty capsule with filled capsule in player's hand

## Important Notes for Development

### Adding Features
1. **Block Entities**: Current implementation stores only block states. To save block entity data (chests, signs, etc.), extend `StructureData` to include NBT tags for block entities.

2. **Entities**: No entity support yet. To save entities within structures, add entity list to `StructureData` and spawn during placement.

3. **Custom Placement Logic**: Modify `AsyncStructurePlacer.PlacementTask.placeNextBlock()` for special handling (e.g., skip certain blocks, post-processing).

### Performance Tuning
- Increase `blocksPerTick` for faster placement on powerful servers
- Decrease for shared hosting or high player counts
- Monitor TPS during large structure placements

### Texture Assets Required
The mod needs two 16x16 PNG textures:
- `assets/structurecapsule/textures/item/empty_capsule.png`
- `assets/structurecapsule/textures/item/filled_capsule.png`

Without these, items will appear as missing textures (purple/black checkerboard).

## Building & Testing

```bash
# Build the mod
./gradlew build

# Run client for testing
./gradlew runClient

# Run server for testing
./gradlew runServer
```

## Known Limitations
1. No block entity data preservation (chests will be empty)
2. No entity saving within structures
3. No undo/rollback for placed structures
4. Structures must fit within loaded chunks
5. No permission system beyond vanilla OP levels

## Future Enhancements
- Add block entity support
- Add entity support
- Add structure preview in GUI before saving
- Web-based structure browser
- Structure marketplace/sharing system
- Multi-selection tool (select multiple regions)
- Structure versioning/history
