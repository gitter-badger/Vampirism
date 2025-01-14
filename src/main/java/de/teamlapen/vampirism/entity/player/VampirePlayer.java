package de.teamlapen.vampirism.entity.player;

import com.google.common.base.Predicate;
import de.teamlapen.vampirism.Configs;
import de.teamlapen.vampirism.ModItems;
import de.teamlapen.vampirism.ModPotion;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.block.BlockCoffin;
import de.teamlapen.vampirism.entity.*;
import de.teamlapen.vampirism.entity.minions.*;
import de.teamlapen.vampirism.entity.minions.SaveableMinionHandler.Call;
import de.teamlapen.vampirism.entity.player.skills.ILastingSkill;
import de.teamlapen.vampirism.entity.player.skills.ISkill;
import de.teamlapen.vampirism.entity.player.skills.Skills;
import de.teamlapen.vampirism.item.ItemBloodBottle;
import de.teamlapen.vampirism.item.ItemVampireArmor;
import de.teamlapen.vampirism.network.SpawnParticlePacket;
import de.teamlapen.vampirism.network.UpdateEntityPacket;
import de.teamlapen.vampirism.network.UpdateEntityPacket.ISyncableExtendedProperties;
import de.teamlapen.vampirism.util.*;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S0APacketUseBed;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.*;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * IExtendedEntityPropertiesClass which extends the EntityPlayer with vampire properties
 * 
 * @author Maxanier
 */
public class VampirePlayer implements ISyncableExtendedProperties, IMinionLord {
	public class BloodStats {
		private float bloodExhaustionLevel;
		private float bloodSaturationLevel;
		private int bloodTimer;
		private int prevBloodLevel;
		private int bloodToAdd;

		private final float maxExhaustion = 40F;

		/**
		 * Adds blood to the players bar
		 * 
		 * @param amount
		 * @return The amount which could not be added
		 */
		public int addBlood(int amount) {
			int oldBlood = getBlood();

			// Adds the blood
			int bloodToAdd = Math.min(amount, MAXBLOOD - oldBlood);
			changeBlood(bloodToAdd);
			// Add saturation effect
			this.bloodSaturationLevel = (float) Math.min(bloodSaturationLevel + bloodToAdd * BALANCE.BLOOD_SATURATION * 2.0F, oldBlood + bloodToAdd);

			// Calculate the amount of left blood and handles it
			int bloodLeft = amount - bloodToAdd;

			return bloodLeft;
		}

		public void addExhaustion(float amount) {
			if (isSkillActive(Skills.vampireRage)) {
				amount = amount * 1.5F;
			}
			this.bloodExhaustionLevel = Math.min(bloodExhaustionLevel + amount, maxExhaustion);
		}

		/**
		 * Changes the blood amount on next update
		 * 
		 * @param amount
		 *            Amount to add or remove (+/-)
		 * @return
		 */
		private void changeBlood(int amount) {
			bloodToAdd += amount;
		}

		/**
		 * Removes blood from the vampires blood level
		 * 
		 * @param a
		 *            amount
		 * @return whether the vampire had enough blood or not
		 */
		public boolean consumeBlood(int a) {
			int blood = getBlood();
			int bloodToRemove = Math.min(a, blood);

			changeBlood(-bloodToRemove);
			return bloodToRemove <= blood;
		}

		public int getBloodLevel() {
			return getBlood();
		}

		@SideOnly(Side.CLIENT)
		public int getPrevBloodLevel() {
			return prevBloodLevel;
		}

		/**
		 * Updates players bloodlevel. Working similar to player foodstats
		 */
		private synchronized void onUpdate() {
			if (player.worldObj.isRemote) {
				return;
			}
			if (player.getFoodStats().getFoodLevel() < 10) {
				player.getFoodStats().addStats(5, 1);
			}

			EnumDifficulty enumdifficulty = player.worldObj.getDifficulty();

			int newBloodLevel = getBlood();
			newBloodLevel = Math.min(newBloodLevel + bloodToAdd, MAXBLOOD);
			if (newBloodLevel < 0)
				newBloodLevel = 0;
			bloodToAdd = 0;
			int playerSaturation = 1;
			if (player.isPotionActive(ModPotion.saturation.id)) {
				playerSaturation = player.getActivePotionEffect(ModPotion.saturation).getAmplifier() + 2;
			}
			if (this.bloodExhaustionLevel > BALANCE.BLOOD_EXH_PER_BL * playerSaturation) {
				this.bloodExhaustionLevel -= BALANCE.BLOOD_EXH_PER_BL * playerSaturation;

				if (this.bloodSaturationLevel > 0.0F) {
					this.bloodSaturationLevel = Math.max(bloodSaturationLevel - 1.0F, 0F);
				} else if (enumdifficulty != EnumDifficulty.PEACEFUL) {

					newBloodLevel = (Math.max(newBloodLevel - 1, 0));
				}
			}

			if (player.worldObj.getGameRules().getGameRuleBooleanValue("naturalRegeneration") && newBloodLevel >= 0.9 * MAXBLOOD && player.shouldHeal()) {
				++this.bloodTimer;
				if (this.bloodTimer >= 80) {
					player.heal(1.0F);
					this.addExhaustion(3.0F);
					this.bloodTimer = 0;
				}
			} else if (newBloodLevel <= 0) {
				++this.bloodTimer;
				if (this.bloodTimer >= 80) {
					if (player.getHealth() > 10.0F || enumdifficulty == EnumDifficulty.HARD || player.getHealth() > 1.0F && enumdifficulty == EnumDifficulty.NORMAL) {
						player.attackEntityFrom(DamageSource.starve, 1.0F);
					}
					this.bloodTimer = 0;
				}
			} else {
				this.bloodTimer = 0;
			}
			setBloodData(newBloodLevel);
		}

