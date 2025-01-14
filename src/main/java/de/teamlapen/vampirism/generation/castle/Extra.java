package de.teamlapen.vampirism.generation.castle;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.entity.EntityDracula;
import de.teamlapen.vampirism.tileEntity.TileEntityBloodAltar1;
import de.teamlapen.vampirism.tileEntity.TileEntityBloodAltar2;
import de.teamlapen.vampirism.tileEntity.TileEntityCoffin;
import de.teamlapen.vampirism.util.Logger;
import de.teamlapen.vampirism.util.REFERENCE;
import de.teamlapen.vampirism.util.TickRunnable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * Used to save additional information from tileentitys or other things with building tiles
 * @author Maxanier
 */
public class Extra {
	public final TYPE type;
	public final BlockPos pos;
	private JsonObject extra;
	public Extra(TYPE t,BlockPos pos){
		this.type=t;
		this.pos=pos;
	}

	/**
	 * Applies this extra to the given world
	 * @param world
	 * @param wPos absolute position in the world
	 */
	public void applyExtra(final World world, BlockPos wPos) {
		if(type==TYPE.SPAWN_ENTITY){
			int c=extra.get("count").getAsInt();
			String entity=extra.get("entity").getAsString();
			if(REFERENCE.ENTITY.DRACULA_NAME.equals(entity)&&world.provider.getDimensionId()!= VampirismMod.castleDimensionId){
				return;
			}
			for(int i=0;i<c;i++){
				final Entity e = EntityList.createEntityByName(entity, world);
				if(e!=null) {
					e.setPosition(wPos.getX(), wPos.getY() + 0.19D, wPos.getZ());
					boolean success = world.spawnEntityInWorld(e);
					if (e instanceof EntityDracula) Logger.t("Spawned Dracula %s (%b)", e, success);
					if (!success) {
						VampirismMod.proxy.addTickRunnable(new TickRunnable() {
							int tick = 20;

							@Override
							public boolean shouldContinue() {
								return tick > 0;
							}

							@Override
							public void onTick() {
								tick--;
								if (tick == 0) {
									world.spawnEntityInWorld(e);
								}
							}
						});
					}
				} else {
					MinecraftServer.getServer().getConfigurationManager().sendChatMsg(new ChatComponentText("Failed to spawn " + entity));//TODO remove
					Logger.w("Extra", "Failed to create %s in world %s", entity, world);
				}
			}

		}
		else if(type==TYPE.PAINTING){
			String facing = extra.get("facing").getAsString();
			String title=extra.get("title").getAsString();
			EntityPainting.EnumArt art=null;
			for(EntityPainting.EnumArt a: EntityPainting.EnumArt.values()){
				if(a.title.equals(title)){
					art=a;
					break;
				}
			}
			if(art==null)art= EntityPainting.EnumArt.ALBAN;
			EntityPainting p = new EntityPainting(world,wPos, EnumFacing.byName(facing));
			p.art=art;
			world.spawnEntityInWorld(p);

		} else{
			TileEntity tileEntity=world.getTileEntity(wPos);
			switch (type) {
			case SPAWNER:
				((TileEntityMobSpawner) tileEntity).getSpawnerBaseLogic().setEntityName(extra.get("entity_name").getAsString());
				break;
			case CHEST:
				JsonArray items = extra.getAsJsonArray("items");
				TileEntityChest chest = (TileEntityChest) tileEntity;
				for (int i = 0; i < items.size(); ++i) {
					JsonObject item = items.get(i).getAsJsonObject();
					int slot = item.get("s").getAsInt();

					if (slot >= 0 && slot < chest.getSizeInventory()) {
						String[] part = item.get("name").getAsString().split(":");
						Item it = GameRegistry.findItem(part[0], part[1]);
						ItemStack st = new ItemStack(it, item.get("count").getAsInt());
						st.setItemDamage(item.get("damage").getAsInt());
						chest.setInventorySlotContents(slot, st);
					}
				}
				break;
			case COFFIN:
				TileEntityCoffin te = ((TileEntityCoffin) tileEntity);
				te.tryToFindOtherTile();
				te.color = extra.get("color").getAsInt();
				te.occupied=extra.get("closed").getAsBoolean();
				break;
			case WALL_SIGN:
				TileEntitySign sign2=(TileEntitySign)tileEntity;
				sign2.signText[0]=new ChatComponentText(extra.get("t0").getAsString());
				sign2.signText[1]=new ChatComponentText(extra.get("t1").getAsString());
				sign2.signText[2]=new ChatComponentText(extra.get("t2").getAsString());
				sign2.signText[3]=new ChatComponentText(extra.get("t3").getAsString());
				break;
			case ALTAR_2:
				TileEntityBloodAltar2 altar2= (TileEntityBloodAltar2) tileEntity;
				altar2.addBlood(extra.get("blood").getAsInt());
				break;
			case  ALTAR_1:
				TileEntityBloodAltar1 altar1= (TileEntityBloodAltar1) tileEntity;
				if(extra.get("infinite").getAsBoolean()) {
					altar1.makeInfinite();
				}

			}

		}
	}

