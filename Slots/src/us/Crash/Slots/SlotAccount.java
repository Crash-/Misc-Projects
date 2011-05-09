package us.Crash.Slots;

public class SlotAccount {

	private String name;
	
	public SlotAccount(String user){
		
		name = user;
		
	}
	
	public String addMoney(double amt){
		
		return Slots.giveMoneyTo(name, amt);
		
	}
	
	public String subtractMoney(double amt){
		
		return Slots.takeMoneyFrom(name, amt);
		
	}
	
	public boolean hasEnough(double amt){
		
		return Slots.canAfford(name, amt);
		
	}
	
	public String getName(){ return name; }
	
	public double getAmount(){ return Slots.getAmount(name); }
	
}
