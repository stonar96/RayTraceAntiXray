# RayTraceAntiXray
Paper plugin for server-side async multithreaded ray tracing to hide ores that are exposed to air using Paper Anti-Xray engine-mode 1.
## How to install
* Download and install [Paper](https://papermc.io/downloads) 1.19.1.
* Enable [Paper Anti-Xray](https://docs.papermc.io/paper/anti-xray) using `engine-mode: 1`.
* Download and install [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/).
* Download and install [RayTraceAntiXray](https://github.com/stonar96/RayTraceAntiXray/releases).
* Configure RayTraceAntiXray by editing the file plugins/RayTraceAntiXray/[config.yml](RayTraceAntiXray/src/main/resources/config.yml).
* See also: [Recommended settings](https://gist.github.com/stonar96/69ca0311392188b7ac2ece226286147f).
## Known issues
* The plugin is generally very resource intensive. I only recommend using it if you have "unused" CPU threads available on your server in order to minimize the impact on the main thread.
* In principle, the plugin can also hide tile entities. However, even though the blocks themselves are being hidden, the tile entity packets are still being sent, which means that more clever hack clients could bypass this. (Could be fixed in a future release by not sending the tile entity packets until the block is visible.)
* The culling algorithm is intentionally not 100% accurate for performance and functional reasons. When in doubt, it is assumed that a block is visible. Thus hidden blocks tend to be revealed rather earlier than late, provided that the server isn't overloaded and doesn't lag. Usually, however, this cannot be abused.
## Demo
![RayTraceAntiXray](https://user-images.githubusercontent.com/18699205/112784731-aed75e00-9052-11eb-92d6-b0dd4af79290.gif)