	/**
	 * Retrieves the information from a tileentity as long as this extra has the right type
	 * @param tileEntity
	 */
	public void retrieveExtra(TileEntity tileEntity){
		extra=new JsonObject();
		switch (type){
		case SPAWNER:
			NBTTagCompound compound=new NBTTagCompound();
			((TileEntityMobSpawner) tileEntity).getSpawnerBaseLogic().writeToNBT(compound);
			extra.addProperty("entity_name", compound.getString("EntityId"));
			break;
		case CHEST:
			JsonArray items=new JsonArray();
			TileEntityChest chest= (TileEntityChest) tileEntity;
			for (int i = 0; i < chest.getSizeInventory(); ++i)
			{
				ItemStack stack=chest.getStackInSlot(i);
				if (stack != null)
				{
					JsonObject item=new JsonObject();
					item.addProperty("s",i);
					GameRegistry.UniqueIdentifier un=GameRegistry.findUniqueIdentifierFor(stack.getItem());
					if(un==null)continue;
					item.addProperty("name", un.modId + ":" + un.name);
					item.addProperty("count", stack.stackSize);
					item.addProperty("damage", stack.getItemDamage());
					items.add(item);
				}
			}

			extra.add("items",items);
			break;
		case COFFIN:
			extra.addProperty("color",((TileEntityCoffin)tileEntity).color);
			extra.addProperty("closed",((TileEntityCoffin)tileEntity).occupied);
			break;
		case SPAWN_ENTITY:
			TileEntitySign sign=(TileEntitySign)tileEntity;
			int count=Integer.parseInt(sign.signText[0].getUnformattedText());
			String entity=sign.signText[1].getUnformattedText()+sign.signText[2].getUnformattedText()+sign.signText[3].getUnformattedText();
			extra.addProperty("count",count);
			extra.addProperty("entity",entity.trim());
			break;
		case WALL_SIGN:
			TileEntitySign sign2=(TileEntitySign)tileEntity;
			extra.addProperty("t0",sign2.signText[0].getUnformattedText());
			extra.addProperty("t1",sign2.signText[1].getUnformattedText());
			extra.addProperty("t2",sign2.signText[2].getUnformattedText());
			extra.addProperty("t3",sign2.signText[3].getUnformattedText());
			break;
		case ALTAR_2:
			TileEntityBloodAltar2 altar2=(TileEntityBloodAltar2)tileEntity;
			extra.addProperty("blood",altar2.getBloodAmount());
			break;
		case ALTAR_1:
			TileEntityBloodAltar1 altar1= (TileEntityBloodAltar1) tileEntity;
			extra.addProperty("infinite",altar1.isInfinite());
			break;
		}

	}

	/**
	 * Retrieves information from a painting
	 * @param p
	 */
	public void retrieveExtra(EntityPainting p){
		extra=new JsonObject();
		extra.addProperty("dir",p.getHorizontalFacing().getName2().toLowerCase());
		extra.addProperty("title",p.art.title);
	}

	/**
	 * Types of {@link Extra}s
	 */
	public enum TYPE {
		SPAWNER, COFFIN, CHEST, SPAWN_ENTITY, PAINTING, WALL_SIGN, ALTAR_2, ALTAR_1
	}
}
