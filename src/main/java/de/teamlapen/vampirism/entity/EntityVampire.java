package de.teamlapen.vampirism.entity;

import com.google.common.base.Predicate;
import de.teamlapen.vampirism.ModItems;
import de.teamlapen.vampirism.entity.ai.VampireAIFleeSun;
import de.teamlapen.vampirism.entity.ai.VampireAIMoveToBiteable;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.util.BALANCE;
import de.teamlapen.vampirism.util.Helper18;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityVampire extends EntityDefaultVampire {
	private boolean inCastle = false;
	private int bloodtimer = 100;
	public EntityVampire(World par1World) {
		super(par1World);
		// Avoids Vampire Hunters
		this.tasks.addTask(3, new EntityAIAvoidEntity(this, Helper18.getPredicateForClass(EntityHunterBase.class), BALANCE.MOBPROP.VAMPIRE_DISTANCE_HUNTER, 1.0, 1.2));
		this.tasks.addTask(3, new EntityAIRestrictSun(this));
		this.tasks.addTask(4, new VampireAIFleeSun(this, 0.9F));
		// Low priority tasks
		this.tasks.addTask(9, new VampireAIMoveToBiteable(this));
		this.tasks.addTask(10, new EntityAIMoveThroughVillage(this, 0.6, true));
		this.tasks.addTask(11, new EntityAIWander(this, 0.7));
		this.tasks.addTask(12, new EntityAILookIdle(this));

		// Search for players
		this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
		this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true, false, new Predicate() {
			@Override
			public boolean apply(Object entity) {
				if (entity instanceof EntityPlayer) {
					return VampirePlayer.get((EntityPlayer) entity).getLevel() <= BALANCE.VAMPIRE_FRIENDLY_LEVEL || (!EntityVampire.this.isInCastle() && VampirePlayer.get((EntityPlayer) entity).isVampireLord());
				}
				return false;
			}
		}));


	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(BALANCE.MOBPROP.VAMPIRE_MAX_HEALTH);
		this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(BALANCE.MOBPROP.VAMPIRE_ATTACK_DAMAGE);
		this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(BALANCE.MOBPROP.VAMPIRE_MOVEMENT_SPEED);
	}

	@Override
	public void onDeath(DamageSource s) {
		if (s.getEntity() != null && s.getEntity() instanceof EntityPlayer) {
			this.dropItem(ModItems.vampireFang, 1);
		}
	}

	public void makeCastleVampire() {
		inCastle = true;
	}

	public boolean isInCastle() {
		return inCastle;
	}

	@Override
	public boolean wantsBlood() {
		return bloodtimer == 0;
	}

	@Override
	public void addBlood(int amt) {
		super.addBlood(amt);
		bloodtimer += amt * 40 + rand.nextInt(10) * 20;
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (bloodtimer > 0) bloodtimer--;
	}
}
