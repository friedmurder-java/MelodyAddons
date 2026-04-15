package com.skyblockmod.client.mixin;

import com.skyblockmod.client.feature.ChatTriggerManager;
import com.skyblockmod.client.feature.CocoonAlert;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class HudRenderMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext ctx, RenderTickCounter tickCounter, CallbackInfo ci) {
        CocoonAlert.get().render(ctx, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());
        ChatTriggerManager.get().render(ctx, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());
    }
}