package nuber.students;

public class Driver extends Person {

	// the passenger currently riding
	private Passenger currentPassenger;
	
	public Driver(String driverName, int maxSleep)
	{
		super(driverName, maxSleep);
		
		// no passenger on default state
		this.currentPassenger = null;
	}
	
	/**
	 * Stores the provided passenger as the driver's current passenger and then
	 * sleeps the thread for between 0-maxDelay milliseconds.
	 * 
	 * @param newPassenger Passenger to collect
	 * @throws InterruptedException
	 */
	public void pickUpPassenger(Passenger newPassenger) throws InterruptedException
	{
		// take a passenger
		this.currentPassenger = newPassenger;
		
		// random sleep from 0 ~ maxSleep
		int sleepTime = (int)(Math.random() * (maxSleep + 1));
		Thread.sleep(sleepTime);
	}

	/**
	 * Sleeps the thread for the amount of time returned by the current 
	 * passenger's getTravelTime() function
	 * 
	 * @throws InterruptedException
	 */
	public void driveToDestination() throws InterruptedException {
		
		// nothing to return when no passenger
		if (this.currentPassenger == null) {
			return;
		}
		
		// get travel time by getTravelTime
		int travelTime = this.currentPassenger.getTravelTime();
		
		// sleep on travelTime
		Thread.sleep(travelTime);
		
		// clear a passenger when arrives
		this.currentPassenger = null;

	}
}
	
