package whaleghost.sanitydimr.passive;

import whaleghost.sanitydimr.capability.IPersistentSanity;
import whaleghost.sanitydimr.capability.ISanity;
import whaleghost.sanitydimr.config.ConfigProxy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;

public class EnderManAnger implements IPassiveSanitySource
{
    @Override
    public float get(@Nonnull ServerPlayer player, @Nonnull ISanity cap, @Nonnull ResourceLocation dim)
    {
        if (cap instanceof IPersistentSanity ps && ps.getEnderManAngerTimer() > 0)
        {
            ps.setEnderManAngerTimer(ps.getEnderManAngerTimer() - 1);
            return ConfigProxy.getEnderManAnger(dim);
        }

        return 0;
    }
}