package de.teamlapen.vampirism;

import de.teamlapen.vampirism.castleDim.WorldProviderCastle;
import de.teamlapen.vampirism.entity.convertible.BiteableRegistry;
import de.teamlapen.vampirism.entity.player.skills.Skills;
import de.teamlapen.vampirism.generation.WorldGenVampirism;
import de.teamlapen.vampirism.generation.castle.CastleGenerator;
import de.teamlapen.vampirism.generation.villages.VillageBiomes;
import de.teamlapen.vampirism.generation.villages.VillageCreationHandler;
import de.teamlapen.vampirism.generation.villages.VillageGenReplacer;
import de.teamlapen.vampirism.generation.villages.VillageModChurchPiece;
import de.teamlapen.vampirism.guide.VampirismGuide;
import de.teamlapen.vampirism.network.*;
import de.teamlapen.vampirism.proxy.IProxy;
import de.teamlapen.vampirism.util.Helper;
import de.teamlapen.vampirism.util.Logger;
import de.teamlapen.vampirism.util.REFERENCE;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.VillagerRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = REFERENCE.MODID, name = REFERENCE.NAME, version = REFERENCE.VERSION, acceptedMinecraftVersions = "[1.8]", dependencies = "required-after:Forge@[11.14.3.1450,)", guiFactory = "de.teamlapen.vampirism.client.gui.ModGuiFactory")
public class VampirismMod {

	@Instance(value = REFERENCE.MODID)
	public static VampirismMod instance;

	@SidedProxy(clientSide = "de.teamlapen.vampirism.proxy.ClientProxy", serverSide = "de.teamlapen.vampirism.proxy.ServerProxy")
	public static IProxy proxy;

	public static SimpleNetworkWrapper modChannel;

	public static boolean inDev = false;
	
	public static boolean potionFail =false;

	public static boolean vampireCastleFail = false;

	public static int castleDimensionId;

	public static CreativeTabs tabVampirism = new CreativeTabs("vampirism") {
		@Override
		@SideOnly(Side.CLIENT)
		public Item getTabIconItem() {
			return ModItems.leechSword;
		}
	};

	public static DamageSource sunDamage = (new DamageSource("sun")).setDamageBypassesArmor().setMagicDamage();

	public static boolean isSunDamageTime(World world) {
		if (world == null)
			return false;
		int t = (int) (world.getWorldTime() % 24000);
		return (t > 0 && t < 12500);
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		castleDimensionId=DimensionManager.getNextFreeDimId();
		DimensionManager.registerProviderType(castleDimensionId, WorldProviderCastle.class,false);
		DimensionManager.registerDimension(castleDimensionId,castleDimensionId);
		GameRegistry.registerWorldGenerator(new WorldGenVampirism(), 1000);
		VillagerRegistry.instance().registerVillageCreationHandler(new VillageCreationHandler());
		MapGenStructureIO.registerStructureComponent(VillageModChurchPiece.class, "ViVMC");
		FMLCommonHandler.instance().bus().register(new Configs());
		if (Configs.village_gen_enabled) {
			Logger.i("Init", "Registering replacer for village generation.");
			MinecraftForge.TERRAIN_GEN_BUS.register(new VillageGenReplacer());
		}
		proxy.init();
		FMLInterModComms.sendMessage("Waila", "register", "de.teamlapen.vampirism.WailaDataProvider.callbackRegister");
	}

	@EventHandler
	public void onServerStart(FMLServerStartingEvent e) {
		e.registerServerCommand(new TestCommand()); // Keep there until final
		e.registerServerCommand(new SummonCommand());
		e.registerServerCommand(new VampirismCommand());

	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		proxy.postInit();
		VillageBiomes.postInit(event);
		BiteableRegistry.finishRegistration();
		String potion=ModPotion.checkPotions();
		if(potion!=null){
			Logger.e("PostInit", "Not all potions were successfully added {%s}", potion);
			potionFail=true;
		}
		
		if(Loader.isModLoaded("guideapi"))
        {
			Logger.d("PostInit", "Found Guide-API -> Registering guide book");
			registerGuideBook();
        }
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		// Make sure the Config initialisation is the first mod relating call
		if ((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
			inDev = true;
			Logger.inDev = true;
		}
		Configs.init(event.getModConfigurationDirectory(), inDev);
		Helper.Obfuscation.fillMap();

		proxy.preInit();
		setupNetwork();

		VillageBiomes.preInit(event);
		CastleGenerator.loadTiles();

		Skills.registerDefaultSkills();

		// Sends message to VersionChecker if installed
		FMLInterModComms.sendRuntimeMessage(REFERENCE.MODID, "VersionChecker", "addVersionCheck", REFERENCE.UPDATE_FILE_LINK);
	}

	private void setupNetwork() {
		NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());

		modChannel = NetworkRegistry.INSTANCE.newSimpleChannel(REFERENCE.MODID);
		int id = 0;
		modChannel.registerMessage(InputEventPacket.Handler.class, InputEventPacket.class, id++, Side.SERVER);
		modChannel.registerMessage(SpawnParticlePacket.Handler.class, SpawnParticlePacket.class, id++, Side.CLIENT);
		modChannel.registerMessage(SpawnCustomParticlePacket.Handler.class, SpawnCustomParticlePacket.class, id++, Side.CLIENT);
		modChannel.registerMessage(RenderScreenRedPacket.Handler.class, RenderScreenRedPacket.class, id++, Side.CLIENT);
		modChannel.registerMessage(UpdateEntityPacket.Handler.class, UpdateEntityPacket.class, id++, Side.CLIENT);
		modChannel.registerMessage(RequestEntityUpdatePacket.Handler.class, RequestEntityUpdatePacket.class, id++, Side.SERVER);
	}
	
	@Optional.Method(modid = "guideapi")
	private static void registerGuideBook(){
		VampirismGuide.registerGuide();
	}

}
