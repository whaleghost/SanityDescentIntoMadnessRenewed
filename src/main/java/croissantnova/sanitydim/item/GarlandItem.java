package croissantnova.sanitydim.item;

import croissantnova.sanitydim.SanityMod;
import croissantnova.sanitydim.client.ItemTooltipHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class GarlandItem extends ArmorItem
{
    public GarlandItem()
    {
        super(SanityMod.FLOWER_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Properties()
                .stacksTo(1)
                .setNoRepair());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @NotNull Item.TooltipContext pContext, @NotNull List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced)
    {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pIsAdvanced);
        ItemTooltipHelper.showTooltipOnShift(pTooltipComponents, "garland");
    }
}