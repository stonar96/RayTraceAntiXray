# RayTraceAntiXray
Paper plugin for server-side async multithreaded ray tracing to hide ores that are exposed to air using Paper Anti-Xray engine-mode 1.
## How to install
* Download and install [Paper](https://papermc.io/downloads) 1.18.2.
* Enable [Paper Anti-Xray](https://docs.papermc.io/paper/anti-xray) using `engine-mode: 1`.
* Download and install [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/).
* Download and install [RayTraceAntiXray](https://github.com/stonar96/RayTraceAntiXray/releases).
* Configure RayTraceAntiXray by editing the file plugins/RayTraceAntiXray/[config.yml](RayTraceAntiXray/src/main/resources/config.yml).
* See also: [Recommended settings](https://gist.github.com/stonar96/69ca0311392188b7ac2ece226286147f).
## Known issues
* The plugin is generally very resource intensive. I only recommend using it if you have "unused" CPU threads available on your server.
* In principle, the plugin can also hide tile entities. However, the tile entity packets are still being sent, which means that more clever hacked clients could bypass this. (Could be fixed by not sending the tile entity packets until the block is updated.)
* The culling algorithm is not 100% accurate for performance reasons. When in doubt, it is assumed that a block is visible. Usually, however, this cannot be abused.
## Demo
![RayTraceAntiXray](https://user-images.githubusercontent.com/18699205/112784731-aed75e00-9052-11eb-92d6-b0dd4af79290.gif)
