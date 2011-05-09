package us.Crash.Slots;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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

				double winningAmount;
				if(myMachine.getPlugin().multiplyRows)
					winningAmount = myMachine.getCost() * Slots.getSlotData().get(roll[0]).getPay();
				else
					winningAmount = Slots.getSlotData().get(roll[0]).getPay();
				
				if(myMachine.getAccount() != null){

					if(myMachine.getPlugin().connectToAccounts){

						if(myMachine.getAccount().getAmount() - winningAmount < 0){

							roller.sendMessage(ChatColor.GOLD + "Sorry, the owner's account has run out of money, you won " + Slots.formatMoney(myMachine.getAccount().getAmount()) + ".");
							winningAmount = myMachine.getAccount().getAmount();

						} else {

							roller.sendMessage(ChatColor.GOLD + "Congratulations you won " + Slots.formatMoney(winningAmount) + "!");

						}

					} else {

						if(!myMachine.hasEnoughMoney(winningAmount)){

							roller.sendMessage(ChatColor.GOLD + "Sorry, there isn't enough money inside the machine, you won " + Slots.formatMoney(myMachine.getAmountInside()));
							winningAmount = myMachine.getAmountInside();

						} else {

							roller.sendMessage(ChatColor.GOLD + "Congratulations you won " + Slots.formatMoney(winningAmount) + "!");

						}

					}

				} else {

					roller.sendMessage(ChatColor.GOLD + "Congratulations you won " + Slots.formatMoney(winningAmount) + "!");

				}

				SlotAccount account = new SlotAccount(roller.getName());
				if(!Slots.accountExists(roller.getName())){

					roller.sendMessage(ChatColor.RED + "Could not find a bank account for you.");

				} else {

					if(myMachine.getAccount() != null){
						if(myMachine.getPlugin().connectToAccounts)
							myMachine.getAccount().subtractMoney(winningAmount);
						else
							myMachine.subtractMoneyInside(winningAmount);

					}
					account.addMoney(winningAmount);

				}
				
				SlotData d = Slots.getSlotData().get(roll[0]);
				if(d.announceWin())
					myMachine.getPlugin().getServer().broadcastMessage(ChatColor.WHITE + roller.getName() + ChatColor.GOLD + " has won " + ChatColor.WHITE + Slots.formatMoney(winningAmount) + " from " + ChatColor.GOLD + (myMachine.getAccount() == null ? "a slot machine!" : (myMachine.getAccount().getName().equalsIgnoreCase(roller.getName()) ? "their own" : myMachine.getAccount().getName() + "'s") + " slot machine!"));

			} else {

				boolean gotCombo = false;

				for(SlotCombo c : Slots.getSlotCombos()){

					if(c.compare(roll, myMachine.getPlugin().comboOrderMatters)){

						double pay = myMachine.getPlugin().multiplyComboPayout ? c.getPay() * myMachine.getCost() : c.getPay();
						gotCombo = true;
						if(myMachine.getPlugin().useComboNames)
							roller.sendMessage(ChatColor.GOLD + "You rolled the combo, " + c.getName() + "!");
						else
							roller.sendMessage(ChatColor.GOLD + "You got a combo!");

						if(myMachine.getAccount() != null){

							if(myMachine.getPlugin().connectToAccounts){

								if(!myMachine.getAccount().hasEnough(pay)){

									roller.sendMessage(ChatColor.GOLD + "Sorry, the owner's account has run out of money, you won " + Slots.formatMoney(myMachine.getAccount().getAmount()) + ".");
									pay = myMachine.getAccount().getAmount();

								} else {

									roller.sendMessage(ChatColor.GOLD + "You won " + Slots.formatMoney(pay) + "!");

								}

							} else {
								
								if(!myMachine.hasEnoughMoney(pay)){

									roller.sendMessage(ChatColor.GOLD + "Sorry, there isn't enough money inside the machine, you won " + Slots.formatMoney(myMachine.getAmountInside()));
									pay = myMachine.getAmountInside();

								} else {

									roller.sendMessage(ChatColor.GOLD + "Congratulations you won " + Slots.formatMoney(pay) + "!");

								}
								
							}

						} else {

							roller.sendMessage(ChatColor.GOLD + "You won " + Slots.formatMoney(pay) + "!");

						}

						SlotAccount account = new SlotAccount(roller.getName());
						if(!Slots.accountExists(roller.getName()))
							roller.sendMessage(ChatColor.RED + "Could not find a bank account for you.");
						else {

							if(myMachine.getAccount() != null){
								if(myMachine.getPlugin().connectToAccounts)
									myMachine.getAccount().subtractMoney(pay);
								else
									myMachine.subtractMoneyInside(pay);

							}
							account.addMoney(pay);

						}
						if(c.announceWin())
							myMachine.getPlugin().getServer().broadcastMessage(ChatColor.WHITE + roller.getName() + ChatColor.GOLD + " has won " + ChatColor.WHITE + Slots.formatMoney(pay) + " from " + ChatColor.GOLD + (myMachine.getAccount() == null ? "a slot machine!" : (myMachine.getAccount().getName().equalsIgnoreCase(roller.getName()) ? "their own" : myMachine.getAccount().getName() + "'s") + " slot machine!"));
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
			myMachine.isRolling = false;
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
			myMachine.isRolling = false;
			myMachine.getPlugin().getServer().getScheduler().cancelTask(taskId);
			return;

		} else {
			roll[numRolled] = index;
			Slots.outputMessage(roller, addChatColor(ChatColor.GOLD + "You rolled a " + rolled.getName() + "."));
			line = addSignColor(insertSymbol(ChatColor.stripColor(line), rolled.getSymbol()));
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
	
	public String insertSymbol(String line, String symbol){
		
		if(numRolled == 0)
			return symbol + line.substring(1);
		if(numRolled == 1)
			return line.substring(0, 2) + symbol + line.substring(3);
		if(numRolled == 2)
			return line.substring(0, 4) + symbol;
		
		return "";
		
	}

	public String addSignColor(String line){

		int place = 0;
		StringBuilder b = new StringBuilder("");
		String[] split = new String[3];
		for(int i = 0; i < 3; i++)
			if(i != 2)
				split[i] = line.substring(i * 2, (i + 1) * 2);
			else
				split[i] = line.substring(i * 2);

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
