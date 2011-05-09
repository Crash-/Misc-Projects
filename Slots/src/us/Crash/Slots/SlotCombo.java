package us.Crash.Slots;

public class SlotCombo {

	private String comboName;
	private String[] comboNames;
	private double payAmount;
	private boolean announceWin;

	public SlotCombo(String name, String[] combos, double pay, boolean announce){

		comboName = name;
		comboNames = combos;
		payAmount = pay;
		announceWin = announce;

	}

	public boolean compare(int[] rolls, boolean order){

		if(order){

			int i = 0;

			for(int index : rolls){

				SlotData d = Slots.getSlotData().get(index);

				if(!d.getName().equalsIgnoreCase(comboNames[i]))
					return false;

				i++;

			}

		} else {
			
			boolean[] chosenRolls = { false, false, false };
			
			for(int index : rolls){
				
				SlotData d = Slots.getSlotData().get(index);
				boolean succeeded = false;
				
				for(int i = 0; i < comboNames.length; i++){
					
					if(!chosenRolls[i] && d.getName().equalsIgnoreCase(comboNames[i])){
					
						succeeded = true;
						chosenRolls[i] = true;
						break;
						
					}
					
				}
				
				if(!succeeded)
					return false;
				
			}
			
		}

		return true;

	}

	public double getPay(){ return payAmount; }
	
	public boolean announceWin(){ return announceWin; }
	
	public String getName(){ return comboName; }

	public String[] getNames(){ return comboNames; }

}
