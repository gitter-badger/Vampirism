package de.teamlapen.vampirism.entity.player.skills;

import de.teamlapen.vampirism.entity.EntityBlindingBat;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.util.BALANCE;
import de.teamlapen.vampirism.util.REFERENCE;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;

public class SummonBatSkill extends DefaultSkill {

	@Override
	public boolean canBeUsedBy(VampirePlayer vampire, EntityPlayer player) {
		return vampire.isSkillActive(Skills.batMode);
	}

	@Override
	public int getCooldown() {
		return BALANCE.VP_SKILLS.SUMMON_BAT_COOLDOWN * 20;
	}

	@Override
	public int getMinLevel() {
		return BALANCE.VP_SKILLS.SUMMON_BAT_MIN_LEVEL;
	}

	@Override
	public int getMinU() {
		return 96;
	}

	@Override
	public int getMinV() {
		return 0;
	}

	@Override
	public String getUnlocalizedName() {
		return "skill.vampirism.summon_bats";
	}

	@Override
	public boolean onActivated(VampirePlayer vampire, EntityPlayer player) {
		for (int i = 0; i < BALANCE.VP_SKILLS.SUMMON_BAT_COUNT; i++) {
			Entity e = EntityList.createEntityByName(REFERENCE.ENTITY.BLINDING_BAT_NAME, player.worldObj);
			((EntityBlindingBat)e).restrictLiveSpan();
			e.copyLocationAndAnglesFrom(player);
			player.worldObj.spawnEntityInWorld(e);
		}
		return true;
	}

}
