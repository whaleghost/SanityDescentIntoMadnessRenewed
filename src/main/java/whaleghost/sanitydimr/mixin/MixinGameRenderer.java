package whaleghost.sanitydimr.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import whaleghost.sanitydimr.SanityMod;
import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Inject(method = "resize(II)V", at = @At("TAIL"))
    private void resize(int pWidth, int pHeight, CallbackInfo ci) {
        if (SanityMod.getInstance() != null && SanityMod.getInstance().getGui() != null) {
            SanityMod.getInstance().getGui().resize(pWidth, pHeight);
        }
    }

}