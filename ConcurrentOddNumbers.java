import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/** 
 * If N is the supplied argument, this code uses the fork-join model to generate
 * N random numbers and identify the odd numbers in this list and their count. 
 * If is not supplied, then a default value of 6 is assumed. 
 */
public class ConcurrentOddNumbers {
	static int[] randomNumbers; // input list populated based on main argument
	
	// The main method is executed by the Main thread created by the JVM when this class is executed
	public static void main(String[] args) {
		Thread.currentThread().setName("Dispatcher thread");	
		OddNumbersUtil.setTracing();


		// Ask the util class to generate a sequence of random numbers
		randomNumbers = OddNumbersUtil.generateRandomNumbers(args);

		// This code will create worker threads that can work independently
		// of the Main thread and together process the generated numbers to find the odd numbers.
		// This work is distributed among multiple classes. We start this chain of work from
		// the class BasicSynchronizationDemo. From here it goes to OddNumbersDispatcherCode, and then
		// to OddNumbersDispatcherCode, and then to BasicSynchronizationDemo, and then back to OddNumbersDispatcherCode.
		BasicSynchronizationDemo.processRandomNumbers(randomNumbers);

		// Print the odd numbers from the shared data structure
		OddNumbersUtil.printOddNumbers();
	}
}

/**
 * This class contains contains "dispatching" code executed by main().
 * 
 * This code creates the worker threads, associates them with Runnable
 * instances (Described later), starts them, waits for them to finish, and prints the results.
 * 
 * When the Main thread executes this code, it becomes the dispatching thread in
 * the fork-join model, dispatching work to forked child threads.
 *
 */
class OddNumbersDispatcherCode {

	public static final int NUM_THREADS = 4; // number of forked threads is fixed

	// A thread is represented in Java by an instance of the Thread class.
	// So this array keeps track of the worker threads created.
	private static Thread[] threads = new Thread[NUM_THREADS];

	// These are the Runnable instances to be bound to the threads above.
	// There is a one to one correspondence between a thread and a Runnable.
	// Each Runnable has a run() method in it, which is executed when the associated
	// thread is started
	private static Runnable[] runnables = new Runnable[NUM_THREADS]; 

	public static void parallelProcessRandomNumbers(int[] anInputList) {
		createRunnables(anInputList); // First create the runnables
		forkAndJoinThreads(); // Next create, start, and join threads
	}
		
	
	/**
	 * The goal of this method is to create a thread, set its name, and start it.
	 */
	private static void forkThread(int aThreadIndex) {
		Thread aNewThread = new Thread(runnables[aThreadIndex]);
		// The call new Thread(aRunnable) creates a new thread object and
		// associated it with the Runnable argument passed to the constructor
		
		aNewThread.setName("Worker thread-" + aThreadIndex);	

		threads[aThreadIndex] = aNewThread;	

        			
		OddNumbersUtil.traceThreadStart(aNewThread, runnables[aThreadIndex]);
		 // In this code, we make several such calls of the form trace<Some action>() 
         // These calls print information about some action, if the tracing of that 
         // action is enabled
		

		aNewThread.start();	
		// The call t.start() starts the thread represented by t.
		// t.start() calls t.run(), which then calls the run() method of the
		// Runnable instance bound to t. 
	}

