package com.vanillage.raytraceantixray.tasks;

import java.util.Queue;

import com.vanillage.raytraceantixray.util.SchedulerUtil;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.data.Result;

import io.netty.channel.Channel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class UpdateBukkitRunnable implements Runnable {
    private final RayTraceAntiXray plugin;

    public UpdateBukkitRunnable(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getServer().getOnlinePlayers().forEach(p -> {
            PlayerData playerData = plugin.getPlayerData().get(p.getUniqueId());

            if (playerData == null) {
                return;
            }

            World world = playerData.getLocations()[0].getWorld();

            if (!p.getWorld().equals(world)) {
                return;
            }

            Queue<Result> results = playerData.getResults();

            for (Result result = results.poll(); result != null; result = results.poll()) {
                ChunkBlocks chunkBlocks = result.getChunkBlocks();

                // Check if the client still has the chunk loaded and if it wasn't resent in the meantime.
                // Note that even if this check passes, the server could have already unloaded or resent the chunk but the corresponding packet is still in the packet queue.
                // Technically the null check isn't necessary but we don't need to send an update packet because the client will unload the chunk.
                if (chunkBlocks.getChunk() == null || playerData.getChunks().get(chunkBlocks.getKey()) != chunkBlocks) {
                    continue;
                }

                BlockPos block = result.getBlock();

                // Similar to the null check above, this check isn't actually necessary.
                // However, we don't need to send an update packet because the client will unload the chunk.
                // Thus we can avoid loading the chunk just for the update packet.
                if (!world.isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
                    continue;
                }

                BlockState blockState;

                if (result.isVisible()) {
                    SchedulerUtil.runRegionTask(plugin, world, block.getX() >> 4, block.getZ() >> 4, () -> {
                        BlockState blockStateLambda = ((CraftWorld) world).getHandle().getBlockState(block);
                        sendPacketImmediately(p, new ClientboundBlockUpdatePacket(block, blockStateLambda));

                        if (!blockStateLambda.hasBlockEntity()) {
                            return;
                        }

                        BlockEntity blockEntity = ((CraftWorld) world).getHandle().getBlockEntity(block);
                        Object packet = blockEntity.getUpdatePacket();

                        if (packet != null) {
                            sendPacketImmediately(p, packet);
                        }

                    });
                    return;
                } else if (world.getEnvironment() == Environment.NETHER) {
                    blockState = Blocks.NETHERRACK.defaultBlockState();
                } else if (world.getEnvironment() == Environment.THE_END) {
                    blockState = Blocks.END_STONE.defaultBlockState();
                } else if (block.getY() < 0) {
                    blockState = Blocks.DEEPSLATE.defaultBlockState();
                } else {
                    blockState = Blocks.STONE.defaultBlockState();
                }

                // We can't send the packet normally (through the packet queue).
                // We bypass the packet queue since our calculations are based on the packet state (not the server state) as seen by the packet listener.
                // As described above, the packet queue could for example already contain a chunk unload packet.
                // Thus we send our packet immediately before that.
                sendPacketImmediately(p, new ClientboundBlockUpdatePacket(block, blockState));


            }
        });
    }

    private static boolean sendPacketImmediately(Player player, Object packet) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;

        if (connection == null || connection.processedDisconnect) {
            return false;
        }

        Channel channel = connection.connection.channel;

        if (channel == null || !channel.isOpen()) {
            return false;
        }

        channel.writeAndFlush(packet);
        return true;
    }
}
