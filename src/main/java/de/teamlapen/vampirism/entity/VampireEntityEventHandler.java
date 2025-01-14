package de.teamlapen.vampirism.entity;

import com.google.common.base.Predicate;
import de.teamlapen.vampirism.Configs;
import de.teamlapen.vampirism.ModItems;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.entity.ai.EntityAIAvoidVampirePlayer;
import de.teamlapen.vampirism.generation.castle.CastlePositionData;
import de.teamlapen.vampirism.network.ISyncable;
import de.teamlapen.vampirism.network.RequestEntityUpdatePacket;
import de.teamlapen.vampirism.util.BALANCE;
import de.teamlapen.vampirism.util.DifficultyCalculator;
import de.teamlapen.vampirism.util.DifficultyCalculator.Difficulty;
import de.teamlapen.vampirism.util.DifficultyCalculator.IAdjustableLevel;
import de.teamlapen.vampirism.util.Logger;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.village.Village;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class VampireEntityEventHandler {

	@SubscribeEvent(receiveCanceled = true)
	public void onEntityConstructing(EntityConstructing event) {
		if (event.entity instanceof EntityCreature && VampireMob.get((EntityCreature) event.entity) == null) {
			VampireMob.register((EntityCreature) event.entity);
		}
	}

	@SubscribeEvent
	public void onEntityJoinWorld(EntityJoinWorldEvent event) {
		if (!event.entity.worldObj.isRemote && event.entity instanceof IAdjustableLevel) {
			IAdjustableLevel e = (IAdjustableLevel) event.entity;
			if (e.getLevel() == 0) {
				Difficulty d = DifficultyCalculator.getLocalDifficulty(event.world, event.entity.posX, event.entity.posZ, 10);
				if (d.isZero()) {
					d = DifficultyCalculator.getWorldDifficulty(event.entity.worldObj);
				}
				int l = e.suggestLevel(d);
				if (l > e.getMaxLevel()) {
					l = e.getMaxLevel();
				} else if (l < 1) {
					if (event.entity.worldObj.rand.nextBoolean()) {
						event.setCanceled(true);
					}
					l = 1;
				}
				e.setLevel(l);
			}
		}
		if (event.world.isRemote) {
			if (event.entity instanceof ISyncable || event.entity instanceof EntityCreature) {
				VampirismMod.modChannel.sendToServer(new RequestEntityUpdatePacket(event.entity));
			}
		}

		if (event.entity instanceof EntityHunterBase) {
			if (event.world.provider.getDimensionId() == VampirismMod.castleDimensionId) {
				event.entity.setDead();
			} else if (event.entity instanceof EntityVampireHunter) {
				// Set the home position of VampireHunters to a near village if one
				// is found
				EntityVampireHunter e = (EntityVampireHunter) event.entity;
				if (!e.isLookingForHome())
					return;

				if (event.world.villageCollectionObj != null) {
					Village v = event.world.villageCollectionObj.getNearestVillage(e.getPosition(), 20);
					if (v != null) {
						int r = v.getVillageRadius();
						//AxisAlignedBB box = AxisAlignedBB.getBoundingBox(v.getCenter().posX - r, 0, v.getCenter().posZ - r, v.getCenter().posX + r, event.world.getActualHeight(), v.getCenter().posZ + r);
						BlockPos cc = v.getCenter();
						e.setVillageArea(cc, r);
					}
				}
			}

		} else if (event.entity instanceof EntityIronGolem) {
			// Replace the EntityAINearestAttackableTarget of Irongolems, so
			// they do not attack VampireHunters
			EntityIronGolem golem = (EntityIronGolem) event.entity;
			EntityAITasks targetTasks = golem.targetTasks;
			if (targetTasks == null) {
				Logger.w("VampireEntityEventHandler", "Cannot change the target tasks of irongolem");
			} else {
				for (Object o : targetTasks.taskEntries) {
					EntityAIBase t = ((EntityAITasks.EntityAITaskEntry) o).action;
					if (t instanceof EntityAINearestAttackableTarget) {
						targetTasks.removeTask(t);
						targetTasks.addTask(3, new EntityAINearestAttackableTarget(golem, EntityLiving.class, 0, false, true, new Predicate() {

							@Override
							public boolean apply(Object entity) {
								return entity instanceof IMob && !(entity instanceof EntityHunterBase);
							}

						}));
						break;
					}
				}
			}
		} else if (event.entity instanceof EntityCreeper) {
			EntityCreeper creeper = (EntityCreeper) event.entity;
			EntityAITasks tasks = creeper.tasks;
			if (tasks == null) {
				Logger.w("VampireEntityEventHandler", "Cannot change the target tasks of creeper");
			} else {
				tasks.addTask(3, new EntityAIAvoidVampirePlayer(creeper, 12.0F, 1.0D, 1.2D, BALANCE.VAMPIRE_PLAYER_CREEPER_AVOID_LEVEL));
			}
		}
		else if(!event.world.isRemote&&event.entity instanceof EntityDracula){
			CastlePositionData.Position pos=CastlePositionData.get(event.world).findPosAt(MathHelper.floor_double(event.entity.posX),MathHelper.floor_double(event.entity.posZ),true);
			if(pos!=null){
				((EntityDracula)event.entity).makeCastleLord(pos);
			}
			else{
				Logger.w("EntityEventHandler","Dracula was spawned outside a castle");
			}
		} else if (!event.world.isRemote && event.entity instanceof EntityVampire) {
			if (CastlePositionData.get(event.world).isPosAt(MathHelper.floor_double(event.entity.posX), MathHelper.floor_double(event.entity.posZ))) {
				((EntityVampire) event.entity).makeCastleVampire();
			}
		} else if (event.entity instanceof EntityZombie) {
			try {
				((EntityZombie) event.entity).tasks.addTask(3, new EntityAIAttackOnCollide((EntityCreature) event.entity, EntityVampirism.class, 1.0F, false));
			} catch (Exception e) {
				Logger.e("EntityEventHandler", e, "Failed to add attack task to zombie %s", event.entity);
			}

		}
	}

	@SubscribeEvent
	public void onLivingDeathEvent(LivingDeathEvent event) {
		if (event.entity instanceof EntityCreature && !event.entity.worldObj.isRemote && BALANCE.DEAD_MOB_PROP > 0 && EntityDeadMob.canBecomeDeadMob((EntityCreature) event.entity)
				&& (BALANCE.DEAD_MOB_PROP == 0 || event.entity.worldObj.rand.nextInt(BALANCE.DEAD_MOB_PROP) == 0)) {
			event.entity.worldObj.spawnEntityInWorld(EntityDeadMob.createFromEntity((EntityCreature) event.entity));
		}
	}

	@SubscribeEvent
	public void onLivingDrops(LivingDropsEvent e) {
		if (e.entityLiving instanceof EntityVillager) {
			ItemStack stack = new ItemStack((Configs.disable_hunter ? ModItems.humanHeart : ModItems.weakHumanHeart), 1);
			e.drops.add(new EntityItem(e.entity.worldObj, e.entity.posX, e.entity.posY + 0.4, e.entity.posZ, stack));
		}
	}

	@SubscribeEvent
	public void onLivingUpdate(LivingUpdateEvent event) {
		if (event.entity instanceof EntityCreature) {
			VampireMob.get((EntityCreature) event.entity).onUpdate();
		}
	}

}
