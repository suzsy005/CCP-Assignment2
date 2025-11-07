package nuber.students;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;


/**
 * A single Nuber region that operates independently of other regions, other than getting 
 * drivers from bookings from the central dispatch.
 * 
 * A region has a maxSimultaneousJobs setting that defines the maximum number of bookings 
 * that can be active with a driver at any time. For passengers booked that exceed that 
 * active count, the booking is accepted, but must wait until a position is available, and 
 * a driver is available.
 * 
 * Bookings do NOT have to be completed in FIFO order.
 * 
 * @author james
 *
 */
public class NuberRegion {
	
	private final NuberDispatch dispatch;
	private final String regionName;
	
	// this semaphore is for limiting the number of tasks that happen at the same time
	private final Semaphore jobLimiter;
	
	// this ExcecutorServie is for excecuting the booking tasks
	private final ExecutorService regionExecutor;
	
	private volatile boolean isShuttingDown = false;

	/**
	 * Creates a new Nuber region
	 * 
	 * @param dispatch The central dispatch to use for obtaining drivers, and logging events
	 * @param regionName The regions name, unique for the dispatch instance
	 * @param maxSimultaneousJobs The maximum number of simultaneous bookings the region is allowed to process
	 */
	public NuberRegion(NuberDispatch dispatch, String regionName, int maxSimultaneousJobs)
	{
		this.dispatch = dispatch;
		this.regionName = regionName;
		
		// initialize Semaphore with maxSimultaneousJobs
		this.jobLimiter = new Semaphore(maxSimultaneousJobs);
		
		// ExecutorService executes tasks in each region
		// Thread uses SingleThreadExecutor() in order to use ExecutorService of Dispatch
		this.regionExecutor = Executors.newSingleThreadExecutor();

	}
	
	/**
	 * Creates a booking for given passenger, and adds the booking to the 
	 * collection of jobs to process. Once the region has a position available, and a driver is available, 
	 * the booking should commence automatically. 
	 * 
	 * If the region has been told to shutdown, this function should return null, and log a message to the 
	 * console that the booking was rejected.
	 * 
	 * @param waitingPassenger
	 * @return a Future that will provide the final BookingResult object from the completed booking
	 */
	public Future<BookingResult> bookPassenger(Booking newBooking)
	{		
		// rejects a booking when shuttingd down
		if (isShuttingDown) {
			dispatch.logEvent(newBooking, "Booknig is rejected: Region is shutting down.");
			return null;
		}
		
		Future<BookingResult> future = regionExecutor.submit(() -> {
			
			try 
			{
				// wait until it's available
				jobLimiter.acquire();
				
				// get the slot and commence job
				dispatch.logEvent(newBooking, "Acquired slot. Commencing job.");
				
				// executes call() from Booking
				return newBooking.call();

			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				dispatch.logEvent(newBooking, "Booking interrupted.");
				return null;
			}
			finally
			{
				// release after finishing executing
				jobLimiter.release();
				dispatch.logEvent(newBooking, "Slot released.");

			}
		});
		
		return future;	
		
	}
	
	/**
	 * Called by dispatch to tell the region to complete its existing bookings and stop accepting any new bookings
	 */
	public void shutdown()
	{
		// set the shut down flag
		isShuttingDown = true;
		
		// stop receiving any new tasks
		regionExecutor.shutdown();
		
		// wait until RegionExecutor completes
		try
		{
			// set only 10 seconds because Region task should finish soon
			if (!regionExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
			}
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
		
	}
		
}
