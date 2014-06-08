package tterrag.rtc;

import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import org.lwjgl.input.Keyboard;

public class TooltipHandler
{	
	@ForgeSubscribe
	public void onItemTooltip(ItemTooltipEvent event)
	{
		String[] lines = TweakingRegistry.getTooltip(event.itemStack.itemID, event.itemStack.getItemDamage());
		if (lines != null)
		{
			if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
			{
				event.toolTip.add(EnumChatFormatting.RED + "Tweaked Item:");
				for (int i = 0; i < lines.length; i++)
					event.toolTip.add((i == 0 ? EnumChatFormatting.AQUA : EnumChatFormatting.YELLOW)+ lines[i]);
			}
			else
				event.toolTip.add(EnumChatFormatting.YELLOW + "Tweaked Item! - " + EnumChatFormatting.RED + "Shift " + EnumChatFormatting.YELLOW + "for Info");
		}
	}
}
