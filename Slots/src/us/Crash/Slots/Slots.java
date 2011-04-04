package us.Crash.Slots;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

import com.nijiko.coelho.iConomy.iConomy;
import com.nijiko.coelho.iConomy.system.Account;
import com.nijikokun.bukkit.Permissions.*;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

public class Slots extends JavaPlugin {

	private static ArrayList<SlotMachine> slotsList = new ArrayList<SlotMachine>();
	private static SlotSelectors Selectors = new SlotSelectors();
	private static ArrayList<SlotCombo> comboList = new ArrayList<SlotCombo>();
	private static ArrayList<SlotData> rollInfo = new ArrayList<SlotData>();
	private static ArrayList<String> noDebugList = new ArrayList<String>();
	public boolean isOPOnly = true, requireOwnership = false;
	public int tickDelay = 50;
	public static Permissions Permissions = null;
	private File configFile, saveFile, rollsFile, combosFile;

	public static ArrayList<SlotMachine> getSlotList(){ return slotsList; }
	
	public static SlotSelectors getSlotSelectors(){ return Selectors; }
	
	public static ArrayList<SlotCombo> getSlotCombos(){ return comboList; }
	
	public static ArrayList<SlotData> getSlotData(){ return rollInfo; }
	
	public static ArrayList<String> getNoDebugList(){ return noDebugList; }
	
	public static boolean hasPermission(Player p, String command){

		command = command.toLowerCase();

		if(Permissions == null)
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
			if(command.equals("info"))
				return Permissions.getHandler().permission(p, "slots.info");

		}