	private static void joinThread(int aThreadIndex) {
		try {
			OddNumbersUtil.traceThreadInitiateJoin(threads[aThreadIndex]);
			
			threads[aThreadIndex].join();			
			// t.join blocks the calling thread until t finishes,
			// that is, stops the caller from executing the next instruction
			// until t finishes executing its run method.

			// This blocking akin to a readLine input call blocking the caller
			// until the user types a line.

			// Thus, this call reduces the concurrency in the system, as it
			// makes the joining thread wait until the joined thread t finishes.
			// In other words, even though the joining thread exists concurrently
			// with the joined thread, it is not allowed to execute concurrently
			// with it.

			// If t has finished executing when t.join() is called, the call
			// does not block the joining thread.

			// A thread can join multiple threads, one at a time, by
			// making a series of such calls on each of these threads
			
			OddNumbersUtil.traceThreadEndJoin(threads[aThreadIndex]);

		} catch (InterruptedException e) {
			e.printStackTrace();
			// The joined thread may be interrupted while the joiner thread
			// is waiting for it to finish. If this happens the joiner
			// unblocks, and services an InterruptedException.
		}
	}
	
		
	/**
	 * The goal of this method to make a set of worker threads execute concurrently,
	 * and then make the forking dispatcher thread wait for all of them to finish.
	 * 
	 * The method is buggy.
	 */
	static void forkAndJoinThreads() {
    // Fork all threads
    for (int aThreadIndex = 0; aThreadIndex < threads.length; aThreadIndex++) {
        forkThread(aThreadIndex);
    }
    // Join all threads
    for (int aThreadIndex = 0; aThreadIndex < threads.length; aThreadIndex++) {
        joinThread(aThreadIndex);
    }
}
	
	/**
	 * This method decomposes the work of processing the random numbers into a bunch
	 * of Runnable instances, one for each worker thread to be started.
	 * 
	 * A decomposed work unit is a subsequence of the input random number sequence.
	 * 
	 * It is represented by the start and end indices of this portion.
	 */
	protected static void createRunnables(int[] randomNumbers) {
		int aStartIndex = 0;
		for (int aThreadIndex = 0; aThreadIndex < NUM_THREADS; aThreadIndex++) {
			int aProblemSize = threadProblemSize(aThreadIndex, randomNumbers.length);
			int aStopIndex = aStartIndex + aProblemSize;
			runnables[aThreadIndex] = new OddNumbersWorkerCode(randomNumbers, aStartIndex, aStopIndex);
			aStartIndex = aStopIndex; // next thread's start is this thread's stop
		}
	}

	/**
	 * This method determines how many elements of the input list, whose size is,
	 * aProblemSize, will be processed by the thread whose index in the thread array
	 * is aThreadIndex.
	 * 
	 * The problem may not be evenly divided among the threads. The thread index is
	 * used to determine which threads do extra work. 
	 * 
	 */
	private static int threadProblemSize(int aThreadIndex, int aProblemSize) {
		// Following is the work assigned to a thread 
		// if the problem can be evenly divided among the threads
		int aMinimumProblemSize = aProblemSize / NUM_THREADS;
		OddNumbersUtil.traceMinimumAllocation(aThreadIndex, aMinimumProblemSize);

		// This is the total remaining work to be divided fairly among the worker threads
		int aRemainder = aProblemSize % NUM_THREADS;
		
		// Calculate out how much of the remaining work is done by this thread
		// This is the part of the total remainder assigned to this thread
		int aThreadRemainder = fairThreadRemainderSize(aThreadIndex, aRemainder);
		
		OddNumbersUtil.traceRemainderAllocation(aThreadIndex, aThreadRemainder);

		return aMinimumProblemSize + aThreadRemainder;
	}

	/**
	 * 
	 * This method is called by the method threadProblemSize declared above.
	 * Understand what threadProblemSize does before understanding this method.
	 * 
	 * A list of items may not divided evenly among a bunch of threads.
	 * 
	 * What remains	after giving each thread the minimum number of items is passed 
	 * in the aRemainder argument.
	 * 
	 * This method determines how much of the remainder is given to the thread whose
	 * index is passed to  this method along with the remainder argument.	
	 * 
	 * The goal of this method, as its name suggests, is to divide aRemainder items
	 * work "fairly" among the available threads, which means ensuring that the
	 * differences in the sizes of the portions allocated to threads with
	 * different indices is as small as small as possible.
	 * 
	 * In summary, given aThreadIndex the method indicates how many of aRemaninder items are
	 * given to the indexed thread.
	 * 
	 * aRemainder is expected to be between 0 and NUM_THREADS - 1.
	 * 
	 * aThreadIndex is expected to between 0 and NUM_THREADS - 1;
	 * 
	 * This method is buggy.
	 * 
	 */
	private static int fairThreadRemainderSize(int aThreadIndex, int aRemainder) {
    if (aThreadIndex < aRemainder) {
        return 1;
    } else {
        return 0;
    }
}
}


