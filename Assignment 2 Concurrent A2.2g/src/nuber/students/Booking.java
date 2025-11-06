package nuber.students;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 
 * Booking represents the overall "job" for a passenger getting to their destination.
 * 
 * It begins with a passenger, and when the booking is commenced by the region 
 * responsible for it, an available driver is allocated from dispatch. If no driver is 
 * available, the booking must wait until one is. When the passenger arrives at the destination,
 * a BookingResult object is provided with the overall information for the booking.
 * 
 * The Booking must track how long it takes, from the instant it is created, to when the 
 * passenger arrives at their destination. This should be done using Date class' getTime().
 * 
 * Booking's should have a globally unique, sequential ID, allocated on their creation. 
 * This should be multi-thread friendly, allowing bookings to be created from different threads.
 * 
 * @author james
 *
 */
public class Booking implements Callable<BookingResult>	{
	
	private static final AtomicInteger nextId = new AtomicInteger(1);
	
	private final int bookingId;
	private final NuberDispatch dispatch;
	private final Passenger passenger;
	private final long createdTime;
	
	// set an assigned driver
	private Driver assignedDriver = null;

		
	/**
	 * Creates a new booking for a given Nuber dispatch and passenger, noting that no
	 * driver is provided as it will depend on whether one is available when the region 
	 * can begin processing this booking.
	 * 
	 * @param dispatch
	 * @param passenger
	 */
	public Booking(NuberDispatch dispatch, Passenger passenger)
	{
		this.bookingId = nextId.getAndIncrement();
		this.dispatch = dispatch;
		this.passenger = passenger;
		
		// get a created time
		this.createdTime = new Date().getTime();
	}
	
	/**
	 * At some point, the Nuber Region responsible for the booking can start it (has free spot),
	 * and calls the Booking.call() function, which:
	 * 1.	Asks Dispatch for an available driver
	 * 2.	If no driver is currently available, the booking must wait until one is available. 
	 * 3.	Once it has a driver, it must call the Driver.pickUpPassenger() function, with the 
	 * 			thread pausing whilst as function is called.
	 * 4.	It must then call the Driver.driveToDestination() function, with the thread pausing 
	 * 			whilst as function is called.
	 * 5.	Once at the destination, the time is recorded, so we know the total trip duration. 
	 * 6.	The driver, now free, is added back into Dispatch�s list of available drivers. 
	 * 7.	The call() function the returns a BookingResult object, passing in the appropriate 
	 * 			information required in the BookingResult constructor.
	 *
	 * @return A BookingResult containing the final information about the booking 
	 */
	public BookingResult call() {
		
		// 1. asks Dispatch for an available driver
		// 2. the booking must wait until a driver is available
		assignedDriver = dispatch.getDriver();
		dispatch.logEvent(this,  assignedDriver.name + "assigned. On way to passenger!");
		
		// 3. once it has a driver, it calls Driver.pickUpPassenger()
		assignedDriver.pickUpPassenger(passenger);
		dispatch.logEvent(this,  "Collected passenger. on way to destination");
		
		// 4. then it calls Driver.driveToDestination()
		assignedDriver.driveToDestination();
		
		// 5. once at the destination, the time is recorded
		long tripDuration = new Date().getTime() - createdTime;
		
		// 6. the driver is now free and added back into Dispatch's list of available drivers
		dispatch.logEvent(this, "At destination, driver is now free");
		dispatch.addDriver(assignedDriver);
		
		// 7. returns a BookingResult obj
		return new BookingResult(bookingId, passenger, assignedDriver, tripDuration);		

	}
	
	/***
	 * Should return the:
	 * - booking ID, 
	 * - followed by a colon, 
	 * - followed by the driver's name (if the driver is null, it should show the word "null")
	 * - followed by a colon, 
	 * - followed by the passenger's name (if the passenger is null, it should show the word "null")
	 * 
	 * @return The compiled string
	 */
	@Override
	public String toString()
	{
		String driverName = (assignedDriver != null) ? assignedDriver.name : "null";
		String passengerName = (passenger != null) ? passenger.name : "null";
		
		// follow the return style mentioned above
		return bookingId + " : " + driverName + " : " + passengerName;
	}

}
