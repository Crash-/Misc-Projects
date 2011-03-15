package us.Crash.Slots;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

import com.nijikokun.bukkit.Permissions.*;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRightClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.anjocaido.groupmanager.GroupManager;

public class Slots extends JavaPlugin {

	public static ArrayList<SlotMachine> slotsList = new ArrayList<SlotMachine>();
	public static ArrayList<String> createSlot = new ArrayList<String>();
	public static ArrayList<String> removeSlot = new ArrayList<String>();
	public static ArrayList<SlotData> rollInfo = new ArrayList<SlotData>();
	private static ArrayList<String> noDebugList = new ArrayList<String>();
	public boolean isOPOnly = true;
	public int tickDelay = 50;
	public static Permissions Permissions = null;
	public static GroupManager GroupManager = null;
	private File configFile, saveFile, rollsFile;

	public static boolean hasPermission(Player p, String command){

		command = command.toLowerCase();

		if(Permissions == null && GroupManager == null)
			return p.isOp();

		if(Permissions != null){

			if(command.equals("create"))
				return Permissions.getHandler().permission(p, "slots.create");
			if(command.equals("remove"))
				return Permissions.getHandler().permission(p, "slots.remove");
			if(command.equals("save"))
				return Permissions.getHandler().permission(p, "slots.save");
			if(command.equals("reload"))
				return Permissions.getHandler().permission(p, "slots.load");
			if(command.equals("set"))
				return Permissions.getHandler().permission(p, "slots.set");

		} else if(GroupManager != null){
			
			if(command.equals("create"))
				return GroupManager.getWorldsHolder().getWorldPermissions(p).has(p, "slots.create");
			if(command.equals("remove"))
				return GroupManager.getWorldsHolder().getWorldPermissions(p).has(p, "slots.remove");
			if(command.equals("save"))
				return GroupManager.getWorldsHolder().getWorldPermissions(p).has(p, "slots.save");
			if(command.equals("reload"))
				return GroupManager.getWorldsHolder().getWorldPermissions(p).has(p, "slots.load");
			if(command.equals("set"))
				return GroupManager.getWorldsHolder().getWorldPermissions(p).has(p, "slots.set");
			
		}

		return true;

	}

	public static void outputMessage(Player p, String s){

		if(!noDebugList.contains(p.getName()))
			p.sendMessage(s);

	}

	@Override
	public void onDisable() {

		saveData();
		System.out.println("[Slots] Slots v" + getDescription().getVersion() + " disabled.");

	}