		private void readNBT(NBTTagCompound nbt) {
			if (nbt.hasKey("bloodTimer")) {
				bloodTimer = nbt.getInteger("bloodTimer");
				bloodExhaustionLevel = nbt.getFloat("bloodExhaustionLevel");
				bloodSaturationLevel = nbt.getFloat("bloodSaturationLevel");
			}
		}

		private void writeNBT(NBTTagCompound nbt) {
			nbt.setInteger("bloodTimer", bloodTimer);
			nbt.setFloat("bloodExhaustionLevel", bloodExhaustionLevel);
			nbt.setFloat("bloodSaturationlevel", bloodSaturationLevel);
		}

	}

	public final static String EXT_PROP_NAME = "VampirePlayer";

	public final static String TAG = "VampirePlayer";

	public final static int MAXBLOOD = 20;

	private static final String KEY_VAMPIRE_LORD = "vampire_lord";

	private static final String KEY_COMEBACK_CALL = "l_cbc";

	private static final String KEY_VISION = "vision";

	@SideOnly(Side.CLIENT)
	private final static ResourceLocation minionCommandIconLoc = new ResourceLocation(REFERENCE.MODID + ":textures/gui/minion_commands.png");

	private static final String KEY_MINIONS = "minions";

	private int biteCooldown;

	private static final int BITE_COOLDOWN = 15;

	/**
	 * 
	 * @param player
	 * @return VampirePlayer property of player
	 */
	public static VampirePlayer get(EntityPlayer player) {
		return (VampirePlayer) player.getExtendedProperties(VampirePlayer.EXT_PROP_NAME);
	}

	/**
	 * Handles player loading and syncing on world join. Only called server side
	 * 
	 * @param player
	 */
	public static void onPlayerJoinWorld(EntityPlayer player) {
		VampirePlayer p = VampirePlayer.get(player);
		for (int i = 0; i < p.skillTimer.length; i++) {
			if (p.skillTimer[i] > 0) {
				((ILastingSkill) Skills.getSkill(i)).onReActivated(p, player);
			}
		}
		p.refreshVampireLordState();
		p.sync(false);
	}

	/**
	 * Registers vampire property to player
	 * 
	 * @param player
	 */
	public static void register(EntityPlayer player) {
		player.registerExtendedProperties(VampirePlayer.EXT_PROP_NAME, new VampirePlayer(player));
	}

	public boolean sleepingCoffin = false;

	private final EntityPlayer player;

	private final String KEY_LEVEL = "level";

	private final String KEY_BLOOD = "blood";

	private final String KEY_AUTOFILL = "autofill";

	private final String KEY_SKILLS = "skills";

	private final String KEY_EXTRADATA = "extra";

	private final BloodStats bloodStats;

	private int level;

	private int[] skillTimer;

	private boolean dirty = false;

	private boolean autoFillBlood;

	private int vision;

	private EntityLivingBase minionTarget;

	private final SaveableMinionHandler minionHandler;

	private boolean skipFallDamageReduction = false;

	private boolean vampireLord = false;

	private boolean batTransformed = false;

	private int ticksInSun = 0;

	private long lastRemoteMinionComebackCall = 0;

	private NBTTagCompound extraData;

	public VampirePlayer(EntityPlayer player) {
		this.player = player;
		this.player.getDataWatcher().addObject(Configs.player_blood_watcher, MAXBLOOD);
		bloodStats = new BloodStats();
		autoFillBlood = true;
		vision = 1;
		skillTimer = new int[Skills.getSkillCount()];
		extraData = new NBTTagCompound();
		minionHandler = new SaveableMinionHandler(this);
	}

	public void copyFrom(EntityPlayer original) {
		NBTTagCompound nbt = new NBTTagCompound();
		VampirePlayer.get(original).saveNBTData(nbt);
		this.loadNBTData(nbt);
	}

	/**
	 * Tries to fill blood into blood bottles in the hotbar or tries to convert glas bottles from the hotbar to blood bottles
	 * 
	 * @param amt
	 */
	protected void fillBloodIntoInventory(int amt) {
		if (amt <= 0)
			return;
		ItemStack stack = ItemBloodBottle.getBloodBottleInInventory(player.inventory, true);
		if (stack != null) {
			fillBloodIntoInventory(ItemBloodBottle.addBlood(stack, amt));
		} else {
			ItemStack glas = ItemBloodBottle.getGlasBottleInInventory(player.inventory);
			if (glas != null) {
				ItemStack bloodBottle = new ItemStack(ModItems.bloodBottle, 1, 0);
				amt = ItemBloodBottle.addBlood(bloodBottle, amt);
				player.inventory.consumeInventoryItem(Items.glass_bottle);
				if (!player.inventory.addItemStackToInventory(bloodBottle)) {
					player.dropPlayerItemWithRandomChoice(bloodBottle, false);
				}
				if (amt > 0)
					fillBloodIntoInventory(amt);
			}
		}
	}



	@SideOnly(Side.CLIENT)
	public List<IPieElement> getAvailableMinionCalls() {
		List<IPieElement> list = new ArrayList<IPieElement>();
		if (this.isVampireLord()) {
			list.add(new DefaultPieElement(1, "minioncommand.vampirism.comeback", 128, 0, minionCommandIconLoc, new float[]{1, 1, 0.05F}));
			list.add(new DefaultPieElement(2, "minioncommand.vampirism.defendlord", 64, 0, minionCommandIconLoc, new float[] { 0.88F, 0.45F, 0 }));
			list.add(new DefaultPieElement(5, "minioncommand.vampirism.justfollow", 112, 0, minionCommandIconLoc, new float[]{0.88F, 0.45F, 0}));
			if (this.getMinionHandler().getMinionCount() > 0) {
				list.add(new DefaultPieElement(3, "minioncommand.vampirism.attackhostilenoplayers", 0, 0, minionCommandIconLoc, new float[]{0.6F, 0.3F, 0.01F}));
				list.add(new DefaultPieElement(4, "minioncommand.vampirism.attackhostile", 32, 0, minionCommandIconLoc, new float[] { 0.6F, 0.3F, 0.01F }));
			}

		}
		return list;
	}

