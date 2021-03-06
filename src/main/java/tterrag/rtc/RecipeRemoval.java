package tterrag.rtc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import tterrag.rtc.RecipeAddition.EventTime;

/**
 * Marks a method as one that removes recipes
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RecipeRemoval
{
	/**
	 * The {@link EventTime} at which this method is to be executed
	 */
	EventTime time() default EventTime.POST_INIT;
	
	/**
	 * The modids that must be loaded for this method to execute
	 */
	String[] requiredModids() default {};
}
