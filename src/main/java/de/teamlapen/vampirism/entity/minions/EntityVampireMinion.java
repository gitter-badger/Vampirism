package de.teamlapen.vampirism.entity.minions;

import de.teamlapen.vampirism.entity.EntityDefaultVampire;
import de.teamlapen.vampirism.entity.EntityDracula;
import de.teamlapen.vampirism.entity.EntityPortalGuard;
import de.teamlapen.vampirism.entity.EntityVampireBaron;
import de.teamlapen.vampirism.entity.ai.MinionAIHurtByNonLord;
import de.teamlapen.vampirism.network.ISyncable;
import de.teamlapen.vampirism.network.UpdateEntityPacket;
import de.teamlapen.vampirism.util.BALANCE;
import de.teamlapen.vampirism.util.Helper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Iterator;
import java.util.List;

/**
 * Base class for all vampire minions. Handles conversion and commands
 * 
 * @author Max
 *
 */
public abstract class EntityVampireMinion extends EntityDefaultVampire implements IMinion, ISyncable {

	/**
	 * Used for the visual transition from normal vampire to players minion
	 */
	private int oldVampireTexture = -1;

	private IMinionCommand activeCommand;

	@Override
	public boolean wantsBlood() {
		return wantsBlood;
	}


	public void setWantsBlood(boolean wantsBlood) {
		this.wantsBlood = wantsBlood;
	}

	private boolean wantsBlood = false;

	@SideOnly(Side.CLIENT)
	private int activeCommandId;

	@Override
	public void onKillEntity(EntityLivingBase entity) {
		super.onKillEntity(entity);
		if (entity instanceof EntityPlayer) {
			if (this.getLord() != null && this.getLord() instanceof EntityDracula) {
				((EntityDracula) this.getLord()).restoreOnPlayerKill((EntityPlayer) entity);
			}
		}
		else{
			if(this.getLord() !=null && this.getLord() instanceof EntityVampireBaron){
				((EntityVampireBaron) this.getLord()).onKillEntity(entity);
			}
		}
	}

	public EntityVampireMinion(World world) {
		super(world);
		// this.setSize(0.5F, 1.1F);
		this.enablePersistence();
		this.tasks.addTask(6, new EntityAIAttackOnCollide(this, EntityLivingBase.class, 1.0, false));
		this.tasks.addTask(15, new EntityAIWander(this, 0.7));
		this.tasks.addTask(16, new EntityAIWatchClosest(this, EntityPlayer.class, 10));

		this.targetTasks.addTask(8, new MinionAIHurtByNonLord(this, false));

		activeCommand = this.getDefaultCommand();
		activeCommand.onActivated();
	}

	@Override
	public void activateMinionCommand(IMinionCommand command) {
		if (command == null)
			return;
		this.activeCommand.onDeactivated();
		this.activeCommand = command;
		this.activeCommand.onActivated();
		this.sync();
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(30D);
		this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(BALANCE.MOBPROP.VAMPIRE_MINION_MAX_HEALTH);
		this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(BALANCE.MOBPROP.VAMPIRE_MINION_ATTACK_DAMAGE);
		this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(BALANCE.MOBPROP.VAMPIRE_MINION_MOVEMENT_SPEED);
	}

	@Override
	public void copyDataFromOld(Entity from) {
		super.copyDataFromOld(from);
		if (from instanceof EntityVampireMinion) {
			EntityVampireMinion m = (EntityVampireMinion) from;
			this.copyDataFromMinion(m);
		}
	}

	/**
	 * Copies vampire minion data
	 * 
	 * @param from
	 */
	protected void copyDataFromMinion(EntityVampireMinion from) {
		this.setOldVampireTexture(from.getOldVampireTexture());
	}