		return true;

	}

	public static void outputMessage(Player p, String s){

		if(!noDebugList.contains(p.getName()))
			p.sendMessage(s);

	}

	@Override
	public void onDisable() {

		System.out.println("[Slots] Slots v" + getDescription().getVersion() + " disabled.");

	}

	@Override
	public void onEnable() {

		BListener blockListener = new BListener(this);
		PListener playerListener = new PListener(this);
		rollsFile = new File("plugins/Slots/rolls.txt");
		configFile = new File("plugins/Slots/config.yml");
		saveFile = new File("plugins/Slots/slots.txt");
		combosFile = new File("plugins/Slots/combos.txt");

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

				out.write("tick-delay: 50\r\nop-only: true\r\nrequire-ownership: false\r\n");

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

		if(!combosFile.exists()){

			try { combosFile.createNewFile(); } catch(Exception e){

				System.out.println("[Slots] Error creating the combos file.");
				getServer().getPluginManager().disablePlugin(this);
				return;

			}

			System.out.println("[Slots] Created new combos file.");

		}

		Permissions = (Permissions)getServer().getPluginManager().getPlugin("Permissions");
		if(Permissions == null){
				if(isOPOnly)
					System.out.println("[Slots] Cannot find Permissions, switching to OP-only.");
				else
					System.out.println("[Slots] Warning : Cannot find a permissions plugin, there are no restrictions on commands now!");
		}

		if(Permissions != null){

			System.out.println("[Slots] Using Permissions plugin for permissions.");

		}
		
		if(getServer().getPluginManager().getPlugin("iConomy") == null){

			System.out.println("[Slots] Error : Unable to find iConomy plugin.");
			getServer().getPluginManager().disablePlugin(this);
			return;

		}

		loadData();

		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.BLOCK_BREAK, blockListener, Event.Priority.High, this);

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

			if(args[0].equalsIgnoreCase("create")){

				String name = "";

				if(!requireOwnership){

					if(args.length == 2){

						name = args[1];

						if(name == null || !iConomy.getBank().hasAccount(name)){

							p.sendMessage(ChatColor.RED + name + " has no iConomy account!");
							return false;

						}

					}
					Selectors.addNewCreator(p, name);
				
				} else {
					
					Selectors.addNewCreator(p, p.getName());
					
				}
				p.sendMessage(ChatColor.GOLD + "Left click a sign to create the machine!");
				return true;

			} else if(args[0].equalsIgnoreCase("remove")){

				Selectors.addNewSelector(p, 1);
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

				} else if(args[1].equals("ownership")){
					
					requireOwnership = !requireOwnership;
					p.sendMessage(ChatColor.GREEN + "Toggled require-ownership.");
					return true;
					
				}

			} else if(args[0].equalsIgnoreCase("info")){
				
				Selectors.addNewSelector(p, 2);
				p.sendMessage(ChatColor.GREEN + "Left click on a slot machine to get info.");
				
			}

		}

		return false;

	}

	public boolean loadData(){

		ArrayList<SlotMachine> tempSlots = new ArrayList<SlotMachine>();
		ArrayList<SlotCombo> tempCombos = new ArrayList<SlotCombo>();
		ArrayList<SlotData> tempRolls = new ArrayList<SlotData>();
		noDebugList.clear();
		Selectors = new SlotSelectors();
		
		Configuration config = new Configuration(configFile);
		config.load();

		isOPOnly = config.getBoolean("op-only", true);
		requireOwnership = config.getBoolean("require-ownership", false);
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

					int w = Integer.parseInt(data[0]), x = Integer.parseInt(data[1]), y = Integer.parseInt(data[2]), z = Integer.parseInt(data[3]), uses;
					double cost = Double.parseDouble(data[4]);
					String name;
					Account acc = null;
					try {
						name = data[6];
					} catch(ArrayIndexOutOfBoundsException e){
						name = "";
					}
					try {
						uses = Integer.parseInt(data[5]);
					} catch(ArrayIndexOutOfBoundsException e){
						uses = 0;
					}

					if(name != null && !name.isEmpty())
						if(iConomy.getBank().hasAccount(name))
							acc = iConomy.getBank().getAccount(name);
						else
							System.out.println("[Slots]iConomy has no account for " + name + ".");

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

					if(acc == null)
						tempSlots.add(new SlotMachine(this, b, cost, uses));
					else
						tempSlots.add(new SlotMachine(this, b, cost, acc, uses));

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
					tempRolls.add(new SlotData(name, symb, ChatColor.getByCode(Integer.parseInt(line)), pay, chance1, chance2));

				} catch(Exception e){

					System.out.println("[Slots] Error with roll " + name + ".");

				}

			}

		} catch(Exception e){

			System.out.println("[Slots] Error when opening rolls file.");

		}

		try {

			s = new Scanner(combosFile);

			String line;
			int lineNum = 0;

			while(s.hasNextLine()){

				line = s.nextLine();

				try {

					if(line == null || line.isEmpty()){

						lineNum++;
						continue;

					}

					String[] names = line.split("=")[0].split(":");
					double pay = Double.parseDouble(line.split("=")[1]);

					tempCombos.add(new SlotCombo(names, pay));

				} catch(Exception e){

					System.out.println("[Slots] Error in combo file on line " + lineNum + ".");

				}

			}

		} catch(Exception e){

			System.out.println("[Slots] Error when opening combos file.");

		}

		slotsList = tempSlots;
		comboList = tempCombos;
		rollInfo = tempRolls;
		
		return true;

	}

	public void saveData(){

		BufferedWriter out = null;
		try {

			out = new BufferedWriter(new FileWriter(configFile));

			out.write("tick-delay: " + tickDelay + "\r\nop-only: " + isOPOnly + "\r\nrequire-ownership: " + requireOwnership + "\r\n");

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
				append(",").append(m.getBlock().getLocation().getBlockZ()).append(",").append(m.getCost()).append(",").append(m.getUses()).append(",").
				append(m.getAccount() == null ? "" : m.getAccount().getName()).append("\r\n");
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

				StringBuilder b = new StringBuilder();
				b.append("name=").append(d.getName()).append("\r\n").
				append("symbol=").append(d.getSymbol()).append("\r\n").
				append("pay=").append(d.getPay()).append("\r\n").
				append("chance=").append(d.getNumerator()).append("/").append(d.getDenominator()).append("\r\n").
				append("color=").append(d.getChatColor().getCode()).append("\r\n\r\n");
				out.write(b.toString());

			}

		} catch(Exception e){

			System.out.println("[Slots]Error writing rolls file.");
			return;

		} finally {

			try { out.close(); } catch (IOException e) { }

		}

		try {

			out = new BufferedWriter(new FileWriter(combosFile));

			for(SlotCombo c : comboList){

				StringBuilder b = new StringBuilder();
				b.append(c.getNames()[0]).append(":").append(c.getNames()[1]).append(":").append(c.getNames()[2]).append("=").append(c.getPay());
				out.write(b.toString());

			}

		} catch(Exception e){

			System.out.println("[Slots]Error writing combos file.");
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

class PListener extends PlayerListener {
	
	Slots plugin;

	public PListener(Slots p){

		plugin = p;

	}
	
	@Override
	public void onPlayerInteract(PlayerInteractEvent event){
		
		if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK)){
			
			Block b = event.getClickedBlock();

			SlotMachine m = Slots.getMachine(b);
			if(m == null)
				return;

			if(!m.isRolling())
				m.rollSlots(event.getPlayer());
			else
				event.getPlayer().sendMessage(ChatColor.RED + "This slot machine is already rolling.");
			
		} else if(event.getAction().equals(Action.LEFT_CLICK_BLOCK)){
			
			Block b = event.getClickedBlock();
			
			if(b.getTypeId() == 63 || b.getTypeId() == 68){

				int type = Slots.getSlotSelectors().getType(event.getPlayer().getName());
				String account = Slots.getSlotSelectors().getAccount(event.getPlayer().getName());
				Slots.getSlotSelectors().remove(event.getPlayer().getName());
				if(type == 0){

					Sign sign = (Sign)b.getState();
					Account acc = null;
					if(account != null && iConomy.getBank().hasAccount(account))
						acc = iConomy.getBank().getAccount(account);

					if(ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("[Slots]")){

						Double cost = null;
						if(Slots.getMachine(b) != null){

							event.getPlayer().sendMessage(ChatColor.RED + "This is already a slot machine!");
							return;

						}

						try {

							cost = Double.parseDouble(sign.getLine(1));

						} catch(Exception e){

							event.getPlayer().sendMessage(ChatColor.RED + "Unable to get the cost..");
							return;

						}
						if(!plugin.loadData()){
							
							event.getPlayer().sendMessage(ChatColor.RED + "Error while attempting to load data.");
							return;
							
						}
							
						if(acc == null){

							Slots.getSlotList().add(new SlotMachine(plugin, b, cost, 0));
							event.getPlayer().sendMessage(ChatColor.GREEN + "Slot machine created.");

						} else {

							Slots.getSlotList().add(new SlotMachine(plugin, b, cost, acc, 0));
							event.getPlayer().sendMessage(ChatColor.GREEN + "Slot machine created and linked with " + account + "'s iConomy account.");

						}
						sign.setLine(0, ChatColor.YELLOW + sign.getLine(0));
						sign.update();
						plugin.saveData();
						
					}

				}
				if(type == 1){

					SlotMachine m = Slots.getMachine(b);
					if(m == null)
						return;

					Slots.getSlotList().remove(m);

					m.stopRoller();
					m.getSign().setLine(0, "[Slots]");

					event.getPlayer().sendMessage(ChatColor.GREEN + "The slot machine was removed.");

				}
				if(type == 2){
					
					SlotMachine m = Slots.getMachine(b);
					if(m == null)
						return;
					
					Account acc = m.getAccount();
					Player p = event.getPlayer();
					p.sendMessage(ChatColor.GOLD + "Owner account : " + (acc == null ? "<none>" : acc.getName()));
					p.sendMessage(ChatColor.GOLD + "Balance left : " + (acc == null ? "infinite" : "" + acc.getBalance()));
					p.sendMessage(ChatColor.GOLD + "Cost : " + m.getCost());
					p.sendMessage(ChatColor.GOLD + "Uses : " + m.getUses());
					
				}

			}
			
		}
		
	}
	
}

class BListener extends BlockListener {

	Slots plugin;

	public BListener(Slots p){

		plugin = p;

	}

	@Override
	public void onBlockBreak(BlockBreakEvent event){
		
		Block b = (org.bukkit.block.Block)event.getBlock();
		SlotMachine m = Slots.getMachine(b);

		if(m == null)
			return;

		event.setCancelled(true);
		m.getSign().update();
		event.getPlayer().sendMessage(ChatColor.RED + "You must use the remove command first to remove the slot machine!");

	}


}
