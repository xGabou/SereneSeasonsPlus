package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.event.SeasonChangeEvent;
import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.mixin.MinecraftServerMixin;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.FabricAsyncExecutorHandler;
import com.Gabou.sereneseasonsplus.util.FabricEnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.RealTimeSeasonHelper;
import com.Gabou.sereneseasonsplus.util.SereneService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

public class SereneSeasonsPlusFabric extends SereneSeasonPlusCommon implements ModInitializer {


    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Serene Seasons Plus (Fabric)");
        // Server lifecycle hooks
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        EnvironmentHelper.init(new FabricEnvironmentHelper());
        SeasonChangeEvent.register();
        // Register chunk load to cache surface height only (no enqueue)
        ServerChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);
        SereneExtendedConfig.registerReloadListener(this::onConfigReload);



        // Server tick hook
        ServerTickEvents.START_WORLD_TICK.register(this::onWorldTick);

        // If you have client-only stuff, register it in SereneSeasonsPlusClientFabric
    }

    private void onBlockBreak(Level lvl, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (!(lvl instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;

        if (!state.is(Blocks.SNOW) && !state.is(Blocks.SNOW_BLOCK)) return;

        com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData sd = com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData.get();
        int activeId = (sd != null) ? sd.currentStormId : 0;
        if (activeId <= 0) return; // only track during active storm

        LevelChunk chunk = level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, false);
        if (!(chunk instanceof com.Gabou.sereneseasonsplus.access.ISnowTrackedChunk tracked)) return;

        if (tracked.sereneseasonsplus$getDestroyedStormId() != activeId) {
            tracked.sereneseasonsplus$getDestroyedColumns().clear();
            tracked.sereneseasonsplus$setDestroyedStormId(activeId);
        }
        long xz = (((long) pos.getX()) << 32) ^ (pos.getZ() & 0xffffffffL);
        tracked.sereneseasonsplus$getDestroyedColumns().add(xz);

        // Remove any tracked snow column entries for this X/Z so sync won't try to re-add this storm
        tracked.sereneseasonsplus$getSnowColumns().keySet().removeIf(p -> p.getX() == pos.getX() && p.getZ() == pos.getZ());
        chunk.setUnsaved(true);
    }

    private void onConfigReload() {
        CommonSnowBlockFeature.onConfigReload(SereneExtendedConfig.TICK_SNOW_REPLACER.get(), SereneExtendedConfig.SNOWSTORM_ENABLED.get(), SereneExtendedConfig.MAX_SNOW_ACCUMULATION_LAYERS.get());
        SereneService.reloadConfig();
    }

    //    private void onChunkLoad(ServerLevel level, LevelChunk chunk) {
//        CommonSnowBlockReplacer.handleOnChunkLoad(chunk);
//    }
    private void onServerStarting(MinecraftServer server) {
        LOGGER.info("Serene Seasons Plus server starting!");
        SereneService.HANDLER = new FabricAsyncExecutorHandler();
        CommonSnowBlockFeature.onServerStarting(SereneExtendedConfig.TICK_SNOW_REPLACER.get(), SereneExtendedConfig.SNOWSTORM_ENABLED.get(), SereneExtendedConfig.MAX_SNOW_ACCUMULATION_LAYERS.get());
    }

    private void onServerStopping(MinecraftServer server) {
        SereneService.shutdown();
        SereneService.HANDLER = null;
        EnvironmentHelper.onServerStopping(server.getLevel(Level.OVERWORLD));
    }

    private void onWorldTick(ServerLevel level) {
        if( level.dimension() != Level.OVERWORLD) return;
        this.onTick(level, SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.get(), SereneExtendedConfig.ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT.get(), SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.get(), SereneExtendedConfig.CUSTOM_DAY_LENGTH.get(), SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.get());
        CommonSnowBlockFeature.handleServerTick(level.getServer(), level);

    }

    private void onServerStarted(MinecraftServer server) {
        EnvironmentHelper.onServerStarted(server.getLevel(Level.OVERWORLD));
        RealTimeSeasonHelper.sync(server.getLevel(Level.OVERWORLD), SereneExtendedConfig.REAL_TIME_CANADIAN_SEASONS.get());
        // Apply configured maximum snow accumulation (in layers)
        server.getGameRules()
                .getRule(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT)
                .set(SereneExtendedConfig.MAX_SNOW_ACCUMULATION_LAYERS.get(), server);
    }

    private void onChunkLoad(ServerLevel level, ChunkAccess chunkAccess) {
        if (level == null) return;
        if (!(chunkAccess instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) return;
        if (level.isClientSide()) return;
        if (level.dimension() != Level.OVERWORLD) return;
        // Cache surface height only; no enqueue to avoid dual input
        CommonSnowBlockFeature.handleOnChunkLoad(chunk);
    }

}
