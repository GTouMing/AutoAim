package com.gtouming.autoaim;

import com.google.gson.JsonObject;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

public class CustomArgumentInfo implements ArgumentTypeInfo<CustomArgumentType, CustomArgumentInfo.Template> {

    @Override
    public void serializeToNetwork(@NotNull Template template, @NotNull FriendlyByteBuf buffer) {
        // 如果需要向客户端发送额外数据，可以在这里实现
        buffer.writeEnum(template.type);
    }

    @Override
    public @NotNull Template deserializeFromNetwork(@NotNull FriendlyByteBuf buffer) {
        return new Template(buffer.readEnum(CustomArgumentType.Type.class));
    }

    @Override
    public void serializeToJson(@NotNull Template template, @NotNull JsonObject json) {
        // 可选：序列化到命令定义JSON
    }

    @Override
    public @NotNull Template unpack(@NotNull CustomArgumentType argumentType) {
        return new Template(argumentType.type());
    }

    public final class Template implements ArgumentTypeInfo.Template<CustomArgumentType> {
        private final CustomArgumentType.Type type;

        public Template(CustomArgumentType.Type type) {
            this.type = type;
        }
        @Override
        public @NotNull CustomArgumentType instantiate(@NotNull CommandBuildContext context) {
            if (type == CustomArgumentType.Type.ITEM) {
                return CustomArgumentType.item();
            }
            return CustomArgumentType.entity();
        }

        @Override
        public @NotNull ArgumentTypeInfo<CustomArgumentType, ?> type() {
            return CustomArgumentInfo.this;
        }
    }

    public static void register(IEventBus eventBus) {
        DeferredRegister<ArgumentTypeInfo<?, ?>> register = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, Autoaim.MOD_ID);
                register.register("custom", () -> ArgumentTypeInfos.registerByClass(CustomArgumentType.class, new CustomArgumentInfo()));
        register.register(eventBus);
    }
}