/**
 * The following is  a Runnable class, which represents the code executed by
 * each forked worker thread.
 * 
 * Different worker threads are bound to different Runnable instances of this
 * class.
 * 
 * This binding is done by the dispatcher class.
 * 
 */
class OddNumbersWorkerCode implements Runnable {
	int[] inputList;
	int startIndex, stopIndex;

	SynchronizationDemo synchronizationDemo = new BasicSynchronizationDemo();

	/**
	 * Unlike the main() method executed by the Main thread, 
	 * the run() method executed by the worker threads does not take parameters.
	 * 
	 * Any parameters needed by it are passed as parameters to the constructor(s) of
	 * the enclosing Runnable class.
	 * 
	 * In this example, these indicate the list of generated random numbers, and the
	 * portion of this list that this instance of Runnable must process.
	 */
	public OddNumbersWorkerCode(int[] anInputList, int aStartIndex, int aStopIndex) {
		inputList = anInputList;
		startIndex = aStartIndex;
		stopIndex = aStopIndex;
	}

	/**
	 * This instance method is executed when we start the thread with which this
	 * Runnable instance is associated.
	 * 
	 * How to associate a Runnable instance with a new thread is shown by
	 * the createRunnables() method of OddNumbersDispactherCode
	 */
	@Override
	public void run() {
		
		OddNumbersUtil.traceThreadStart(startIndex, stopIndex);	
		
		 // we ask another class to do worker tasks to illustrate race conditions	
		synchronizationDemo.fillOddNumbers(inputList, startIndex, stopIndex); 
		
		OddNumbersUtil.traceThreadEnd();
	}
}

/**
 * The goal of the following class is to demonstrate the need for synchronized static and
 * instance methods. Such a method has the synchronized keyword in its header.
 
 * This class is buggy because the synchronized keyword does not appear in any method
 * header
 *
 * You have to determine where to put this keyword.
 */
class BasicSynchronizationDemo implements SynchronizationDemo {
	// The following variables are accessed through this class by all worker threads and the dispatcher thread
	// The second variable is not structly needed but is used to illustrate race conditions
	static List<Integer> oddNumbers; // list of odd numbers found by the worker threads
	static int totalNumberOddNumbers;// number of odd numbers found. 
	
	/**
	 * The values must be reset before workers start modifying them.
	 */
	private static void reset() {
		totalNumberOddNumbers = 0;
		oddNumbers = new ArrayList();
	}

	/**
	 * The following two static methods are called by only the dispatcher thread
	 */
	public static List<Integer> getOddNumbers() {
		return oddNumbers;
	}
	public static int getTotalNumberOddNumbers() {
		return totalNumberOddNumbers;
	}

	private static long LOAD_SAVE_DELAY = 10; //used in the sleep call below
	
	/**
	 * This method is called by the main method.
	 */
	static void processRandomNumbers(int[] anInputList) {
		OddNumbersUtil.traceEntry(BasicSynchronizationDemo.class, "processRandomNumbers");
		reset(); // initialize the shared data
		OddNumbersDispatcherCode.parallelProcessRandomNumbers(anInputList);//ask dispatcher code to create worker threads
		OddNumbersUtil.traceExit(BasicSynchronizationDemo.class, "processRandomNumbers");
	}

