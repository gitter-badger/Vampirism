package de.teamlapen.vampirism;

import de.teamlapen.vampirism.entity.convertible.BiteableRegistry;
import de.teamlapen.vampirism.util.*;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class Configs {

	public static final String CATEGORY_GENERAL = Configuration.CATEGORY_GENERAL;

	public static final String CATEGORY_GUI = "gui";

	public static final String CATEGORY_VILLAGE = "village_settings";

	public static final String CATEGORY_BALANCE = "balance";

	public static final String CATEGORY_DISABLE = "disabled";

	public static final String CATEGORY_BALANCE_PLAYER_MOD = "balance_player_mod";

	public static final String CATEGORY_BALANCE_PLAYER_SKILLS = "balance_player_skills";
	public static final String CATEGORY_BALANCE_LEVELING = "balance_leveling";
	public static final String CATEGORY_BALANCE_MOBPROP = "balance_mob_properties";
	public static final String CATEGORY_BALANCE_VVPROP = "balance_vv_properties";

	public static boolean village_gen_enabled;
	public static int village_density;
	public static int village_minDist;
	public static int village_size;
	public static int player_blood_watcher;
	public static boolean disable_vampire_biome;

	public static boolean disable_village_biome;

	public static boolean reset_balance_in_dev;

	public static int blood_vision_recompile_ticks;
	
	public static int potion_id_sanguinare;
	
	public static int potion_id_saturation;
	
	public static int potion_id_sunscreen;
	
	public static int potion_id_thirst;

	public static boolean mulitple_lords;

	public static boolean render_fog_vampire_biome;

	public static Configuration config;

	public static Configuration balance;

	public static int looseLordDaysCount;

	public static boolean realismMode;

	public static boolean modify_vampire_player_texture;

	public static boolean disable_hunter;

	public static int gui_level_offset_x;

	public static int gui_level_offset_y;

	public static boolean gui_yellow_border;

	public static boolean disable_blood_vision;

	public static int getVampireBiomeId() {
		return config.getInt("vampirism_biome_id", CATEGORY_GENERAL, -1, -1, 1000, "If you set this to -1 the mod will try to find a free biome id");
	}

	/**
	 * Called when the mod was updated, before the config values are reset
	 * 
	 * @param oldVersion
	 */
	private static void handleModUpdated(String oldVersion) {
		if (oldVersion.matches("0\\.[0-5]\\.[0-3]") || oldVersion.matches("0\\.[0-5]\\.[0-3]\\..+")) {
			config = reset(config);
			// Deletes VillageBiomes since it was moved to vampirism_village_biomes.cfg
			// Will cause an error, but it is only executed once and does not crash the game
			reset(new Configuration(new File(config.getConfigFile().getParentFile(), "VillageBiomes.cfg")));
			loadConfiguration();
		}

	}

	public static void init(File configDir, boolean inDev) {
		File mainConfig = new File(configDir, REFERENCE.MODID + ".cfg");
		File balanceConfig = new File(configDir, REFERENCE.MODID + "_balance.cfg");
		File bloodConfig = new File(configDir,REFERENCE.MODID+"_blood_values.txt");
		
		try {

			Map<String, Integer> defaultValues = loadBloodValuesFromReader(new InputStreamReader(Configs.class.getResourceAsStream("/default_blood_values.txt")), "default_blood_values.txt");
			BiteableRegistry.addBloodValues(defaultValues);
		} catch (IOException e) {
			Logger.e("Configs", e, "Could not read default blood values, this should not happen and destroys the mod experience");
		}
		
		if(bloodConfig.exists()){
			try {
				Map<String, Integer> override = loadBloodValuesFromReader(new FileReader(bloodConfig), bloodConfig.getName());
				BiteableRegistry.overrideBloodValues(override);
				Logger.i("Configs","Succesfully loaded additional blood value file");
			} catch (IOException e) {
				Logger.e("Configs", e, "Could not read blood values from config file %s",bloodConfig.getName());
			}
		}
		
		config = new Configuration(mainConfig);
		balance = new Configuration(balanceConfig);
		String old = loadConfiguration();
		if (old != null || (inDev && reset_balance_in_dev)) {
			balance = reset(balance);
		}
		loadBalanceConfiguration();
		if (old != null) {
			handleModUpdated(old);
		}
		Logger.i("Config", "Loaded configuration");
	}

	public static void loadBalanceConfiguration() {
		// ConfigCategory cat_general=balance.getCategory(CATEGORY_GENERAL);
		// cat_general.setComment("General settings");
		ConfigCategory cat_balance = balance.getCategory(CATEGORY_BALANCE);
		cat_balance.setComment("You can adjust these values to change the balancing of this mod");

		ConfigCategory cat_balance_player_mod = balance.getCategory(CATEGORY_BALANCE_PLAYER_MOD);
		cat_balance_player_mod.setComment("You can adjust these values to change the vampire player modifiers");
		ConfigCategory cat_balance_player_skills = balance.getCategory(CATEGORY_BALANCE_PLAYER_SKILLS);
		cat_balance_player_skills.setComment("You can adjust these values to change the vampire player skills");
		ConfigCategory cat_balance_leveling = balance.getCategory(CATEGORY_BALANCE_LEVELING);
		cat_balance_leveling.setComment("You can adjust these values to change the level up requirements");
		ConfigCategory cat_balance_mobprop = balance.getCategory(CATEGORY_BALANCE_MOBPROP);
		cat_balance_mobprop.setComment("You can adjust the properties of the added mobs");
		ConfigCategory cat_balance_vvprop = balance.getCategory(CATEGORY_BALANCE_VVPROP);
		cat_balance_vvprop.setComment("You can adjust the configuration of village managment (Agressive hunters, etc.)");

		// Balance
		loadFields(balance, cat_balance, BALANCE.class);
		loadFields(balance, cat_balance_player_mod, BALANCE.VP_MODIFIERS.class);
		loadFields(balance, cat_balance_player_skills, BALANCE.VP_SKILLS.class);
		loadFields(balance, cat_balance_leveling, BALANCE.LEVELING.class);
		loadFields(balance, cat_balance_mobprop, BALANCE.MOBPROP.class);
		loadFields(balance, cat_balance_vvprop, BALANCE.VV_PROP.class);

		if (balance.hasChanged()) {
			balance.save();
		}
	}

	/**
	 * Loads/refreshes the configuration and adds comments if there aren't any {@link #init(File,boolean) init} has to be called once before using this
	 * 
	 * @return If the config are of an older version it returns the old version otherwise its null
	 */
	private static String loadConfiguration() {
		// Categories
		ConfigCategory cat_village = config.getCategory(CATEGORY_VILLAGE);
		cat_village.setComment("Here you can configure the village generation");
		ConfigCategory cat_general = config.getCategory(CATEGORY_GENERAL);
		cat_general.setComment("General settings");
		ConfigCategory cat_disabled = config.getCategory(CATEGORY_DISABLE);
		cat_disabled.setComment("You can disable some features here, but it is not recommend and might cause problems (e.g. you can't get certain items");
		ConfigCategory cat_gui = config.getCategory(CATEGORY_GUI);
		cat_gui.setComment("Adjust some of Vampirism's gui elements");

		// General
		player_blood_watcher = config.get(CATEGORY_GENERAL, "player_data_watcher_id", 21, "ID for datawatcher. HAS TO BE THE SAME ON CLIENT AND SERVER").getInt();
		getVampireBiomeId();
		reset_balance_in_dev = config.getBoolean("reset_balance_in_dev", CATEGORY_GENERAL, true, "For developers: Should the balance values be reset on start in dev environment");
		modify_vampire_player_texture = config.getBoolean("modify_vampire_player_texture", CATEGORY_GENERAL, true, "(Temporarly)modify the players skill to look vampirish");
		String conf_version = config.get(CATEGORY_GENERAL, "config_mod_version", REFERENCE.VERSION).getString();
		config.get(CATEGORY_GENERAL, "config_mod_version", REFERENCE.VERSION).set(REFERENCE.VERSION);
		blood_vision_recompile_ticks = config.getInt("blood_vision_recompile", CATEGORY_GENERAL, 2, 1, 100, "Every n tick the blood vision entities are recompiled - Might have a performance impact");
		potion_id_sanguinare= config.getInt("potion_id_sanguinare", CATEGORY_GENERAL, 43, 30, 255, "Potion id for sanguinare (Have to be the same on server and client)");
		potion_id_thirst = config.getInt("potion_id_thirst", CATEGORY_GENERAL, 41, 30, 255, "Potion id thirst (Have to be the same on server and client)");
		potion_id_saturation = config.getInt("potion_id_saturation", CATEGORY_GENERAL, 42, 30, 255, "Potion id for saturation (Have to be the same on server and client)");
		potion_id_sunscreen = config.getInt("potion_id_sunscreen", CATEGORY_GENERAL, 40, 30, 255, "Potion id for sunscreen (Have to be the same on server and client)");
		render_fog_vampire_biome = config.getBoolean("fog_vampire_biome", CATEGORY_GENERAL, true, "Render fog in the vampire biome");
		mulitple_lords = config.getBoolean("multiple_lords", CATEGORY_GENERAL, false, "Allows multiple player to be a vampire lord at a time. If changed from true to false, all players will loose their lord status");
		looseLordDaysCount = config.getInt("loose_lord_after_days", CATEGORY_GENERAL, 300, 1, Integer.MAX_VALUE, "Loose vampire lord status if not being online for n Minecraft days on multiplayer servers");

		realismMode = config.getBoolean("vampire_realism_mode", CATEGORY_GENERAL, false, "Changes a few things and changes some default balance values to make it more 'realistic' ");

		// Village
		village_gen_enabled = config.get(cat_village.getQualifiedName(), "change_village_gen_enabled", true, "Should the custom generator be injected? (Enables/Disables the village mod)")
				.getBoolean();
		village_density = config.get(cat_village.getQualifiedName(), "village_density", 22, "Minecraft will try to generate 1 village per NxN chunk area. Vanilla: 32").getInt();
		village_minDist = config.get(cat_village.getQualifiedName(), "village_minimumDistance", 6, "Village centers will be at least N chunks apart. Must be smaller than density. Vanilla: 8")
				.getInt();
		village_size = config.get(cat_village.getQualifiedName(), "village_size", 0, "A higher size increases the overall spawn weight of buildings.").getInt();

		if (village_minDist < 0) {
			Logger.e("VillageDensity", "Invalid config: Minimal distance must be non-negative.");
			village_gen_enabled = false;
		}
		if (village_minDist >= village_density) {
			Logger.e("VillageDensity", "Invalid config: Minimal distance must be smaller than density.");
			village_gen_enabled = false;
		}
		if (village_size < 0) {
			village_gen_enabled = false;
			Logger.e("VillageDensity", "Invalid config: Size must be non-negative.");
		}

		// Gui
		gui_level_offset_x = config.getInt("level_offset_x", CATEGORY_GUI, 0, -250, 250, "X-Offset of the level indicator from the center in pixels");
		gui_level_offset_y = config.getInt("level_offset_y", CATEGORY_GUI, 47, 0, 270, "Y-Offset of the level indicator from the bottom in pixels");
		gui_yellow_border = config.getBoolean("yellow_border", CATEGORY_GUI, true, "Enables/disables the yellow border which is rendered when you are standing in the sun");

		// Disable

		disable_vampire_biome = config.getBoolean("disable_vampire_biome", cat_disabled.getQualifiedName(), false, "Disable the generation of the vampire biome. !You wont be able to become a vampire lord!");
		disable_village_biome = config.getBoolean("disable_village_biomes", CATEGORY_DISABLE, false, "Disables the biome based alternation of village generation");
		disable_hunter = config.getBoolean("disable_vampire_hunter", CATEGORY_DISABLE, false, "Disable hunter spawn. Will make the mod  easier and unbalanced");
		disable_blood_vision = config.getBoolean("disable_blood_vision", CATEGORY_DISABLE, false, "Disables the blood vision ability");

		if (config.hasChanged()) {
			config.save();
		}
		if (!conf_version.equals(REFERENCE.VERSION)) {
			Logger.i("Config", "Detected Modupdate");
			return conf_version;
		}
		return null;

	}

	/**
	 * This methods makes variables from a class available in the config file. To use this, the given class can only contain static non-final variables, which should have a '@Default*' annotation
	 * containing the default value. Currently only boolean,int and double are supported
	 * 
	 * @param config
	 *            Configuration the fields are in or should be inserted
	 * @param cat
	 *            Config Category to put the properties inside
	 * @param cls
	 *            Class to go through
	 * @author Maxanier
	 */
	private static void loadFields(Configuration config, ConfigCategory cat, Class cls) {
		for (Field f : cls.getDeclaredFields()) {
			String name = f.getName();
			Class type = f.getType();
			try {
				if (type == int.class) {
					// Possible exception should not be caught so you can't forget a default value
					DefaultInt a = f.getAnnotation(DefaultInt.class);
					f.set(null, config.get(cat.getQualifiedName(), a.name(), a.value(), a.comment(), a.minValue(), a.maxValue()).getInt());
				} else if (type == double.class) {
					// Possible exception should not be caught so you can't forget a default value
					DefaultDouble a = f.getAnnotation(DefaultDouble.class);
					f.set(null, config.get(cat.getQualifiedName(), a.name(), a.value(), a.comment(), a.minValue(), a.maxValue()).getDouble());
				} else if (type == boolean.class) {
					DefaultBoolean a = f.getAnnotation(DefaultBoolean.class);
					f.set(null, config.get(cat.getQualifiedName(), a.name(), a.value(), a.comment()).getBoolean());
				}
			} catch (NullPointerException e1) {
				Logger.e("Configs", "Author probably forgot to specify a default value for " + name + " in " + cls.getCanonicalName(), e1);
				throw new Error("Please check you default values");
			} catch (Exception e) {
				Logger.e("Configs", "Cant set " + cls.getName() + " values", e);
				throw new Error("Please check your vampirism config file");
			}
		}
	}

	public static Configuration reset(Configuration config) {
		Logger.i("Configs", "Resetting config file " + config.getConfigFile().getName());
		try {
			PrintWriter writer = new PrintWriter(config.getConfigFile());
			writer.write("");
			writer.flush();
			writer.close();
			return new Configuration(config.getConfigFile());
		} catch (Exception e) {
			Logger.e("Configs", "Failed to reset config file");
		}
		return config;

	}

	public static void setVampireBiomeId(int i) {
		config.get(CATEGORY_GENERAL, "vampirism_biome_id", -1).set(i);
		config.save();
	}

	@SubscribeEvent
	public void onConfigurationChanged(ConfigChangedEvent.OnConfigChangedEvent e) {
		if (e.modID.equalsIgnoreCase(REFERENCE.MODID)) {
			// Resync configs
			Logger.i("Configs", "Configuration has changed");
			Configs.loadConfiguration();
			Configs.loadBalanceConfiguration();
		}
	}
	
	
	/**
	 * 
	 * @param r Reader the values should be read from
	 * @param file Just for logging of errors
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static Map<String, Integer> loadBloodValuesFromReader(Reader r, String file) throws IOException {
		Map<String, Integer> bloodValues = new HashMap<String, Integer>();
		BufferedReader br=null;
			try {
				br=new BufferedReader(r);
				String line;
				while((line=br.readLine())!=null){
					if(line.startsWith("#"))continue;
					if(line.isEmpty())continue;
					String[] p=line.split("=");
					if(p.length!=2){
						Logger.w("ReadBlood", "Line %s  in %s is not formatted properly", line,file);
						continue;
					}
					int val;
					try {
						val=Integer.parseInt(p[1]);
					} catch (NumberFormatException e) {
						Logger.w("ReadBlood", "Line %s  in %s is not formatted properly", line,file);
						continue;
					}
					bloodValues.put(p[0], val);
				}
			} finally{
				if(br!=null){
					br.close();
				}
				r.close();
			}
		return bloodValues;
		
	}

}
