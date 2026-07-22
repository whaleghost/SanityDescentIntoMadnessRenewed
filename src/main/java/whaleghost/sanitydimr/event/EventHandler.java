package whaleghost.sanitydimr.event;

import whaleghost.sanitydimr.SanityMod;
import whaleghost.sanitydimr.SanityProcessor;
import whaleghost.sanitydimr.capability.SanityLevelChunk;
import whaleghost.sanitydimr.client.SoundPlayback;
import whaleghost.sanitydimr.command.SanityCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class EventHandler
{
    @SubscribeEvent
    public void tickPlayer(final PlayerTickEvent.Post event)
    {
        if (event.getEntity() instanceof ServerPlayer sp)
            SanityProcessor.tickPlayer(sp);
    }

    @SubscribeEvent
    public void tickLevel(final LevelTickEvent.Post event)
    {
        if (event.getLevel() instanceof ServerLevel sl)
            SanityProcessor.tickLevel(sl);
    }

    @SubscribeEvent
    public void onLivingDamage(final LivingDamageEvent.Pre event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            SanityProcessor.handlePlayerHurt(player, event.getNewDamage());
        else if (event.getEntity() instanceof Animal animal && event.getSource().getEntity() instanceof ServerPlayer player)
            SanityProcessor.handlePlayerHurtAnimal(player, animal, event.getNewDamage());
    }

    @SubscribeEvent
    public void onLivingDeath(final LivingDeathEvent event)
    {
        if (event.getEntity() instanceof TamableAnimal ta && ta.getOwnerUUID() != null)
            SanityProcessor.handlePlayerPetDeath(ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(ta.getOwnerUUID()), ta);
    }

    @SubscribeEvent
    public void onPlayerGotAdvancement(final AdvancementEvent.AdvancementEarnEvent event)
    {
        SanityProcessor.handlePlayerGotAdvancement((ServerPlayer)event.getEntity(), event.getAdvancement().value());
    }

    @SubscribeEvent
    public void onPlayerBredAnimals(final BabyEntitySpawnEvent event)
    {
        if (event.getCausedByPlayer() instanceof ServerPlayer sp)
            SanityProcessor.handlePlayerBredAnimals(sp);
    }

    @SubscribeEvent
    public void onSleepFinished(final SleepFinishedTimeEvent event)
    {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl)
            SanityProcessor.handlePlayerSlept(sl);
    }

    @SubscribeEvent
    public void onTradeWithVillager(final TradeWithVillagerEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer sp)
            SanityProcessor.handlePlayerTradedWithVillager(sp);
    }

    @SubscribeEvent
    public void onPlayerUsedItem(final LivingEntityUseItemEvent.Finish event)
    {
        if (event.getEntity() instanceof ServerPlayer sp)
            SanityProcessor.handlePlayerUsedItem(sp, event.getItem());
    }

    @SubscribeEvent
    public void onItemFished(final ItemFishedEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer sp)
            SanityProcessor.handlePlayerFishedItem(sp);
    }

    @SubscribeEvent
    public void onFarmlandTrample(final BlockEvent.FarmlandTrampleEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer sp)
            SanityProcessor.handlePlayerTrampledFarmland(sp);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer sp)
            SanityProcessor.handlePlayerChangedDimensions(sp);
    }

    @SubscribeEvent
    public void onPlayerStruckByLightning(final EntityStruckByLightningEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer sp)
            SanityProcessor.handlePlayerStruckByLightning(sp);
    }

    @SubscribeEvent
    public void registerCommands(final RegisterCommandsEvent event)
    {
        SanityMod.LOGGER.info("Registering sanity command...");
        SanityCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onVanillaGameEvent(final VanillaGameEvent event)
    {
        if (event.getVanillaEvent() == GameEvent.BLOCK_PLACE)
        {
            Vec3 pos = event.getEventPosition();
            BlockPos bPos = BlockPos.containing(pos.x, pos.y, pos.z);
            SanityLevelChunk slc = event.getLevel().getChunkAt(bPos).getData(SanityLevelChunk.ATTACHMENT);
            slc.getArtificiallyPlacedBlocks().add(bPos);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void tickLocalPlayer(final PlayerTickEvent.Post event)
    {
        if (event.getEntity() instanceof LocalPlayer localPlayer)
        {
            SoundPlayback.playSounds(localPlayer);
            SanityMod.getInstance().getGui().tick(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void localLevelLoad(final LevelEvent.Load event)
    {
        if (event.getLevel() instanceof ClientLevel)
            SoundPlayback.onClientLevelLoad((ClientLevel) event.getLevel());
    }
}