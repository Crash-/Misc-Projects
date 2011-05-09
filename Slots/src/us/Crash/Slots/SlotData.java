package us.Crash.Slots;

import org.bukkit.ChatColor;

public class SlotData {

	private String symbol, name;
	private ChatColor color;
	private int pay;
	private double chance;
	private boolean announceWin;
	
	public SlotData(String slotname, String slotsymbol, ChatColor slotcolor, int slotpay, int slotchance, boolean announce){
		
		name = slotname;
		symbol = slotsymbol;
		color = slotcolor;
		pay = slotpay;
		chance = slotchance / 100.0;
		announceWin = announce;
		
	}
	
	public String getSymbol(){ return symbol; }
	public String getName(){ return name; }
	public String getColor(){ return color + ""; }
	public boolean announceWin(){ return announceWin; }
	public ChatColor getChatColor(){ return color; }
	public int getPay(){ return pay; }
	public double getChance(){ return chance; }
	public void setPay(int newPay){ pay = newPay; }
	
}
