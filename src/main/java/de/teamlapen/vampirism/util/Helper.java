package de.teamlapen.vampirism.util;

import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.biome.BiomeVampireForest;
import de.teamlapen.vampirism.network.SpawnCustomParticlePacket;
import de.teamlapen.vampirism.villages.VillageVampire;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.*;
import net.minecraft.village.Village;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.commons.lang3.ArrayUtils;
import scala.actors.threadpool.Arrays;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Helper {
	public static class Obfuscation {
		private static final HashMap<String, String[]> posNames = new HashMap<String, String[]>();

		private static void add(String key, String... value) {
			posNames.put(key, value);
		}

		public static void fillMap() {
			add("EntityPlayer/setSize", "setSize", "func_70105_a");
			add("EntityPlayer/sleeping", "sleeping", "field_71083_bS");
			add("EntityPlayer/sleepTimer", "sleepTimer", "field_71076_b");
			add("Minecraft/fileAssets", "fileAssets", "field_110446_Y");
			add("TileEntityBeacon/field_146015_k", "field_146015_k");
			add("Entity/setSize", "setSize", "func_70105_a");
			add("EntityPlayer/updateItemUse", "updateItemUse", "func_71010_c");
			add("EntityRenderer/loadShader","loadShader","func_175069_a");
			add("EntityRenderer/theShaderGroup","theShaderGroup","field_147707_d");
		}

		public static String[] getPosNames(String key) {
			return posNames.get(key);
		}
	}

	public static class Reflection {

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public static Object callMethod(Class cls, Object obj, String[] methodName, Class[] paramtype, Object... param) {
			if (param != null && paramtype.length != param.length) {
				Logger.w("ReflectCallMethod", "Param count doesnt fit paramtype count");
				return null;
			}

			try {
				Method method = ReflectionHelper.findMethod(cls, obj, methodName, paramtype);
				return method.invoke(obj, param);

			} catch (Exception e) {
				Logger.e("ReflectCallMethod", "Failed to invoke method");
				e.printStackTrace();
				return null;
			}
		}

		@SuppressWarnings("rawtypes")
		public static Object callMethod(Object obj, String[] methodName, Class[] paramtype, Object... param) {
			return Reflection.callMethod(obj.getClass(), obj, methodName, paramtype, param);
		}

		/**
		 * Create Class Array
		 * 
		 * @param objects
		 * @return
		 */
		@SuppressWarnings("rawtypes")
		public static Class[] createArray(Class... objects) {
			return objects;
		}

		@SuppressWarnings("rawtypes")
		public static Object getPrivateFinalField(Class cls, Object obj, String... fieldname) {
			try {
				Field privateStringField = ReflectionHelper.findField(cls, fieldname);
				return privateStringField.get(obj);
			} catch (Exception e) {
				Logger.e("Reflection", "Failed to get " + Arrays.toString(fieldname) + " from " + obj.toString() + " of class " + cls.getCanonicalName(), e);
				return null;
			}
		}

		@SuppressWarnings("rawtypes")
		public static void setPrivateField(Class cls, Object obj, Object value, String... fieldname) {
			try {
				Field privateStringField = ReflectionHelper.findField(cls, fieldname);
				privateStringField.set(obj, value);
			} catch (Exception e) {
				Logger.e("Reflection", "Failed to get " + Arrays.toString(fieldname) + " from " + obj.toString() + " of class " + cls.getCanonicalName(), e);
				return;
			}
		}
	}

	public static String entityToString(Entity e) {
		if (e == null) {
			return "Entity is null";
		}
		return e.toString();// +" at "+e.posX+" "+e.posY+" "+e.posZ+" Id "+e.getEntityId();
	}

	/**
	 * Gets players looking spot (blocks only).
	 * 
	 * @param player
	 * @param restriction
	 *            Max distance or 0 for player reach distance or -1 for not restricted
	 * @return The position as a MovingObjectPosition, null if not existent cf: https ://github.com/bspkrs/bspkrsCore/blob/master/src/main/java/bspkrs /util/CommonUtils.java
	 */
	public static MovingObjectPosition getPlayerLookingSpot(EntityPlayer player, double restriction) {
		float scale = 1.0F;
		float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * scale;
		float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * scale;
		double x = player.prevPosX + (player.posX - player.prevPosX) * scale;
		double y = player.prevPosY + (player.posY - player.prevPosY) * scale + 1.62D ;
		double z = player.prevPosZ + (player.posZ - player.prevPosZ) * scale;
		Vec3 vector1 = new Vec3(x, y, z);
		float cosYaw = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
		float sinYaw = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
		float cosPitch = -MathHelper.cos(-pitch * 0.017453292F);
		float sinPitch = MathHelper.sin(-pitch * 0.017453292F);
		float pitchAdjustedSinYaw = sinYaw * cosPitch;
		float pitchAdjustedCosYaw = cosYaw * cosPitch;
		double distance = 500D;
		if (restriction == 0 && player instanceof EntityPlayerMP) {
			distance = ((EntityPlayerMP) player).theItemInWorldManager.getBlockReachDistance();
		} else if (restriction > 0) {
			distance = restriction;
		}

		Vec3 vector2 = vector1.addVector(pitchAdjustedSinYaw * distance, sinPitch * distance, pitchAdjustedCosYaw * distance);
		return player.worldObj.rayTraceBlocks(vector1, vector2);
	}

	public static BlockPos getRandomPosInBox(World w, AxisAlignedBB box) {
		int x = (int) box.minX + w.rand.nextInt((int) (box.maxX - box.minX) + 1);
		int z = (int) box.minZ + w.rand.nextInt((int) (box.maxZ - box.minZ) + 1);
		int y = w.getHorizon(new BlockPos(x,0, z)).getY() + 1;
		if (y < box.minX || y > box.maxY) {
			y = (int) box.minY + w.rand.nextInt((int) (box.maxY - box.minY) + 1);
		}
		return new BlockPos(x, y, z);
	}

	public static void sendPacketToPlayersAround(IMessage message, Entity e) {
		VampirismMod.modChannel.sendToAllAround(message, new TargetPoint(e.dimension, e.posX, e.posY, e.posZ, 100));
	}

	public static Entity spawnEntityBehindEntity(EntityLivingBase p, String name) {
		EntityLiving e = (EntityLiving) EntityList.createEntityByName(name, p.worldObj);
		float yaw = p.rotationYawHead;
		float cosYaw = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
		float sinYaw = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
		int distance = 2;
		double x = p.posX + sinYaw * distance;
		double z = p.posZ + cosYaw * distance;

		e.setPosition(x, p.posY, z);

		if (e.getCanSpawnHere()) {
			p.worldObj.spawnEntityInWorld(e);
			return e;
		} else {
			int y=p.worldObj.getHorizon(new BlockPos(x,0,z)).getY();
			e.setPosition(x, y, z);
			if (e.getCanSpawnHere()) {
				p.worldObj.spawnEntityInWorld(e);
				return e;
			}
		}
		e.setDead();
		return null;
	}

	public static List<Entity> spawnEntityInVillage(Village v, int max, String name, World world) {
		// VillageVampire vv=VillageVampireData.get(world).getVillageVampire(v);
		List<Entity> list = new ArrayList<Entity>();
		Entity e = null;
		for (int i = 0; i < max; i++) {
			if (e == null) {
				e = EntityList.createEntityByName(name, world);
			}
			if (spawnEntityInWorld(world, VillageVampire.getBoundingBox(v), e, 5)) {
				list.add(e);
				e = null;
			}

		}
		if (e != null) {
			e.setDead();
		}
		return list;
	}

	public static boolean spawnEntityInWorld(World world, AxisAlignedBB box, Entity e, int maxTry) {
		boolean flag = false;
		int i = 0;
		while (!flag && i++ < maxTry) {
			BlockPos c = getRandomPosInBox(world, box);
			e.setPosition(c.getX(), c.getY(), c.getZ());
			if (!(e instanceof EntityLiving) || ((EntityLiving) e).getCanSpawnHere()) {
				flag = true;
			}
		}
		if (flag) {
			world.spawnEntityInWorld(e);
			return true;
		} else {
		}
		return false;
	}

	public static Entity spawnEntityInWorld(World world, AxisAlignedBB box, String name, int maxTry) {
		Entity e = EntityList.createEntityByName(name, world);
		if (spawnEntityInWorld(world, box, e, maxTry)) {
			return e;
		} else {
			e.setDead();
			return null;
		}
	}

	public static void spawnParticlesAroundEntity(EntityLivingBase e, EnumParticleTypes particle, double maxDistance, int amount) {
		if (!e.worldObj.isRemote) {
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setInteger("particle_id", particle.getParticleID());
			nbt.setInteger("id", e.getEntityId());
			nbt.setDouble("distance", maxDistance);
			Helper.sendPacketToPlayersAround(new SpawnCustomParticlePacket(2, 0, 0, 0, amount, nbt), e);
			return;
		}
		short short1 = (short) amount;
		for (int l = 0; l < short1; ++l) {
			double d6 = l / (short1 - 1.0D) - 0.5D;
			float f = (e.getRNG().nextFloat() - 0.5F) * 0.2F;
			float f1 = (e.getRNG().nextFloat() - 0.5F) * 0.2F;
			float f2 = (e.getRNG().nextFloat() - 0.5F) * 0.2F;
			double d7 = e.posX + (maxDistance) * d6 + (e.getRNG().nextDouble() - 0.5D) * e.width * 2.0D;
			double d8 = e.posY + (maxDistance / 2) * d6 + e.getRNG().nextDouble() * e.height;
			double d9 = e.posZ + (maxDistance) * d6 + (e.getRNG().nextDouble() - 0.5D) * e.width * 2.0D;
			e.worldObj.spawnParticle(particle, d7, d8, d9, f, f1, f2);
		}
	}

	/**
	 * Teleports the entity
	 * 
	 * @param entity
	 * @param x
	 * @param y
	 * @param z
	 * @param sound
	 *            If a teleport sound should be played
	 * @return Wether the teleport was successful or not
	 */
	public static boolean teleportTo(EntityLiving entity, double x, double y, double z, boolean sound) {
		double d3 = entity.posX;
		double d4 = entity.posY;
		double d5 = entity.posZ;
		entity.posX = x;
		entity.posY = y;
		entity.posZ = z;
		boolean flag = false;
		BlockPos blockPos=entity.getPosition();


		if (entity.worldObj.isBlockLoaded(blockPos)) {
			boolean flag1 = false;

			while (!flag1 && blockPos.getY() > 0) {
				Block block = entity.worldObj.getBlockState(blockPos.down()).getBlock();
				if (block.getMaterial().blocksMovement())
					flag1 = true;
				else {
					--entity.posY;
					blockPos=blockPos.down();
				}
			}

			if (flag1) {
				entity.setPosition(entity.posX, entity.posY, entity.posZ);

				if (entity.worldObj.getCollidingBoundingBoxes(entity, entity.getEntityBoundingBox()).isEmpty() && !entity.worldObj.isAnyLiquid(entity.getEntityBoundingBox()))
					flag = true;
			}
		}

		if (!flag) {
			entity.setPosition(d3, d4, d5);
			return false;
		} else {
			short short1 = 128;

			for (int l = 0; l < short1; ++l) {
				double d6 = l / (short1 - 1.0D);
				float f = (entity.getRNG().nextFloat() - 0.5F) * 0.2F;
				float f1 = (entity.getRNG().nextFloat() - 0.5F) * 0.2F;
				float f2 = (entity.getRNG().nextFloat() - 0.5F) * 0.2F;
				double d7 = d3 + (entity.posX - d3) * d6 + (entity.getRNG().nextDouble() - 0.5D) * entity.width * 2.0D;
				double d8 = d4 + (entity.posY - d4) * d6 + entity.getRNG().nextDouble() * entity.height;
				double d9 = d5 + (entity.posZ - d5) * d6 + (entity.getRNG().nextDouble() - 0.5D) * entity.width * 2.0D;
				entity.worldObj.spawnParticle(EnumParticleTypes.PORTAL, d7, d8, d9, f, f1, f2);
			}

			if (sound) {
				entity.worldObj.playSoundEffect(d3, d4, d5, "mob.endermen.portal", 1.0F, 1.0F);
				entity.playSound("mob.endermen.portal", 1.0F, 1.0F);
			}

			return true;
		}
	}

	/**
	 *
	 * @param ran
	 * @return Random double between -1 and 1
	 */
	public static double rnd1n1(Random ran){
		return (ran.nextDouble()-0.5D)*2;
	}

	public static boolean isEntityInVampireBiome(Entity e){
		if(e==null||e.worldObj==null)return false;
		return e.worldObj.getBiomeGenForCoords(e.getPosition()) instanceof BiomeVampireForest;
	}

	/**
	 * Sends the component message to all players except the given one.
	 * Only use on server or common side
	 * @param player
	 * @param message
	 */
	public static void sendMessageToAllExcept(EntityPlayer player,IChatComponent message){
		for(Object o:MinecraftServer.getServer().getConfigurationManager().playerEntityList){
			if(!o.equals(player)){
				((EntityPlayer)o).addChatComponentMessage(message);
			}
		}
	}

	public static void sendMessageToAll(IChatComponent message){
		sendMessageToAllExcept(null,message);
	}


	/**
	 * Checks if the target entity is in the field of view (180 degree) of the base entity. Only works reliable for players (due to server-client sync)
	 *
	 * @param entity
	 * @param target
	 * @param alsoRaytrace Raytraces first
	 * @return
	 */
	public static boolean canReallySee(EntityLivingBase entity, EntityLivingBase target, boolean alsoRaytrace) {
		if (alsoRaytrace && !entity.canEntityBeSeen(target)) {
			return false;
		}
		Vec3 look1 = new Vec3(-Math.sin(entity.rotationYawHead / 180 * Math.PI), 0, Math.cos(entity.rotationYawHead / 180 * Math.PI));
		Vec3 dist = new Vec3(target.posX - entity.posX, 0, target.posZ - entity.posZ);
		//look1.yCoord = 0;
		look1 = look1.normalize();
		dist = dist.normalize();

		//Check if the vector is left or right of look1
		double alpha = Math.acos(look1.dotProduct(dist));
		return alpha < Math.PI / 2;

	}

	public static interface IntToString {
		String match(int i);
	}

	public static interface StackToString{
		String match(ItemStack stack);
	}

	public static void write(NBTTagCompound nbt,String base,BlockPos pos){
		nbt.setInteger(base+"_x",pos.getX());
		nbt.setInteger(base+"_y",pos.getY());
		nbt.setInteger(base+"_z",pos.getZ());
	}

	public static BlockPos readPos(NBTTagCompound nbt,String base){
		return new BlockPos(nbt.getInteger(base+"_x"),nbt.getInteger(base+"_y"),nbt.getInteger(base+"_z"));
	}

	public static String[] prefix(String prefix,String... strings){
		String[] result=new String[strings.length];
		for(int i=0;i<strings.length;i++){
			result[i]=prefix+strings[i];
		}
		return result;
	}

}
