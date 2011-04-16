package com.Crash.Shift;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.util.config.Configuration;
import org.bukkit.event.*;

import com.nijikokun.bukkit.Permissions.*;

public class ShiftPlugin extends JavaPlugin {

	private static long timeToCool;
	private static int moveToolId, destroyToolId, maxMoveBlocks, maxDestroyBlocks;
	private static boolean movePlayers, moveMobs, moveAttached, moveAttachDiagonals, destroyAttachDiagonals;
	private static File configFile;
	private Permissions Permissions;
	private PListener playerListener = new PListener(this);
	private ArrayList<ShiftUser> shiftUsers = new ArrayList<ShiftUser>();
	private ArrayList<Integer> allowedDestroyableItems = new ArrayList<Integer>();
	private ArrayList<Integer> allowedMovableItems = new ArrayList<Integer>();
	private ArrayList<Integer> allowedAttachedItems = new ArrayList<Integer>();
	private HashMap<String, Long> cooldownTime = new HashMap<String, Long>();

	public static void outputConsole(String s){

		System.out.println("[Shift] " + s);

	}

	public ShiftUser getUser(String s){
		
		for(ShiftUser user : shiftUsers)
			if(user.equals(s))
				return user;
		
		return null;
		
	}

	public boolean isMoving(Player p){

		ShiftUser user = getUser(p.getName());
		if(user == null)
			return false;
		
		return user.isMoving();

	}

	public boolean isDestroying(Player p){

		ShiftUser user = getUser(p.getName());
		if(user == null)
			return false;

		return user.isDestroying();

	}

	public boolean hasPermission(String cmd, Player p){

		if(Permissions == null)
			return p.isOp();

		return Permissions.getHandler().permission(p, "shift." + cmd);

	}

	public void startCoolDown(String name){

		if(timeToCool > 0)
			cooldownTime.put(name, System.currentTimeMillis());

	}

	public boolean hasCooledDown(String name){

		if(timeToCool <= 0)
			return true;
		
		Long useTime = cooldownTime.get(name);
		if(useTime == null)
			return true;

		if(System.currentTimeMillis() - useTime > timeToCool){

			cooldownTime.remove(name);
			return true;

		}

		return false;

	}

	public static int getMoveToolId(){ return moveToolId; }

	public static int getDestroyToolId(){ return destroyToolId; }

	public static int getMaxBlocksToMove(){ return maxMoveBlocks; }

	public static int getMaxBlocksToDestroy(){ return maxDestroyBlocks; }

	public static long getCoolTime(){ return timeToCool; }

	public static boolean movePlayers(){ return movePlayers; }

	public static boolean moveMobs(){ return moveMobs; }

	public ArrayList<ShiftUser> getUsers(){ return shiftUsers; }

	public ArrayList<Integer> getAllowedDestroyableItems(){
		
		return allowedDestroyableItems;
		
	}
	
	public ArrayList<Integer> getAllowedAttachedItems(){

		return allowedAttachedItems;

	}

	public ArrayList<Integer> getAllowedItems(){

		return allowedMovableItems;

	}

	public boolean canAttach(int id){

		return allowedMovableItems.contains(id) || allowedAttachedItems.contains(id);

	}

	@Override
	public void onDisable() {
		
		outputConsole("Shift disabled.");

	}


	@Override
	public void onEnable() {

		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, playerListener, Event.Priority.Normal, this);

		configFile = new File("plugins/Shift/config.yml");
		
		if(new File("plugins/Shift/").mkdir() || !configFile.exists()){
			
			try {
				configFile.createNewFile();
			} catch (IOException e) {
				outputConsole("Error making the config file.");
				getServer().getPluginManager().disablePlugin(this);
				return;
			}
			
			loadConfig(configFile);
			saveConfig(configFile);
			
			outputConsole("Created default config file.");
			
		} else {
		
			loadConfig(configFile);
			
		}

		Permissions = (Permissions)getServer().getPluginManager().getPlugin("Permissions");

		if(Permissions == null)
			outputConsole("Permissions not found, defaulting to OP-Only");
		else
			outputConsole("Found and hooked Permissions plugin.");

