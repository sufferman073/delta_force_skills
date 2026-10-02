# delta_force_skills - NeoForge 1.21.1

This branch contains the NeoForge 1.21.1 edition of delta_force_skills.
The Forge 1.20.1 edition is maintained on the `1.20.1forge` branch.

Build with Java 21 and `gradlew.bat build` on Windows or `./gradlew build`
on Linux/macOS. The mod JAR is generated in `build/libs/`.

Repository license notices are retained in `LICENSE-CODE.txt` and
`ASSETS_LICENSE.md`. The original MDK notice is in `TEMPLATE_LICENSE.txt`.

## Original NeoForge MDK notes


### Installation information

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything
{this does not affect your code} and then start the process again.

### Mapping names

By default, the MDK is configured to use the official mapping names from Mojang for methods and fields
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

### Additional resources

Community Documentation: https://docs.neoforged.net/
NeoForged Discord: https://discord.neoforged.net/
