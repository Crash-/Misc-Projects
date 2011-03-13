package us.Crash.ItemGiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

	@Override
	public void onDisable() {

		System.out.println("ItemGiver disabled.");

	}

	@Override
	public void onEnable() {

		ItemGiver giver = new ItemGiver(getServer(), (Plugin)this);
		
		if(!giver.loadSettings())
			getServer().getPluginManager().disablePlugin(this);

		if(!getServer().getPluginManager().isPluginEnabled(this))
			return;

		getServer().getScheduler().scheduleSyncRepeatingTask(this, giver, 0, 3);

		System.out.println("ItemGiver by Crash, enabled.");

	}

}

class ItemGiver implements Runnable {

	public Plugin self;
	public Server server;
	public HashMap<Long, HashMap<Integer, Integer>> itemList = new HashMap<Long, HashMap<Integer, Integer>>(); 
	public HashMap<Long, String> messageList = new HashMap<Long, String>();
	public HashMap<Long, Boolean> waitList = new HashMap<Long, Boolean>();

	public ItemGiver(Server s, Plugin p){ 

		self = p;
		server = s;

	}

	public long isTime(long time){

		for(long key : waitList.keySet()){

			if(Math.abs(time - key) < 100){

				if(!waitList.get(key)){

					waitList.put(key, true);
					return key;

				}

			} else {

				if(waitList.get(key))
					waitList.put(key, false);

			}

		}

		return -1;

	}

	private int getMax(Inventory inv, int id){

		ItemStack d = inv.getItem(0);
		inv.setItem(0, new ItemStack(id));
		int max = inv.getItem(0).getMaxStackSize();
		inv.setItem(0, d);

		return max;

	}

	public boolean hasRoom(int id, int amount, Inventory inv){

		int total = 0, maxPerSlot = getMax(inv, id);

		for(int i = 0; i < inv.getContents().length && total < amount; i++){

			if(inv.getItem(i) != null){

				if(inv.getItem(i).getTypeId() < 1){

					total += maxPerSlot;

				} else if(inv.getItem(i).getTypeId() == id){

					total += maxPerSlot - inv.getItem(i).getAmount();

				}

			}

		}

		if(total >= amount)
			return true;
		else
			return false;

	}
	public ItemStack[] addItemsToInventory(int id, int amount, Inventory inv){

		ItemStack[] items = inv.getContents();
		int amtLeft = amount, maxSize = getMax(inv, id);

		for(int i = 0; i < items.length && amtLeft > 0; i++){

			if(items[i].getTypeId() == id && items[i].getAmount() != maxSize){

				if(amtLeft - (maxSize - items[i].getAmount()) <= 0){

					inv.setItem(i, new ItemStack(id, items[i].getAmount() + amtLeft));
					amtLeft = 0;

				} else {

					inv.setItem(i, new ItemStack(id, maxSize));
					amtLeft -= maxSize - items[i].getAmount();

				}

			}

		}

		while(amtLeft > 0){

			amtLeft -= maxSize;
			if(amtLeft < 1)
				inv.addItem(new ItemStack(id, amtLeft + maxSize));
			else
				inv.addItem(new ItemStack(id, maxSize));

		}

		return inv.getContents();

	}

	public boolean loadSettings(){

		BufferedReader in = null;

		try {

			in = new BufferedReader(new FileReader("plugins/ItemGiver/settings.txt"));

			if(new File("plugins/ItemGiver").mkdir()){

				System.out.println("[ItemGiver] Created save file directory.");

			}

			if(new File("plugins/ItemGiver/settings.txt").createNewFile()){

				System.out.println("[ItemGiver] Settings file not found, creating file and disabling plugin.");
				server.getPluginManager().disablePlugin(self);
				return false;

			}

			String line = in.readLine().split("=")[1];

			try {

				int times = Integer.parseInt(line);
				for(int i = 0; i < times; i++){

					line = in.readLine();
					long time = Long.parseLong(line.split("\"=")[1]);

					String name = line.split("\"=")[0].substring(1);
					messageList.put(time, name);

					String[] items = in.readLine().split(":");
					HashMap<Integer, Integer>itemsList = new HashMap<Integer, Integer>();
					for(int j = 0; j < items.length; j++){

						itemsList.put(Integer.parseInt(items[j].split(",")[0]), Integer.parseInt(items[j].split(",")[1]));

					}
					itemList.put(time, itemsList);
					waitList.put(time, false);

				}

			} catch(Exception e){

				System.out.println("[ItemGiver] Error loading settings file.");
				return false;

			}

		} catch(Exception e){

			System.out.println("[ItemGiver] Error loading settings file.");
			return false;

		}

		finally {
			try { in.close(); } catch (Exception e){ }
		}

		return true;

	}

	@Override
	public void run() {

		for(int i = 0; i < server.getWorlds().size(); i++){

			long time = server.getWorlds().get(i).getTime();

			time = isTime(time);

			if(time != -1){

				Player[] players = server.getOnlinePlayers();

				for(int j = 0; j < players.length; j++){

					if(players[j].getWorld().equals(server.getWorlds().get(i))){

						if(messageList.containsKey(time))
							players[j].sendMessage(ChatColor.YELLOW + messageList.get(time));

						for(Integer item : itemList.get(time).keySet()){

							if(hasRoom(item, itemList.get(time).get(item), players[j].getInventory())){

								addItemsToInventory(item, itemList.get(time).get(item), players[j].getInventory());

							} else {

								server.getWorlds().get(i).dropItem(players[j].getLocation(), new ItemStack(item, itemList.get(time).get(item)));

							}

						}

					}

				}

			}

		}

	}

}
