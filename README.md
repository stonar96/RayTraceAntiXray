# RayTraceAntiXray
Paper plugin for server-side async ray tracing to hide ores that are exposed to air using Paper Anti-Xray engine-mode 1.
## How to install
* Download and install [Paper](https://papermc.io/downloads) 1.16.5.
* Enable [Paper Anti-Xray](https://gist.github.com/stonar96/ba18568bd91e5afd590e8038d14e245e) in engine-mode 1.
* Download and install [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/).
* Download and install [RayTraceAntiXray](https://github.com/stonar96/RayTraceAntiXray/releases).
* Configure RayTraceAntiXray by editing the file plugins/RayTraceAntiXray/[config.yml](src/main/resources/config.yml).
## Known issues
* The plugin is generally very resource intensive. I do not recommend using it in production. It is only an experimental project that I have made for fun.
* The plugin only supports the first person perspective (F5 key). (Could be fixed with much overhead by calculating all possible perspectives.)
* The plugin does not distinguish generated blocks from blocks that were placed by players. If players place many hidden blocks, it can become very resource intensive. (Could be fixed by storing placed blocks.)
* In principle, the plugin can also hide tile entities. However, the tile entity packets are still being sent, which means that more clever hacked clients could bypass this. (Could be fixed by not sending the tile entity packets until the block is updated.)
* The culling algorithm is not 100% accurate for performance reasons. When in doubt, it is assumed that a block is visible. Usually, however, this cannot be abused.
## Demo
![RayTraceAntiXray](https://user-images.githubusercontent.com/18699205/112784731-aed75e00-9052-11eb-92d6-b0dd4af79290.gif)
