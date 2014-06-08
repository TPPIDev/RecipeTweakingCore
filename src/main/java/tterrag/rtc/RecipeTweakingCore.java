package tterrag.rtc;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import net.minecraftforge.common.MinecraftForge;
import tterrag.rtc.RecipeAddition.EventTime;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;

@Mod(modid = "recipeTweakingCore", name = "Recipe Tweaking Core", version = "1.0.0")
@NetworkMod(serverSideRequired = true, clientSideRequired = true)
public class RecipeTweakingCore
{
	@Instance
	public static RecipeTweakingCore instance;

	public static HashSet<String> packageNames = new HashSet<String>();

	static boolean donePlayerJoinTweaks = false;

	public static Logger logger = Logger.getLogger("RecipeTweakingCore");

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		MinecraftForge.EVENT_BUS.register(new TooltipHandler());
		
		doTweaks(EventTime.INIT);
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		MinecraftForge.EVENT_BUS.register(new TweakingRegistry());
		
		doTweaks(EventTime.POST_INIT);
	}

	/**
	 * Registers a package domain (non-recursive) to be analyzed
	 * 
	 * @param packageName
	 */
	public static void registerPackageName(String packageName)
	{
		packageNames.add(packageName);
	}

	static void doTweaks(EventTime event)
	{
		for (String packageName : packageNames)
		{
			removeRecipes(event, packageName);
			addRecipes(event, packageName);
		}
	}

	private static void removeRecipes(EventTime event, String packageName)
	{
		try
		{
			ClassPath classpath = ClassPath.from(RecipeTweakingCore.class.getClassLoader());
			Set<ClassInfo> classes = classpath.getTopLevelClasses(packageName);

			for (ClassInfo c : classes)
			{
				for (Method m : loadClassSafe(c))
				{
					RecipeRemoval r = m.getAnnotation(RecipeRemoval.class);
					if (r != null && allModsLoaded(r.requiredModids()) && r.time() == event)
					{
						m.invoke(null, new Object[] {});
					}
				}
			}
		}
		catch (Throwable t)
		{
			logger.severe("Could not perform recipe removals. This is a serious error!");
			t.printStackTrace();
			throw new RuntimeException("Recipe tweaks failed.");
		}

		TweakingRegistry.removeRecipes();
	}

	private static void addRecipes(EventTime event, String packageName)
	{
		try
		{
			ClassPath classpath = ClassPath.from(RecipeTweakingCore.class.getClassLoader());
			Set<ClassInfo> classes = classpath.getTopLevelClasses(packageName);

			for (ClassInfo c : classes)
			{
				for (Method m : loadClassSafe(c))
				{
					RecipeAddition r = m.getAnnotation(RecipeAddition.class);
					if (r != null && allModsLoaded(r.requiredModids()) && r.time() == event)
					{
						m.invoke(null, new Object[] {});
					}
				}
			}
		}
		catch (Throwable t)
		{
			logger.severe("Could not perform recipe additions. This is a serious error!");
			t.printStackTrace();
			throw new RuntimeException("Recipe tweaks failed.");
		}
	}
	
	private static Method[] loadClassSafe(ClassInfo c)
	{
		try
		{
			Class<?> clazz = c.load();
			return clazz.getDeclaredMethods();
		}
		catch (Throwable t)
		{
			logger.info(String.format("Class %s threw an error, skipping...", c.getName()));
			return new Method[]{};
		}
	}

	private static boolean allModsLoaded(String[] modids)
	{
		for (String s : modids)
		{
			if (!Loader.isModLoaded(s))
				return false;
		}
		return true;
	}
}
