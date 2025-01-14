package de.teamlapen.vampirism.entity.player.skills;

import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.util.Logger;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;

/**
 * Manages the vampire skills and their registration
 * 
 * @author maxanier
 *
 */
public class Skills {

	public static ISkill vampireRage;
	public static ISkill batMode;

	private final static ArrayList<ISkill> skills = new ArrayList<ISkill>();

	/**
	 * Used by {@link de.teamlapen.vampirism.client.gui.GUISelectSkill}
	 * 
	 * @return
	 */
	public static ArrayList<ISkill> getAvailableSkills(VampirePlayer player) {
		ArrayList<ISkill> sl = new ArrayList<ISkill>();
		for (ISkill s : skills) {
			if (1 == s.canUse(player, (EntityPlayer) player.getRepresentingEntity())) {
				sl.add(s);
			}
		}
		return sl;
	}

	/**
	 * Returns the skill with the given id, might return null if the skill doesn't exist
	 * 
	 * @param i
	 * @return
	 */
	public static ISkill getSkill(int i) {
		try {
			return skills.get(i);
		} catch (IndexOutOfBoundsException e) {
			Logger.e("Skills", "Skill with id " + i + " doesn't exist");
			return null;
		}
	}

	public static int getSkillCount() {
		return skills.size();
	}

	/**
	 * Register all default skills
	 */
	public static void registerDefaultSkills() {
		vampireRage = Skills.registerSkill(new VampireRageSkill());
		Skills.registerSkill(new RegenSkill());
		Skills.registerSkill(new ChangeWeatherSkill());
		Skills.registerSkill(new ReviveFallenSkill());
		batMode = Skills.registerSkill(new BatSkill());
		Skills.registerSkill(new SummonBatSkill());
		Skills.registerSkill(new InvisibilitySkill());
		Skills.registerSkill(new TeleportSkill());
		Skills.registerSkill(new FreezeSkill());
	}

	/**
	 * 
	 * @param s
	 * @return The assigned id
	 */
	public static ISkill registerSkill(ISkill s) {
		int id = skills.size();
		s.setId(id);
		skills.add(s);
		return s;
	}
}
