package tterrag.rtc;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

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
		log("Doing tweaks at time: " + event.toString());

		for (String packageName : packageNames)
		{
			try
			{
				log("Processing package: " + packageName);
				removeRecipes(event, packageName);
				addRecipes(event, packageName);
			}
			catch (IOException e)
			{
				logErr("Error processing package: " + packageName);
				throw new RuntimeException(e);
			}
		}
	}

	private static void removeRecipes(EventTime event, String packageName) throws IOException
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
					try
					{
						log("Processing remove method \"" + m.getName() + "\" in class \"" + c.getName() + "\"");
						m.invoke(null, new Object[] {});
					}
					catch (Throwable t)
					{
						logErr("[Removals] An exception was thrown processing \"" + m.getName() + "\" in class \"" + c.getName() + "\"" + "these removals will likely not occur.");
						t.printStackTrace();
					}
				}
			}
		}

		TweakingRegistry.removeRecipes();
	}

	private static void addRecipes(EventTime event, String packageName) throws IOException
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
					try
					{
						log("Processing add method \"" + m.getName() + "\" in class \"" + c.getName() + "\"");
						m.invoke(null, new Object[] {});
					}
					catch (Throwable t)
					{
						logErr("[Additions] An exception was thrown processing \"" + m.getName() + "\" in class \"" + c.getName() + "\"" + "these additions will likely not occur.");
						t.printStackTrace();
					}
				}
			}
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
			logErr(String.format("Class %s threw an error on load, skipping...", c.getName()));
			return new Method[] {};
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
	
	public static void log(String msg)
	{
		System.out.println("[RTC] " + msg);
	}
	
	public static void logErr(String msg)
	{
		System.err.println("[RTC] " + msg);
	}
}
