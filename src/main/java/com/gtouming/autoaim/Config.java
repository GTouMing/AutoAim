package com.gtouming.autoaim;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = Autoaim.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER.comment("Whether to log the dirt block on common setup").define("logDirtBlock", true);

    private static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER.comment("A magic number").defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER.comment("What you want the introduction message to be for the magic number").define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER.comment("A list of items to log on common setup.").defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    // AutoAim 配置
    private static final ModConfigSpec.ConfigValue<List<? extends String>> TARGET_ENTITIES = BUILDER
            .comment("目标实体列表，支持格式: modid:entity_type")
            .defineListAllowEmpty("autoaim.targetEntities", 
                    List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:creeper"), 
                    Config::validateEntityName);

    private static final ModConfigSpec.DoubleValue SEARCH_DISTANCE = BUILDER
            .comment("自动瞄准的最大搜索距离（方块）")
            .defineInRange("autoaim.searchDistance", 6.0, 1.0, 20.0);

    private static final ModConfigSpec.BooleanValue ENABLE_AUTOAIM = BUILDER
            .comment("是否启用自动瞄准功能")
            .define("autoaim.enabled", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    // AutoAim 配置字段
    public static Set<EntityType<?>> targetEntities;
    public static double searchDistance;
    public static boolean enableAutoAim;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }

    private static boolean validateEntityName(final Object obj) {
        return obj instanceof String entityName && BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(entityName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream().map(itemName -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemName))).collect(Collectors.toSet());

        // 加载自动瞄准配置
        targetEntities = TARGET_ENTITIES.get().stream()
                .map(entityName -> BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(entityName)))
                .collect(Collectors.toSet());
        searchDistance = SEARCH_DISTANCE.get();
        enableAutoAim = ENABLE_AUTOAIM.get();
    }
}
