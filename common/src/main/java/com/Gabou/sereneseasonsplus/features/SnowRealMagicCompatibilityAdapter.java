package com.Gabou.sereneseasonsplus.features;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class SnowRealMagicCompatibilityAdapter {
    private boolean initialized;
    private boolean available;

    private Class<?> snowVariantClass;
    private Method hooksCanContainStateMethod;
    private Method hooksConvertMethod;
    private Method snowVariantLayersMethod;
    private Method snowVariantMaxLayersMethod;
    private Method snowVariantDecreaseLayerMethod;
    private Method snowVariantGetRawMethod;
    private Field placeSnowOnBlockNaturallyField;

    boolean isManagedSnow(BlockState state) {
        return isAvailable() && snowVariantClass.isInstance(state.getBlock());
    }

    boolean canManagePlacementState(BlockState state) {
        return isManagedSnow(state) || canContainState(state);
    }

    boolean canPlaceManagedSnow(ServerLevel level, BlockPos pos, BlockState state) {
        if (!isAvailable()) {
            return false;
        }
        if (!canManagePlacementState(state)) {
            return false;
        }
        return CommonSnowBlockFeature.canReceiveSnowAt(level, pos);
    }

    int getManagedLayers(ServerLevel level, BlockPos pos, BlockState state) {
        if (!isManagedSnow(state)) {
            return 0;
        }
        try {
            Object result = snowVariantLayersMethod.invoke(state.getBlock(), state, level, pos);
            return result instanceof Integer layers ? Math.max(0, layers) : 0;
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    int getMaxManagedLayers(ServerLevel level, BlockPos pos, BlockState state) {
        if (!isManagedSnow(state)) {
            return 8;
        }
        try {
            Object result = snowVariantMaxLayersMethod.invoke(state.getBlock(), state, level, pos);
            return result instanceof Integer maxLayers ? Math.max(1, maxLayers) : 8;
        } catch (ReflectiveOperationException e) {
            return 8;
        }
    }

    @Nullable
    SnowWorldMutation createLayerMutation(ServerLevel level,
                                          BlockPos pos,
                                          BlockState state,
                                          int targetLayers,
                                          boolean allowPlace) {
        if (!isAvailable()) {
            return null;
        }
        if (!canManagePlacementState(state)) {
            return null;
        }
        int maxLayers = isManagedSnow(state) ? getMaxManagedLayers(level, pos, state) : 8;
        int clampedLayers = Mth.clamp(targetLayers, 1, Math.max(1, maxLayers));
        return new SnowRealMagicLayerMutation(pos.immutable(), clampedLayers, allowPlace, this);
    }

    @Nullable
    SnowWorldMutation createClearMutation(BlockPos pos, BlockState state, boolean toWater) {
        if (!isManagedSnow(state) || toWater) {
            return null;
        }
        return new SnowRealMagicClearMutation(pos.immutable(), this);
    }

    private boolean canContainState(BlockState state) {
        if (!isAvailable()) {
            return false;
        }
        try {
            Object result = hooksCanContainStateMethod.invoke(null, state);
            return result instanceof Boolean canContain && canContain;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private boolean convert(ServerLevel level, BlockPos pos, BlockState state, int targetLayers, int flags) {
        try {
            Object result = hooksConvertMethod.invoke(
                    null,
                    level,
                    pos,
                    state,
                    targetLayers,
                    flags,
                    shouldPlaceSnowOnBlockNaturally()
            );
            return result instanceof Boolean converted && converted;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private boolean shouldPlaceSnowOnBlockNaturally() {
        if (!isAvailable()) {
            return false;
        }
        try {
            return placeSnowOnBlockNaturallyField.getBoolean(null);
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    private BlockState getRawState(BlockState state, ServerLevel level, BlockPos pos) {
        try {
            Object raw = snowVariantGetRawMethod.invoke(state.getBlock(), state, level, pos);
            if (raw instanceof BlockState blockState) {
                return blockState;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    private BlockState decreaseLayer(BlockState state, ServerLevel level, BlockPos pos) {
        try {
            Object result = snowVariantDecreaseLayerMethod.invoke(state.getBlock(), state, level, pos, false);
            if (result instanceof BlockState blockState) {
                return blockState;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return getRawState(state, level, pos);
    }

    private boolean isAvailable() {
        initializeIfNeeded();
        return available;
    }

    private void initializeIfNeeded() {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            ClassLoader loader = SnowRealMagicCompatibilityAdapter.class.getClassLoader();
            Class<?> hooksClass = Class.forName("snownee.snow.Hooks", false, loader);
            Class<?> configClass = Class.forName("snownee.snow.SnowCommonConfig", false, loader);
            snowVariantClass = Class.forName("snownee.snow.block.SnowVariant", false, loader);

            hooksCanContainStateMethod = hooksClass.getMethod("canContainState", BlockState.class);
            hooksConvertMethod = hooksClass.getMethod(
                    "convert",
                    net.minecraft.world.level.LevelAccessor.class,
                    BlockPos.class,
                    BlockState.class,
                    int.class,
                    int.class,
                    boolean.class
            );
            snowVariantLayersMethod = snowVariantClass.getMethod(
                    "layers",
                    BlockState.class,
                    net.minecraft.world.level.BlockGetter.class,
                    BlockPos.class
            );
            snowVariantMaxLayersMethod = snowVariantClass.getMethod(
                    "maxLayers",
                    BlockState.class,
                    net.minecraft.world.level.Level.class,
                    BlockPos.class
            );
            snowVariantDecreaseLayerMethod = snowVariantClass.getMethod(
                    "decreaseLayer",
                    BlockState.class,
                    net.minecraft.world.level.Level.class,
                    BlockPos.class,
                    boolean.class
            );
            snowVariantGetRawMethod = snowVariantClass.getMethod(
                    "getRaw",
                    BlockState.class,
                    net.minecraft.world.level.BlockGetter.class,
                    BlockPos.class
            );
            placeSnowOnBlockNaturallyField = configClass.getField("placeSnowOnBlockNaturally");
            available = true;
        } catch (ReflectiveOperationException ignored) {
            available = false;
        }
    }

    private record SnowRealMagicLayerMutation(BlockPos key,
                                              int targetLayers,
                                              boolean allowPlace,
                                              SnowRealMagicCompatibilityAdapter adapter) implements SnowWorldMutation {
        @Override
        public boolean apply(ServerLevel level) {
            BlockState current = level.getBlockState(key);
            if (allowPlace && !CommonSnowBlockFeature.canReceiveSnowAt(level, key)) {
                return false;
            }
            if (!(adapter.isManagedSnow(current) || adapter.canContainState(current))) {
                return false;
            }
            int currentLayers = adapter.getManagedLayers(level, key, current);
            int maxLayers = adapter.isManagedSnow(current)
                    ? adapter.getMaxManagedLayers(level, key, current)
                    : 8;
            int clampedTarget = Mth.clamp(targetLayers, 1, Math.max(1, maxLayers));
            if (currentLayers == clampedTarget && adapter.isManagedSnow(current)) {
                return false;
            }
            return adapter.convert(level, key, current, clampedTarget, Block.UPDATE_CLIENTS);
        }
    }

    private record SnowRealMagicClearMutation(BlockPos key,
                                              SnowRealMagicCompatibilityAdapter adapter) implements SnowWorldMutation {
        @Override
        public boolean apply(ServerLevel level) {
            BlockState current = level.getBlockState(key);
            if (!adapter.isManagedSnow(current)) {
                return false;
            }
            int layers = adapter.getManagedLayers(level, key, current);
            BlockState next = layers > 1
                    ? adapter.decreaseLayer(current, level, key)
                    : adapter.getRawState(current, level, key);
            return level.setBlock(key, next, Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
        }
    }
}
