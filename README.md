# RayTraceAntiXray
Paper plugin for server-side async multithreaded ray tracing to hide ores that are exposed to air using Paper Anti-Xray engine-mode 1.

Paper Anti-Xray can't hide ores that are exposed to air in caves for example (see picture below). This plugin is an add-on for Paper Anti-Xray to hide those ores too, using ray tracing to calculate whether or not those ores are visible to players. This plugin can also fully hide block entities such as chests since Minecraft 1.20.6.

![RayTraceAntiXray](https://user-images.githubusercontent.com/18699205/185815590-4b2efce6-5a26-4579-b079-e9958a454fd0.gif)
## How to install
* Download and install [Paper](https://papermc.io/downloads/paper) 1.21.5. Folia is supported since Minecraft 1.20.1.
* Enable [Paper Anti-Xray](https://docs.papermc.io/paper/anti-xray) using `engine-mode: 1`.
* Download and install [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/).
* Download and install [RayTraceAntiXray](https://builtbybit.com/resources/raytraceantixray.24914/). (For older Minecraft versions, browse the update history.)
* Configure RayTraceAntiXray by editing the file plugins/RayTraceAntiXray/[config.yml](RayTraceAntiXray/src/main/resources/config.yml).
* See also: [Recommended settings](https://gist.github.com/stonar96/69ca0311392188b7ac2ece226286147f).
* Note that you should restart your server after each of these steps. Don't enable, disable or reload this plugin on a running server under any circumstances (e.g. using `/reload`, plugin managers, etc.). It won't work properly and will cause issues.
## Known issues
* Depending on the number of players and config settings, this plugin can be resource intensive. I only recommend using it if you have "unused" CPU threads available on your server in order to minimize the impact on the main thread.
* The culling algorithm is intentionally not 100% accurate for performance and functional reasons. When in doubt, it is assumed that a block is visible. Thus hidden blocks tend to be revealed rather earlier than late, provided that the server isn't overloaded and doesn't lag. Usually, however, this cannot be abused.
* There is currently no way to reload this plugin.
## Demo
![RayTraceAntiXray](https://user-images.githubusercontent.com/18699205/112784731-aed75e00-9052-11eb-92d6-b0dd4af79290.gif)
## License
The [LICENSE](LICENSE) file applies to the **source code** of this project. Please don't (re)distribute **compiled binary versions** of this project or derivative works that are directly usable as intended by this project. Shading or using this project as a library for other purposes is permitted.
