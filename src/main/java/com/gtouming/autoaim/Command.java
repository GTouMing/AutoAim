package com.gtouming.autoaim;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.gtouming.autoaim.Config.*;

public class Command {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 一级指令：item
        LiteralArgumentBuilder<CommandSourceStack> itemCommand = Commands.literal("item")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("enable")
                    .executes(context -> {
                        checkHeldItem = true;
                        return 1;
                    }))
            .then(Commands.literal("disable")
                    .executes(context -> {
                        checkHeldItem = false;
                        return 1;
                    }))
            .then(Commands.literal("add")
                .then(Commands.argument("item", CustomArgumentType.item())
                    .suggests(Command::suggestAddItem)
                .executes(Command::addItem)))
            .then(Commands.literal("remove")
                .then(Commands.argument("item", CustomArgumentType.item())
                    .suggests(Command::suggestRemoveItem)
                .executes(Command::removeItem)))
            .then(Commands.literal("list")
                .executes(Command::listItems));



        // 一级指令：entity
        LiteralArgumentBuilder<CommandSourceStack> entityCommand = Commands.literal("entity")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("enable")
                .executes(context -> {
                    enableAutoAim = true;
                    return 1;
                }))
            .then(Commands.literal("disable")
                .executes(context -> {
                    enableAutoAim = false;
                    return 1;
                }))
            .then(Commands.literal("add")
                .then(Commands.argument("targets", CustomArgumentType.entity())
                        .suggests(Command::suggestAddEntity)
                        .executes(Command::addEntity)))
            .then(Commands.literal("remove")
                .then(Commands.argument("targets", CustomArgumentType.entity())
                        .suggests(Command::suggestRemoveEntity)
                .executes(Command::removeEntity)))
            .then(Commands.literal("list")
                .executes(Command::listEntities));

        // 注册主指令
        dispatcher.register(
            Commands.literal("autoaim")
                .executes(context -> {
                    Config.saveConfig();
                    Config.initList();
                    return 1;
                })
                .then(itemCommand)
                .then(entityCommand)
        );
    }

    // ========== 物品相关方法 ==========

    private static int addItem(CommandContext<CommandSourceStack> context) {
        List<Item> items = resolveItemPattern(getParam(context, "item"));
        if (items.isEmpty()) return 0;
        String param = getParam(context, "item");
         if (!items.getFirst().toString().equals(param)) return 0;
        rawEnabledItems.add(param);
        reLoad();
        if (rawEnabledItems.contains(param)) context.getSource().sendSuccess(() -> Component.translatable("autoaim.command.item.added", getParam(context, "item")), false);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestAddItem(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        String prefix = remaining.startsWith("#") ? "#" : "";

        // 根据类型提供不同的标签建议
        List<String> suggestions;
        if (prefix.equals("#")) {
            suggestions = BuiltInRegistries.ITEM.getTagNames()
                    .map(key -> prefix + key.location())
                    .collect(Collectors.toList());
        }
        else {
            suggestions = BuiltInRegistries.ITEM.stream()
                    .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                    .collect(Collectors.toList());
        }

        String searchTerm = remaining.toLowerCase();
        if (!searchTerm.isEmpty()) {
            suggestions = suggestions.stream()
                    .filter(s -> s.toLowerCase().contains(searchTerm))
                    .collect(Collectors.toList());
        }
        for (String suggestion : suggestions) {
            builder.suggest(suggestion);
        }

        return builder.buildFuture();
    }

    private static int removeItem(CommandContext<CommandSourceStack> context) {
        String param = getParam(context, "item");
        if (!rawEnabledItems.contains(param)) return 0;
        else {
            rawEnabledItems.remove(param);
            reLoad();
            if (!rawEnabledItems.contains(param)) context.getSource().sendSuccess(() -> Component.translatable("autoaim.command.item.removed", getParam(context, "item")), false);
        }
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestRemoveItem(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(rawEnabledItems, builder);
    }

    private static int listItems(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.translatable("autoaim.command.item.status", 
            Component.translatable(checkHeldItem ? "autoaim.command.item.enabled" : "autoaim.command.item.disabled")), false);
        source.sendSuccess(() -> Component.translatable("autoaim.command.item.title"), false);

        source.sendSuccess(() -> Component.translatable("autoaim.command.item.types"), false);
        for (Item item : Config.enabledItems) {
            source.sendSuccess(() -> Component.literal("  - " + BuiltInRegistries.ITEM.getKey(item)), false);
        }
        
        return 1;
    }

    // ========== 实体相关方法 ==========

    private static int addEntity(CommandContext<CommandSourceStack> context) {
        List<EntityType<?>> entities = resolveEntityPattern(getParam(context, "targets"));
        if (entities.isEmpty()) return 0;
        String param = getParam(context, "targets");
        if (!entities.getFirst().toShortString().equals(getShortParam(param))) return 0;
        rawTargetEntities.add(param);
        reLoad();
        if (rawTargetEntities.contains(param)) context.getSource().sendSuccess(() -> Component.translatable("autoaim.command.entity.added", param), false);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestAddEntity(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        String prefix = remaining.startsWith("#") ? "#" : "";

        // 根据类型提供不同的标签建议
        List<String> suggestions;
        if (prefix.equals("#")) {
            suggestions = BuiltInRegistries.ENTITY_TYPE.getTagNames()
                    .map(key -> prefix + key.location())
                    .collect(Collectors.toList());
        }
        else {
            suggestions = BuiltInRegistries.ENTITY_TYPE.stream()
                    .map(entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity).toString())
                    .collect(Collectors.toList());
        }

        String searchTerm = remaining.toLowerCase();
        if (!searchTerm.isEmpty()) {
            suggestions = suggestions.stream()
                    .filter(s -> s.toLowerCase().contains(searchTerm))
                    .collect(Collectors.toList());
        }
        for (String suggestion : suggestions) {
            builder.suggest(suggestion);
        }

        return builder.buildFuture();
    }

    private static int removeEntity(CommandContext<CommandSourceStack> context) {
        String param = getParam(context, "targets");
        if (!rawTargetEntities.contains(param)) return 0;
        else {
            rawTargetEntities.remove(param);
            reLoad();
            if (!rawTargetEntities.contains(param)) context.getSource().sendSuccess(() -> Component.translatable("autoaim.command.entity.removed", param), false);
        }
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestRemoveEntity(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(rawTargetEntities, builder);
    }

    private static int listEntities(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.translatable("autoaim.command.entity.status", 
            Component.translatable(enableAutoAim ? "autoaim.command.entity.enabled" : "autoaim.command.entity.disabled")), false);
        source.sendSuccess(() -> Component.translatable("autoaim.command.entity.title"), false);

        source.sendSuccess(() -> Component.translatable("autoaim.command.entity.types"), false);
        for (EntityType<?> type : Config.targetEntities) {
            source.sendSuccess(() -> Component.literal("  - " + BuiltInRegistries.ENTITY_TYPE.getKey(type)), false);
        }
        
        source.sendSuccess(() -> Component.translatable("autoaim.command.entity.variants"), false);
        for (String variant : Config.entityVariants) {
            source.sendSuccess(() -> Component.literal("  - " + variant), false);
        }
        
        return 1;
    }
    
    private static void reLoad() {
        Config.saveConfig();
        Config.initList();
    }
    
    private static String getParam(CommandContext<CommandSourceStack> context, String param) {
        return CustomArgumentType.getString(context, param);
    }

    private static String getShortParam(String longParam) {
        String[] parts = longParam.split(":");
        return parts[1];
    }
}