		outputConsole("Shift v" + getDescription().getVersion() + " enabled, by Crash");

	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){

		if(!(sender instanceof Player))
			return false;

		Player p = (Player)sender;

		if(command.getName().equalsIgnoreCase("shift")){

			if(args.length == 0)
				p.sendMessage(ChatColor.RED + "You must put in move or destroy to toggle their usage!");
			else { 

				if(hasPermission(args[0], p)){

					if(args[0].equalsIgnoreCase("move")){

						ShiftUser user = getUser(p.getName());
						if(user == null){

							user = new ShiftUser(p.getName());
							shiftUsers.add(user);

						}

						p.sendMessage(ChatColor.GREEN + "Moving is toggled to " + (user.toggleMoving() ? "on" : "off") + ".");

					} else if(args[0].equalsIgnoreCase("destroy")){

						ShiftUser user = getUser(p.getName());
						if(user == null){

							user = new ShiftUser(p.getName());
							shiftUsers.add(user);

						}

						p.sendMessage(ChatColor.GREEN + "Destroying is toggled to " + (user.toggleDestroying() ? "on" : "off") + ".");

					} else if(args[0].equalsIgnoreCase("reload")){
						
						loadConfig(configFile);
						p.sendMessage(ChatColor.GREEN + "Saved config successfully.");
						
					} else {

						p.sendMessage(ChatColor.RED + "Unknown command.");

					}

				} else {

					p.sendMessage(ChatColor.RED + "You don't have the permission to use this command.");

				}

			}

			return true;			

		}

		return false;

	}

	public void saveConfig(File f){

		Configuration config = new Configuration(f);
		config.setProperty("move-tool.tool-id", moveToolId);
		config.setProperty("move-tool.maxblocks", maxMoveBlocks);
		config.setProperty("move-tool.move-cooltime", timeToCool);
		config.setProperty("move-tool.moveable-blocks", allowedMovableItems);
		config.setProperty("move-tool.move-attached", moveAttached);
		config.setProperty("move-tool.attached-moveable-blocks", allowedAttachedItems);
		config.setProperty("move-tool.move-players", movePlayers);
		config.setProperty("move-tool.move-mobs", moveMobs);
		config.setProperty("move-tool.attach-to-diagonals", moveAttachDiagonals);
		config.setProperty("destroy-tool.tool-id", destroyToolId);
		config.setProperty("destroy-tool.maxblocks", maxDestroyBlocks);
		config.setProperty("destroy-tool.destroyable-blocks", allowedDestroyableItems);
		config.setProperty("destroy-tool.attach-to-diagonals", destroyAttachDiagonals);
		config.save();
		
	}

	public void loadConfig(File f){

		Configuration config = new Configuration(f);
		config.load();
		moveToolId = config.getInt("move-tool.tool-id", 262);
		maxMoveBlocks = config.getInt("move-tool.maxblocks", 100);
		timeToCool = config.getInt("move-tool.move-cooltime", 0);
		List<Object> blockIds = config.getList("move-tool.moveable-blocks");
		if(blockIds != null){
			for(Object o : blockIds)
				if(o instanceof Integer)
					allowedMovableItems.add((Integer)o);
		} else {
			
			allowedMovableItems.add(1);
			
		}

		moveAttached = config.getBoolean("move-tool.move-attached", false);
		if(moveAttached){

			blockIds = config.getList("move-tool.attached-moveable-blocks");
			if(blockIds != null){
				for(Object o : blockIds)
					if(o instanceof Integer)
						allowedAttachedItems.add((Integer)o);
			} else { 
			
				allowedAttachedItems.add(1);
				
			}
			
		} else {
			
			allowedAttachedItems.add(1);
			
		}
		movePlayers = config.getBoolean("move-tool.move-players", true);
		moveMobs = config.getBoolean("move-tool.move-mobs", true);
		moveAttachDiagonals = config.getBoolean("move-tool.attach-to-diagonals", true);

		destroyToolId = config.getInt("destroy-tool.tool-id", 280);
		maxDestroyBlocks = config.getInt("destroy-tool.maxblocks", 100);

		blockIds = config.getList("destroy-tool.destroyable-blocks");
		if(blockIds != null){
			for(Object o : blockIds)
				if(o instanceof Integer)
					allowedDestroyableItems.add((Integer)o);	
		} else {
			
			allowedDestroyableItems.add(1);
			
		}
		destroyAttachDiagonals = config.getBoolean("destroy-tool.attach-to-diagonals", false);

	}

	public void getConnectedBlocks(Block b, ArrayList<Block> connected, int maxBlocks){

		if(connected == null)
			return;

		if(connected.size() == 0)
			return;

		if(connected.size() > maxBlocks)
			connected.clear();
		
		BlockFace[] checkDirs = null;

		if(destroyAttachDiagonals){

			checkDirs = BlockFace.values();

		} else {

			checkDirs = new BlockFace[6];
			checkDirs[0] = BlockFace.NORTH;
			checkDirs[1] = BlockFace.EAST;
			checkDirs[2] = BlockFace.WEST;
			checkDirs[3] = BlockFace.SOUTH;
			checkDirs[4] = BlockFace.UP;
			checkDirs[5] = BlockFace.DOWN;

		}

		for(int i = 0; i < checkDirs.length && connected.size() > 0; i++){

			Block block = b.getFace(checkDirs[i]);

			if(b.getTypeId() == block.getTypeId() && !connected.contains(block)){

				connected.add(block);

				getConnectedBlocks(block, connected, maxBlocks);

			}

		}

	}

	public boolean isSpecialBlock(Block b){

		return b.getTypeId() == 6 || b.getTypeId() == 35 || b.getTypeId() == 37 || b.getTypeId() == 38 || b.getTypeId() == 39 || b.getTypeId() == 40 || b.getTypeId() == 50 || b.getTypeId() == 51 || b.getTypeId() == 53 || b.getTypeId() == 55 || b.getTypeId() == 59 || b.getTypeId() == 63 || b.getTypeId() == 64 || b.getTypeId() == 65 || b.getTypeId() == 66 || b.getTypeId() == 68 || b.getTypeId() == 69 || b.getTypeId() == 71 || b.getTypeId() == 75 || b.getTypeId() == 76 || b.getTypeId() == 77 || b.getTypeId() == 78 || b.getTypeId() == 83 || b.getTypeId() == 90;

	}

	public void getAll(Block b, BlockStructure struct){

		if(struct.isFinished())
			return;

		if(struct.getNumBlocksInStructure() >= maxMoveBlocks || (b.getFace(struct.getDirection()).getTypeId() != 0 && b.getFace(struct.getDirection()).getTypeId() != b.getTypeId() && !canAttach(b.getFace(struct.getDirection()).getTypeId()))){

			struct.setFinished();
			return;

		}

		if(isSpecialBlock(b))
			struct.addSpecialCaseBlock(b);
		else
			struct.addToStructure(b);

		BlockFace[] checkDirs = null;

		if(moveAttachDiagonals){

			checkDirs = BlockFace.values();

		} else {

			checkDirs = new BlockFace[6];
			checkDirs[0] = BlockFace.NORTH;
			checkDirs[1] = BlockFace.EAST;
			checkDirs[2] = BlockFace.WEST;
			checkDirs[3] = BlockFace.SOUTH;
			checkDirs[4] = BlockFace.UP;
			checkDirs[5] = BlockFace.DOWN;

		}

		for(int i = 0; i < checkDirs.length && !struct.isFinished(); i++){

			if(checkDirs[i].equals(BlockFace.SELF))
				continue;

			Block block = b.getFace(checkDirs[i]);

			if((b.getTypeId() == block.getTypeId() || allowedAttachedItems.contains(block.getTypeId())) && !struct.hasBlock(block)){

				getAll(block, struct);

			}

		}

	}

}

