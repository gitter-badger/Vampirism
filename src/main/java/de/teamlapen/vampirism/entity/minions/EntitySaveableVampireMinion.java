package de.teamlapen.vampirism.entity.minions;

import de.teamlapen.vampirism.entity.ai.MinionAIFollowBoss;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.util.Logger;
import de.teamlapen.vampirism.util.REFERENCE;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.ai.EntityAIFleeSun;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import org.eclipse.jdt.annotation.NonNull;

import javax.annotation.Nullable;
import java.util.ArrayList;

/**
 * Vampire minion which is saved with it's lord and is designed to protect the lord
 * 
 * @author Maxanier
 *
 */
public class EntitySaveableVampireMinion extends EntityVampireMinion {

	private final static String TAG = "SVampireMinion";

	protected IMinionLord lord;

	private final ArrayList<IMinionCommand> commands;

	public EntitySaveableVampireMinion(World world) {
		super(world);
		this.tasks.addTask(7, new MinionAIFollowBoss(this, 1.0D));
		this.tasks.addTask(14, new EntityAIFleeSun(this, 0.9F));
		commands = new ArrayList<IMinionCommand>();
		commands.add(getActiveCommand());
		commands.add(new AttackHostileExceptPlayer(1, this));
		commands.add(new AttackHostileIncludingPlayer(2, this));
		commands.add(new JustFollowCommand(3));
	}

	@Override
	public boolean attackEntityFrom(DamageSource src, float value) {
		if (DamageSource.inWall.equals(src)) {
			return false;
		} else {
			return super.attackEntityFrom(src, value);
		}
	}

	/**
	 * Converts this minion to a remote minion
	 */
	public void convertToRemote() {
		EntityRemoteVampireMinion remote = (EntityRemoteVampireMinion) EntityList.createEntityByName(REFERENCE.ENTITY.VAMPIRE_MINION_REMOTE_NAME, worldObj);
		remote.copyDataFromMinion(this);
		remote.setHealth(this.getHealth());
		remote.copyLocationAndAnglesFrom(this);
		IMinionLord lord = getLord();
		if (lord != null) {
			if (lord instanceof VampirePlayer) {
				lord.getMinionHandler().unregisterMinion(this);
				remote.setLord(lord);
			} else {
				Logger.w(TAG, "The converted minion %s cannot be controlled by this lord %s", remote, lord);
			}

		}
		worldObj.spawnEntityInWorld(remote);
		this.setDead();
	}

	@Override
	public void copyDataFromOld(Entity from) {
		super.copyDataFromOld(from);
		if (from instanceof EntitySaveableVampireMinion) {
			EntitySaveableVampireMinion m = (EntitySaveableVampireMinion) from;
			this.setLord(m.getLord());
			this.activateMinionCommand(m.getActiveCommand());
		}

	}

	@Override
	public ArrayList<IMinionCommand> getAvailableCommands() {
		return commands;
	}

	@Override
	public IMinionCommand getCommand(int id) {
		if (id < commands.size())
			return commands.get(id);
		return null;
	}

	@Override
	protected @NonNull IMinionCommand getDefaultCommand() {
		return new DefendLordCommand(0, this);
	}

	@Override
	public @Nullable IMinionLord getLord() {
		return lord;
	}

	@Override
	protected void loadPartialUpdateFromNBT(NBTTagCompound nbt) {
		if (nbt.hasKey("eid")) {
			Entity e = worldObj.getEntityByID(nbt.getInteger("eid"));
			if (e instanceof EntityPlayer) {
				this.lord = VampirePlayer.get((EntityPlayer) e);
				this.lord.getMinionHandler().registerMinion(this, true);
			} else if (e instanceof IMinionLord) {
				this.lord = (IMinionLord) e;
				this.lord.getMinionHandler().registerMinion(this, true);
			} else {
				Logger.w("EntityVampireMinion", "PartialUpdate: The given id(" + nbt.getInteger("eid") + ")[" + e + "] is no Minion Lord");
				return;
			}
		}

	}

	public void onCall(SaveableMinionHandler.Call c) {
		switch (c) {
		case DEFEND_LORD:
			this.activateMinionCommand(this.getCommand(0));
			break;
		case ATTACK:
			this.activateMinionCommand(this.getCommand(2));
			break;
		case ATTACK_NON_PLAYER:
			this.activateMinionCommand(this.getCommand(1));
			break;
		case FOLLOW:
			this.activateMinionCommand(this.getCommand(3));
			break;
		}
	}

	@Override
	public void onLivingUpdate() {
		if (!this.worldObj.isRemote) {
			if (lord == null) {

			} else if (!lord.isTheEntityAlive()) {
				lord = null;
				this.attackEntityFrom(DamageSource.magic, 1000);
			}

		}
		super.onLivingUpdate();
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);
	}

	/**
	 * Makes sure minions which are saved with their lord do not interact with portals
	 */
	@Override
	public void setInPortal() {
	}

	@Override
	public void setLord(IMinionLord lord) {
		if (!lord.equals(this.lord)) {
			lord.getMinionHandler().registerMinion(this, true);
			this.lord = lord;
		}
	}

	@Override
	public boolean shouldBeSavedWithLord() {
		return true;
	}

	@Override
	protected void writeUpdateToNBT(NBTTagCompound nbt) {
		if (lord != null) {
			nbt.setInteger("eid", lord.getRepresentingEntity().getEntityId());
		}

	}

}
