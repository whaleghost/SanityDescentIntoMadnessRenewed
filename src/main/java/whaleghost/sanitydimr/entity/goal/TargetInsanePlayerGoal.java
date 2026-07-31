package whaleghost.sanitydimr.entity.goal;

import whaleghost.sanitydimr.SanityProcessor;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

public class TargetInsanePlayerGoal extends TargetGoal {

    private final float sanityThreshold;
    private boolean alertSameType;

    @Nullable
    private Class<?>[] m_toIgnoreAlert;
    private Player m_insanePlayer;

    public TargetInsanePlayerGoal(Mob pMob, boolean pMustSee, float sanityThreshold) {
        super(pMob, pMustSee);
        setFlags(EnumSet.of(Goal.Flag.TARGET));
        this.sanityThreshold = sanityThreshold;
    }

    public TargetInsanePlayerGoal(Mob pMob, boolean pMustSee) {
        this(pMob, pMustSee, -1f);
    }

    @Override
    public boolean canUse() {
        return (m_insanePlayer = getMostInsanePlayer()) != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        if (target instanceof Player player) {
            float sanity = player.getData(whaleghost.sanitydimr.capability.Sanity.ATTACHMENT).getSanity();
            float threshold = sanityThreshold < 0f ? SanityProcessor.SANITY_TARGET_THRESHOLD : sanityThreshold;
            if (sanity < threshold) {
                return false;
            }
        }
        return super.canContinueToUse();
    }

    @Override
    public void start() {
        Player target = m_insanePlayer;
        if (target != null) {
            mob.setTarget(target);
            targetMob = mob.getTarget();
            if (alertSameType) {
                alertOthers();
            }
        }
        super.start();
    }

    public TargetInsanePlayerGoal setAlertOthers(Class<?>... pReinforcementTypes) {
        alertSameType = true;
        m_toIgnoreAlert = pReinforcementTypes;
        return this;
    }

    private Player getMostInsanePlayer() {
        return sanityThreshold < 0f ? SanityProcessor.getMostInsanePlayer(mob.level()) : SanityProcessor.getMostInsanePlayer(mob.level(), sanityThreshold);
    }

    protected void alertOthers() {
        double d0 = this.getFollowDistance();
        AABB aabb = AABB.unitCubeFromLowerCorner(mob.position()).inflate(d0, 10.0D, d0);
        List<? extends Mob> list = mob.level().getEntitiesOfClass(mob.getClass(), aabb, EntitySelector.NO_SPECTATORS);
        Iterator<? extends Mob> iterator = list.iterator();
        while(true) {
            Mob mob;
            while(true) {
                if (!iterator.hasNext()) {
                    return;
                }
                mob = iterator.next();
                if (this.mob != mob && mob.getTarget() == null &&
                    (
                        !(this.mob instanceof TamableAnimal) ||
                        ((TamableAnimal)this.mob).getOwnerUUID() == ((TamableAnimal)mob).getOwnerUUID()
                    ) && !mob.isAlliedTo(this.mob.getTarget())
                ) {
                    if (m_toIgnoreAlert == null) {
                        break;
                    }
                    boolean flag = false;
                    for(Class<?> oclass : m_toIgnoreAlert) {
                        if (mob.getClass() == oclass) {
                            flag = true;
                            break;
                        }
                    }
                    if (!flag) {
                        break;
                    }
                }
            }
            this.alertOther(mob, mob.getTarget());
        }
    }

    protected void alertOther(Mob pMob, LivingEntity pTarget) {
        pMob.setTarget(pTarget);
    }

}