package us.Crash.Slots;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.nijiko.coelho.iConomy.iConomy;
import com.nijiko.coelho.iConomy.system.Account;

public class SlotRoller implements Runnable {

	private SlotMachine myMachine;
	private int taskId, numRolled = 0;
	private int[] roll = new int[3];
	private Player roller;
	
	public SlotRoller(SlotMachine m){
		
		myMachine = m;
		
	}
	
	@Override
	public void run(){
		
		if(numRolled == 3){
			
			myMachine.isRolling = false;
			if(roll[0] == roll[1] && roll[1] == roll[2]){
				
				double winningAmount = myMachine.getCost() * Slots.payoutList[roll[0]];
				roller.sendMessage(ChatColor.GOLD + "Congratulations you won " + iConomy.getBank().format(winningAmount) + "!");
				Account account = iConomy.getBank().getAccount(roller.getName());
				if(account == null){
					
					roller.sendMessage(ChatColor.RED + "Could not find an iConomy account for you.");
					
				} else {
					
					account.add(winningAmount);
					account.save();
					
				}
				
			} else { 
			
				roller.sendMessage(ChatColor.GOLD + "Sorry, you lost.");
				
			}
			myMachine.getPlugin().getServer().getScheduler().cancelTask(taskId);
			return;
			
		}
		if(numRolled > 5){//Should never happen.
			
			myMachine.getPlugin().getServer().getScheduler().cancelTask(taskId);
			return;
			
		}
			
		String line = myMachine.getSign().getLine(2);
		double rand = Math.random();
		if(rand <= (1/3.0))
			roll[numRolled] = 4;
		else if(rand > (1/3.0) && rand <= (1/3.0 + 1/4.0))
			roll[numRolled] = 3;
		else if(rand > (1/3.0 + 1/4.0) && rand <= (1/3.0 + 1/4.0 + 1/6.0))
			roll[numRolled] = 2;
		else if(rand > (1/3.0 + 1/4.0+ 1/6.0) && rand <= (1/3.0 + 1/4.0 + 1/6.0 + 1/8.0))
			roll[numRolled] = 1;
		else if(rand > (1/3.0 + 1/4.0 + 1/6.0 + 1/8.0))
			roll[numRolled] =  0;
		
		String character = " ";
		
		if(roll[numRolled] == 0)//Jackpot
			character = "J";
		if(roll[numRolled] == 1)//7
			character = "7";
		if(roll[numRolled] == 2)//Cherry
			character = "C";
		if(roll[numRolled] == 3)//Heart
			character = "H";
		if(roll[numRolled] == 4)//Bar
			character = "B";
		
		Slots.outputMessage(roller, addColor(ChatColor.GOLD + "You rolled a " + character + ChatColor.GOLD + "."));
		
		line = addColor(ChatColor.stripColor(line).substring(0, 4 * numRolled) + character + ChatColor.stripColor(line).substring(4 * numRolled + 1));
		
		myMachine.getSign().setLine(2, line);
		myMachine.getSign().update();
		
		numRolled++;
		
	}
	
	public String addColor(String line){
		
		return line.replace("J", ChatColor.LIGHT_PURPLE + "J" + ChatColor.BLACK).
					replace("7", ChatColor.RED + "7" + ChatColor.BLACK).
					replace("C", ChatColor.DARK_RED + "C" + ChatColor.BLACK).
					replace("H", ChatColor.AQUA + "H" + ChatColor.BLACK).
					replace("B", ChatColor.GREEN + "B" + ChatColor.BLACK);
	}
	
	public void removeTask(){
		
		myMachine.getPlugin().getServer().getScheduler().cancelTask(taskId);
		
	}
	
	public void setTask(int id, Player p){ 
	
		taskId = id;
		roller = p;
		
	}
	
}
