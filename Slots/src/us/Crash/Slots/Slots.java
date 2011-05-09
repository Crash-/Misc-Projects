package us.Crash.Slots;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import com.earth2me.essentials.User;
import com.iConomy.iConomy;
import com.iConomy.system.Account;
import com.nijikokun.bukkit.Permissions.*;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
	public static boolean useiConomy = true;
	public static Server server;
	public boolean isOPOnly = true, requireOwnership = false, comboOrderMatters = true, multiplyComboPayout = false, connectToAccounts = true, backupData = false, saveOnShutdown = true, useComboNames = true, multiplyRows = true;
	public int tickDelay = 50, activateType = 0;
	public static Permissions Permissions = null;
	private File configFile, saveFile, rollsFile, combosFile;

	public static ArrayList<SlotMachine> getSlotList(){ return slotsList; }

	public static SlotSelectors getSlotSelectors(){ return Selectors; }

	public static ArrayList<SlotCombo> getSlotCombos(){ return comboList; }

	public static ArrayList<SlotData> getSlotData(){ return rollInfo; }

	public static ArrayList<String> getNoDebugList(){ return noDebugList; } 

	public static String formatMoney(double amount){

		if(useiConomy){

			return iConomy.format(amount);

		} else {

			return "$" + amount;

		}

	}

	public static boolean accountExists(String player){

		if(useiConomy){

			Account account = iConomy.getAccount(player);
			if(account == null)
				return false;

		} else {

			if(player == null || player.isEmpty())
				return false;

			Player p = server.matchPlayer(player).get(0);
			User account = User.get(p);
			if(account == null)
				return false;

		}

		return true;

	}

	public static double getAmount(String player){

		if(useiConomy){

			Account account = iConomy.getAccount(player);
			if(account == null)
				return 0;

			return account.getHoldings().balance();

		} else {

			Player p = server.matchPlayer(player).get(0);
			User account = User.get(p);

			return account.getMoney();

		}

	}

	public static boolean canAfford(String player, double amount){

		if(useiConomy){

			Account account = iConomy.getAccount(player);
			if(account == null)
				return false;

			return account.getHoldings().hasEnough(amount);

		} else {

			Player p = server.matchPlayer(player).get(0);
			User account = User.get(p);

			return account.canAfford(amount);

		}

	}

	public static String takeMoneyFrom(String player, double amount){

		if(useiConomy){

			Account account = iConomy.getAccount(player);
			if(account == null)
				return "Could not find an iConomy account for you.";

			account.getHoldings().subtract(amount);

		} else {

			Player p = server.matchPlayer(player).get(0);
			User account = User.get(p);

			account.takeMoney(amount);

		}

		return "";

	}

	public static String giveMoneyTo(String player, double amount){

		if(useiConomy){

			Account account = iConomy.getAccount(player);
			if(account == null)
				return "Could not find an iConomy account for you.";

			account.getHoldings().add(amount);

		} else {

			Player p = server.matchPlayer(player).get(0);
			User account = User.get(p);

			account.giveMoney(amount);

		}

		return "";

	}

	public static boolean hasPermission(Player p, String command){

		command = command.toLowerCase();

		if(Permissions == null)
			return p.isOp();

		if(Permissions != null){

			if(command.equals("create"))
				return Permissions.getHandler().permission(p, "slots.create");
			if(command.equals("remove"))
				return Permissions.getHandler().permission(p, "slots.remove");
			if(command.equals("buy"))
				return Permissions.getHandler().permission(p, "slots.buy");
			if(command.equals("deposit"))
				return Permissions.getHandler().permission(p, "slots.deposit");
			if(command.equals("withdraw"))
				return Permissions.getHandler().permission(p, "slots.withdraw");
			if(command.equals("backup"))
				return Permissions.getHandler().permission(p, "slots.backup");
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

		if(saveOnShutdown)
			saveData();
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

		if(!new File("plugins/Slots/backup-slots.txt").exists()){

			try { new File("plugins/Slots/backup-slots.txt").createNewFile(); } catch (IOException e) {

				System.out.println("[Slots]Error creating backup slots file.");
				getServer().getPluginManager().disablePlugin(this);
				return;

			}

		}

		if(!configFile.exists()){

			try { configFile.createNewFile(); } catch (IOException e) {

				System.out.println("[Slots]Error creating config file.");
				getServer().getPluginManager().disablePlugin(this);
				return;

			}
			BufferedWriter out = null;
			try {

				out = new BufferedWriter(new FileWriter(configFile));

				out.write("tick-delay: 50\r\nop-only: true\r\nrequire-ownership: false\r\ncombo-order-matters: true\r\nmultiply-combo-payout: false" +
						"\r\nconnect-to-accounts: true\r\nbackup-slots: false" +
						"\r\nroll-type: any #any is redstone or right clicking, put redstone for redstone only or click for right clicking only" +
				"\r\nuse-combo-names: true\r\nmultiply-3-in-a-rows: true");

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

		if(getServer().getPluginManager().getPlugin("iConomy") != null)
			useiConomy = true;
		else
			if(getServer().getPluginManager().getPlugin("Essentials") == null){

				System.out.println("[Slots] Error : Unable to find iConomy or Essentials plugin.");
				getServer().getPluginManager().disablePlugin(this);
				return;

			} else {

				useiConomy = false;

			}

		System.out.println("[Slots] Using " + (useiConomy ? "iConomy" : "Essentials") + " plugin for economy.");

		loadData(true);
		if(backupData){

			if(backupData(saveFile,new File("plugins/Slots/backup-slots.txt")))
				System.out.println("[Slots] Saved data into backup file.");
			else
				System.out.println("[Slots] There was an error when saving the backup file.");


		}

		server = getServer();

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

						if(!Permissions.getHandler().permission(p, "slots.create.hook")){

							p.sendMessage(ChatColor.RED + "You can't use this command!");
							return false;

						}


						name = args[1];

						if(name == null || !Slots.accountExists(name)){

							p.sendMessage(ChatColor.RED + name + " has no bank account!");
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

				if(loadData(true))
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

					if(ind < 0 || ind >= rollInfo.size()){

						p.sendMessage(ChatColor.RED + "The index must be 0 through " + (rollInfo.size() - 1) + ".");
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

				} else if(args[1].equals("order")){

					comboOrderMatters = !comboOrderMatters;
					p.sendMessage(ChatColor.GREEN + "Toggled combo-order-matters.");
					return true;

				} else if(args[1].equals("multiply")){

					multiplyComboPayout = !multiplyComboPayout;
					p.sendMessage(ChatColor.GREEN + "Toggled multiply-combo-payouts.");
					return true;

				}

			} else if(args[0].equalsIgnoreCase("info")){

				Selectors.addNewSelector(p, 2);
				p.sendMessage(ChatColor.GREEN + "Left click on a slot machine to get info.");

			} else if(args[0].equalsIgnoreCase("backup")){

				if(backupData(saveFile, new File("plugins/slots/backup-slots.txt")))
					p.sendMessage(ChatColor.GREEN + "The save data has been backed up.");
				else
					p.sendMessage(ChatColor.RED + "There was an issue when backing up the data.");

			} else if(!connectToAccounts){

				if(args[0].equalsIgnoreCase("buy")){

					Selectors.addNewSelector(p, 3);
					p.sendMessage(ChatColor.GREEN + "Left click on a slot machine to buy it.");

				} else if(args[0].equalsIgnoreCase("deposit")){

					Integer amount = null;

					try {

						amount = Integer.parseInt(args[1]);

					} catch(Exception e){

						p.sendMessage(ChatColor.RED + "Error getting amount to deposit.");
						return false;

					}

					if(amount == 0){

						p.sendMessage(ChatColor.RED + "Put in something higher than 0!");
						return false;

					}

					if(!canAfford(p.getName(), amount)){

						p.sendMessage(ChatColor.RED + "You don't have enough money!");
						return false;

					}

					Selectors.addNewMoneyPlayer(p, amount);
					p.sendMessage(ChatColor.GREEN + "Left click on a slot machine to put money into it");

				} else if(args[0].equalsIgnoreCase("withdraw")){

					Integer amount = null;

					try {

						amount = Integer.parseInt(args[1]);

					} catch(Exception e){

						p.sendMessage(ChatColor.RED + "Error getting amount to withdraw.");
						return false;

					}

					if(amount == 0){

						p.sendMessage(ChatColor.RED + "Put in something higher than 0!");
						return false;

					}

					Selectors.addNewMoneyPlayer(p, -amount);
					p.sendMessage(ChatColor.GREEN + "Left click on a slot machine to take money from it");

				}

			}

		}

		return false;

	}

	public boolean backupData(File saveFile, File backupFile){

		ArrayList<String> lines = new ArrayList<String>();

		try {

			Scanner s = new Scanner(saveFile);

			while(s.hasNextLine())
				lines.add(s.nextLine());

			BufferedWriter out = new BufferedWriter(new FileWriter(backupFile));
			StringBuilder savedData = new StringBuilder();

			for(String str : lines)
				savedData.append(str).append("\r\n");

			out.write(savedData.toString());

			out.close();

		} catch(Exception e){

			System.out.println("[Slots] Error when backuping up the data.");
			return false;

		}

		return true;

	}

	public boolean loadData(boolean outputErrors){

		ArrayList<SlotMachine> tempSlots = new ArrayList<SlotMachine>();
		ArrayList<SlotCombo> tempCombos = new ArrayList<SlotCombo>();
		ArrayList<SlotData> tempRolls = new ArrayList<SlotData>();
		if(outputErrors){

			noDebugList.clear();
			Selectors = new SlotSelectors();

		}

		Configuration config = new Configuration(configFile);
		config.load();

		isOPOnly = config.getBoolean("op-only", true);
		requireOwnership = config.getBoolean("require-ownership", false);
		tickDelay = config.getInt("tick-delay", 50);
		comboOrderMatters = config.getBoolean("combo-order-matters", true);
		multiplyComboPayout = config.getBoolean("multiply-combo-payout", false);
		connectToAccounts = config.getBoolean("connect-to-accounts", true);
		backupData = config.getBoolean("backup-slots", false);
		saveOnShutdown = config.getBoolean("save-on-shutdown", true);
		String roll = config.getString("roll-type", "any").toLowerCase();
		if(roll.equals("redstone"))
			activateType = 1;
		else if(roll.equals("click"))
			activateType = 2;
		else
			activateType = 0;
		useComboNames = config.getBoolean("use-combo-names", true);
		multiplyRows = config.getBoolean("multiply-3-in-a-rows", true);

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
					double cost = Double.parseDouble(data[4]), amtInside;
					String name;
					SlotAccount acc = null;
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
					try {
						amtInside = Double.parseDouble(data[7]);
					} catch(ArrayIndexOutOfBoundsException e){
						amtInside = 0;
					}

					if(name != null && !name.isEmpty())
						if(Slots.accountExists(name))
							acc = new SlotAccount(name);
						else
							if(outputErrors)
								System.out.println("[Slots]There is no bank account for " + name + ".");

					Block b = getServer().getWorlds().get(w).getBlockAt(x, y, z);

					if(b.getTypeId() != 63 && b.getTypeId() != 68){

						if(outputErrors)
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
						tempSlots.add(new SlotMachine(this, b, cost, uses, amtInside));
					else
						tempSlots.add(new SlotMachine(this, b, cost, acc, uses, amtInside));

					i++;

				} catch(Exception e){

					i++;
					if(outputErrors)
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

			String name = "", symb, col, line = "";
			int pay, chance1 = 0, chance2 = 0;
			Integer chance = null;
			boolean hadError = false, announce, isUpdated = true;

			while(s.hasNextLine()){

				if(isUpdated)
					line = s.nextLine();
				else {

					isUpdated = true;
					continue;

				}

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

					try {

						chance = Integer.parseInt(line);

					} catch(NumberFormatException e){

						chance1 = Integer.parseInt(line.split("/")[0]);
						chance2 = Integer.parseInt(line.split("/")[1]);

					}

					if(chance == null)
						chance = (int)(100 * (chance1 / (double)chance2));

					col = s.nextLine().split("=")[1];

					line = s.nextLine().toLowerCase();

					if(line.split("=")[0].equals("announce-win"))
						announce = Boolean.parseBoolean(line.split("=")[1]);
					else {

						announce = false;
						isUpdated = false;

					}
					tempRolls.add(new SlotData(name, symb, ChatColor.getByCode(Integer.parseInt(col)), pay, chance, announce));

					chance = null;

				} catch(Exception e){

					if(outputErrors)
						System.out.println("[Slots] Error with roll " + name + ".");

				}

			}

		} catch(Exception e){

			System.out.println("[Slots] Error when opening rolls file.");
			return false;

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
					double pay = Double.parseDouble(line.split("=")[1].split(",")[0]);
					boolean announce;
					String name = "";

					if(useComboNames){

						try {
							name = line.split("=")[1].split(",")[1];
						} catch(ArrayIndexOutOfBoundsException e){
							System.out.println("[Slots] NOTE : Combos have names added after the pay in combos.txt! If you wish to name the combos, edit the file and reload.");
							useComboNames = false;
						}

					}
					try {
						announce = Boolean.parseBoolean(line.split("=")[1].split(",")[2]);
					} catch(ArrayIndexOutOfBoundsException e){
						announce = false;
					}

					tempCombos.add(new SlotCombo(name, names, pay, announce));

				} catch(Exception e){

					if(outputErrors)
						System.out.println("[Slots] Error in combo file on line " + lineNum + ".");

				}

			}

		} catch(Exception e){

			System.out.println("[Slots] Error when opening combos file.");
			return false;

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

			out.write("tick-delay: " + tickDelay + "\r\nop-only: " + isOPOnly + "\r\nrequire-ownership: " + requireOwnership + "\r\nmultiply-combo-payout: " + multiplyComboPayout + "\r\ncombo-order-matters: " + comboOrderMatters + "\r\nconnect-to-accounts: " + connectToAccounts + "\r\nbackup-slots: " + backupData + "\r\nsave-on-shutdown: " + saveOnShutdown + "\r\nroll-type: " + (activateType == 0 ? "any" : (activateType == 1 ? "redstone" : "click")) + "\r\nuse-combo-names: " + useComboNames + "\r\nmultiply-3-in-a-rows: " + multiplyRows);

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
				append(m.getAccount() == null ? "" : m.getAccount().getName()).append(",").append(m.getAmountInside()).append("\r\n");

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
				append("chance=").append((int)(d.getChance() * 100)).append("\r\n").
				append("color=").append(d.getChatColor().getCode()).append("\r\n").
				append("announce-win=").append(d.announceWin()).append("\r\n\r\n");
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
				b.append(c.getNames()[0]).append(":").append(c.getNames()[1]).append(":").append(c.getNames()[2]).append("=").append(c.getPay()).append(",").append(c.getName()).append(",").append(c.announceWin()).append("\r\n");
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

			if((b.getTypeId() == 63 || b.getTypeId() == 68) && (plugin.activateType == 0 || plugin.activateType == 2)){

				SlotMachine m = Slots.getMachine(b);
				if(m == null)
					return;

				if(!m.isRolling())
					m.rollSlots(event.getPlayer());
				else
					event.getPlayer().sendMessage(ChatColor.RED + "This slot machine is already rolling.");

			} else if((b.getTypeId() == 69 || b.getTypeId() == 77) && (plugin.activateType ==  0 || plugin.activateType == 1)){

				BlockFace faces[] = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
				SlotMachine m;
				Block fromBlock = null;
				if(b.getTypeId() == 69){

					switch(b.getData()){

					case 0x1:
						fromBlock = b.getFace(BlockFace.NORTH);
						break;
					case 0x2:
						fromBlock = b.getFace(BlockFace.SOUTH);
						break;
					case 0x3:
						fromBlock = b.getFace(BlockFace.EAST);
						break;
					case 0x4:
						fromBlock = b.getFace(BlockFace.WEST);
						break;

					default:
						fromBlock = b;

					}

				}

				Block block;

				for(BlockFace f : faces){

					block = b.getFace(f);
					if(block.getTypeId() == 68 || block.getTypeId() == 63){

						m = Slots.getMachine(block);
						if(m != null){

							if(!m.isRolling())
								m.rollSlots(event.getPlayer());
							else
								event.getPlayer().sendMessage(ChatColor.RED + "This slot machine is already rolling.");

						}

						return;

					}

				}

				if(b.getTypeId() != 69)
					return;

				for(BlockFace f : faces){

					block = fromBlock.getFace(f);
					if(block.getTypeId() == 68 || block.getTypeId() == 63){

						m = Slots.getMachine(block);
						if(m != null){

							if(!m.isRolling())
								m.rollSlots(event.getPlayer());
							else
								event.getPlayer().sendMessage(ChatColor.RED + "This slot machine is already rolling.");

						}

						return;

					}

				}

			}

		} else if(event.getAction().equals(Action.LEFT_CLICK_BLOCK)){

			Block b = event.getClickedBlock();

			if(b.getTypeId() == 63 || b.getTypeId() == 68){

				int type = Slots.getSlotSelectors().getType(event.getPlayer().getName());
				String account = Slots.getSlotSelectors().getAccount(event.getPlayer().getName());
				Slots.getSlotSelectors().remove(event.getPlayer().getName());

				if(type == 0){

					Sign sign = (Sign)b.getState();
					SlotAccount acc = null;
					if(account != null && Slots.accountExists(account))
						acc = new SlotAccount(account);

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

						System.out.println(account);

						if(account.equalsIgnoreCase(event.getPlayer().getName())){

							if(!acc.hasEnough(cost)){

								event.getPlayer().sendMessage("Sorry, you don't have enough money.");
								return;

							} else {

								acc.subtractMoney(cost);

							}

						}

						if(acc == null){

							Slots.getSlotList().add(new SlotMachine(plugin, b, cost, 0, 0));
							event.getPlayer().sendMessage(ChatColor.GREEN + "Slot machine created.");

						} else {

							Slots.getSlotList().add(new SlotMachine(plugin, b, cost, new SlotAccount(event.getPlayer().getName()), 0, 0));
							if(plugin.connectToAccounts)
								event.getPlayer().sendMessage(ChatColor.GREEN + "Slot machine created and linked with " + account + "'s bank account.");
							else
								event.getPlayer().sendMessage(ChatColor.GREEN + "Slot machine created with " + account + " as the owner.");

						}
						sign.setLine(0, ChatColor.YELLOW + "[Slots]");
						sign.update();

					}

				}
				if(type == 1){

					SlotMachine m = Slots.getMachine(b);
					if(m == null)
						return;

					if(m.getAccount() == null){

						if(!Slots.Permissions.getHandler().permission(event.getPlayer(), "slots.remove.removeall")){

							event.getPlayer().sendMessage(ChatColor.RED + "You cannot destroy slots owned by nobody!");
							return;

						}

					} else {

						if(!m.getAccount().getName().equalsIgnoreCase(event.getPlayer().getName()) && !Slots.Permissions.getHandler().permission(event.getPlayer(), "slots.remove.removeall")){

							event.getPlayer().sendMessage(ChatColor.RED + "You cannot destroy others' slots!");
							return;

						}

					}

					SlotAccount acc = new SlotAccount(event.getPlayer().getName());
					double addAmount = m.getAmountInside();

					Slots.getSlotList().remove(m);

					m.stopRoller();
					m.getSign().setLine(0, "[Slots]");
					m.getSign().update();

					if(!Slots.accountExists(acc.getName()))
						event.getPlayer().sendMessage(ChatColor.RED + "Could not find a bank account for you, the money will not be transferred to your account.");
					else {

						acc.addMoney(addAmount);
						event.getPlayer().sendMessage(ChatColor.GREEN + "The slot machine was removed, you got " + Slots.formatMoney(addAmount) + " from it.");

					}

				}
				if(type == 2){

					SlotMachine m = Slots.getMachine(b);
					if(m == null)
						return;

					SlotAccount acc = m.getAccount();
					Player p = event.getPlayer();
					p.sendMessage(ChatColor.GOLD + "Owner account : " + (acc == null ? "<none>" : acc.getName()));
					p.sendMessage(ChatColor.GOLD + "Balance left : " + (acc == null ? "infinite" : (plugin.connectToAccounts ? "" + acc.getAmount() : "" + m.getAmountInside())));
					p.sendMessage(ChatColor.GOLD + "Cost : " + m.getCost());
					p.sendMessage(ChatColor.GOLD + "Uses : " + m.getUses());

				}
				if(type == 3){

					SlotMachine m = Slots.getMachine(b);
					if(m == null)
						return;

					String error = m.sellOwnership(event.getPlayer().getName());

					if(error != null)
						event.getPlayer().sendMessage(ChatColor.RED + error);
					else
						event.getPlayer().sendMessage(ChatColor.GREEN + "You bought the slot machine!");

				}
				if(type == 4){

					SlotMachine m = Slots.getMachine(b);
					if(m == null)
						return;

					if(m.getAccount() == null){

						event.getPlayer().sendMessage(ChatColor.RED + "That slot machine is not owned by anybody! You can buy it using /slots buy");
						return;

					}
					if(!m.getAccount().getName().equalsIgnoreCase(event.getPlayer().getName())){

						event.getPlayer().sendMessage(ChatColor.RED + "That is not your slot machine!");
						return;

					}
					String name = event.getPlayer().getName();
					int depositAmount = Slots.getSlotSelectors().getAmount(name);

					m.addMoneyInside(depositAmount);
					Slots.takeMoneyFrom(name, depositAmount);

					event.getPlayer().sendMessage(ChatColor.GREEN + "You depositied " + Slots.formatMoney(depositAmount) + " into the machine.");

				}
				if(type == 5){

					SlotMachine m = Slots.getMachine(b);
					if(m == null)
						return;

					if(m.getAccount() == null){

						event.getPlayer().sendMessage(ChatColor.RED + "That slot machine is not owned by anybody! You can buy it using /slots buy");
						return;

					}
					if(!m.getAccount().getName().equalsIgnoreCase(event.getPlayer().getName())){

						event.getPlayer().sendMessage(ChatColor.RED + "That is not your slot machine!");
						return;

					}

					String name = event.getPlayer().getName();
					int withdrawAmount = -1 * Slots.getSlotSelectors().getAmount(name);

					if(!m.hasEnoughMoney(withdrawAmount)){

						event.getPlayer().sendMessage(ChatColor.RED + "That slot machine doesn't have enough money inside!");
						return;

					}

					m.subtractMoneyInside(withdrawAmount);
					Slots.giveMoneyTo(name, withdrawAmount);

					event.getPlayer().sendMessage(ChatColor.GREEN + "You withdrew " + Slots.formatMoney(withdrawAmount) + " from the machine.");

				}

			} else if((b.getTypeId() == 69 || b.getTypeId() == 77) && (plugin.activateType ==  0 || plugin.activateType == 1)){

				BlockFace faces[] = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
				SlotMachine m;
				Block fromBlock = null;
				if(b.getTypeId() == 69){

					switch(b.getData()){

					case 0x1:
						fromBlock = b.getFace(BlockFace.NORTH);
						break;
					case 0x2:
						fromBlock = b.getFace(BlockFace.SOUTH);
						break;
					case 0x3:
						fromBlock = b.getFace(BlockFace.EAST);
						break;
					case 0x4:
						fromBlock = b.getFace(BlockFace.WEST);
						break;

					default:
						fromBlock = b;

					}

				}

				Block block;

				for(BlockFace f : faces){

					block = b.getFace(f);
					if(block.getTypeId() == 68 || block.getTypeId() == 63){

						m = Slots.getMachine(block);
						if(m != null){

							if(!m.isRolling())
								m.rollSlots(event.getPlayer());
							else
								event.getPlayer().sendMessage(ChatColor.RED + "This slot machine is already rolling.");

						}

						return;

					}

				}

				if(b.getTypeId() != 69)
					return;

				for(BlockFace f : faces){

					block = fromBlock.getFace(f);
					if(block.getTypeId() == 68 || block.getTypeId() == 63){

						m = Slots.getMachine(block);
						if(m != null){

							if(!m.isRolling())
								m.rollSlots(event.getPlayer());
							else
								event.getPlayer().sendMessage(ChatColor.RED + "This slot machine is already rolling.");

						}

						return;

					}

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
