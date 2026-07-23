package whaleghost.sanitydimr.event;

import net.minecraft.client.DeltaTracker;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.IShearable;
import whaleghost.sanitydimr.SanityMod;
import whaleghost.sanitydimr.SanityProcessor;
import whaleghost.sanitydimr.capability.Sanity;
import whaleghost.sanitydimr.capability.SanityLevelChunk;
import whaleghost.sanitydimr.client.SoundPlayback;
import whaleghost.sanitydimr.client.render.layer.Blackout;
import whaleghost.sanitydimr.command.SanityCommand;
import whaleghost.sanitydimr.entity.goal.AvoidInsanePlayerGoal;
import whaleghost.sanitydimr.entity.goal.TargetInsanePlayerGoal;
import whaleghost.sanitydimr.sound.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class EventHandler {

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
            SanityProcessor.handlePlayerHurt(player, event.getNewDamage());
        } else if (
                event.getEntity() instanceof Animal animal &&
                event.getSource().getEntity() instanceof ServerPlayer player
        ) {
            float realDamage = Math.min(animal.getHealth(), event.getNewDamage());
            SanityProcessor.handlePlayerHurtAnimal(player, animal, realDamage);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(final LivingDeathEvent event) {
        if (event.getEntity() instanceof TamableAnimal ta && ta.getOwnerUUID() != null) {
            SanityProcessor.handlePlayerPetDeath(
                    ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(ta.getOwnerUUID()), ta
            );
        }
    }

    @SubscribeEvent
    public void onPlayerGotAdvancement(final AdvancementEvent.AdvancementEarnEvent event) {
        SanityProcessor.handlePlayerGotAdvancement((ServerPlayer)event.getEntity(), event.getAdvancement().value());
    }

    @SubscribeEvent
    public void onPlayerBredAnimals(final BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer sp) {
            SanityProcessor.handlePlayerBredAnimals(sp);
        }
    }

    @SubscribeEvent
    public void onSleepFinished(final SleepFinishedTimeEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            SanityProcessor.handlePlayerSlept(sl);
        }
    }

    @SubscribeEvent
    public void onTradeWithVillager(final TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SanityProcessor.handlePlayerTradedWithVillager(sp);
        }
    }

    @SubscribeEvent
    public void onPlayerUsedItem(final LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SanityProcessor.handlePlayerUsedItem(sp, event.getItem());
        }
    }

    @SubscribeEvent
    public void onItemFished(final ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SanityProcessor.handlePlayerFishedItem(sp);
        }
    }

    @SubscribeEvent
    public void onFarmlandTrample(final BlockEvent.FarmlandTrampleEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SanityProcessor.handlePlayerTrampledFarmland(sp);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SanityProcessor.handlePlayerChangedDimensions(sp);
        }
    }

    @SubscribeEvent
    public void onPlayerStruckByLightning(final EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SanityProcessor.handlePlayerStruckByLightning(sp);
        }
    }

    @SubscribeEvent
    public void registerCommands(final RegisterCommandsEvent event) {
        SanityMod.LOGGER.info("Registering sanity command...");
        SanityCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onVanillaGameEvent(final VanillaGameEvent event) {
        if (event.getVanillaEvent() == GameEvent.BLOCK_PLACE) {
            Vec3 pos = event.getEventPosition();
            BlockPos bPos = BlockPos.containing(pos.x, pos.y, pos.z);
            SanityLevelChunk slc = event.getLevel().getChunkAt(bPos).getData(SanityLevelChunk.ATTACHMENT);
            slc.getArtificiallyPlacedBlocks().add(bPos);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void tickLocalPlayer(final PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof LocalPlayer localPlayer) {
            SoundPlayback.playSounds(localPlayer);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onRenderFrame(final RenderFrameEvent.Post event) {
        DeltaTracker delta = event.getPartialTick();
        float deltaInTicks = delta.getGameTimeDeltaTicks();
        SanityMod.getInstance().getGui().tick(deltaInTicks);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onRenderLevelStage(final RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            SanityMod.getInstance().getGui().initPostProcessor();
            SanityMod.getInstance().getGui().renderPostProcess(event.getPartialTick().getGameTimeDeltaTicks());
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void localLevelLoad(final LevelEvent.Load event) {
        if (event.getLevel() instanceof ClientLevel) {
            SoundPlayback.onClientLevelLoad((ClientLevel) event.getLevel());
        }
    }

    @SubscribeEvent
    public void onEntityJoinLevel(final EntityJoinLevelEvent event) {
        switch (event.getEntity()) {
            case Cow cow -> cow.goalSelector.addGoal(-1,
                    new AvoidInsanePlayerGoal(cow, 6.0f, 1.7d, 1.8d));
            case Chicken chicken -> chicken.goalSelector.addGoal(-1,
                    new AvoidInsanePlayerGoal(chicken, 6.0f, 1.5d, 1.6d));
            case Pig pig -> pig.goalSelector.addGoal(-1,
                    new AvoidInsanePlayerGoal(pig, 6.0f, 1.6d, 1.7d));
            case Sheep sheep -> sheep.goalSelector.addGoal(-1,
                    new AvoidInsanePlayerGoal(sheep, 6.0f, 1.6d, 1.7d));
            case ZombifiedPiglin zp -> zp.targetSelector.addGoal(1,
                    new TargetInsanePlayerGoal(zp, true, .7f).setAlertOthers());
            default -> {}
        }
    }

    @SubscribeEvent
    public void onEntityInteract(final PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (event.getTarget() instanceof IShearable && event.getItemStack().getItem() instanceof ShearsItem) {
            SanityProcessor.handlePlayerUsedShears(sp);
        }
        Sanity s = sp.getData(Sanity.ATTACHMENT);
        if (event.getTarget() instanceof Villager) {
            if (s.getSanity() >= .6f) {
                event.setCanceled(true);
            }
        } else if (event.getTarget() instanceof Animal) {
            if (s.getSanity() >= Blackout.THRESHOLD) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        if (
            event.getEntity() instanceof ServerPlayer sp &&
            event.getLevel().getBlockState(event.getHitVec().getBlockPos()).getBlock() instanceof FlowerPotBlock &&
            sp.getItemInHand(event.getHand()).is(ItemTags.FLOWERS)
        ) {
            SanityProcessor.handlePlayerPottedFlower(sp);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(final BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer sp) {
            Block block = event.getState().getBlock();
            boolean notCreative = !sp.isCreative();
            SanityProcessor.handlePlayerMinedBlock(sp, event.getPos(), event.getState(), block, notCreative);
        }
    }

    @SubscribeEvent
    public void onProjectileImpact(final ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof ThrownEgg egg && egg.getOwner() instanceof ServerPlayer sp) {
            SanityProcessor.handlePlayerSpawnedChicken(sp);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onPlaySound(final PlayLevelSoundEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }
        Sanity s = player.getData(Sanity.ATTACHMENT);
        if (s.getSanity() < Blackout.THRESHOLD) {
            return;
        }
        Holder<SoundEvent> soundHolder = event.getSound();
        if (soundHolder == null) {
            return;
        }
        SoundEvent soundEvent = soundHolder.value();
        if (
            soundEvent.equals(SoundEvents.CHICKEN_AMBIENT) || soundEvent.equals(SoundEvents.COW_AMBIENT) ||
            soundEvent.equals(SoundEvents.PIG_AMBIENT)     || soundEvent.equals(SoundEvents.SHEEP_AMBIENT)
        ) {
            event.setCanceled(true);
        } else if (
            soundEvent.equals(SoundEvents.CHICKEN_HURT) || soundEvent.equals(SoundEvents.CHICKEN_DEATH) ||
            soundEvent.equals(SoundEvents.COW_HURT)     || soundEvent.equals(SoundEvents.COW_DEATH) ||
            soundEvent.equals(SoundEvents.PIG_HURT)     || soundEvent.equals(SoundEvents.PIG_DEATH) ||
            soundEvent.equals(SoundEvents.SHEEP_HURT)   || soundEvent.equals(SoundEvents.SHEEP_DEATH)
        ) {
            event.setSound(SoundRegistry.INNER_ENTITY_HURT);
        }
    }

}