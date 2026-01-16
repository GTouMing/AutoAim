package com.gtouming.autoaim;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = Autoaim.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    // AutoAim 配置
    private static final ModConfigSpec.ConfigValue<List<? extends String>> TARGET_ENTITIES = BUILDER
            .comment("目标实体列表，支持格式: modid:entity_type")
            .define("autoaim.targetEntities",
                    List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:creeper"), 
                    Config::validateEntityName);

    private static final ModConfigSpec.BooleanValue ENABLE_AUTOAIM = BUILDER
            .comment("是否启用自动瞄准功能")
            .define("autoaim.enabled", true);

    static final ModConfigSpec SPEC = BUILDER.build();


    // AutoAim 配置字段
    public static Set<EntityType<?>> targetEntities;
    public static boolean enableAutoAim;

    private static boolean validateEntityName(final Object obj) {
        return obj instanceof String entityName && BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(entityName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // 加载自动瞄准配置
        targetEntities = TARGET_ENTITIES.get().stream()
                .map(entityName -> BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(entityName)))
                .collect(Collectors.toSet());
        enableAutoAim = ENABLE_AUTOAIM.get();
    }
}
