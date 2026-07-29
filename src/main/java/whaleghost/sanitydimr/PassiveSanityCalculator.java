package whaleghost.sanitydimr;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import whaleghost.sanitydimr.capability.ISanity;
import whaleghost.sanitydimr.config.ConfigProxy;
import whaleghost.sanitydimr.item.ItemRegistry;
import whaleghost.sanitydimr.passive.*;

import java.util.Arrays;
import java.util.List;

public final class PassiveSanityCalculator {

    private static int garlandTimer;
    public static final int MAX_GARLAND_TIMER = 60;

    public static final List<IPassiveSanitySource> PASSIVE_SANITY_SOURCES = Arrays.asList(
            new Passive(),
            new InWaterOrRain(),
            new Hungry(),
            new EnderManAnger(),
            new Pet(),
            new Monster(),
            new Darkness(),
            new Lightness(),
            new PassiveBlocks(),
            new PlayerCompany(),
            new Jukebox(),
            new BlockStuck(),
            new DirtPath()
    );

    private PassiveSanityCalculator() {}

    static float calcPassive(ServerPlayer player, ISanity sanity) {
        ResourceLocation dim = player.level().dimension().location();
        float passive = 0;

        for (IPassiveSanitySource pss : PASSIVE_SANITY_SOURCES) {
            float val = pss.get(player, sanity, dim);
            val *= SanityProcessor.getSanityMultiplier(player, val);
            passive += val;
        }

        garlandTimer--;
        ItemStack headItem = player.getItemBySlot(EquipmentSlot.HEAD);
        if (headItem.is(ItemRegistry.GARLAND.get())) {
            passive += ConfigProxy.getGarland(dim) * ConfigProxy.getPosMul(dim) * 50;
            if (garlandTimer <= 0) {
                headItem.hurtAndBreak(player.isInWaterOrRain() ? 2 : 1,
                        player.serverLevel(), player, ent -> {});
            }
        }
        if (garlandTimer <= 0) {
            garlandTimer = MAX_GARLAND_TIMER;
        }

        return passive;
    }
}
