package us.Crash.Slots;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

public class SlotMachine {

	private SlotRoller myRoller;
	private Slots plugin;
	protected boolean isRolling = false;
	private int usesAmount;
	private Block mySign;
	private double costPer, moneyInside;
	private SlotAccount ownerAccount;
	
	public SlotMachine(Slots p, Block sign, double cost, SlotAccount account, int uses, double amtInside){
		
		plugin = p;
		mySign = sign;
		costPer = cost;
		ownerAccount = account;
		usesAmount = uses;
		moneyInside = amtInside;
		
	}
	
	public SlotMachine(Slots p, Block sign, double cost, int uses, double amtInside){
		
		plugin = p;
		mySign = sign;
		costPer = cost;
		ownerAccount = null;
		usesAmount = uses;
		moneyInside = amtInside;
		
	}
	
	
	public void rollSlots(Player roller){
		
		isRolling = true;
		
		if(!Slots.accountExists(roller.getName())){
			
			roller.sendMessage(ChatColor.RED + "Could not find a bank account for you.");
			isRolling = false;
			return;
			
		}
		if(!Slots.canAfford(roller.getName(), costPer)){
			
			roller.sendMessage(ChatColor.RED + "You do not have enough money for slots!");
			isRolling = false;
			return;
			
		}
		if(ownerAccount != null)
			if(plugin.connectToAccounts)
				ownerAccount.addMoney(costPer);
			else
				moneyInside += costPer;

		usesAmount++;
		
		new SlotAccount(roller.getName()).subtractMoney(costPer);
		getSign().setLine(2, " | | ");
		getSign().update();
		
		myRoller = new SlotRoller(this);
		myRoller.setTask(plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, myRoller, plugin.tickDelay, plugin.tickDelay), roller);
		
	}
	
	public void stopRoller(){
		
		if(isRolling)
			myRoller.removeTask();
		
	}
	
	public Slots getPlugin(){ return plugin; }
	
	public boolean isRolling(){ return isRolling; }
	
	public Sign getSign(){ return (Sign)mySign.getState(); }
	
	public Block getBlock(){ return mySign; }
	
	public SlotAccount getAccount(){ return ownerAccount; }
	
	public double getCost(){ return costPer; }
	
	public double getAmountInside(){ return moneyInside; }
	
	public boolean hasEnoughMoney(double amount){ return moneyInside >= amount; }
	
	public void subtractMoneyInside(double amount){ moneyInside -= amount; }
	
	public void addMoneyInside(double amount){ moneyInside += amount; }
	
	public String sellOwnership(String player){
		
		SlotAccount account = new SlotAccount(player);
		
		if(ownerAccount != null)
			return "This slot machine is already owned!";
		if(!account.hasEnough(getCost()))
			return "You don't have enough money to buy this slot machine.";
		
		account.subtractMoney(getCost());
		
		ownerAccount = account;
		
		return null;
		
	}
	
	public boolean isSelf(Block b){
		
		return b.getLocation().getBlockX() == mySign.getLocation().getBlockX() &&
			   b.getLocation().getBlockY() == mySign.getLocation().getBlockY() &&
			   b.getLocation().getBlockZ() == mySign.getLocation().getBlockZ() &&
			   b.getWorld().equals(mySign.getWorld());
		
	}
	
	public int getUses(){ return usesAmount; }
	
	@Override
	public boolean equals(Object o){
		
		if(!(o instanceof SlotMachine))
			return false;
		
		SlotMachine m = (SlotMachine)o;
		
		if(m.getBlock().equals(this.getBlock()))
			return true;
		
		return false;
		
	}
	
}