class ShiftUser {

	private boolean moving, destroying;
	private String name;

	public ShiftUser(String userName){

		name = userName;
		moving = false;
		destroying = false;

	}

	public String getName(){ return name; }

	public boolean toggleMoving(){ moving = !moving; return moving; }

	public boolean toggleDestroying(){ destroying = !destroying; return destroying; }

	public boolean isMoving(){ return moving; }

	public boolean isDestroying(){ return destroying; }


	@Override
	public boolean equals(Object o){

		if(o instanceof String){

			if(getName().equalsIgnoreCase((String)o))
				return true;

		} else if(o instanceof ShiftUser){

			if(getName().equalsIgnoreCase(((ShiftUser)o).getName()))
				return true;

		}

		return false;

	}

}

class PListener extends PlayerListener{

	private ShiftPlugin plugin;

	public PListener(ShiftPlugin instance) {

		plugin = instance;

	}

	@Override
	public void onPlayerQuit(PlayerQuitEvent event){

		String name = event.getPlayer().getName();

		plugin.getUsers().remove(name);

	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event){

		if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getAction().equals(Action.LEFT_CLICK_BLOCK)){

			
			if(event.getItem() != null && event.getItem().getTypeId() == ShiftPlugin.getMoveToolId() && plugin.hasCooledDown(event.getPlayer().getName()) && plugin.isMoving(event.getPlayer())){

				int type = event.getClickedBlock().getTypeId();

				if(plugin.getAllowedItems().contains(type)){

					BlockFace direction = event.getBlockFace();

					if(event.getAction().equals(Action.LEFT_CLICK_BLOCK))
						direction = direction.getOppositeFace();

					BlockStructure struct = new BlockStructure(direction);

					plugin.getAll(event.getClickedBlock(), struct);

					if(struct.isFinished())
						return;

					struct.moveBlocks();

					struct.moveEntities();

					plugin.startCoolDown(event.getPlayer().getName());

				}

			} else if(event.getItem() != null && event.getItem().getTypeId() == ShiftPlugin.getDestroyToolId()){

				if(event.getAction().equals(Action.LEFT_CLICK_BLOCK) && plugin.isDestroying(event.getPlayer())){

					ArrayList<Block> blocksToClear = new ArrayList<Block>();

					blocksToClear.add(event.getClickedBlock());

					plugin.getConnectedBlocks(event.getClickedBlock(), blocksToClear, ShiftPlugin.getMaxBlocksToDestroy());

					for(Block b : blocksToClear){

						b.setTypeId(0);

					}

				}

			}

		}

	}

}