	@Override
	public void onEnable() {

		BListener blockListener = new BListener(this);
		rollsFile = new File("plugins/Slots/rolls.txt");
		configFile = new File("plugins/Slots/config.yml");
		saveFile = new File("plugins/Slots/slots.txt");
		if(new File("plugins/Slots/").mkdir())
			System.out.println("[Slots]Created Slots directory.");

		if(!configFile.exists()){

			try { configFile.createNewFile(); } catch (IOException e) {
				
				System.out.println("[Slots]Error creating config file.");
				getServer().getPluginManager().disablePlugin(this);
				return;
				
			}
			BufferedWriter out = null;
			try {

				out = new BufferedWriter(new FileWriter(configFile));

				out.write("tick-delay: 50\r\nop-only: true\r\n\r\npayout:\r\n    jackpot: 10000\r\n    red7: 3000\r\n    cherry: 1000\r\n    heart: 500\r\n    bar: 100\r\n");

				System.out.println("[Slots] Created/wrote default config.");

			} catch(Exception e){

				System.out.println("[Slots] Error writing default config file.");
				getServer().getPluginManager().disablePlugin(this);
				return;

			} finally {

				try { out.close(); } catch (IOException e) { }

			}

		}

		if(!saveFile.exists()){

			try { saveFile.createNewFile(); } catch(Exception e){

				System.out.println("[Slots] Error creating save file.");
				getServer().getPluginManager().disablePlugin(this);
				return;

			}
			System.out.println("[Slots] Created new save file.");

		}
		
		if(!rollsFile.exists()){
			
			try { rollsFile.createNewFile(); } catch(Exception e){
				
				System.out.println("[Slots] Error creating the rolls file.");
				getServer().getPluginManager().disablePlugin(this);
				return;
				
			}
			
			BufferedWriter out = null;
			try {
				
				out = new BufferedWriter(new FileWriter(rollsFile));
				
				out.write("name=Jackpot\r\nsymbol=J\r\npay=10000\r\nchance=1/8\r\ncolor=13\r\n\r\n" +
						  "name=Red 7\r\nsymbol=7\r\npay=3000\r\nchance=1/8\r\ncolor=12\r\n\r\n" +
						  "name=Cherry\r\nsymbol=C\r\npay=1000\r\nchance=1/6\r\ncolor=4\r\n\r\n" +
						  "name=Heart\r\nsymbol=H\r\npay=500\r\nchance=1/4\r\ncolor=11\r\n\r\n" +
						  "name=Bar\r\nsymbol=B\r\npay=100\r\nchance=1/3\r\ncolor=10\r\n\r\n");
				
				System.out.println("[Slots] Created/Wrote default rolls.");
				
			} catch(Exception e){
				
				System.out.println("[Slots] Error when writing to the rolls file.");
				getServer().getPluginManager().disablePlugin(this);
				return;
				
			} finally {
				
				try { out.close(); } catch (IOException e) { }
				
			}
			
			
		}

		Permissions = (Permissions)getServer().getPluginManager().getPlugin("Permissions");
		if(Permissions == null){

			GroupManager = (GroupManager)getServer().getPluginManager().getPlugin("GroupManager");

			if(GroupManager == null)
				if(isOPOnly)
					System.out.println("[Slots] Cannot find Permissions, switching to OP-only.");
				else
					System.out.println("[Slots] Warning : Cannot find a permissions plugin, there are no restrictions on commands now!");

		}

		if(Permissions != null){

			System.out.println("[Slots] Using Permissions plugin for permissions.");

		} else if(GroupManager != null){

			System.out.println("[Slots] Using GroupManager plugin for permissions.");

		}

		if(getServer().getPluginManager().getPlugin("iConomy") == null){

			System.out.println("[Slots] Error : Unable to find iConomy plugin.");
			getServer().getPluginManager().disablePlugin(this);
			return;

		}

		loadData();

		getServer().getPluginManager().registerEvent(Event.Type.BLOCK_RIGHTCLICKED, blockListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.BLOCK_DAMAGED, blockListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.BLOCK_BREAK, blockListener, Event.Priority.Normal, this);

		System.out.println("[Slots] Slots v" + getDescription().getVersion() + " enabled, by Crash");

	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {

		if(!(sender instanceof Player))
			return false;

		Player p = (Player)sender;
		
		if(command.getName().equalsIgnoreCase("slots")){
			
			if(args.length == 0){

				p.sendMessage(ChatColor.RED + "You must put in an argument.");
				return false;

			}

			if(!hasPermission(p, args[0])){

				p.sendMessage(ChatColor.RED + "You can't use this command!");
				return false;

			}
			
			if(args[0].equalsIgnoreCase("try")){
				
				p.sendMessage(ChatColor.getByCode(Integer.parseInt(args[1])) + "YES!");
				
			}

			if(args[0].equalsIgnoreCase("create")){

				createSlot.add(p.getName());
				p.sendMessage(ChatColor.GOLD + "Left click a sign to create the machine!");
				return true;

			} else if(args[0].equalsIgnoreCase("remove")){

				removeSlot.add(p.getName());
				p.sendMessage(ChatColor.GOLD + "Left click a sign to remove the machine!");
				return true;

			} else if(args[0].equalsIgnoreCase("message")){

				if(noDebugList.contains(p.getName()))
					noDebugList.remove(p.getName());
				else
					noDebugList.add(p.getName());

				p.sendMessage(ChatColor.GOLD + "Toggled messages.");
				
				return true;

			} else if(args[0].equals("load")){

				slotsList.clear();
				slotsList = new ArrayList<SlotMachine>();
				if(loadData())
					p.sendMessage(ChatColor.GREEN + "Reloaded files successfully.");
				else
					p.sendMessage(ChatColor.RED + "There was an error when reloading.");
				
				return true;

			} else if(args[0].equals("save")){

				saveData();
				p.sendMessage(ChatColor.GREEN + "Saved files successfully.");
				return true;

			} else if(args[0].equals("set")){
				
				if(args.length < 2){
					
					p.sendMessage(ChatColor.RED + "Wrong amount of arguments.");
					return false;
					
				}
				
				args[1] = args[1].toLowerCase();
				
				if(args[1].equals("payout")){
					
					Integer ind = null, newpay = null;
					
					try {
						
						ind = Integer.parseInt(args[2]);
						
					} catch(Exception e){
						
						p.sendMessage(ChatColor.RED + "Error getting pay index.");
						return false;
						
					}
					
					if(ind < 0 || ind > rollInfo.size()){
						
						p.sendMessage(ChatColor.RED + "The index must be 0 through " + rollInfo.size() + ".");
						return false;
						
					}
					
					try {
						
						newpay = Integer.parseInt(args[3]);
						
					} catch(Exception e){
						
						p.sendMessage(ChatColor.RED + "Error getting new pay amount.");
						return false;
						
					}
					
					rollInfo.get(ind).setPay(newpay);
					
					p.sendMessage(ChatColor.GREEN + "Successfully set the new pay.");
					
					return true;
					
				} else if(args[1].equals("ops")){
					
					isOPOnly = !isOPOnly;
					
					p.sendMessage(ChatColor.GREEN + "Toggled op-only.");
					return true;
					
				} else if(args[1].equals("ticks")){
					
					Integer newtick = null;
					
					try {
						
						newtick = Integer.parseInt(args[2]);
						
					} catch(Exception e){
						
						p.sendMessage(ChatColor.RED + "Error getting the new tick delay.");
						return false;
						
					}
					
					if(newtick < 0){
						
						p.sendMessage(ChatColor.RED + "You must put in a delay greater than 0");
						return false;
						
					}
					
					tickDelay = newtick;
					p.sendMessage(ChatColor.GREEN + "You set the new tick delay.");
					return true;
					
				}
				
			}

		}

		return false;

	}

	public boolean loadData(){

		Configuration config = new Configuration(configFile);
		config.load();
		
		isOPOnly = config.getBoolean("op-only", true);
		tickDelay = config.getInt("tick-delay", 50);

		Scanner s = null;

		try {

			s = new Scanner(saveFile);

			int i = 0;

			while(s.hasNextLine()){

				try {

					String line = s.nextLine();
					if(line == null){

						i++;
						continue;

					}

					String signLine = line.split("\"=")[0].substring(1);
					String[] data = line.split("\"=")[1].split(",");

					int w = Integer.parseInt(data[0]), x = Integer.parseInt(data[1]), y = Integer.parseInt(data[2]), z = Integer.parseInt(data[3]);
					double cost = Double.parseDouble(data[4]);

					Block b = getServer().getWorlds().get(w).getBlockAt(x, y, z);

					if(b.getTypeId() != 63 && b.getTypeId() != 68){

						System.out.println("[Slots]Line " + (i + 1) + " was found to not be a sign.");
						i++;
						continue;

					}

					((Sign)b.getState()).setLine(0, ChatColor.YELLOW + "[Slots]");
					((Sign)b.getState()).setLine(1, "" + cost);
					((Sign)b.getState()).setLine(2, "  |   |  ");
					((Sign)b.getState()).setLine(3, signLine);
					((Sign)b.getState()).update();

					slotsList.add(new SlotMachine(this, b, cost));

					i++;

				} catch(Exception e){

					System.out.println("[Slots]Error loading slot on line " + (i + 1) + ".");

				}

			}
			
			s.close();

		} catch(Exception e){

			System.out.println("[Slots]Error when opening save file.");
			return false;

		}
		
		try {
			
			s = new Scanner(rollsFile);
			
			String name = "", symb, line;
			int pay, chance1, chance2;
			boolean hadError = false;
			
			while(s.hasNextLine()){
				
				line = s.nextLine();
				
				if(line == null || line.isEmpty())
					continue;
				
				if(hadError){
					
					if(line.split("=")[0].equals("name"))
						hadError = false;
					else
						continue;
					
				}
				
				try {

					name = line.split("=")[1];
					line = s.nextLine();
					symb = line.split("=")[1];
					line = s.nextLine();
					pay = Integer.parseInt(line.split("=")[1]);
					line = s.nextLine().split("=")[1];
					chance1 = Integer.parseInt(line.split("/")[0]);
					chance2 = Integer.parseInt(line.split("/")[1]);
					line = s.nextLine().split("=")[1];
					rollInfo.add(new SlotData(name, symb, ChatColor.getByCode(Integer.parseInt(line)), pay, chance1, chance2));
					
				} catch(Exception e){
					
					System.out.println("[Slots] Error with roll " + name + ".");
					
				}
				
			}
			
		} catch(Exception e){
			
			System.out.println("[Slots] Error when opening rolls file.");
			
		}

		return true;

	}

	public void saveData(){

		BufferedWriter out = null;
		try {

			out = new BufferedWriter(new FileWriter(configFile));

			out.write("tick-delay: " + tickDelay + "\r\nop-only: " + isOPOnly + "\r\n");

		}catch(Exception e){

			System.out.println("[Slots]Error writing config file.");
			return;

		} finally {

			try { out.close(); } catch (IOException e) { }

		}

		try {

			out = new BufferedWriter(new FileWriter(saveFile));

			for(SlotMachine m : slotsList){

				StringBuilder builder = new StringBuilder();
				builder.append("\"").append(m.getSign().getLine(3)).append("\"=").append(getServer().getWorlds().indexOf(m.getBlock().getWorld())).
				append(",").append(m.getBlock().getLocation().getBlockX()).append(",").append(m.getBlock().getLocation().getBlockY()).
				append(",").append(m.getBlock().getLocation().getBlockZ()).append(",").append(m.getCost()).append("\r\n");
				out.write(builder.toString());

			}

		} catch(Exception e){

			System.out.println("[Slots]Error writing slots file.");
			return;

		} finally {

			try { out.close(); } catch (IOException e) { }

		}
		
		try {
			
			out = new BufferedWriter(new FileWriter(rollsFile));
			
			for(SlotData d : rollInfo){
				
				StringBuilder builder = new StringBuilder();
				builder.append("name=").append(d.getName()).append("\r\n").
						append("symbol=").append(d.getSymbol()).append("\r\n").
						append("pay=").append(d.getPay()).append("\r\n").
						append("chance=").append(d.getNumerator()).append("/").append(d.getDenominator()).append("\r\n").
						append("color=").append(d.getChatColor().getCode()).append("\r\n\r\n");
				out.write(builder.toString());
				
			}
			
		} catch(Exception e){
			
			System.out.println("[Slots]Error writing rolls file.");
			return;
			
		} finally {
			
			try { out.close(); } catch (IOException e) { }
			
		}

	}

	public static SlotMachine getMachine(Block b){

		for(SlotMachine m : slotsList){

			if(m.isSelf(b))
				return m;

		}

		return null;

	}

}

class BListener extends BlockListener {

