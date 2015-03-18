package de.teamlapen.vampirism.entity.player.skills;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.util.BALANCE;

public class VampireLordSkill extends DefaultSkill implements ILastingSkill {

	/**
	 * Skill ID, has to be set when this is registered
	 */
	public static int ID;

	@Override
	public int getCooldown() {
		return BALANCE.VP_SKILLS.LORD_COOLDOWN * 20;
	}

	@Override
	public int getDuration(int level) {
		return BALANCE.VP_SKILLS.getVampireLordDuration(level);
	}

	@Override
	public int getMinLevel() {
		return BALANCE.VP_SKILLS.LORD_MIN_LEVEL;
	}

	@Override
	public int getMinU() {
		return 32;
	}

	@Override
	public int getMinV() {
		return 0;
	}

	@Override
	public void onActivated(VampirePlayer vampire, EntityPlayer player) {
		player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, getDuration(vampire.getLevel()), 2));

	}

	@Override
	public void onDeactivated(VampirePlayer vampire, EntityPlayer player) {
		player.removePotionEffect(Potion.moveSpeed.id);

	}

	@Override
	public void onUpdate(VampirePlayer vampire, EntityPlayer player) {

	}

	@Override
	public String toString() {
		return "VampireLordSkill ID: " + VampireLordSkill.ID;
	}

}