	/**
	 * @return The current blood level
	 */
	public int getBlood() {
		return this.player.getDataWatcher().getWatchableObjectInt(Configs.player_blood_watcher);
	}

	public BloodStats getBloodStats() {
		return bloodStats;
	}

	public NBTTagCompound getExtraDataTag() {
		return extraData;
	}

	@Override
	public long getLastComebackCall() {
		return this.lastRemoteMinionComebackCall;
	}

	/**
	 * 
	 * @return Vampire level of the player
	 */
	public int getLevel() {
		return this.level;
	}

	@Override
	public int getMaxMinionCount() {
		return Math.round(getLevel() / 3F) + (this.isVampireLord() ? 4 : 1);
	}

	@Override
	public SaveableMinionHandler getMinionHandler() {
		return this.minionHandler;
	}

	/**
	 * Returns the number of minions which can be recruited
	 * 
	 * @param notify
	 *            If true the player is notified, if he cannot controll any more minions
	 * @return
	 */
	public int getMinionsLeft(boolean notify) {
		int left = minionHandler.getMinionsLeft();
		if (notify && left == 0) {
			player.addChatMessage(new ChatComponentTranslation("text.vampirism.no_more_minions"));
		}
		return left;
	}

	@Override
	public EntityLivingBase getMinionTarget() {
		if (this.minionTarget != null) {
			return minionTarget;
		}
		if (player.getLastAttackerTime() < player.ticksExisted + 200) {
			return player.getLastAttacker();
		}
		return null;
	}

	@Override
	public EntityLivingBase getRepresentingEntity() {
		return player;
	}

	/**
	 * Returns the skill time for rendering
	 * 
	 * @param id
	 * @return
	 */
	@SideOnly(Side.CLIENT)
	public int getSkillTime(int id) {
		if (id >= 0) {
			return this.skillTimer[id];
		}
		return 0;
	}

	/**
	 * The TicksInSun value is increased every tick in the sun and decreased every tick if not in sun. It always stays >=0
	 * 
	 * @return TicksInSun, if the player wont receive damage returns 0
	 */
	public int getSunDamageTicksInSun() {
		if (getLevel() < 3)
			return 0;
		return this.ticksInSun;
	}

	@Override
	public double getTheDistanceSquared(Entity e) {
		return player.getDistanceSqToEntity(e);
	}

	@Override
	public int getTheEntityID() {
		return player.getEntityId();
	}

	@Override
	public UUID getThePersistentID() {
		return player.getPersistentID();
	}

	public boolean gettingSundamage() {
		if (player.worldObj != null && player.worldObj.provider.getDimensionId() == 0) {
			if (player.worldObj.canBlockSeeSky(player.getPosition())) {
				if(Helper.isEntityInVampireBiome(player))return false;
				return VampirismMod.isSunDamageTime(player.worldObj);
			}
		}

		return false;
	}

	public int getVision() {
		return vision;
	}

	private void handleSunDamage() {
		if (ticksInSun < 101) {
			ticksInSun++;
		}
		if(player.capabilities.isCreativeMode)return;
		/** Non programmatically reference to #getSunDamageTicksInSun */
		int type = Math.min(3, Math.round(getLevel() / 2F - 0.51F));
		if (player.isPotionActive(ModPotion.sunscreen) && type > 0)
			type--;

		if (type > 0) {
			long t = player.worldObj.getTotalWorldTime();
			boolean armor = ItemVampireArmor.isFullyWorn(player);
			if ((t % 250 == 0 || ticksInSun == 1) && !armor) {
				player.addPotionEffect(new PotionEffect(Potion.confusion.id, 180));
			}
			if (type > 1) {
				if (t % 30 == 0) {
					player.addPotionEffect(new PotionEffect(Potion.weakness.id, 30, 1));
				}

				if (type > 2 && ticksInSun >= 100) {
					if (t % 40 == 0) {
						float damage = (float) BALANCE.VAMPIRE_PLAYER_SUN_DAMAGE;
						if (isVampireLord())
							damage *= 1.8F;
						if (armor)
							damage *= 0.6F;
						player.attackEntityFrom(VampirismMod.sunDamage, damage);
					}
				}

			}
		}
	}

	@Override
	public void init(Entity entity, World world) {

	}

	/**
	 * @return true if auto fill of blood bottle enabled
	 */
	public boolean isAutoFillBlood() {
		return autoFillBlood;
	}

	private boolean isRemote() {
		return player.worldObj.isRemote;
	}

	public boolean isSkillActive(int id) {
		if (id >= skillTimer.length) {
			Logger.w(TAG, "The skill with id " + id + " doesn't exist");
			return false;
		}
		return (skillTimer[id] > 0);
	}

	public boolean isSkillActive(ISkill s) {
		if (s == null)
			return false;
		return isSkillActive(s.getId());
	}

	@Override
	public boolean isTheEntityAlive() {
		return player.isEntityAlive();
	}

	public boolean isVampireLord() {
		return vampireLord;
	}

	public void levelUp() {
		int level = getLevel();
		level++;
		setLevel(level);
	}

