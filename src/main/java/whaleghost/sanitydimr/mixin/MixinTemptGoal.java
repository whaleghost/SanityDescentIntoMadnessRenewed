package whaleghost.sanitydimr.mixin;

import whaleghost.sanitydimr.capability.Sanity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TemptGoal.class)
public abstract class MixinTemptGoal
{
    @Inject(method = "shouldFollow(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("RETURN"),
            cancellable = true)
    private void shouldFollow(LivingEntity living, CallbackInfoReturnable<Boolean> ci)
    {
        if (!living.level().isClientSide() && living instanceof ServerPlayer sp)
        {
            Sanity s = sp.getData(Sanity.ATTACHMENT);
            if (s.getSanity() >= .5f)
                ci.setReturnValue(false);
        }
    }
}