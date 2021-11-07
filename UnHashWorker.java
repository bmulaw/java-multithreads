package hw6;
import java.lang.*;
import java.util.*;
import java.util.concurrent.*;
import java.security.*;

/***************************************************/
/* CS-350 Fall 2021 - Homework 6 - Code Solution   */
/* Author: Renato Mancuso (BU)                     */
/*                                                 */
/* Description: This class implements the logic of */
/*   a worker thread that performs MD5 hash        */
/*   cracking. It takes the next hash to crack     */
/*   from a shared queue and outputs the result    */
/*   into another shared queue. It uses semaphores */
/*   to synchronize with the rest of the system on */
/*   all the shared data structures and a          */
/*   brute-force search to reverse each hash.      */
/*                                                 */
/***************************************************/

public class UnHashWorker extends Thread {

    /* Timeout in milliseconds */
    int timeoutMillis;
    
    /* Queue for inputs to be processed */
    LinkedList<WorkUnit> workQueue;

    /* Semaphore to synch up on the number of input items */
    Semaphore wqSem;

    /* Mutex to protect input queue */   
    Semaphore wqMutex;

    /* Queue for processed outputs */
    LinkedList<WorkUnit> resQueue;
    
    /* Semaphore to synch up on the number of output items */
    Semaphore rsSem;

    /* Mutex to protect output queue */
    Semaphore rsMutex;

    /* Boolean to kill the thread */
    volatile boolean stopThread = false;
    
    public UnHashWorker (LinkedList<WorkUnit> workQueue, LinkedList<WorkUnit> resQueue,
			 Semaphore wqSem, Semaphore wqMutex,
			 Semaphore rsSem, Semaphore rsMutex)
    {
	super();
	this.workQueue = workQueue;
	this.resQueue = resQueue;
	this.wqSem = wqSem;
	this.rsSem = rsSem;
	this.wqMutex = wqMutex;
	this.rsMutex = rsMutex;	
	
	/* Default to 10 seconds timeout */
	this.timeoutMillis = 10000;
    }
    

    public WorkUnit timedUnhash (WorkUnit input)
    {
	WorkUnit result = input;
	long timeStart = System.currentTimeMillis();

    	/* Construct a simple hasher class */
        Hash hasher = new Hash();
		String to_unhash = input.getHash();
	
	/* Loop forever until a match is found */
        for(int cur = input.getLowerBound()+1; cur < input.getUpperBound(); ++cur) {
		String numString = Integer.toString(cur);
            String tmpHash = "";

	    try {
		tmpHash = hasher.hash(numString);
            } catch (NoSuchAlgorithmException ex) {
		System.err.println("Unable to compute MD5 hashes.");
		result.setResult("???");
		break;
	    }
	    
            /* Does the current hash matches the target hash? */
            if(tmpHash.equals(to_unhash)) {
		/* Found it! Return right away. */
		result.setResult(numString);
		break;
	    }

	    /* Check timeout, break if time budget exceeded */
	    if (System.currentTimeMillis() > timeStart + this.timeoutMillis) {
		result.setResult(null);
		break;
	    }
        }

	return result;
	
    }
    
    public void run () 
    {
	/* Loop forever until killed */
	while(!this.stopThread) {

	    WorkUnit work = null, result = null;
	    
	    /* Figre out if we can fetch a new piece of data from the
	     * input queue */
	    try {
		wqSem.acquire();
	    } catch (InterruptedException ex) {
		System.err.println("Thread interrupted while waiting for work queue smaphore.");
		continue;
	    }
	    
	    /* Before trying to do more work, check that the thread is
	     * still supposed to be alive */
	    if (this.stopThread)
		break;
	    
	    try {	    
		wqMutex.acquire();
	    } catch (InterruptedException ex) {
		System.err.println("Thread interrupted while waiting for work queue mutex.");
		wqSem.release();
		continue;
	    }

	    /* CRITICAL SECTION */
	    try {
		work = workQueue.remove();
	    } catch (NoSuchElementException ex) {
		/* If we did things right, this should never happen! */
		System.err.println("Attempted to remove element from empty work queue.");
	    }
	    /* END OF CRITICAL SECTION */
	    wqMutex.release();
	    
	    if (work != null) {
		result = timedUnhash(work);

		/* Got some result, add it to the output queue */
		try {
		    rsMutex.acquire();
		} catch (InterruptedException ex) {
		    /* We are in trouble if this happens. We need
		     * better handling here. But keeping it simple for
		     * sake of code readibility. */
		    System.err.println("Thread interrupted while waiting for output queue mutex.");
		}

		/* CRITICAL SECTION */
		
		resQueue.add(result);

		/* Signal that new output is available */
		rsSem.release();
		
		/* END OF CRITICAL SECTION */ 
		rsMutex.release();
	    }
	}
    }

    /* Used to interrupt the thread at the end of the run */
    public void exitWorker () {
	this.stopThread = true;
    }

    /* Set a new timeout for the timed unhash */
    public void setTimeout(int timeout) {
	this.timeoutMillis = timeout;
    }
    
}

/* END -- Q1BSR1QgUmVuYXRvIE1hbmN1c28= */
