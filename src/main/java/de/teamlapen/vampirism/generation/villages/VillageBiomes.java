package de.teamlapen.vampirism.generation.villages;

import de.teamlapen.vampirism.Configs;
import de.teamlapen.vampirism.ModBiomes;
import de.teamlapen.vampirism.util.Logger;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;
import java.util.regex.Pattern;

/**
 * All the initialization for new Village Biomes
 * 
 * @author WILLIAM
 *
 */
public class VillageBiomes {

	public static void postInit(FMLPostInitializationEvent ev) {
		if (Configs.disable_village_biome)
			return;

		// All other mods should be done registering by now.
		BiomeRegistrant.init();

		for (String name : ConfigHandler.getAddBiomes()) {
			if (Pattern.matches("\\d+", name))
				BiomeRegistrant.addBiomeById(Integer.parseInt(name));
			else
				BiomeRegistrant.addBiomeByName(name);
		}
		for (String name : ConfigHandler.getAddTypes()) {
			Logger.d("VillageBiomes", "Adding all %s biomes as village biomes.", name);
			BiomeRegistrant.addBiomesByTypeName(name);
		}

		for (String name : ConfigHandler.getRemoveBiomes()) {
			if (Pattern.matches("\\d+", name))
				BiomeRegistrant.removeBiomeById(Integer.parseInt(name));
			else
				BiomeRegistrant.removeBiomeByName(name);
		}
		for (String name : ConfigHandler.getRemoveTypes()) {
			Logger.d("VillageBiomes", "Removing all " + name + " biomes from village biomes.");
			BiomeRegistrant.removeBiomesByTypeName(name);
		}
		if (!Configs.disable_vampire_biome) {
			BiomeRegistrant.removeBiome(ModBiomes.biomeVampireForest);
		}

		// Register the custom village block replacer
		MinecraftForge.TERRAIN_GEN_BUS.register(new BiomeBlockReplacer());
	}

	public static void preInit(FMLPreInitializationEvent event) {
		if (Configs.disable_village_biome)
			return;
		// Load Config
		File ConfigFile = new File(event.getModConfigurationDirectory(), "vampirism_village_biomes.cfg");
		ConfigHandler.loadConfig(ConfigFile);
	}
}
