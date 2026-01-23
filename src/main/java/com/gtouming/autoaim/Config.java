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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EventBusSubscriber(modid = Autoaim.MOD_ID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLE_AUTOAIM = BUILDER
            .comment("是否开启自瞄")
            .comment("enable auto aim")
            .define("autoaim.enabled", true);

    private static final ModConfigSpec.BooleanValue CHECK_HELD_ITEM = BUILDER
            .comment("是否检查手中物品以决定是否尝试自瞄")
            .comment("check held item to decide whether to enable auto aim")
            .define("autoaim.checkHeldItem", true);
    
    private static final ModConfigSpec.ConfigValue<List<? extends String>> TARGET_ENTITIES = BUILDER
            .comment("目标实体列表，支持格式: modid:entity_type, modid:entity_type@variant (实体变种（可通过数据标签查看），如minecraft:zombie@IsBaby), modid:* (通配符), #tag (实体标签)")
            .comment("target entities list, support format: modid:entity_type, modid:entity_type@variant (entity variant(from data tag), such as minecraft:zombie@IsBaby), modid:* (wildcard), #tag (entity tag)")
            .define("autoaim.targetEntities",
                    List.of("minecraft:zombie@IsBaby", "minecraft:vex", "minecraft:phantom", "minecraft:silverfish", "minecraft:endermite"),
                    Config::verifyList);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> ENABLED_ITEMS = BUILDER
            .comment("启用自瞄的物品列表，支持格式: modid:item, modid:* (通配符), #tag (物品标签)")
            .comment("enabled items list, support format: modid:item, modid:* (wildcard), #tag (item tag)")
            .define("autoaim.enabledItems",
                    List.of("#swords"),
                    Config::verifyList);

    static final ModConfigSpec SPEC = BUILDER.build();

    static List<String> rawTargetEntities;
    static List<String> rawEnabledItems;

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
    static List<EntityType<?>> resolveEntityPattern(String pattern) {
        // 处理实体标签
        if (pattern.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.parse(pattern.substring(1));
            TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, tagId);
            return BuiltInRegistries.ENTITY_TYPE.getTag(tagKey)
                    .map(tag -> {
                        List<EntityType<?>> result = new ArrayList<>();
                        tag.forEach(holder -> result.add(holder.value()));
                        return result;
                    })
                    .orElse(new ArrayList<>());
        }

        String[] parts = pattern.split("@");
        String[] parts2 = parts[0].split(":");
        if (parts2[1].equals("*")) {
            // 通配符匹配：匹配指定模组的所有实体
            return BuiltInRegistries.ENTITY_TYPE.stream()
                    .filter(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(parts2[0]))
                    .collect(Collectors.toList());
        } else {
            // 精确匹配
            ResourceLocation entityId = ResourceLocation.parse(parts[0]);
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
            //不存在则返回猪（神秘）
            return List.of(type);
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
    static List<Item> resolveItemPattern(String pattern) {
        if (pattern.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.parse(pattern.substring(1));
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
            return BuiltInRegistries.ITEM.getTag(tagKey)
                    .map(tag -> tag.stream().map(Holder::value).collect(Collectors.toList()))
                    .orElse(Collections.emptyList());
        }

        String[] parts = pattern.split(":");
        if (parts[1].equals("*")) {
            // 通配符匹配：匹配指定模组的所有物品
            return BuiltInRegistries.ITEM.stream()
                    .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(parts[0]))
                    .collect(Collectors.toList());
        } else {
            ResourceLocation itemId = ResourceLocation.parse(pattern);
            // 精确匹配
            Item item = BuiltInRegistries.ITEM.get(itemId);
            //不存在则返回空气
            return List.of(item);
        }
    }

    private static boolean verifyList(Object object) {
        if (!(object instanceof List<?> configs)) return false;
        for (Object configObj : configs) {
            if (!(configObj instanceof String)) return false;
        }
        return true;
    }
    
    static void initList() {
        entityVariants.clear();
        targetEntities = TARGET_ENTITIES.get().stream()
                .flatMap(entityPattern -> {
                    // 检查是否是实体变种模式
                    if (entityPattern.contains("@")) {
                        entityVariants.add(entityPattern);
                        return Stream.empty();
                    }
                    return resolveEntityPattern(entityPattern).stream();
                })
                .collect(Collectors.toSet());

        enabledItems = ENABLED_ITEMS.get().stream()
                .flatMap(itemPattern -> resolveItemPattern(itemPattern).stream())
                .collect(Collectors.toSet());
        rawTargetEntities = TARGET_ENTITIES.get().stream()
                .collect(Collectors.toList());
        rawEnabledItems = ENABLED_ITEMS.get().stream()
                .collect(Collectors.toList());
    }

    static void saveConfig() {
        // 卸载配置时，将列表保存到配置文件
        ENABLE_AUTOAIM.set(enableAutoAim);
        CHECK_HELD_ITEM.set(checkHeldItem);

        TARGET_ENTITIES.set(rawTargetEntities);
        ENABLED_ITEMS.set(rawEnabledItems);
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        enableAutoAim = ENABLE_AUTOAIM.get();
        checkHeldItem = CHECK_HELD_ITEM.get();
        // 初始化列表（此时标签未注册）
        initList();
    }

    @SubscribeEvent
    static void onLevelLoad(LevelEvent.Load event) {
        //再次初始化（此时标签已注册）
        initList();
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        saveConfig();
        initList();
    }

    @SubscribeEvent
    static void onUnload(final ModConfigEvent.Unloading event) {
        saveConfig();
    }
}
