package com.gtouming.autoaim;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EventBusSubscriber(modid = Autoaim.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    // AutoAim 配置

    private static final ModConfigSpec.BooleanValue ENABLE_AUTOAIM = BUILDER
            .comment("是否开启自瞄")
            .define("autoaim.enabled", true);

    private static final ModConfigSpec.BooleanValue CHECK_HELD_ITEM = BUILDER
            .comment("是否检查手中物品以决定是否尝试自瞄")
            .define("autoaim.checkHeldItem", true);
    
    private static final ModConfigSpec.ConfigValue<List<? extends String>> TARGET_ENTITIES = BUILDER
            .comment("目标实体列表，支持格式: modid:entity_type, modid:entity_type@variant (实体变种，如minecraft:zombie@isBaby), modid:* (通配符), #tag (实体标签)")
            .define("autoaim.targetEntities",
                    List.of("minecraft:zombie@isBaby", "minecraft:vex", "minecraft:phantom", "minecraft:silverfish", "minecraft:endermite"),
                    Config::verifyList);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> ENABLED_ITEMS = BUILDER
            .comment("启用自瞄的物品列表，支持格式: modid:item, modid:* (通配符), #tag (物品标签)")
            .define("autoaim.enabledItems",
                    List.of("#swords"),
                    Config::verifyList);

    static final ModConfigSpec SPEC = BUILDER.build();


    // AutoAim 配置字段
    public static boolean enableAutoAim;
    public static boolean checkHeldItem;
    public static Set<EntityType<?>> targetEntities = new HashSet<>();
    public static Set<String> entityVariants = new HashSet<>();
    public static Set<Item> enabledItems = new HashSet<>();

    /**
     * 解析实体模式字符串，返回匹配的实体类型集合
     * 支持以下格式：
     * - modid:entity_type: 精确匹配实体类型
     * - modid:entity_type@variant: 精确匹配实体类型的变种
     * - modid:*: 匹配指定模组的所有实体
     * - #tag: 匹配指定标签的所有实体
     * 
     * @param pattern 实体模式字符串
     * @return 匹配的实体类型集合
     */
    private static Set<EntityType<?>> resolveEntityPattern(String pattern) {
        // 处理实体标签
        if (pattern.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.parse(pattern.substring(1));
            TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, tagId);
            return BuiltInRegistries.ENTITY_TYPE.getTag(tagKey)
                    .map(tag -> {
                        Set<EntityType<?>> result = new HashSet<>();
                        tag.forEach(holder -> result.add(holder.value()));
                        return result;
                    })
                    .orElse(new HashSet<>());
        }
        String[] parts = pattern.split("@");
        // 处理通配符和精确匹配
        String[] parts2 = parts[0].split(":");
        if (parts2[1].equals("*")) {
            return BuiltInRegistries.ENTITY_TYPE.stream()
                    .filter(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(parts2[0]))
                    .collect(Collectors.toSet());
        } else {
            ResourceLocation entityId = ResourceLocation.parse(parts[0]);
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
            return Set.of(type);
        }
    }

    /**
     * 解析物品模式字符串，返回匹配的物品集合
     * 支持以下格式：
     * - modid:item: 精确匹配物品
     * - modid:*: 匹配指定模组的所有物品
     * - #tag: 匹配指定标签的所有物品
     *
     * @param pattern 物品模式字符串
     * @return 匹配的物品集合
     */
    private static Set<Item> resolveItemPattern(String pattern) {
        // 处理物品标签
        if (pattern.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.parse(pattern.substring(1));
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
            return BuiltInRegistries.ITEM.getTag(tagKey)
                    .map(tag -> tag.stream().map(Holder::value).collect(Collectors.toSet()))
                    .orElse(Collections.emptySet());
        }

        // 处理通配符和精确匹配
        String[] parts = pattern.split(":");
        if (parts[1].equals("*")) {
            // 通配符匹配：匹配指定模组的所有物品
            return BuiltInRegistries.ITEM.stream()
                    .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(parts[0]))
                    .collect(Collectors.toSet());
        } else {
            ResourceLocation itemId = ResourceLocation.parse(pattern);
            // 精确匹配
            Item item = BuiltInRegistries.ITEM.get(itemId);
            return Set.of(item);
        }
    }

    private static boolean verifyList(Object object) {
        if (!(object instanceof List<?> configs)) return false;
        for (Object configObj : configs) {
            if (!(configObj instanceof String)) return false;
        }
        return true;
    }
    
    private static void initList() {
        targetEntities = TARGET_ENTITIES.get().stream()
                .flatMap(entityPattern -> {
                    // 检查是否是实体变种模式
                    if (entityPattern.contains("@")) {
                        // 这是实体变种模式，添加到变种集合中
                        entityVariants.add(entityPattern);
                        return Stream.empty();
                    }
                    return resolveEntityPattern(entityPattern).stream();
                })
                .collect(Collectors.toSet());

        enabledItems = ENABLED_ITEMS.get().stream()
                .flatMap(itemPattern -> resolveItemPattern(itemPattern).stream())
                .collect(Collectors.toSet());
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // 加载自动瞄准配置
        enableAutoAim = ENABLE_AUTOAIM.get();
        checkHeldItem = CHECK_HELD_ITEM.get();
        initList();
    }

    @SubscribeEvent
    static void onConfigReload(LevelEvent.Load event) {
        initList();
    }
}
