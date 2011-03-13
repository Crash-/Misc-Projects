package us.Crash.Slots;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import com.nijiko.coelho.iConomy.iConomy;
import com.nijiko.coelho.iConomy.system.Account;

public class SlotMachine {

	private SlotRoller myRoller;
	private Slots plugin;
	protected boolean isRolling = false;
	private Block mySign;
	private double costPer;
	
	public SlotMachine(Slots p, Block sign, double cost){
		
		plugin = p;
		mySign = sign;
		costPer = cost;
		
	}
	
	public void rollSlots(Player roller){
		
		isRolling = true;
		
		Account account = iConomy.getBank().getAccount(roller.getName());
		if(account == null){
			
			roller.sendMessage(ChatColor.RED + "Could not find an iConomy account for you.");
			isRolling = false;
			return;
			
		}
		if(!account.hasEnough(costPer)){
			
			roller.sendMessage(ChatColor.RED + "You do not have enough money for slots!");
			isRolling = false;
			return;
			
		}
		account.subtract(costPer);
		account.save();
		getSign().setLine(2, "  |   |  ");
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
	
	public double getCost(){ return costPer; }
	
	public boolean isSelf(Block b){
		
		return b.getLocation().getBlockX() == mySign.getLocation().getBlockX() &&
			   b.getLocation().getBlockY() == mySign.getLocation().getBlockY() &&
			   b.getLocation().getBlockZ() == mySign.getLocation().getBlockZ() &&
			   b.getWorld().equals(mySign.getWorld());
		
	}
	
}