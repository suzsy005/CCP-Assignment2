package nuber.students;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.atomic.AtomicInteger;

/**
 * The core Dispatch class that instantiates and manages everything for Nuber
 * 
 * @author james
 *
 */
public class NuberDispatch {

	/**
	 * The maximum number of idle drivers that can be awaiting a booking 
	 */
	private final int MAX_DRIVERS = 999;
	
	private boolean logEvents = false;
	
	// a driver pool
	private final BlockingQueue<Driver> driverPool;
	
	// maps the region and NuberRegion instance
	private final Map<String, NuberRegion> regions;
	
	// in order to excute the all Booking tasks
	private final ExecutoService executor;
	
	
	
	
	/**
	 * Creates a new dispatch objects and instantiates the required regions and any other objects required.
	 * It should be able to handle a variable number of regions based on the HashMap provided.
	 * 
	 * @param regionInfo Map of region names and the max simultaneous bookings they can handle
	 * @param logEvents Whether logEvent should print out events passed to it
	 */
	public NuberDispatch(HashMap<String, Integer> regionInfo, boolean logEvents)
	{
		this.logEvents = logEvents;
		
		// initializes drivePool
		this.driverPool = new ArrayBlockingQueue<>(MAX_DRIVERS);
		// initializes regions (region map)
		this.regions = new HashMap<>();
		// initializes thread pool for task execution
		this.executor = Executor.newCachedThreadPool();
		
		// creates NuberRegion by HashMap<String, Integer>
		for (Map.Entry<String, Integer> entry : regionInfo.entrySet()) {
			this.regions.put(entry.getKey(), new NuberRegion(entry.getKey(), entry.getValue());
		}
	}
	
	/**
	 * Adds drivers to a queue of idle driver.
	 *  
	 * Must be able to have drivers added from multiple threads.
	 * 
	 * @param The driver to add to the queue.
	 * @return Returns true if driver was added to the queue
	 */
	public boolean addDriver(Driver newDriver)
	{
		// put driver back to driverPool
		boolean added = driverPool.offer(newDriver);
		
		if (added) {
			logEvent(null, newDriver.name + " added to drier pool.");
		}
		
		return added;
		
	}
	
	/**
	 * Gets a driver from the front of the queue
	 *  
	 * Must be able to have drivers added from multiple threads.
	 * 
	 * @return A driver that has been removed from the queue
	 */
	public Driver getDriver()
	{
		try
		{
			// if the queue is empty, it blocks a thread untill a driver is added
			Driver driver = driverPool.take();
			
			// aftet this getDriver() is called inside of Booking.call(), then the counter decreases as the driver is assigned 
			bookingAwaitingDriver.decrementAndGet();
			return driver;
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			logEvent(null, "Driver acquisition interrupted.");
			
			return null;
		}
	}

	/**
	 * Prints out the string
	 * 	    booking + ": " + message
	 * to the standard output only if the logEvents variable passed into the constructor was true
	 * 
	 * @param booking The booking that's responsible for the event occurring
	 * @param message The message to show
	 */
	public void logEvent(Booking booking, String message) {
		
		if (!logEvents) return;
		
		System.out.println(booking + ": " + message);
		
	}

	/**
	 * Books a given passenger into a given Nuber region.
	 * 
	 * Once a passenger is booked, the getBookingsAwaitingDriver() should be returning one higher.
	 * 
	 * If the region has been asked to shutdown, the booking should be rejected, and null returned.
	 * 
	 * @param passenger The passenger to book
	 * @param region The region to book them into
	 * @return returns a Future<BookingResult> object
	 */
	public Future<BookingResult> bookPassenger(Passenger passenger, String region) {
		
		// rejects bookings when shutting down
		if (isShuttingDown) {
			logEvent(null, "Booking for " + passenger.name + " rejected: Dispatch is shutting down.");
			return null;
		}
		
		NuberRegion targetRegion = regions.get(region);
		// if can't find the regions, return null
		if (targetRegion == null) {
			return null;
		}
		
		// creates Booking obj
		Booking newBooking = new Booking(this, passenger);
		
		// increase the counter of people waiting for drivers
		bookingsAwaitingDriver.incrementAndGet();
		
		// gives newBooking to Region and returns Future<BookingResult> obj
		return targetRegion.bookPassenger(newBooking);

	}

	/**
	 * Gets the number of non-completed bookings that are awaiting a driver from dispatch
	 * 
	 * Once a driver is given to a booking, the value in this counter should be reduced by one
	 * 
	 * @return Number of bookings awaiting driver, across ALL regions
	 */
	public int getBookingsAwaitingDriver()
	{
		// return a current value of AtomicInteger
		return bookingAwaitingDriver.get();
	}
	
	/**
	 * Tells all regions to finish existing bookings already allocated, and stop accepting new bookings
	 */
	public void shutdown() {
		
		// set the shut down flag
		isShuttingDonw = true;
		
		// asks All of NuberRegion to shut down
		for (NuberRegion region : regions.values()) {
			region.shutdown();
		}
		
		// asks ExecutorService not to take any new tasks
		executor.shutdown();
		
		// 
		
		
	}
}