	/**
	 * This method is called by a worker thread's run()
	 * 
	 * The code executed by this method is a typical iteration loop executed by
	 * worker threads.
	 * 
	 * It finds all odd numbers in the subsequence of the anInputList between whose
	 * indices are >= aStartIndex and < aStopIndex.
	 * 
	 * It deposits its results in a data structure shared with the  dispatcher 
	 * thread
	 *
	 */
	public void fillOddNumbers(int[] anInputList, int aStartIndex, int aStopIndex) {
		OddNumbersUtil.traceEntry(this, "fillOddNumbers");
		
		if (!OddNumbersUtil.checkPrecondition(anInputList, aStartIndex, aStopIndex)) {
			OddNumbersUtil.threadError("Precondition failed, not performing any iterations for this thread");
			return;
		}
		int aNumberOfOddNumbers = 0;
		for (int index = aStartIndex; index < aStopIndex; index++) {
			// trace the input processed by this iteration
			OddNumbersUtil.printProperty("Index", index);
			int aNumber = anInputList[index];
			OddNumbersUtil.printProperty("Number", aNumber);

			// compute and trace the result computed by this iteration
			boolean isOdd = OddNumbersUtil.isOddNumber(aNumber);
			OddNumbersUtil.printProperty("Is Odd", isOdd);

			if (isOdd) {
				// Deposit the result in shared variables
				OddNumbersUtil.traceCall(BasicSynchronizationDemo.class, "addOddNumber");
				addOddNumber(aNumber); //
				

				aNumberOfOddNumbers++; // updating a local variable
			}
		}

		// This is the number of odd numbers found by the thread that
		// executes this loop. The total number computed by all threads
		// is in a shared data structure.
		OddNumbersUtil.printProperty("Num Odd Numbers", aNumberOfOddNumbers);
		OddNumbersUtil.traceExit(this, "fillOddNumbers");
	}
	
	static synchronized void addOddNumber(int aNumber) {
		OddNumbersUtil.traceEntry(BasicSynchronizationDemo.class,"addOddNumber");
		OddNumbersUtil.traceCall(BasicSynchronizationDemo.class, "incrementTotalOddNumbers");
		incrementTotalOddNumbers();
		
		OddNumbersUtil.traceStartListAdd(aNumber, oddNumbers);
		oddNumbers.add(aNumber);
		OddNumbersUtil.traceEndListAdd(aNumber, oddNumbers);
		
		OddNumbersUtil.traceExit(BasicSynchronizationDemo.class,"addOddNumber");
	}

	/**
	 * This method is called by addOddNumbers to change the shared integer.
	 * It can be called only by addOddNumbers and hence is private
	 */
	static private void incrementTotalOddNumbers() {
		OddNumbersUtil.traceEntry(BasicSynchronizationDemo.class,"incrementTotalOddNumbers");
		// Here we are simulating register-based increments, if you do not know
		// how that works, think of aRegister as a temporary variable

		int aRegister = totalNumberOddNumbers; // Simulate load memory to register
		OddNumbersUtil.traceLoad(aRegister, "totalNumberOddNumbers");

		aRegister++; // increment register
		try {
			Thread.sleep(LOAD_SAVE_DELAY);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// The above sleep call will block the current thread for LOAD_SAVE_DELAY ms 
		// and lets	some other thread execute before the save operation is executed.

		totalNumberOddNumbers = aRegister; // Simulate save register to memory
		OddNumbersUtil.traceSave(aRegister, "totalNumberOddNumbers");
		
		OddNumbersUtil.traceExit(BasicSynchronizationDemo.class,"incrementTotalOddNumbers");
	}
}





// IGNORE ALL CODE BELOW

interface SynchronizationDemo {
	void fillOddNumbers(int[] anInputList, int aStartIndex, int aStopIndex);
}

/**
 * Ignore this class, it has no concurrency concept
 */
class OddNumbersUtil {
	public static final int MAX_RANDOM_NUMBER = 1000;
	public static final int DEFAULT_INPUT_LENGTH = 6;

	static void printOddNumbers() {
		printProperty("Total Num Odd Numbers", BasicSynchronizationDemo.getTotalNumberOddNumbers());
		printProperty("Odd Numbers", BasicSynchronizationDemo.getOddNumbers());
	}

