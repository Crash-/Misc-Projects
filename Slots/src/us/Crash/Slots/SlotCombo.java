package us.Crash.Slots;

public class SlotCombo {

	private String[] comboNames;
	private double payAmount;
	
	public SlotCombo(String[] combos, double pay){
		
		comboNames = combos;
		payAmount = pay;
		
	}
	
	public boolean compare(int[] rolls){
		
		int i = 0;
		
		for(int index : rolls){
			
			SlotData d = Slots.rollInfo.get(index);
			
			if(!d.getName().equalsIgnoreCase(comboNames[i]))
				return false;
			
			i++;
			
		}
		
		return true;
		
	}
	
	public double getPay(){ return payAmount; }
	
	public String[] getNames(){ return comboNames; }
	
}
