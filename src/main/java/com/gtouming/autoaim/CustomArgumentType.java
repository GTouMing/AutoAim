package com.gtouming.autoaim;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 自定义参数类型，支持标签和通配符匹配
 */
public record CustomArgumentType(Type type) implements ArgumentType<String> {

    private static final DynamicCommandExceptionType INVALID_PATTERN = new DynamicCommandExceptionType(
            param -> Component.literal("无效的参数: " + param)
    );

    public enum Type {
        ITEM,      // 物品类型
        ENTITY     // 实体类型
    }

    /**
     * 创建物品参数类型
     */
    public static CustomArgumentType item() {
        return new CustomArgumentType(Type.ITEM);
    }

    /**
     * 创建实体参数类型
     */
    public static CustomArgumentType entity() {
        return new CustomArgumentType(Type.ENTITY);
    }
    public static String getString(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        StringBuilder input = new StringBuilder();

        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            input.append(reader.read());
        }
        String result = input.toString();
        // 验证输入模式是否有效
        if (!isValidPattern(result)) {
            throw INVALID_PATTERN.create(result);
        }
        return result;
    }

    /**
     * 验证输入模式是否有效
     * 支持以下格式：
     * - 普通ID: minecraft:diamond
     * - 通配符: minecraft:*
     * - 标签: #minecraft:tools
     * - 通配符标签: #minecraft:*
     */
    private boolean isValidPattern(String pattern) {
        // 检查是否是标签格式 (#namespace:value 或 #namespace:*)
        if (pattern.startsWith("#")) {
            String tagPattern = pattern.substring(1);
            // 验证标签格式: namespace:value 或 namespace:*
            return Pattern.matches("^[a-z0-9_.\\-]+:(\\*|[a-z0-9_.\\-/]+)$", tagPattern);
        }

        // 检查是否是普通ID或通配符格式 (namespace:value 或 namespace:*)
        return Pattern.matches("^[a-z0-9_.\\-]+:(\\*|[a-z0-9_.\\-/]+)(@[a-zA-Z0-9_]+)?$", pattern);
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("minecraft:diamond", "minecraft:*", "#minecraft:tools", "#minecraft:*");
    }
}