	public static int firstArgToInteger(String[] args) {
		// If nothing was passed on the command line, then print error and exit

		if (args.length < 1) {
			System.err.println(
					"No argument supplied to the main class, assuming default value of " + DEFAULT_INPUT_LENGTH);
			return DEFAULT_INPUT_LENGTH;
			// System.exit(0);;
		}
		// Convert the first command line argument to an integer, exit if error
		try {
			return Integer.parseInt(args[0]);
		} catch (Exception ex) {
			System.err.println("Cannot convert argument on command line to integer");
			System.exit(1);
		}
		return -1;
	}

	static int[] generateRandomNumbers(String[] args) {
		int aNumRandomNumbers = firstArgToInteger(args); // get the number from arguments
		int[] aRandomNumbers = generateRandomNumbers(aNumRandomNumbers);
		printProperty("Random Numbers", Arrays.toString(aRandomNumbers));
		return aRandomNumbers;
	}

	private static int[] generateRandomNumbers(int aNumRandomNumbers) {
		int[] retVal = new int[aNumRandomNumbers];
		for (int index = 0; index < retVal.length; index++) {
			double aRandomDouble = Math.random(); // number between 0 and 1
			int aRandomInteger = (int) (aRandomDouble * MAX_RANDOM_NUMBER);
			retVal[index] = aRandomInteger;
		}
		return retVal;
	}

	public static String threadName(Thread aThread) {
//		return "Thread " + aThread.getId();
		return aThread.getName();

	}

	public static boolean isOddNumber(int aNumber) {
		return aNumber % 2 == 1;
	}

	private static boolean doTests = false;

	public static void setDoTests(boolean newVal) {
		doTests = newVal;
	}
	public static boolean  isDoTests() {
		return doTests;
	}

	public static void printProperty(String aPropertyName, Object aPropertyValue) {
		if (!doTests) {
			myPrintProperty(aPropertyName, aPropertyValue);
			return;
		}
		boolean tryLocalChecks = localChecksPrintProperty(aPropertyName, aPropertyValue);
		if (!tryLocalChecks) {
			myPrintProperty(aPropertyName, aPropertyValue);
		}
	}

