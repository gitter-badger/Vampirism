package de.teamlapen.vampirism.entity.player.skills;

import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.util.IPieElement;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Interface for vampire skills
 * 
 * @author maxanier
 *
 */
public interface ISkill extends IPieElement {
	/**
	 * @return -1 disabled, 0 level to low, -2 other reason, 1 can be used
	 */
	public int canUse(VampirePlayer vampire, EntityPlayer player);

	/**
	 * @return Cooldown time in ticks until the skill can be used again
	 */
	public int getCooldown();

	@Override
	public String getUnlocalizedName();

	/**
	 * Called when the skill is activated SERVER SIDE ONLY
	 * 
	 * @param vampire
	 * @param player
	 * @return Whether the skill was successfully activated. !Does not give any feedback to the user!
	 */
	public boolean onActivated(VampirePlayer vampire, EntityPlayer player);

	/**
	 * Should only be called when being registered
	 * 
	 * @param id
	 */
	public void setId(int id);
}
