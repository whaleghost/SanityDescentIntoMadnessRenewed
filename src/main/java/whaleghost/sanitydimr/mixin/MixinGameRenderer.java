package whaleghost.sanitydimr.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import whaleghost.sanitydimr.SanityMod;
import whaleghost.sanitydimr.client.GuiHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("TAIL"))
    private void render(DeltaTracker pPartialTick, boolean pRenderLevel, CallbackInfo ci) {
        GameRenderer renderer = (GameRenderer)(Object)this;
        SanityMod inst = SanityMod.getInstance();
        if (renderer != null && inst != null && pRenderLevel && renderer.getMinecraft().level != null) {
            GuiHandler gui = inst.getGui();
            gui.initPostProcessor();
            gui.renderPostProcess(pPartialTick.getGameTimeDeltaTicks());
        }
    }

    @Inject(method = "resize(II)V", at = @At("TAIL"))
    private void resize(int pWidth, int pHeight, CallbackInfo ci) {
        if (SanityMod.getInstance() != null && SanityMod.getInstance().getGui() != null) {
            SanityMod.getInstance().getGui().resize(pWidth, pHeight);
        }
    }

}