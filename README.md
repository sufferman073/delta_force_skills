# dealt_force_skills

A Minecraft Forge mod that adds special-force inspired characters, skills, equipment, and combat mechanics.

## Minecraft Version

* Minecraft: 1.20.1
* Mod Loader: Forge

## Project Status

This project is under active development. APIs, mechanics, balance values, assets, and compatibility behavior may change between versions.

## Build

On Windows:

```bash
gradlew.bat build
```

On Linux/macOS:

```bash
./gradlew build
```

The built mod jar will usually be generated under:

```text
build/libs/
```

Publish only `delta_force_skills-<version>.jar` for each supported loader. The Forge
release includes its required bundled runtime library; its slim JAR is an internal
build input under `build/intermediates/slim/` and is not a separate release.
NeoForge releases are built in `MDK-1.21.1-ModDevGradle-main/build/libs/`.
Keep all runtime assets and behavior when reducing package size.

## Configuration

Gameplay values, `/reload` behavior, energy examples, and local HUD positioning/scaling are documented in [docs/CONFIGURATION.md](docs/CONFIGURATION.md).

## License

Code in this repository is licensed under the MIT License. See `LICENSE-CODE.txt`.

Unless explicitly stated otherwise, textures, models, sounds, character names, character settings, design documents, development notes, and private archives are not covered by the MIT License. See `ASSETS_LICENSE.md`.

## AI-Assisted Development Notice

Parts of this project were implemented with AI-assisted coding tools under human direction and review. Game design, integration decisions, testing, and release ownership are maintained by the project maintainer.
