package whaleghost.sanitydimr.client.render.layer;

import whaleghost.sanitydimr.SanityMod;
import net.minecraft.resources.ResourceLocation;

public abstract class Blackout
{
    public static final float THRESHOLD = .7f;

    public static final ResourceLocation CHICKEN_LOCATION       = ResourceLocation.fromNamespaceAndPath(SanityMod.MODID, "textures/entity/chicken_blackout.png");
    public static final ResourceLocation COW_LOCATION           = ResourceLocation.fromNamespaceAndPath(SanityMod.MODID, "textures/entity/cow_blackout.png");
    public static final ResourceLocation PIG_LOCATION           = ResourceLocation.fromNamespaceAndPath(SanityMod.MODID, "textures/entity/pig_blackout.png");
    public static final ResourceLocation SHEEP_LOCATION         = ResourceLocation.fromNamespaceAndPath(SanityMod.MODID, "textures/entity/sheep_blackout.png");

    public static final ResourceLocation SHEEP_FUR_LOCATION     = ResourceLocation.fromNamespaceAndPath(SanityMod.MODID, "textures/entity/sheep_fur_blackout.png");
}