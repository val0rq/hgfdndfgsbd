package com.lightcraft.mixin;
import com.lightcraft.client.LightCraftClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "close", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        if (LightCraftClient.getInstance() != null) {
            LightCraftClient.getInstance().getConfigManager()
                .saveConfigImmediate(LightCraftClient.getInstance().getConfig());
        }
    }
}
