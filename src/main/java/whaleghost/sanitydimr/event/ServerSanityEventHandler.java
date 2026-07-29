package whaleghost.sanitydimr.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import whaleghost.sanitydimr.command.SanityCommand;
import whaleghost.sanitydimr.SanityMod;
import whaleghost.sanitydimr.SanityProcessor;

public class ServerSanityEventHandler {

    @SubscribeEvent
    public void tickPlayer(final PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SanityProcessor.tickPlayer(sp);
        }
    }

    @SubscribeEvent
    public void tickLevel(final LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel sl) {
            SanityProcessor.tickLevel(sl);
        }
    }

    @SubscribeEvent
    public void onLivingDamage(final LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SanityEventHandlers.handlePlayerHurt(player, event.getNewDamage());
        } else if (event.getEntity() instanceof Animal animal
                && event.getSource().getEntity() instanceof ServerPlayer player) {
            float realDamage = Math.min(animal.getHealth(), event.getNewDamage());
            SanityEventHandlers.handlePlayerHurtAnimal(player, animal, realDamage);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(final LivingDeathEvent event) {
        if (event.getEntity() instanceof TamableAnimal ta && ta.getOwnerUUID() != null) {
            SanityEventHandlers.handlePlayerPetDeath(
                    ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(ta.getOwnerUUID()), ta);
        }
    }

    @SubscribeEvent
    public void onPlayerGotAdvancement(final AdvancementEvent.AdvancementEarnEvent event) {
        SanityEventHandlers.handlePlayerGotAdvancement(
                (ServerPlayer) event.getEntity(), event.getAdvancement().value());
    }

    @SubscribeEvent
    public void onPlayerBredAnimals(final BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer sp) {
            SanityEventHandlers.handlePlayerBredAnimals(sp);
        }
    }

    @SubscribeEvent
    public void onSleepFinished(final SleepFinishedTimeEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            SanityEventHandlers.handlePlayerSlept(sl);
        }
    }

    @SubscribeEvent
    public void onTradeWithVillager(final TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SanityEventHandlers.handlePlayerTradedWithVillager(sp);
        }
    }

    @SubscribeEvent
    public void onPlayerUsedItem(final LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SanityEventHandlers.handlePlayerUsedItem(sp, event.getItem());
        }
    }

    @SubscribeEvent
    public void onItemFished(final ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SanityEventHandlers.handlePlayerFishedItem(sp);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SanityEventHandlers.handlePlayerChangedDimensions(sp);
        }
    }

    @SubscribeEvent
    public void onPlayerStruckByLightning(final EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SanityEventHandlers.handlePlayerStruckByLightning(sp);
        }
    }

    @SubscribeEvent
    public void registerCommands(final RegisterCommandsEvent event) {
        SanityMod.LOGGER.info("Registering sanity command...");
        SanityCommand.register(event.getDispatcher());
    }
}
