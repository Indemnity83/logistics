# Installation

Get Logistics installed and running in your Minecraft instance.

## Requirements

- **Minecraft:** 1.21.11
- **Fabric Loader:** 0.18.4 or newer
- **Fabric API:** Latest version for 1.21.11
- **Java:** 21 or newer

## Installation Steps

### 1. Install Fabric Loader

If you haven't already:
1. Download [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.11
2. Run the installer
3. Select "Client" installation
4. Choose your Minecraft directory
5. Click Install

### 2. Download Fabric API

Fabric API is required for Logistics to work:
1. Download from [Modrinth](https://modrinth.com/mod/fabric-api) or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
2. Choose version for Minecraft 1.21.11
3. Save the `.jar` file

### 3. Download Logistics

Get the Logistics mod:
- **[GitHub Releases](https://github.com/indemnity83/logistics/releases)** - Latest releases and dev builds
- **[Modrinth](https://modrinth.com/mod/logistics)** - Stable releases
- **[CurseForge](https://www.curseforge.com/minecraft/mc-mods/logistics)** - Stable releases

Download the version for Minecraft 1.21.11.

### 4. Install Mods

1. Locate your `.minecraft/mods` folder:
   - **Windows:** `%appdata%\.minecraft\mods`
   - **macOS:** `~/Library/Application Support/minecraft/mods`
   - **Linux:** `~/.minecraft/mods`
2. Create the `mods` folder if it doesn't exist
3. Copy both `.jar` files into the `mods` folder:
   - Fabric API `.jar`
   - Logistics `.jar`

### 5. Launch Minecraft

1. Open the Minecraft Launcher
2. Select the **Fabric** profile
3. Click **Play**
4. Verify Logistics is loaded (check mods menu)

## Verification

In-game, check that Logistics loaded successfully:
1. Open the mods menu (usually Mod Menu mod)
2. Look for "Logistics" in the list
3. Verify version matches what you downloaded

Or just try crafting a [Copper Transport Pipe](../pipes/copper-transport-pipe.md)!

## Troubleshooting

**Game crashes on startup:**
- Verify you're using Minecraft 1.21.11
- Check Fabric Loader is version 0.18.4+
- Ensure Fabric API is installed
- Confirm Java 21 or newer

**Mod doesn't appear:**
- Check `.jar` files are in the `mods` folder
- Verify Fabric profile is selected in launcher
- Look for error messages in launcher log

**Recipe conflicts or issues:**
- Check for mod compatibility issues
- Try with only Logistics and Fabric API installed first

## Next Steps

Once installed, learn how to build your first network:
- [Your First Pipe Network](first-network.md) - Build a basic item transport system
- [Understanding Tiers](understanding-tiers.md) - Learn the progression system

## See Also
- [Pipe Networks](../core/pipe-networks.md) - How pipes connect
- [Pipes](../pipes/index.md) - All available pipe types
- [Copper Transport Pipe](../pipes/copper-transport-pipe.md) - Basic transport pipe