	public IMinionCommand getActiveCommand() {
		return this.activeCommand;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getActiveCommandId() {
		return this.activeCommandId;
	}


	@Override
	public float func_180484_a(BlockPos pos) {
		float i = 0.5F - this.worldObj.getLightBrightness(pos);
		if (i > 0)
			return i;
		return 0.01F;
	}


	/**
	 * Has to return the command which is activated on default
	 * 
	 * @return
	 */
	protected abstract @NonNull IMinionCommand getDefaultCommand();

	public int getOldVampireTexture() {
		return oldVampireTexture;
	}

	@Override
	public @NonNull EntityCreature getRepresentingEntity() {
		return this;
	}

	@Override
	public boolean isChild() {
		return true;
	}

	/**
	 * Vampire minions have no right to complain about sun damage ;)
	 */
	@Override
	public boolean isValidLightLevel() {
		return true;
	}

	/**
	 * Can be used by child classes to write info to {@link #loadUpdateFromNBT(NBTTagCompound)}
	 * 
	 * @param nbt
	 */
	protected void loadPartialUpdateFromNBT(NBTTagCompound nbt) {

	}

	@SideOnly(Side.CLIENT)
	@Override
	public void loadUpdateFromNBT(NBTTagCompound nbt) {
		if (nbt.hasKey("oldvampire")) {
			this.oldVampireTexture = nbt.getInteger("oldvampire");
		}
		this.activeCommandId = nbt.getInteger("active_command_id");
		loadPartialUpdateFromNBT(nbt);
	}

	@Override
	public boolean canAttackClass(Class p_70686_1_) {
		if (EntityPortalGuard.class.equals(p_70686_1_)) return false;
		return super.canAttackClass(p_70686_1_);
	}

	@Override
	public boolean canEntityBeSeen(Entity p_70685_1_) {
		if (p_70685_1_.isInvisible()) return false;
		return super.canEntityBeSeen(p_70685_1_);
	}

	@Override
	public void onLivingUpdate() {
		if (oldVampireTexture != -1 && this.ticksExisted > 50) {
			oldVampireTexture = -1;
		}
		if (oldVampireTexture != -1 && worldObj.isRemote) {
			Helper.spawnParticlesAroundEntity(this, EnumParticleTypes.SPELL_WITCH, 1.0F, 3);
		}
		if (!this.worldObj.isRemote && !this.dead) {
			@SuppressWarnings("rawtypes")
			List list = this.worldObj.getEntitiesWithinAABB(EntityItem.class, this.getEntityBoundingBox().expand(1.0D, 0.0D, 1.0D));
			@SuppressWarnings("rawtypes")
			Iterator iterator = list.iterator();

			while (iterator.hasNext()) {
				EntityItem entityitem = (EntityItem) iterator.next();

				if (!entityitem.isDead && entityitem.getEntityItem() != null) {
					ItemStack itemstack = entityitem.getEntityItem();
					if (activeCommand.shouldPickupItem(itemstack)) {
						ItemStack stack1 = this.getEquipmentInSlot(0);
						if (stack1 != null) {
							this.entityDropItem(stack1, 0.0F);
						}
						this.setCurrentItemOrArmor(0, itemstack);
						entityitem.setDead();
					}

				}
			}
		}
		if (BALANCE.MOBPROP.VAMPIRE_MINION_REGENERATE_SECS >= 0 && this.ticksExisted % (BALANCE.MOBPROP.VAMPIRE_MINION_REGENERATE_SECS * 20) == 0 && (this.getLastAttackerTime() == 0 || this.getLastAttackerTime() - ticksExisted > 100)) {
			this.heal(2F);
		}
		super.onLivingUpdate();
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);
		IMinionCommand command = this.getCommand(nbt.getInteger("command_id"));
		if (command != null) {
			this.activateMinionCommand(command);
		}
		if (nbt.hasKey("CustomName", 8) && nbt.getString("CustomName").length() > 0) {
			this.tryToSetName(nbt.getString("CustomName"), null);
		}
	}

	/**
	 * Does not nothing, since minions should not be named normaly. Use {@link #tryToSetName(String, EntityPlayer)} instead
	 */
	@Override
	public void setCustomNameTag(String s) {

	}

	public void setOldVampireTexture(int oldVampireTexture) {
		this.oldVampireTexture = oldVampireTexture;
	}

	public void sync() {
		if (!worldObj.isRemote) {
			Helper.sendPacketToPlayersAround(new UpdateEntityPacket(this), this);
		}

	}

	/**
	 * Replaces {@link #setCustomNameTag(String)}.
	 * 
	 * @param name
	 * @param player
	 *            If this isn't null, checks if the player is the minions lord
	 * @return success
	 */
	public boolean tryToSetName(String name, @Nullable EntityPlayer player) {
		if (player == null || MinionHelper.isLordSafe(this, player)) {
			super.setCustomNameTag(name);
			return true;
		}
		return false;
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);
		nbt.setInteger("command_id", getActiveCommand().getId());
	}

	@Override
	public void writeFullUpdateToNBT(NBTTagCompound nbt) {
		nbt.setInteger("oldvampire", oldVampireTexture);
		nbt.setInteger("active_command_id", activeCommand.getId());
		writeUpdateToNBT(nbt);
	}

	@Override
	public boolean writeToNBTOptional(NBTTagCompound nbt) {
		if (shouldBeSavedWithLord()) {
			return false;
		}
		return super.writeToNBTOptional(nbt);
	}

	/**
	 * Can be used by child classes to write info to {@link #writeFullUpdateToNBT(NBTTagCompound)}
	 * 
	 * @param nbt
	 */
	protected void writeUpdateToNBT(NBTTagCompound nbt) {

	}

	@Override
	public int getTalkInterval() {
		return 2000;
	}
}
