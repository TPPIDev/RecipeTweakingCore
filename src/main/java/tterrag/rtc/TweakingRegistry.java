package tterrag.rtc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.world.WorldEvent;
import tterrag.rtc.RecipeAddition.EventTime;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class TweakingRegistry
{
	private static HashMap<Item, HashSet<Integer>> recipesToRemove = new HashMap<Item, HashSet<Integer>>();

	private static HashMap<Item, HashMap<Integer, String[]>> removalReasons = new HashMap<Item, HashMap<Integer,String[]>>();
	
	public enum TweakingAction
	{
		REMOVED("Removed:"),
		CHANGED("Recipe Changed:"),
		ADDED("Added:"),
		NOTE("Note: ");

		private String name;
		
		TweakingAction(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	/**
	 * The main way of removing recipes
	 * @param id - ID of the item/block being removed
	 * @param damage - damage value/ metadata (-1 for all)
	 */
	public static void markItemForRecipeRemoval(Item item, int damage) {	
		if(!recipesToRemove.containsKey(item)) {
			recipesToRemove.put(item, new HashSet<Integer>());
		}
		recipesToRemove.get(item).add(damage);
	}
	
	/**
	 * Currently unused, will eventually be to add tooltips to tweaked items automatically
	 * @param id
	 * @param damage
	 * @param action
	 * @param details
	 */
	public static void markItemForRecipeRemoval(Item item, int damage, TweakingAction action, String... details) {	
		markItemForRecipeRemoval(item, damage);
		addTweakedTooltip(item, damage, action, details);
	}
	
	/**
	 * Currently unused, will eventually be to add tooltips to tweaked items automatically
	 */
	public static void addTweakedTooltip(Item item, int damage, TweakingAction action, String... details)
	{
		if (!removalReasons.containsKey(item))
		{
			removalReasons.put(item, new HashMap<Integer, String[]>());
		}
		
		String[] lines = new String[details.length + 1];
		lines[0] = action.toString();
		
		for (int i = 1; i < lines.length; i++)
			lines[i] = details[i - 1];
		
		removalReasons.get(item).put(damage, lines);
	}

	
	@SuppressWarnings({ "unchecked" })
	static void removeRecipes()
	{
		ListIterator<IRecipe> iterator = CraftingManager.getInstance().getRecipeList().listIterator();
		while (iterator.hasNext())
		{
			IRecipe r = iterator.next();
			if (canRemoveRecipe(r))
			{
				iterator.remove();
			}
		}
		recipesToRemove.clear();
	}
	
	static boolean canRemoveRecipe(IRecipe r)
	{
		try
		{
			ItemStack output = r.getRecipeOutput();
			if (output == null) return false;
			HashSet<Integer> validMetas = recipesToRemove.get(output.getItem());
			if (validMetas == null) return false;
			return validMetas.contains(-1) || validMetas.contains(output.getItemDamage());
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Whether or not this ID (and damage value) has been removed
	 * @param data <br>- [0] - ID
	 * 			   <br>- [1] - damage (can be ommitted)<p>
	 * 						No further data will be analyzed
	 */
	static boolean contains(Item item, int damage)
	{
		return removalReasons.get(item) != null && removalReasons.get(item).containsKey(damage);
	}
	
	/**
	 * The tooltip associated with this ID/damage
	 * @param item
	 * @param damage - no sensitivity = -1
	 * @return null if no tooltip associated
	 */
	static String[] getTooltip(Item item, int damage)
	{
		if (contains(item, damage))
		{
			return removalReasons.get(item).get(damage);
		}
		else if (contains(item, -1))
		{
			return removalReasons.get(item).get(-1);
		}
		return null;
	}
	
	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event)
	{
		if (!RecipeTweakingCore.donePlayerJoinTweaks)
		{
			RecipeTweakingCore.doTweaks(EventTime.WORLD_LOAD);
			RecipeTweakingCore.donePlayerJoinTweaks = true;
		}
	}
}