package de.teamlapen.vampirism.generation.villages;

import de.teamlapen.vampirism.util.Logger;
import de.teamlapen.vampirism.util.Pair;
import de.teamlapen.vampirism.util.REFERENCE;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the VillageBiomes.cfg (this is not a standard config file) Has methods to retrieve specific config lines based on codes at the beginning of each line Will copy VillageBiomesDefault.cfg from
 * resources to the config folder as VillageBiomes.cfg if it doesn't already exist
 * 
 * @author WILLIAM
 * TODO make sure this works in 1.8
 */
public class ConfigHandler {

	private static List<String> configLines = new ArrayList<String>();

	public static List<String> getAddBiomes() {
		return getAfterPrefix("+b:");
	}

	public static List<String> getAddTypes() {
		return getAfterPrefix("+t:");
	}

	private static List<String> getAfterPrefix(String prefix) {
		List<String> result = new ArrayList<String>();
		for (String line : configLines) {
			if (line.startsWith(prefix))
				result.add(line.substring(prefix.length()));
		}
		return result;
	}

	private static Map<Block, List<Pair<String, Integer>>> getMetaReplacement() {
		Map<Block, List<Pair<String, Integer>>> map = new HashMap<Block, List<Pair<String, Integer>>>();

		Pattern p = Pattern.compile("^~([\\w:]+),[\\w:]+?:(\\d+),([bt]:.+)");
		for (String line : configLines) {
			Matcher m = p.matcher(line);
			if (m.matches()) {
				Block b = Block.getBlockFromName(m.group(1));
				Integer meta = Integer.valueOf(m.group(2));
				String condition = m.group(3);

				if (!map.containsKey(b))
					map.put(b, new ArrayList<Pair<String, Integer>>());
				map.get(b).add(new Pair<String, Integer>(condition, meta));
			}
		}
		return map;
	}

	public static List<String> getRemoveBiomes() {
		return getAfterPrefix("-b:");
	}

	public static List<String> getRemoveTypes() {
		return getAfterPrefix("-t:");
	}

	public static Map<Block,List<Pair<String,IBlockState>>> getReplacements(){
		Map<Block,List<Pair<String,IBlockState>>> replacements=new HashMap<Block,List<Pair<String,IBlockState>>>();

		Map<Block, List<Pair<String, Block>>> blocks=getBlockReplacements();
		Map<Block, List<Pair<String, Integer>>> metas=getMetaReplacement();

		for(Block block:blocks.keySet()){
			List<Pair<String,IBlockState>> blockstates=new ArrayList<Pair<String, IBlockState>>();
			List<Pair<String, Integer>> meta=metas.get(block);
			for(Pair<String,Block> repBlock:blocks.get(block)){
				boolean replaced=false;
				if(meta!=null) {


					for (Pair<String, Integer> repMeta : meta) {
						if (repMeta.left.equals(repBlock.left)) {
							blockstates.add(new Pair<String, IBlockState>(repBlock.left, repBlock.right.getStateFromMeta(repMeta.right)));
							replaced = true;
							break;
						}
					}
				}
				if(!replaced){
					blockstates.add(new Pair<String, IBlockState>(repBlock.left,repBlock.right.getDefaultState()));
				}
			}
			replacements.put(block,blockstates);
		}
		return replacements;
	}
	private static Map<Block, List<Pair<String, Block>>> getBlockReplacements() {
		// TODO: Handle bad configs.
		Map<Block, List<Pair<String, Block>>> map = new HashMap<Block, List<Pair<String, Block>>>();

		Pattern p = Pattern.compile("^~([\\w:]+),([\\w:]+?)(?::\\d+)?,([bt]:.+)");
		for (String line : configLines) {
			Matcher m = p.matcher(line);
			if (m.matches()) {
				Block b = Block.getBlockFromName(m.group(1));
				Block replacement = Block.getBlockFromName(m.group(2));

				String condition = m.group(3);
				// Logger.i("ConfigHandler", "Will replace " + b.getUnlocalizedName() + " with " + replacement.getUnlocalizedName() + " where "
				// + condition);
				if (b.equals(replacement))
					continue;

				if(b==null||replacement==null){
					Logger.w("BiomeCfgHandler","Cannot read replacements from line %s",line);
					continue;
				}
				if (!map.containsKey(b))
					map.put(b, new ArrayList<Pair<String, Block>>());
				map.get(b).add(new Pair<String, Block>(condition, replacement));
			}
		}
		return map;
	}

	public static void loadConfig(File file) {
		try {
			if (file.createNewFile()) {
				try {
					InputStream defaultConf = VillageBiomes.class.getResourceAsStream("/VillageBiomesDefault.cfg");

					FileWriter writer = new FileWriter(file);
					IOUtils.copy(defaultConf, writer);
					writer.close();
					defaultConf.close();
				} catch (Exception e) {
					Logger.e("ConfigHandler", e.getMessage());
					e.printStackTrace();
				}
			}
			Scanner sc = new Scanner(file);
			while (sc.hasNextLine()) {
				configLines.add(sc.nextLine().trim());
			}
			sc.close();
		} catch (IOException e) {
			Logger.e("ConfigHandler","[%s] Can't load or create its config in %s.", REFERENCE.MODID, file.getAbsolutePath());
		}
	}
}