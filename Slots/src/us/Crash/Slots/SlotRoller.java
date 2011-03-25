package us.Crash.Slots;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import com.nijiko.coelho.iConomy.iConomy;
import com.nijiko.coelho.iConomy.system.Account;

public class SlotRoller implements Runnable {

	private SlotMachine myMachine;
	private int taskId, numRolled = 0;
	private int[] roll = { -1, -1, -1 };
	private Player roller;
	
	public SlotRoller(SlotMachine m){
		
		myMachine = m;
		
	}
	
	@Override
	public void run(){
		
		if(numRolled == 3){
			
			myMachine.isRolling = false;
					
			if(roll[0] == roll[1] && roll[1] == roll[2]){
				
				double winningAmount = myMachine.getCost() * Slots.getSlotData().get(roll[0]).getPay();
				if(myMachine.getAccount() != null){
					
					if(myMachine.getAccount().getBalance() - winningAmount < 0){
						
						roller.sendMessage(ChatColor.GOLD + "Sorry, the owner's account has run out of money, you won " + iConomy.getBank().format(myMachine.getAccount().getBalance()) + ".");
						winningAmount = myMachine.getAccount().getBalance();
						
					} else {
						
						roller.sendMessage(ChatColor.GOLD + "Congratulations you won " + iConomy.getBank().format(winningAmount) + "!");
						
					}
					
				} else {
				
					roller.sendMessage(ChatColor.GOLD + "Congratulations you won " + iConomy.getBank().format(winningAmount) + "!");
					
				}
				
				Account account = iConomy.getBank().getAccount(roller.getName());
				if(account == null){
					
					roller.sendMessage(ChatColor.RED + "Could not find an iConomy account for you.");
					
				} else {
					
					if(myMachine.getAccount() != null){
						
						myMachine.getAccount().subtract(winningAmount);
						myMachine.getAccount().save();
						
					}
					account.add(winningAmount);
					account.save();
					
				}
				
			} else {
			
				boolean gotCombo = false;
				
				for(SlotCombo c : Slots.getSlotCombos()){
					
					if(c.compare(roll)){
						
						double pay = c.getPay();
						gotCombo = true;
						roller.sendMessage(ChatColor.GOLD + "You got a combo!");
						
						if(myMachine.getAccount() != null){
							
							if(!myMachine.getAccount().hasEnough(c.getPay())){
								
								roller.sendMessage(ChatColor.GOLD + "Sorry, the owner's account has run out of money, you won " + iConomy.getBank().format(myMachine.getAccount().getBalance()) + ".");
								pay = myMachine.getAccount().getBalance();
								
							} else {
								
								roller.sendMessage(ChatColor.GOLD + "You won " + iConomy.getBank().format(c.getPay()) + "!");
								
							}
							
						} else {
							
							roller.sendMessage(ChatColor.GOLD + "You won " + iConomy.getBank().format(c.getPay()) + "!");
							
						}
						
						Account account = iConomy.getBank().getAccount(roller.getName());
						if(account == null)
							roller.sendMessage(ChatColor.RED + "Could not find an iConomy account for you.");
						else {
							
							if(myMachine.getAccount() != null){
								
								myMachine.getAccount().subtract(pay);
								myMachine.getAccount().save();
								
							}
							account.add(pay);
							account.save();
							
						}
						
						break;
						
					}
					
				}
				if(!gotCombo)
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
		double current = 0, lastVal = 0;
		int index = 0;
		
		SlotData rolled = null;
		
		for(SlotData d : Slots.getSlotData()){
		
			current += d.getChance();
			
			if(rand >= lastVal && rand < current){
				
				rolled = d;
				
				break;
				
			}
			
			index++;
			
		}
		
		if(rolled == null){
			
			roller.sendMessage(ChatColor.RED + "There was no roll to match the random value.");
			myMachine.getPlugin().getServer().getScheduler().cancelTask(taskId);
			return;
			
		} else {
			roll[numRolled] = index;
			Slots.outputMessage(roller, addChatColor(ChatColor.GOLD + "You rolled a " + rolled.getName() + "."));
			line = addSignColor(ChatColor.stripColor(line).substring(0, 4 * numRolled) + rolled.getSymbol() + ChatColor.stripColor(line).substring(4 * numRolled + 1));
			myMachine.getSign().setLine(2, line);
			myMachine.getSign().update();
			
			
		}
		
		numRolled++;
		
	}
		
	public String addChatColor(String line){
		
		SlotData d = Slots.getSlotData().get(roll[numRolled]);
		
		StringBuilder b = new StringBuilder();
		return line.replace(d.getName(), b.append(d.getColor()).append(d.getName()).append(ChatColor.GOLD).toString());
		
	}
	
	public String addSignColor(String line){
	
		int place = 0;
		StringBuilder b = new StringBuilder("");
		String[] split = new String[3];
		for(int i = 0; i < 3; i++)
			split[i] = line.substring(i * 3, (i + 1) * 3);
		
		for(String s : split){
			
			if(place <= numRolled){
				
				SlotData d = Slots.getSlotData().get(roll[place]);
				b.append(s.replace(d.getSymbol(), new StringBuilder().append(d.getColor()).append(d.getSymbol()).append(ChatColor.BLACK).toString()));

			} else {
				
				b.append(s);
				
			}
			place++;
		}
		
		return b.toString();
		
	}
	
	public void removeTask(){
		
		myMachine.getPlugin().getServer().getScheduler().cancelTask(taskId);
		
	}
	
	public void setTask(int id, Player p){ 
	
		taskId = id;
		roller = p;
		
	}
	
}
