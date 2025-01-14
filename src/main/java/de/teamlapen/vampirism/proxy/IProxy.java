package de.teamlapen.vampirism.proxy;

import de.teamlapen.vampirism.util.REFERENCE;
import de.teamlapen.vampirism.util.TickRunnable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public interface IProxy {

	/**
	 * Called on client to replace the texture location of vampire entitys by the fake vampire version
	 * 
	 * @param entity
	 * @param loc
	 * @return
	 */
	ResourceLocation checkVampireTexture(Entity entity, ResourceLocation loc);

	void enableMaxPotionDuration(PotionEffect p);

	/**
	 * @return Clientside: thePlayer, Serverside: null
	 */
	EntityPlayer getSPPlayer();

	void onTick(TickEvent event);

	void addTickRunnable(TickRunnable run);

	void preInit();
	void init();
	void postInit();
	/**
	 * Registeres all entitys
	 */


	// Coffin methods
	// public void wakeAllPlayers();
	//
	// public boolean areAllPlayersAsleepCoffin();
	//
	// public void updateAllPlayersSleepingFlagCoffin();

	/**
	 * Registeres all renders
	 */

	/**
	 * Registers all important subscriptions, which should be registered at startup (init)
	 */

	void setPlayerBat(EntityPlayer player, boolean bat);

	String getKey(REFERENCE.KEY key);
}