	@Override
	public void loadNBTData(NBTTagCompound compound) {
		NBTTagCompound properties = (NBTTagCompound) compound.getTag(EXT_PROP_NAME);
		if (properties == null) {
			Logger.i(TAG, "VampirePlayer data for %s cannot be loaded. It probably does not exist", player);
			return;
		}
		setBloodData(properties.getInteger(KEY_BLOOD));
		level = properties.getInteger(KEY_LEVEL);
		int[] temp = properties.getIntArray(KEY_SKILLS);
		if (temp.length == Skills.getSkillCount()) {
			skillTimer = temp;
		} else {
			Logger.w(TAG, "Loaded skill timers have a different size than the existing skills");
			skillTimer = new int[Skills.getSkillCount()];
		}
		if (properties.hasKey(KEY_AUTOFILL)) {
			setAutoFillBlood(properties.getBoolean(KEY_AUTOFILL));
		}
		if (properties.hasKey(KEY_VISION)) {
			setVision(properties.getInteger(KEY_VISION));
		}
		if (properties.hasKey(KEY_EXTRADATA)) {
			extraData = properties.getCompoundTag(KEY_EXTRADATA);
		}
		this.lastRemoteMinionComebackCall = properties.getLong(KEY_COMEBACK_CALL);

		this.bloodStats.readNBT(properties);
		PlayerModifiers.applyModifiers(level, player);

		minionHandler.loadMinions(properties.getTagList(KEY_MINIONS, 10));

	}

	@Override
	public void loadUpdateFromNBT(NBTTagCompound nbt) {

		if (nbt.hasKey("level")) {
			this.setLevel(nbt.getInteger("level"));
		}
		if (nbt.hasKey("timers")) {
			this.skillTimer = nbt.getIntArray("timers");
		}
		if (nbt.hasKey("lord")) {
			this.vampireLord = nbt.getBoolean("lord");
		}
		if (nbt.hasKey("sleepingCoffin"))
			this.sleepingCoffin = nbt.getBoolean("sleepingCoffin");
		if (nbt.hasKey("vision"))
			this.setVision(nbt.getInteger("vision"));

	}

	private void looseLevel() {
		int level = getLevel();
		if (level > 1) {
			setLevel(level - 1);
		}
	}

	private void makeVampireToMinion(EntityVampire e) {
		if (getMinionsLeft(true) == 0)
			return;
		EntityVampireMinion m = (EntityVampireMinion) EntityList.createEntityByName(REFERENCE.ENTITY.VAMPIRE_MINION_SAVEABLE_NAME, e.worldObj);
		m.copyLocationAndAnglesFrom(e);
		m.setLord(this);
		m.setOldVampireTexture(e.getEntityId() % 4);
		e.setDead();
		e.worldObj.spawnEntityInWorld(m);
	}

	public void onCallActivated(int i) {
		Logger.d(TAG, "Minion call %d received", i);
		switch (i) {
		case 1:
			this.lastRemoteMinionComebackCall = System.currentTimeMillis();
			break;
		case 2:
			minionHandler.notifyCall(Call.DEFEND_LORD);
			for (VampireMob m : MinionHelper.getNearMobMinions(this, 20)) {
				m.activateMinionCommand(m.getCommand(0));
			}
			break;
		case 3:
			minionHandler.notifyCall(Call.ATTACK_NON_PLAYER);
			break;
		case 4:
			minionHandler.notifyCall(Call.ATTACK);
			break;
		case 5:
			minionHandler.notifyCall(Call.FOLLOW);
			for (VampireMob m : MinionHelper.getNearMobMinions(this, 20)) {
				m.activateMinionCommand(m.getCommand(1));
			}
			break;
		default:
		}
	}

	public void onChangedDimension(int from, int to) {
		minionHandler.teleportMinionsToLord();
	}

	public void onDeath(DamageSource source) {
		if (source instanceof EntityDamageSource) {
			Entity src = source.getEntity();
			if (src instanceof EntityVampireHunter && BALANCE.VAMPIRE_PLAYER_LOOSE_LEVEL) {
				looseLevel();
			}
			if (isVampireLord()) {
				EntityLivingBase old = null;
				if (src instanceof EntityVampire) {
					old = (EntityLivingBase) src;
				} else if(src instanceof EntityPlayer){
					old= (EntityLivingBase) src;
				} else if (src instanceof IMinion) {
					IMinionLord l = ((IMinion) src).getLord();
					if (l != null) {
						old = l.getRepresentingEntity();
					}
				} else if (src instanceof EntityCreature && VampireMob.get((EntityCreature) src).isMinion()) {
					IMinionLord l = (VampireMob.get((EntityCreature) src)).getLord();
					if (l != null) {
						old = l.getRepresentingEntity();
					}
				}
				if (old != null) {
					if (old instanceof EntityPlayer) {
						VampireLordData.get(player.worldObj).replaceLord(player, (EntityPlayer) old);

					} else {
						EntityDracula dracula = (EntityDracula) EntityList.createEntityByName(REFERENCE.ENTITY.DRACULA_NAME, old.worldObj);
						dracula.copyLocationAndAnglesFrom(old);
						dracula.makeDisappear();
						old.worldObj.spawnEntityInWorld(dracula);
						old.setDead();
						VampireLordData.get(player.worldObj).makeNoLord(player, "text.vampirism.lord.vampire_kill_replace");
					}

				}

			}
			if (player instanceof EntityPlayerMP && (src instanceof EntityDracula || src instanceof EntityVampireMinion)) {
				src.onKillEntity(player);
			}
		}
		for (int i = 0; i < skillTimer.length; i++) {
			if (skillTimer[i] > 0) {
				skillTimer[i] = -Skills.getSkill(i).getCooldown();
				((ILastingSkill) Skills.getSkill(i)).onDeactivated(this, player);

			}
		}
		this.bloodStats.addBlood(MAXBLOOD);
	}

