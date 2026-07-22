package whaleghost.sanitydimr.mixin;

import whaleghost.sanitydimr.capability.Sanity;
import whaleghost.sanitydimr.client.render.layer.Blackout;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Animal.class)
public abstract class MixinAnimal
{
    @Inject(method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"),
            cancellable = true)
    private void mobInteract(Player pPlayer, InteractionHand pHand, CallbackInfoReturnable<InteractionResult> ci)
    {
        if (!pPlayer.level().isClientSide())
        {
            Sanity s = pPlayer.getData(Sanity.ATTACHMENT);
            if (s.getSanity() >= Blackout.THRESHOLD)
                ci.setReturnValue(InteractionResult.PASS);
        }
    }
}