class BlockStructure {

	private ArrayList<Block> blocksInStructure;
	private ArrayList<Block> nextLocations;
	private ArrayList<Integer> blockTypeValues;
	private ArrayList<Entity> entitiesOnStructure;
	private ArrayList<Block> specialBlocks;
	private ArrayList<Block> specialNextLocations;
	private ArrayList<Integer> specialTypeValues;
	private BlockFace moveDirection;
	private boolean isFinished = false;

	public BlockStructure(BlockFace moveDir){

		blocksInStructure = new ArrayList<Block>();
		nextLocations = new ArrayList<Block>();
		blockTypeValues = new ArrayList<Integer>();
		entitiesOnStructure = new ArrayList<Entity>();
		specialBlocks = new ArrayList<Block>();
		specialNextLocations = new ArrayList<Block>();
		specialTypeValues = new ArrayList<Integer>();
		moveDirection = moveDir;

	}

	public boolean hasEntity(Entity e){

		return entitiesOnStructure.contains(e);

	}

	public boolean hasBlock(Block b){

		return blocksInStructure.contains(b) || specialBlocks.contains(b);

	}

	public void addToStructure(Block b){

		blocksInStructure.add(b);
		blockTypeValues.add(b.getTypeId());
		nextLocations.add(b.getFace(moveDirection));

	}

	public void addEntity(Entity e){

		entitiesOnStructure.add(e);

	}

	public void addSpecialCaseBlock(Block b){

		specialBlocks.add(b);
		specialNextLocations.add(b.getFace(moveDirection));
		specialTypeValues.add(b.getTypeId());

	}

	public BlockFace getDirection(){ return moveDirection; }

	public int getNumBlocksInStructure(){ return blocksInStructure.size() + specialBlocks.size(); }

	public void setFinished(){ isFinished = true; }

	public boolean isFinished(){ return isFinished; }

