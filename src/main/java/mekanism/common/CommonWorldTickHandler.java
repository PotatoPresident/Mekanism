package mekanism.common;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import mekanism.api.NBTConstants;
import mekanism.common.config.MekanismConfig;
import mekanism.common.content.qio.IQIOCraftingWindowHolder;
import mekanism.common.inventory.container.item.PortableQIODashboardContainer;
import mekanism.common.lib.frequency.FrequencyManager;
import mekanism.common.lib.radiation.RadiationManager;
import mekanism.common.util.WorldUtils;
import mekanism.common.world.GenHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class CommonWorldTickHandler {
    public CommonWorldTickHandler() {
        ServerChunkEvents.CHUNK_LOAD.register(this::onChunkDataLoad);
        ServerChunkEvents.CHUNK_UNLOAD.register(this::chunkUnloadEvent);
        ServerWorldEvents.LOAD.register(this::worldLoadEvent);
        ServerWorldEvents.UNLOAD.register(this::worldUnloadEvent);
        ServerTickEvents.END_SERVER_TICK.register(this::serverTick);
        ServerTickEvents.END_WORLD_TICK.register(this::tickEnd);
    }

    private static final long maximumDeltaTimeNanoSecs = 16_000_000; // 16 milliseconds

    //TODO: I believe this may be fine as is with just the load and save methods being synchronized
    // but there is a chance this is not the case in which case we should adjust how this is done
    private Map<ResourceLocation, Object2IntMap<ChunkPos>> chunkVersions;
    private Map<ResourceLocation, Queue<ChunkPos>> chunkRegenMap;
    public static boolean flushTagAndRecipeCaches;
    public static boolean monitoringCardboardBox;

    public void addRegenChunk(ResourceKey<Level> dimension, ChunkPos chunkCoord) {
        if (chunkRegenMap == null) {
            chunkRegenMap = new Object2ObjectArrayMap<>();
        }
        ResourceLocation dimensionName = dimension.location();
        if (!chunkRegenMap.containsKey(dimensionName)) {
            LinkedList<ChunkPos> list = new LinkedList<>();
            list.add(chunkCoord);
            chunkRegenMap.put(dimensionName, list);
        } else {
            Queue<ChunkPos> regenPositions = chunkRegenMap.get(dimensionName);
            if (!regenPositions.contains(chunkCoord)) {
                regenPositions.add(chunkCoord);
            }
        }
    }

    public void resetChunkData() {
        chunkRegenMap = null;
        chunkVersions = null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntitySpawn(EntityJoinWorldEvent event) {
        //If we are in the middle of breaking a block using a cardboard box, cancel any items
        // that are dropped, we do this at highest priority to ensure we cancel it the same tick
        // before forge replaces items with custom item entities with a tick delay
        //TODO - 1.18: Requires https://github.com/MinecraftForge/MinecraftForge/pull/8417
        if (monitoringCardboardBox && event.getEntity() instanceof ItemEntity entity) {
            entity.discard();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public synchronized void chunkSave(ChunkDataEvent.Save event) {
        LevelAccessor world = event.getWorld();
        if (!world.isClientSide() && world instanceof Level level) {
            int chunkVersion = MekanismConfig.world.userGenVersion.get();
            if (chunkVersions != null) {
                chunkVersion = chunkVersions.getOrDefault(level.dimension().location(), Object2IntMaps.emptyMap())
                        .getOrDefault(event.getChunk().getPos(), chunkVersion);
            }
            event.getData().putInt(NBTConstants.WORLD_GEN_VERSION, chunkVersion);
        }
    }

    public synchronized void onChunkDataLoad(ServerLevel world, LevelChunk chunk) {
            int version = chunk.getData().getInt(NBTConstants.WORLD_GEN_VERSION);
            //When a chunk is loaded, if it has an older version than the latest one
            if (version < MekanismConfig.world.userGenVersion.get()) {
                //Track what version it has so that when we save it, if we haven't gotten a chance to update
                // the chunk yet, then we are able to properly save that we still will need to update it
                if (chunkVersions == null) {
                    chunkVersions = new Object2ObjectArrayMap<>();
                }
                ChunkPos chunkCoord = event.getChunk().getPos();
                ResourceKey<Level> dimension = level.dimension();
                chunkVersions.computeIfAbsent(dimension.location(), dim -> new Object2IntOpenHashMap<>())
                        .put(chunkCoord, version);
                if (MekanismConfig.world.enableRegeneration.get()) {
                    //If retrogen is enabled, then we also need to mark the chunk as needing retrogen
                    addRegenChunk(dimension, chunkCoord);
                }
            }
    }

    public void chunkUnloadEvent(ServerLevel world, LevelChunk chunk) {
        if (chunkVersions != null) {
            //When a chunk unloads, free up the memory tracking what version it has
            chunkVersions.getOrDefault(level.dimension().location(), Object2IntMaps.emptyMap())
                    .removeInt(chunk.getPos());
        }
    }

    public void worldUnloadEvent(MinecraftServer server, ServerLevel world) {
        if (chunkVersions != null) {
            //When a world unloads, free up memory tracking the versions of the chunks in it
            chunkVersions.remove(world.dimension().location());
        }
    }

    public void worldLoadEvent(MinecraftServer server, ServerLevel world) {
        FrequencyManager.load();
        RadiationManager.INSTANCE.createOrLoad();
    }

    private void serverTick(MinecraftServer server) {
        FrequencyManager.tick();
        RadiationManager.INSTANCE.tickServer();
    }

    private void tickEnd(ServerLevel world) {
        if (!world.isClientSide) {
            RadiationManager.INSTANCE.tickServerWorld(world);
            if (flushTagAndRecipeCaches) {
                //Loop all open containers and if it is a portable qio dashboard force refresh the window's recipes
                for (ServerPlayer player : world.players()) {
                    if (player.containerMenu instanceof PortableQIODashboardContainer qioDashboard) {
                        for (byte index = 0; index < IQIOCraftingWindowHolder.MAX_CRAFTING_WINDOWS; index++) {
                            qioDashboard.getCraftingWindow(index).invalidateRecipe();
                        }
                    }
                }
                flushTagAndRecipeCaches = false;
            }

            if (chunkRegenMap == null || !MekanismConfig.world.enableRegeneration.get()) {
                return;
            }
            ResourceLocation dimensionName = world.dimension().location();
            //Credit to E. Beef
            if (chunkRegenMap.containsKey(dimensionName)) {
                Queue<ChunkPos> chunksToGen = chunkRegenMap.get(dimensionName);
                Object2IntMap<ChunkPos> dimensionChunkVersions = chunkVersions.getOrDefault(dimensionName, Object2IntMaps.emptyMap());
                long startTime = System.nanoTime();
                while (System.nanoTime() - startTime < maximumDeltaTimeNanoSecs && !chunksToGen.isEmpty()) {
                    ChunkPos nextChunk = chunksToGen.poll();
                    if (nextChunk == null) {
                        break;
                    }
                    //Ensure the chunk actually exists and is still loaded before trying to retrogen it
                    if (WorldUtils.isChunkLoaded(world, nextChunk)) {
                        if (GenHandler.generate(world, nextChunk)) {
                            Mekanism.logger.info("Regenerating ores and salt at chunk {}", nextChunk);
                        }
                        //Regardless of whether we were able to generate anything in the chunk, now that we have
                        // handled it, update the chunk version. We do this by removing tracking the chunk's
                        // version so that we can just default it to the latest version when saved and free up the
                        // memory as early as possible
                        if (chunkVersions != null) {
                            dimensionChunkVersions.removeInt(nextChunk);
                        }
                    }
                }
                if (chunksToGen.isEmpty()) {
                    chunkRegenMap.remove(dimensionName);
                }
            }
        }
    }
}