	/**
	 * If this returns true, the damage is skipped
	 * 
	 * @param source
	 * @param amount
	 * @return
	 */
	public boolean onEntityAttacked(DamageSource source, float amount) {
		if (source.getEntity() instanceof EntityVampireBase && getLevel() == 0) {
			// Since the method seems to be called 4 times probability is
			// decreased by the factor 4
			if (player.worldObj.rand.nextInt(BALANCE.VAMPIRE_PLAYER_SANGUINARE_PROB * 4) == 0) {
				if (!player.isPotionActive(ModPotion.sanguinare)) {
					player.addPotionEffect(new PotionEffect(ModPotion.sanguinare.id, BALANCE.VAMPIRE_PLAYER_SANGUINARE_DURATION * 20));
				}

			}
		}
		if (source.getEntity() instanceof EntityLivingBase && getLevel() > 0) {
			this.minionTarget = (EntityLivingBase) source.getEntity();
			if (MinionHelper.isLordSafe(minionTarget, this)) {
				this.minionTarget = null;
			}
			return false;
		}
		if (DamageSource.fall.equals(source) && !this.skipFallDamageReduction) {

			float i = amount - (getLevel() / 3) - 1;
			if (i > 0) {
				this.skipFallDamageReduction = true;
				player.attackEntityFrom(DamageSource.fall, i);
				this.skipFallDamageReduction = false;
			}
			return true;

		}

		if (sleepingCoffin && !player.isEntityInvulnerable(source) && !(player.capabilities.disableDamage && !source.canHarmInCreative())) {
			this.wakeUpPlayer(false, true, false, false);
		}
		return false;
	}

	public void onPlayerLoggedIn() {
		minionHandler.addLoadedMinions();
	}

	public void onPlayerLoggedOut() {
		minionHandler.killMinions(true);
	}

	public void onSkillToggled(int i) {
		ISkill s = Skills.getSkill(i);
		if (s == null)
			return;
		int t = skillTimer[i];
		if (t > 0) {// Running, only for lasting skills
			skillTimer[i] = Math.min((-s.getCooldown()) + t, 0);
			((ILastingSkill) s).onDeactivated(this, player);
		} else if (t == 0) {// Ready
			int r = s.canUse(this, player);
			if (r == -1) {
				player.addChatMessage(new ChatComponentTranslation("text.vampirism.skill.deactivated_by_serveradmin"));
			} else if (r == 0) {
				player.addChatMessage(new ChatComponentTranslation("text.vampirism.skill.level_to_low"));
			} else if (r == 1) {
				if (s.onActivated(this, player)) {
					if (s instanceof ILastingSkill) {
						ILastingSkill ls = (ILastingSkill) s;
						skillTimer[i] = ls.getDuration(getLevel());
					} else {
						skillTimer[i] = -s.getCooldown();
					}
				}

			}
		} else {// In cooldown
			player.addChatMessage(new ChatComponentTranslation("text.vampirism.skill.cooldown_not_over"));
		}
		dirty = true;
	}

	public void onToggleAutoFillBlood() {
		if (autoFillBlood) {
			autoFillBlood = false;
			this.player.addChatMessage(new ChatComponentTranslation("text.vampirism.auto_fill_disabled"));
		} else {
			autoFillBlood = true;
			this.player.addChatMessage(new ChatComponentTranslation("text.vampirism.auto_fill_enabled"));
		}
	}

	public void onToggleVision() {
		if (getLevel() == 0) {
			player.addChatMessage(new ChatComponentTranslation("text.vampirism.skill.level_to_low"));
			return;
		}
		int v = getVision() + 1;
		if (v == 2 && Configs.disable_blood_vision) v++;
		if (v > 2)
			v = 0;
		this.setVision(v);
		this.sync(false);
		if (v == 0) {
			player.addChatMessage(new ChatComponentTranslation("text.vampirism.normal_vision"));
		} else if (v == 1) {
			player.addChatMessage(new ChatComponentTranslation("text.vampirism.night_vision"));
		} else {
			player.addChatMessage(new ChatComponentTranslation("text.vampirism.blood_vision"));
		}

	}

