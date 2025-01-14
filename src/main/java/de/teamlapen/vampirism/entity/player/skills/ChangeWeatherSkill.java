package de.teamlapen.vampirism.entity.player.skills;

import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.util.BALANCE;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.storage.WorldInfo;

public class ChangeWeatherSkill extends DefaultSkill {

	@Override
	public int getCooldown() {
		return BALANCE.VP_SKILLS.WEATHER_COOLDOWN * 20;
	}

	@Override
	public int getMinLevel() {
		return BALANCE.VP_SKILLS.WEATHER_MIN_LEVEL;
	}

	@Override
	public int getMinU() {
		return 48;
	}

	@Override
	public int getMinV() {
		return 0;
	}

	@Override
	public String getUnlocalizedName() {
		return "skill.vampirism.change_weather";
	}

	@Override
	public boolean onActivated(VampirePlayer vampire, EntityPlayer player) {
		WorldInfo info = player.worldObj.getWorldInfo();
		info.setRaining(!info.isRaining());
		return true;
	}

}
