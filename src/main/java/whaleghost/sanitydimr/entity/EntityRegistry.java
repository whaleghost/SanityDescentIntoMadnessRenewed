package whaleghost.sanitydimr.entity;

import whaleghost.sanitydimr.SanityMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class EntityRegistry
{
    public static final DeferredRegister<EntityType<?>> DEFERRED_REGISTER = DeferredRegister.create(Registries.ENTITY_TYPE, SanityMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<RottingStalker>> ROTTING_STALKER
            = DEFERRED_REGISTER.register("rotting_stalker",
            () -> EntityType.Builder.of(RottingStalker::new, MobCategory.MONSTER).sized(1f, 2.9f).fireImmune().build("rotting_stalker"));

    public static final DeferredHolder<EntityType<?>, EntityType<SneakingTerror>> SNEAKING_TERROR
            = DEFERRED_REGISTER.register("sneaking_terror",
            () -> EntityType.Builder.of(SneakingTerror::new, MobCategory.MONSTER).sized(1.3f, 4f).fireImmune().build("sneaking_terror"));

    public static final List<Supplier<EntityType<? extends InnerEntity>>> INNER_ENTITIES = Arrays.asList(
            ROTTING_STALKER::get,
            SNEAKING_TERROR::get);

    public static void register(IEventBus eventBus)
    {
        DEFERRED_REGISTER.register(eventBus);
    }
}