	/**
	 * Called every LivingEntityUpdate, returns immediately if level =0;
	 */
	public void onUpdate() {
		if (this.sleepingCoffin && player.isPlayerSleeping()) {
			if (!player.worldObj.isRemote)
				player.motionY = 0;
			else if (player.posY > Math.floor(player.posY) + 0.2)
				player.motionY = -0.05;
			else
				player.motionY = 0;

		}
		if (getLevel() <= 0) {
			PotionEffect sang = player.getActivePotionEffect(ModPotion.sanguinare);
			if (sang != null) {
				if (sang.getDuration() == 1) {
					this.levelUp();
					player.addPotionEffect(new PotionEffect(ModPotion.saturation.id, 300, 2));
					player.addPotionEffect(new PotionEffect(Potion.resistance.id, 300));
				}
			}
			return;
		}
		this.bloodStats.onUpdate();
		this.minionHandler.checkMinions();
		if (!player.worldObj.isRemote) {
			if (gettingSundamage()) {
				handleSunDamage();
			} else {
				if (ticksInSun > 0) {
					ticksInSun--;
				}

			}
			if (biteCooldown > 0) biteCooldown--;
			if (player.isPotionActive(ModPotion.sanguinare.id)) {
				player.removePotionEffect(ModPotion.sanguinare.id);
			}
			if (sleepingCoffin && !this.player.worldObj.isDaytime()) {
				this.wakeUpPlayer(true, false, true, true);
			}
		} else {
			if (gettingSundamage()) {
				if (ticksInSun < 101) {
					ticksInSun++;
				}
			} else {
				if (ticksInSun > 0) {
					ticksInSun--;
				}
			}
		}

		/**
		 * Loop through all skill timers and update them and their tick time
		 */
		for (int i = 0; i < skillTimer.length; i++) {
			int t = skillTimer[i];
			if (t != 0) {// If timer equals 0, there is nothing to do
				if (t < 0) {
					skillTimer[i] = ++t;
				} else {
					skillTimer[i] = --t;
					ILastingSkill s = (ILastingSkill) Skills.getSkill(i);
					if (t == 0) {
						skillTimer[i] = -s.getCooldown();
						if (!isRemote()) {
							s.onDeactivated(this, player);
							dirty = true;
						}
					} else {
						if (s.onUpdate(this, player)) {
							skillTimer[i] = 1;
						}
					}
				}

			}
		}

		if(!player.worldObj.isRemote&&vampireLord){
			if(getLevel()<REFERENCE.HIGHEST_REACHABLE_LEVEL){
				VampireLordData.get(player.worldObj).makeNoLord(player, "text.vampirism.lord.level_to_low");
			}
		}

		if (batTransformed != this.isSkillActive(Skills.batMode)) {
			batTransformed = !batTransformed;
			VampirismMod.proxy.setPlayerBat(player, batTransformed);
		}

		/**
		 * Check minion target
		 */
		if (minionTarget != null && !minionTarget.isEntityAlive()) {
			minionTarget = null;
		}
		if (dirty&&!player.worldObj.isRemote) {
			this.sync(true);
			dirty = false;
		}
	}

	@Override
	public void saveNBTData(NBTTagCompound compound) {
		NBTTagCompound properties = new NBTTagCompound();
		properties.setInteger(KEY_LEVEL, getLevel());
		properties.setInteger(KEY_BLOOD, getBlood());
		properties.setIntArray(KEY_SKILLS, skillTimer);
		properties.setBoolean(KEY_AUTOFILL, isAutoFillBlood());
		properties.setInteger(KEY_VISION, getVision());
		properties.setTag(KEY_EXTRADATA, extraData);
		properties.setTag(KEY_MINIONS, minionHandler.getMinionsToSave());
		properties.setLong(KEY_COMEBACK_CALL, lastRemoteMinionComebackCall);
		this.bloodStats.writeNBT(properties);
		compound.setTag(EXT_PROP_NAME, properties);

	}

	private void setAutoFillBlood(boolean value) {
		autoFillBlood = value;
	}

	/**
	 * DONT USE, only designed to be used at startup and by Bloodstats Try to use addBlood(int amount) or consumeBlood(int amount) instead
	 * 
	 * @param b
	 */
	private synchronized void setBloodData(int b) {
		this.player.getDataWatcher().updateObject(Configs.player_blood_watcher, b);

	}

	/**
	 * For testing only, make private later. This is the only method which should change the level. This method should execute all level related changes e.g. player modifiers Its syncs it with the
	 * client
	 * 
	 * @param l
	 */
	public void setLevel(int l) {
		if (l >= 0) {
			level = l;
			PlayerModifiers.applyModifiers(l, player);
			this.sync(true);
		}
	}

	/**
	 * Should only be called by @{@link VampireLordData}.
	 * Use @{@link VampireLordData#makeLord(EntityPlayer)} )} or @{@link VampireLordData#makeNoLord(EntityPlayer, String)} instead
	 * @param state
	 * @return will return false if the player cannot become a vampire lord
	 */
	public boolean setVampireLord(boolean state) {
		if (state && getLevel() < REFERENCE.HIGHEST_REACHABLE_LEVEL) {
			Logger.w(TAG, "Cannot become a vampire lord since the player has not reached the highest level");
			return false;
		}
		if(state!=vampireLord){
			this.vampireLord = state;
			this.sync(true);
		}
		return true;
	}

	private void setVision(int vision) {
		this.vision = vision;
	}