	public boolean isNonSolidBlock(Block b){

		return b.getTypeId() == 0 || !net.minecraft.server.Block.byId[b.getTypeId()].a();

	}

	public Block getClosestBlock(Player p, Block below){

		BlockFace[] around = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

		Block closestFound = null;
		double distance = 99;

		for(BlockFace face : around){

			Block testBlock = below.getFace(face);

			double testDistance = p.getLocation().toVector().distance(testBlock.getLocation().toVector().add(new Vector(0.5, 0.5 , 0.5)));

			if(testDistance < distance){

				distance = testDistance;
				closestFound = testBlock;

			}

		}

		return closestFound;

	}

	public void moveBlocks(){

		for(Block b : blocksInStructure){

			Block above = b.getFace(BlockFace.UP);
			if(isNonSolidBlock(above) && (ShiftPlugin.moveMobs() || ShiftPlugin.movePlayers())){

				Entity[] entities = b.getChunk().getEntities();
				int x = above.getLocation().getBlockX(), y = above.getLocation().getBlockY(), z = above.getLocation().getBlockZ();
				for(Entity e : entities){

					if(e instanceof Player){
						if(!ShiftPlugin.movePlayers())
							continue;
					} else {
						if(!ShiftPlugin.moveMobs())
							continue;
					}

					if(e instanceof Player){

						Player p = (Player)e;
						Block below = p.getLocation().getBlock().getFace(BlockFace.DOWN);
						if(p.isSneaking() && below.getTypeId() == 0){

							Block block = getClosestBlock(p, below);
							if(block.getFace(BlockFace.UP).equals(above))
								this.addEntity(e);

						}

					}

					if(x == e.getLocation().getBlockX() 
							&& y == e.getLocation().getBlockY()
							&& z == e.getLocation().getBlockZ() 
							&& e.getWorld().equals(b.getWorld()) && !this.hasEntity(e))
						this.addEntity(e);

				}

			}

		}

		for(Block b : specialBlocks){

			Block above = b.getFace(BlockFace.UP);
			if(isNonSolidBlock(above) && (ShiftPlugin.moveMobs() || ShiftPlugin.movePlayers())){

				Entity[] entities = b.getChunk().getEntities();
				int x = above.getLocation().getBlockX(), y = above.getLocation().getBlockY(), z = above.getLocation().getBlockZ();
				for(Entity e : entities){

					if(e instanceof Player){
						if(!ShiftPlugin.movePlayers())
							continue;
					} else {
						if(!ShiftPlugin.moveMobs())
							continue;
					}

					if(e instanceof Player){

						Player p = (Player)e;
						Block below = p.getLocation().getBlock().getFace(BlockFace.DOWN);
						if(p.isSneaking() && below.getTypeId() == 0){

							Block block = getClosestBlock(p, below);
							if(block.getFace(BlockFace.UP).equals(above))
								this.addEntity(e);

						}

					}

					if(x == e.getLocation().getBlockX() 
							&& y == e.getLocation().getBlockY()
							&& z == e.getLocation().getBlockZ() 
							&& e.getWorld().equals(b.getWorld()) && !this.hasEntity(e))
						this.addEntity(e);

				}

			}

		}

		ArrayList<Byte> dataValues = new ArrayList<Byte>();

		for(Block b : specialBlocks){

			dataValues.add(b.getData());
			b.setTypeId(0);

		}

		for(Block b : blocksInStructure)
			b.setTypeId(0);

		for(int i = 0; i < nextLocations.size(); i++)
			nextLocations.get(i).setTypeId(blockTypeValues.get(i));

		for(int i = 0; i < specialTypeValues.size(); i++){

			specialNextLocations.get(i).setTypeId(specialTypeValues.get(i));
			specialNextLocations.get(i).setData(dataValues.get(i));

		}

	}

	public void moveEntities(){

		for(Entity e : entitiesOnStructure)
			e.teleport(new Location(e.getWorld(), e.getLocation().getX() + moveDirection.getModX(), e.getLocation().getY() + moveDirection.getModY(), e.getLocation().getZ() + moveDirection.getModZ(), e.getLocation().getYaw(), e.getLocation().getPitch()));

	}

}