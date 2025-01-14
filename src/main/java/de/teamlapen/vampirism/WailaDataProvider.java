package de.teamlapen.vampirism;

import de.teamlapen.vampirism.entity.VampireMob;
import de.teamlapen.vampirism.entity.minions.IMinion;
import de.teamlapen.vampirism.entity.minions.IMinionCommand;
import de.teamlapen.vampirism.entity.minions.IMinionLord;
import de.teamlapen.vampirism.entity.minions.MinionHelper;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.tileEntity.TileEntityBloodAltar1;
import de.teamlapen.vampirism.tileEntity.TileEntityBloodAltar2;
import de.teamlapen.vampirism.util.REFERENCE;
import mcp.mobius.waila.api.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fml.common.Optional;

import java.util.List;

@Optional.Interface(iface = "mcp.mobius.waila.api.IWailaDataProvider", modid = "Waila")
public class WailaDataProvider implements IWailaDataProvider, IWailaEntityProvider {

	@Optional.Method(modid = "Waila")
	public static void callbackRegister(IWailaRegistrar register) {
		WailaDataProvider instance = new WailaDataProvider();
		register.addConfig(REFERENCE.MODID, "option.vampirism.showAltarInfo", true);
		register.addConfig(REFERENCE.MODID, "option.vampirism.showPlayerInfo", true);
		register.addConfig(REFERENCE.MODID, "option.vampirism.showEntityInfo", true);
		register.registerBodyProvider((IWailaDataProvider) instance, TileEntityBloodAltar1.class);
		register.registerBodyProvider((IWailaDataProvider) instance, TileEntityBloodAltar2.class);
		register.registerNBTProvider((IWailaDataProvider) instance, TileEntityBloodAltar1.class);
		register.registerBodyProvider((IWailaEntityProvider) instance, EntityPlayer.class);
		register.registerBodyProvider((IWailaEntityProvider) instance, EntityCreature.class);
	}

	@Override
	public NBTTagCompound getNBTData(TileEntity te, NBTTagCompound tag, IWailaDataAccessorServer accessor) {
		if (te instanceof TileEntityBloodAltar1) {
			tag.setInteger("vampirism:bloodLeft", ((TileEntityBloodAltar1) te).getBloodLeft());
		}
		return tag;
	}

	@Override
	public NBTTagCompound getNBTData(Entity ent, NBTTagCompound tag, IWailaEntityAccessorServer accessor) {
		return null;
	}


	@Override
	public ITaggedList.ITipList getWailaBody(Entity entity, ITaggedList.ITipList currenttip, IWailaEntityAccessor accessor, IWailaConfigHandler config) {
		if (config.getConfig("option.vampirism.showPlayerInfo", true)) {
			if (entity instanceof EntityPlayer) {
				VampirePlayer vampire = VampirePlayer.get((EntityPlayer) entity);
				if (vampire.getLevel() > 0) {
					currenttip.add(String.format("%s: %d", StatCollector.translateToLocal("text.vampirism.vampirelevel"), vampire.getLevel()));
					if (vampire.isVampireLord()) {
						currenttip.add(SpecialChars.WHITE + StatCollector.translateToLocal("entity.vampirism.vampireLord.name"));
					}
				}
			}
		}
		if (config.getConfig("option.vampirism.showEntityInfo", true)) {
			if (entity instanceof EntityCreature && VampirePlayer.get(accessor.getPlayer()).getLevel() > 0) {
				VampireMob vampire = VampireMob.get((EntityCreature) entity);
				IMinion minion = MinionHelper.getMinionFromEntity(entity);

				int blood = vampire.getBlood();
				if (blood >= 0) {
					currenttip.add(String.format("%s%s: %d", SpecialChars.RED, StatCollector.translateToLocal("text.vampirism.entitysblood"), blood));
				}
				if (minion != null) {
					currenttip.add(SpecialChars.GREEN + StatCollector.translateToLocal("text.vampirism.minion"));
					IMinionLord lord = minion.getLord();
					if (lord != null) {
						currenttip.add(String.format("%s%s: %s", SpecialChars.WHITE, StatCollector.translateToLocal("text.vampirism.lord"), lord.getRepresentingEntity().getName()));
						IMinionCommand c = minion.getCommand(minion.getActiveCommandId());
						if (c != null) {
							currenttip.add(String.format("%s%s: %s", SpecialChars.WHITE, StatCollector.translateToLocal("text.vampirism.current_task"),
									StatCollector.translateToLocal(c.getUnlocalizedName())));
						}

					}
				}
			}
		}
		return currenttip;
	}


	@Override
	public ITaggedList.ITipList getWailaBody(ItemStack itemStack, ITaggedList.ITipList currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
		if (config.getConfig("option.vampirism.showAltarInfo", true)) {
			TileEntity tile = accessor.getTileEntity();
			if (tile instanceof TileEntityBloodAltar1) {
				TileEntityBloodAltar1 altar1 = (TileEntityBloodAltar1) tile;

				if (altar1.isOccupied()) {
					int blood;
					if (accessor.getNBTData().hasKey("vampirism:bloodLeft")) {
						blood = accessor.getNBTData().getInteger("vampirism:bloodLeft");
					} else {
						blood = altar1.getBloodLeft();
					}

					currenttip.add(String.format("%s%s: %d", SpecialChars.RED, StatCollector.translateToLocal("text.vampirism.blood_left"), blood));
				}
			} else if (tile instanceof TileEntityBloodAltar2) {
				TileEntityBloodAltar2 altar2 = (TileEntityBloodAltar2) tile;
				currenttip.add(String.format("%s%s: %d/%d", SpecialChars.RED, StatCollector.translateToLocal("text.vampirism.blood"), altar2.getBloodAmount(), altar2.getMaxBlood()));
			}
		}

		return currenttip;
	}

	@Override
	public ITaggedList.ITipList getWailaHead(ItemStack itemStack, ITaggedList.ITipList currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
		return currenttip;
	}

	@Override
	public ITaggedList.ITipList getWailaHead(Entity entity, ITaggedList.ITipList currenttip, IWailaEntityAccessor accessor, IWailaConfigHandler config) {
		return currenttip;
	}



	@Override
	public Entity getWailaOverride(IWailaEntityAccessor accessor, IWailaConfigHandler config) {
		return null;
	}

	@Override
	public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
		return null;
	}

	@Override
	public ITaggedList.ITipList getWailaTail(ItemStack itemStack, ITaggedList.ITipList currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
		return currenttip;
	}

	@Override
	public ITaggedList.ITipList getWailaTail(Entity entity, ITaggedList.ITipList currenttip, IWailaEntityAccessor accessor, IWailaConfigHandler config) {
		return currenttip;
	}


}