	/**
	 * puts player to sleep on specified coffin if possible
	 */
	public EntityPlayer.EnumStatus trySleepInCoffin(BlockPos pos) {
		// Logger.d("VampirePlayer", String.format(
		// "trySleepInCoffin called, x=%s, y=%s, z=%s, remote=%s", x, y, z, this.isRemote()));

		if (!this.player.worldObj.isRemote) {
			if (this.sleepingCoffin || !this.isTheEntityAlive()) {
				Logger.w(TAG, "Player seems to be either already sleeping or dead");
				return EntityPlayer.EnumStatus.OTHER_PROBLEM;
			}

			if (!this.player.worldObj.provider.isSurfaceWorld()) {
				Logger.w(TAG, "Not possible here");
				return EntityPlayer.EnumStatus.NOT_POSSIBLE_HERE;
			}

			if (!this.player.worldObj.isDaytime()) {
				return EntityPlayer.EnumStatus.NOT_POSSIBLE_NOW;
			}

			if (Math.abs(this.player.posX - pos.getX()) > 3.0D || Math.abs(this.player.posY - pos.getY()) > 2.0D || Math.abs(this.player.posZ - pos.getZ()) > 3.0D) {
				return EntityPlayer.EnumStatus.TOO_FAR_AWAY;
			}

			double d0 = 8.0D;
			double d1 = 5.0D;
			List list = this.player.worldObj.getEntitiesWithinAABB(EntityMob.class, new AxisAlignedBB(pos.add(-d0, -d1, -d0), pos.add(d0, d1, d0)), new Predicate<Entity>() {

				@Override
				public boolean apply(Entity entity) {
					if (!(entity instanceof EntityMob))
						return false;
					if (entity instanceof IMinion) {
						return !(((IMinion) entity).getLord() instanceof EntityPlayer);
					}
					if (VampireMob.get((EntityMob) entity).isMinion()) {
						return !(VampireMob.get((EntityMob) entity).getLord() instanceof EntityPlayer);
					}
					if (entity instanceof EntityVampire) {
						return getLevel() <= BALANCE.VAMPIRE_FRIENDLY_LEVEL || isVampireLord();
					}
					if (entity instanceof EntityVampireHunter) {
						return getLevel() >= BALANCE.VAMPIRE_HUNTER_ATTACK_LEVEL;
					}
					return true;
				}
			});

			if (!list.isEmpty()) {
				return EntityPlayer.EnumStatus.NOT_SAFE;
			}
		}

		if (this.player.isRiding()) {
			this.player.mountEntity(null);
		}

		Helper.Reflection.callMethod(Entity.class, this.player, Helper.Obfuscation.getPosNames("EntityPlayer/setSize"), new Class[]{float.class, float.class}, 0.2F, 0.2F);
		// this.player.setSize(0.2F, 0.2F);

		if (this.player.worldObj.isBlockLoaded(pos)) {
			EnumFacing enumFacing = ((BlockCoffin)player.worldObj.getBlockState(pos).getBlock()).getCoffinDirection(player.worldObj, pos);
			float xOffset = 0.5F;
			float zOffset = 0.5F;
			float yOffset = 0.5F;//0.6875

			switch (enumFacing) {
				case SOUTH:
					zOffset = 0.9F;
					break;
				case NORTH:
					zOffset = 0.1F;
					break;
				case WEST:
					xOffset= 0.1F;
					break;
				case EAST:
					xOffset = 0.9F;

//			OLD
//			case 0:
//				zOffset = 1.8F;
//				break;
//			case 3:
//				xOffset = 1.8F;
//				break;
//			case 2:
//				zOffset = -0.8F;
//				break;
//			case 1:
//				xOffset = -0.8F;
			}

			this.func_175139_a(enumFacing);
			this.player.setPosition(pos.getX() + xOffset, pos.getY() + yOffset, pos.getZ()+ zOffset);
			Logger.i("VampirePlayer", String.format("Setting player position, xOffset=%.3f, yOffset=%.3f, zOffset=%.3f", xOffset, yOffset, zOffset));
		} else {
			this.player.setPosition(pos.getX()+ 0.5F, pos.getY() + 0.9375F, pos.getZ() + 0.5F);
			Logger.i("VampirePlayer", "blockExists(x,y,z) was false, standard offsets");
		}

		S0APacketUseBed s0apacketusebed = new S0APacketUseBed((this.player), pos);
		((EntityPlayerMP) this.player).getServerForPlayer().getEntityTracker().func_151248_b((this.player), s0apacketusebed);
		((EntityPlayerMP) this.player).playerNetServerHandler.setPlayerLocation(((EntityPlayerMP) this.player).posX, ((EntityPlayerMP) this.player).posY, ((EntityPlayerMP) this.player).posZ,
				((EntityPlayerMP) this.player).rotationYaw, ((EntityPlayerMP) this.player).rotationPitch);
		((EntityPlayerMP) this.player).playerNetServerHandler.sendPacket(s0apacketusebed);

		// Following method will replace: this.player.sleeping = true;
		Helper.Reflection.setPrivateField(EntityPlayer.class, this.player, true, Helper.Obfuscation.getPosNames("EntityPlayer/sleeping"));
		this.sleepingCoffin = true;
		Logger.i("VampirePlayer", "sleepingCoffin=" + this.sleepingCoffin);
		// Following method will replace: this.player.sleepTimer = 0;
		Helper.Reflection.setPrivateField(EntityPlayer.class, this.player, 0, Helper.Obfuscation.getPosNames("EntityPlayer/sleepTimer"));

		this.player.playerLocation = pos;
		this.player.motionX = this.player.motionZ = this.player.motionY = 0.0D;

		if (!this.player.worldObj.isRemote) {
			this.player.worldObj.updateAllPlayersSleepingFlag();
		}

		VampirePlayer.get(player).sync(true);
		return EntityPlayer.EnumStatus.OK;
	}

	private void func_175139_a(EnumFacing enumFacing) {
		this.player.renderOffsetX=0F;
		this.player.renderOffsetZ=0F;

		switch (enumFacing){
			case SOUTH:
				this.player.renderOffsetZ=-1.8F;
				break;
			case NORTH:
				this.player.renderOffsetZ=1.8F;
				break;
			case WEST:
				this.player.renderOffsetX=1.8F;
				break;
			case EAST:
				this.player.renderOffsetX=-1.8F;
				break;
		}
	}


