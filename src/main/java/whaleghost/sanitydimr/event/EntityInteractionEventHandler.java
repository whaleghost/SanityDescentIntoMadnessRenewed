package whaleghost.sanitydimr.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import whaleghost.sanitydimr.capability.Sanity;
import whaleghost.sanitydimr.client.render.layer.Blackout;
import whaleghost.sanitydimr.entity.goal.AvoidInsanePlayerGoal;
import whaleghost.sanitydimr.entity.goal.TargetInsanePlayerGoal;
import whaleghost.sanitydimr.SanityEventHandlers;

public class EntityInteractionEventHandler {

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
            SanityEventHandlers.handlePlayerUsedShears(sp);
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
        if (event.getEntity() instanceof ServerPlayer sp
                && event.getLevel().getBlockState(event.getHitVec().getBlockPos()).getBlock() instanceof FlowerPotBlock
                && sp.getItemInHand(event.getHand()).is(ItemTags.FLOWERS)) {
            SanityEventHandlers.handlePlayerPottedFlower(sp);
        }
    }

    @SubscribeEvent
    public void onProjectileImpact(final ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof ThrownEgg egg && egg.getOwner() instanceof ServerPlayer sp) {
            SanityEventHandlers.handlePlayerSpawnedChicken(sp);
        }
    }
}