	Slots plugin;

	public BListener(Slots p){

		plugin = p;

	}

	@Override
	public void onBlockBreak(BlockBreakEvent event){

		Block b = event.getBlock();
		SlotMachine m = Slots.getMachine(b);

		if(m == null)
			return;

		event.setCancelled(true);
		m.getSign().update();
		event.getPlayer().sendMessage(ChatColor.RED + "You must use the remove command first to remove the slot machine!");

	}

	@Override
	public void onBlockDamage(BlockDamageEvent event){

		if(event.getBlock().getTypeId() == 63 || event.getBlock().getTypeId() == 68){

			if(Slots.createSlot.contains(event.getPlayer().getName())){

				Sign sign = (Sign)event.getBlock().getState();
				Slots.createSlot.remove(event.getPlayer().getName());

				if(ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("[Slots]")){

					Double cost = null;

					if(Slots.getMachine(event.getBlock()) != null){

						event.getPlayer().sendMessage(ChatColor.RED + "This is already a slot machine!");
						return;

					}

					try {

						cost = Double.parseDouble(sign.getLine(1));

					} catch(Exception e){

						event.getPlayer().sendMessage(ChatColor.RED + "Unable to get the cost..");
						return;

					}

					Slots.slotsList.add(new SlotMachine(plugin, event.getBlock(), cost));
					sign.setLine(0, ChatColor.YELLOW + sign.getLine(0));
					sign.update();
					event.getPlayer().sendMessage(ChatColor.GREEN + "Slot machine created.");

				}

			}
			if(Slots.removeSlot.contains(event.getPlayer().getName())){

				Slots.removeSlot.remove(event.getPlayer().getName());

				SlotMachine m = Slots.getMachine(event.getBlock());
				if(m == null)
					return;

				Slots.slotsList.remove(m);

				m.stopRoller();
				m.getSign().setLine(0, "[Slots]");

				event.getPlayer().sendMessage(ChatColor.GREEN + "The slot machine was removed.");

			}

		}

	}

	@Override
	public void onBlockRightClick(BlockRightClickEvent event){

		Block b = event.getBlock();

		SlotMachine m = Slots.getMachine(b);
		if(m == null)
			return;

		if(!m.isRolling())
			m.rollSlots(event.getPlayer());
		else
			event.getPlayer().sendMessage(ChatColor.RED + "This slot machine is already rolling.");


	}

}
