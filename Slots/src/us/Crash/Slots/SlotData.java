package us.Crash.Slots;

import org.bukkit.ChatColor;

public class SlotData {

	private String symbol, name;
	private ChatColor color;
	private int pay, numerator, denominator;
	
	public SlotData(String slotname, String slotsymbol, ChatColor slotcolor, int slotpay, int slotchance1, int slotchance2){
		
		name = slotname;
		symbol = slotsymbol;
		color = slotcolor;
		pay = slotpay;
		numerator = slotchance1;
		denominator = slotchance2;
		
	}
	
	public String getSymbol(){ return symbol; }
	public String getName(){ return name; }
	public String getColor(){ return color + ""; }
	public ChatColor getChatColor(){ return color; }
	public int getPay(){ return pay; }
	public int getNumerator(){ return numerator; }
	public int getDenominator(){ return denominator; }
	public double getChance(){ return (double)numerator / denominator; }
	public void setPay(int newPay){ pay = newPay; }
	
}