	/**
	 * Suck blood from an EntityLiving. Only sucks blood if health is low enough and if the entity has blood
	 * 
	 * @param e
	 *            Entity to suck blood from
	 */
	public void biteEntity(EntityLivingBase e) {
		if (e.worldObj.isRemote) {
			return;
		}
		if (getLevel() == 0) {
			return;
		}
		if (biteCooldown > 0) return;
		int blood = 0;
		BITE_TYPE type = determineByteType(e);
		if (type == BITE_TYPE.MAKE_MINION) {
			this.makeVampireToMinion((EntityVampire) e);
		} else if (type == BITE_TYPE.SUCK_BLOOD_PLAYER) {
			blood = VampirePlayer.get((EntityPlayer) e).bite();
		} else if (type == BITE_TYPE.SUCK_BLOOD) {
			blood = VampireMob.get((EntityCreature) e).bite(canTurnOthers());
		} else if (type == BITE_TYPE.ATTACK) {
			e.attackEntityFrom(DamageSource.causePlayerDamage(player), BALANCE.VAMPIRE_PLAYER_BITE_DMG);
			if (player.getRNG().nextInt(4) == 0) {
				player.addPotionEffect(new PotionEffect(Potion.poison.id, 60));
			}
		} else if (type == BITE_TYPE.NONE) {
			return;
		}
		biteCooldown = BITE_COOLDOWN;
		if (blood <= 0) return;

		if (blood > 0) {
			int amt = this.bloodStats.addBlood(blood);
			if (amt > 0 && isAutoFillBlood()) {
				fillBloodIntoInventory(amt);
			}
			VampirismMod.modChannel.sendToAll(new SpawnParticlePacket(EnumParticleTypes.CRIT_MAGIC, e.posX, e.posY, e.posZ, player.posX - e.posX, player.posY - e.posY, player.posZ - e.posZ, 10));
			VampirismMod.modChannel.sendTo(new SpawnParticlePacket(EnumParticleTypes.WATER_WAKE/*replace with custom particle packet*/, 0, 0, 0, 0, 0, 0, 10), (EntityPlayerMP) player);

		}
	}

	public BITE_TYPE determineByteType(EntityLivingBase entity) {
		if (entity instanceof EntityVampire && this.isVampireLord()) {
			PotionEffect p1 = entity.getActivePotionEffect(Potion.moveSlowdown);
			PotionEffect p2 = entity.getActivePotionEffect(Potion.jump);
			//Both should be true, but to increase compatibility one is enought
			if (p1 != null && p1.getAmplifier() == 10 || p2 != null && p2.getAmplifier() == 128) {

					return BITE_TYPE.MAKE_MINION;
			}
		}

		if (entity instanceof EntityPlayer) {
			if (((EntityPlayer) entity).capabilities.isCreativeMode || !MinecraftServer.getServer().isPVPEnabled()) {
				return BITE_TYPE.NONE;
			}
			if (this.canTurnOthers() && !Helper.canReallySee(entity, player, false)) {
				return BITE_TYPE.SUCK_BLOOD_PLAYER;
			}
			return BITE_TYPE.ATTACK;
		} else if (entity instanceof EntityCreature) {
			VampireMob mob = VampireMob.get((EntityCreature) entity);
			if (mob.getBlood() > 0) {
				return BITE_TYPE.SUCK_BLOOD;
			}

		}
		return BITE_TYPE.ATTACK;
	}

	public enum BITE_TYPE {
		ATTACK, SUCK_BLOOD, SUCK_BLOOD_PLAYER, MAKE_MINION, NONE
	}

	/**
	 * Suck blood from an EntityLiving belonging to the given id. Only sucks blood if health is low enough and if the entity has blood
	 * 
	 * @param entityId
	 *            Id of Entity to suck blood from
	 */
	public void biteEntity(int entityId) {
		Entity e = player.worldObj.getEntityByID(entityId);
		if (e != null && e instanceof EntityLivingBase) {
			if (e.getDistanceToEntity(player) <= ((EntityPlayerMP) player).theItemInWorldManager.getBlockReachDistance() + 2) {
				biteEntity((EntityLivingBase) e);
			} else {
				Logger.w(TAG, "Entity sent by client is not in reach " + entityId);
			}
		}
	}

	/**
	 * Bite the player himself.
	 *
	 * @return Amount of blood given
	 */
	private int bite() {
		if (getLevel() == 0) {
			int amt = player.getFoodStats().getFoodLevel();
			player.getFoodStats().setFoodLevel(0);
			player.addExhaustion(1000F);
			if (!player.isPotionActive(ModPotion.sanguinare)) {
				player.addPotionEffect(new PotionEffect(ModPotion.sanguinare.id, BALANCE.VAMPIRE_PLAYER_SANGUINARE_DURATION * 20));
			}
			return amt;
		}
		int amt = this.getBloodStats().getBloodLevel();
		this.getBloodStats().consumeBlood(amt);
		return amt;
	}

	/**
	 * Sends updates to the client
	 */
	public void sync(boolean all) {
		if (!player.worldObj.isRemote && player instanceof EntityPlayerMP) {
			if (all) {
				Helper.sendPacketToPlayersAround(new UpdateEntityPacket(this), player);
			} else {
				VampirismMod.modChannel.sendTo(new UpdateEntityPacket(this), (EntityPlayerMP) player);
			}

		}
	}

	/**
	 * Wakes the player up if he is sleeping in a coffin The last three variables are currently only used when vanilla is true
	 * 
	 * @param vanilla
	 *            Whether the vanilla wakeUp should be called as well
	 */
	public void wakeUpPlayer(boolean vanilla, boolean immediately, boolean updateWorld, boolean setSpawn) {
		this.sleepingCoffin = false;
		this.sync(true);
		if (vanilla) {
			player.wakeUpPlayer(immediately, updateWorld, setSpawn);
		}
	}

	public void refreshVampireLordState(){
		if (player.worldObj != null && !player.worldObj.isRemote) {
			vampireLord=VampireLordData.get(player.worldObj).isLord(player);
		}
	}

	@Override
	public void writeFullUpdateToNBT(NBTTagCompound tag) {
		tag.setInteger("level", getLevel());
		tag.setIntArray("timers", skillTimer);
		tag.setBoolean("lord", isVampireLord());
		tag.setBoolean("sleepingCoffin", sleepingCoffin);
		tag.setInteger("vision", getVision());
	}

	public boolean canTurnOthers() {
		return this.getLevel() >= BALANCE.VAMPIRE_PLAYER_MIN_TURN_LEVEL;
	}
}