	public static boolean localChecksPrintProperty(String aPropertyName, Object aPropertyValue) {
		String aPrintPropertyClassName = "trace.grader.basics.GraderBasicsTraceUtility";
		try {
			Class aPrintPropertyClass = Class.forName(aPrintPropertyClassName);
			Class[] aPrintPropertyArgTypes = { String.class, Object.class };
			Method aPrintPropertyMethod = aPrintPropertyClass.getMethod("printProperty", aPrintPropertyArgTypes);
			Object[] aPrintPropertyArgs = { aPropertyName, aPropertyValue };
			aPrintPropertyMethod.invoke(aPrintPropertyClass, aPrintPropertyArgs);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@SuppressWarnings("deprecation")
	public static String threadPrefix() {
//		return "Thread " + Thread.currentThread().getId() + "->";
		return Thread.currentThread().getName() + "->";

	}

	public static void threadError(String anErrorMessage) {
		String aComposition = threadPrefix() + anErrorMessage;
		System.err.println(aComposition);
	}

	public static void myPrintProperty(String aPropertyName, Object aPropertyValue) {
		String aComposition = threadPrefix() + aPropertyName + ":" + aPropertyValue;
		System.out.println(aComposition);
	}

	protected static boolean checkPrecondition(int[] anInputList, int aStartIndex, int aStopIndex) {
		if (aStartIndex < 0) {
			OddNumbersUtil.threadError("start index < 0");
			return false;
		}
		if (aStopIndex > anInputList.length) {
			OddNumbersUtil.threadError("stop index" + aStopIndex + " > input list length" + anInputList.length);
			return false;
		}
		return true;

	}
	
	private static boolean traceWorkAllocation = false;
	

	public static boolean isTraceFairAllocation() {
		return traceWorkAllocation;
	}

	public static void setTraceFairAllocation(boolean traceWorkAllocation) {
		OddNumbersUtil.traceWorkAllocation = traceWorkAllocation;
	}
	
	protected static void traceMinimumAllocation(int anIndex, int aNumItems) {
		if (isTraceFairAllocation()) {
			printProperty("(MA) Minimum allocation of worker-thread-"+ anIndex, aNumItems + " items of input list");
		}
	}
	
	protected static void traceRemainderAllocation(int anIndex, int aNumItems) {
		if (isTraceFairAllocation()) {
			printProperty("(RA) Remainder allocation of worker-thread-"+ anIndex, aNumItems + " items of input list");
		}
	}
	
	protected static void traceThreadStart(int aStartIndex, int aStopIndex) {
		if (isTraceFairAllocation() || isTraceForkJoin() ) {
			printProperty("(EN) run() called with subsequence", aStartIndex + "-" + aStopIndex);
		}
	}
	
	protected static void traceThreadEnd() {
		if ( isTraceForkJoin() ) {
			printProperty("(EX) Exiting method","run");
		}
	}

	private static boolean traceSynchronization = false;

	public static void setTraceSynchronization(boolean newVal) {
		traceSynchronization = newVal;
	}

	public static boolean isTraceSynchronization() {
		return traceSynchronization;
	}
	private static boolean traceCalls = false;

	public static void setTraceCalls(boolean newVal) {
		traceCalls = newVal;
	}

	public static boolean isTraceCalls() {
		return traceCalls;
	}
	protected static void traceCall(Object anObject, String aMethodName) {
		if (
//			isTraceSynchronization() ||
			isTraceCalls()) {
			printProperty("(CM) Calling method", anObject + "." + aMethodName);
		}
	}

	protected static void traceEntry(Object anObject, String aMethodName) {
		if (
//				isTraceSynchronization() ||
				isTraceCalls()) {
			printProperty("(EN) Entered method", anObject + "." + aMethodName);
		}
	}

	protected static void traceExit(Object anObject, String aMethodName) {
		if (
//				isTraceSynchronization() ||
				isTraceCalls()) {
			printProperty("(EX) Exiting method", anObject + "." + aMethodName);
		}
	}
	
	protected static void traceLoad(int aValue, String aVariable) {
		if (isTraceSynchronization()) {
			printProperty("(LD) Loaded shared " + aVariable,  aValue);
		}
	}
	protected static void traceSave(int aValue, String aVariable) {
		if (isTraceSynchronization()) {
			printProperty("(SV) Saved shared " + aVariable,  aValue);
		}
	}
	protected static void traceStartListAdd(int aValue, List aList) {
		if (isTraceSynchronization()) {
			printProperty("(SA) Start add to shared list", aValue+"-->"+aList);
		}
	}
	protected static void traceEndListAdd(int aValue, List aList) {
		if (isTraceSynchronization()) {
			printProperty("(EA) End add to shared list", aValue+"-->"+aList);
		}
	}
	
	

	private static boolean traceForkJoin = false;

	public static boolean isTraceForkJoin() {
		return traceForkJoin;
	}

	public static void setTraceForkJoin(boolean newVal) {
		OddNumbersUtil.traceForkJoin = newVal;
	}

	protected static void traceThreadStart(Thread aThread, Runnable aRunnable) {
		if (isTraceForkJoin()) {
			printProperty("(TS) Starting new thread bound to runnable", threadName(aThread)+"-->" + aRunnable);
		}
	}
	protected static void traceThreadInitiateJoin(Thread aThread) {
		if (isTraceForkJoin()) {
			printProperty("(SJ) Waiting to join", threadName(aThread));
		}
	}
	protected static void traceThreadEndJoin(Thread aThread) {
		if (isTraceForkJoin()) {
			printProperty("(EJ) Joined", threadName(aThread));
		}
	}
	
	public static void setTracing() {
		if (!OddNumbersUtil.isDoTests()) {
			// replace with true when fixing the fork-join bug
			OddNumbersUtil.setTraceForkJoin(false); 
			
			// replace with true when fixing the work allocation/load balancing bug
			OddNumbersUtil.setTraceFairAllocation(false);
			
			// replace with true when fixing the synchronization bug
			OddNumbersUtil.setTraceSynchronization(false);
		}
	}
}
