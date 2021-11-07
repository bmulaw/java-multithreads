package hw6;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;

/***************************************************/
/* CS-350 Fall 2021 - Homework 6 - Code Solution   */
/* Author: Renato Mancuso (BU)                     */
/*                                                 */
/* Description: This class implements the logic of */
/*   a linear dispatcher that also spans and       */
/*   manages a set of worker threads to crack a    */
/*   a set of MD5 hashes provied in an input file. */
/*                                                 */
/***************************************************/

public class Dispatcher {

    String fileName;
    int numCPUs;
    int timeoutMillis;
    ArrayList<UnHashWorker> workers;

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
    
    public Dispatcher (String fileName, int N, int timeout) {
	this.fileName = fileName;
	this.numCPUs = N;
	this.timeoutMillis = timeout;

	/* Now build the other data structures */
	workQueue = new LinkedList<WorkUnit>();
	resQueue = new LinkedList<WorkUnit>();	

	workers = new ArrayList<UnHashWorker>();

	/* Initialize the semaphores necessary to synchronize over the
	 * input and output queues */
	wqSem = new Semaphore(0);
	wqMutex = new Semaphore(1);

	rsSem = new Semaphore(0);
	rsMutex = new Semaphore(1);
	
	/* Start by spawning the worker threads */
	for (int i = 0; i < N; ++i) {
	    UnHashWorker worker = new UnHashWorker(workQueue, resQueue,
						   wqSem, wqMutex,
						   rsSem, rsMutex);
	    worker.setTimeout(timeout);
	    workers.add(worker);

	    /* Ready to launch the worker */
	    worker.start();
	}
    }
    
    public void dispatch () throws InterruptedException
    {
        /* The fileName parameter contains the full path to the input file */
        Path inputFile = Paths.get(fileName);

	/* Attempt to open the input file, if it exists */
        if (Files.exists(inputFile)) {

	    /* It appears to exists, so open file */
            File fHandle = inputFile.toFile();
            
            /* Use a buffered reader to be a bit faster on the I/O */
            try (BufferedReader in = new BufferedReader(new FileReader(fHandle)))
            {

                String line;
		int count = 0;
		
		/* Pass each line read in input to the dehasher */
                while((line = in.readLine()) != null){

		    ++count;
		    WorkUnit work = new WorkUnit(line);
		    
		    wqMutex.acquire();
		    /* CRITICAL SECTION */

		    workQueue.add(work);

		    /* Signal the presence of new work to be done */
		    wqSem.release();
		    
		    /* END OF CRITICAL SECTION */		    
		    wqMutex.release();		    
                }

		/* At this point, we just wait for all the input to be consumed */
		while(count-- > 0) {
		    rsSem.acquire();
		}

		/* All done, terminate all the worker threads */
		for (UnHashWorker worker : workers) {
		    worker.exitWorker();
		}

		/* Make sure that no worker is stuck on the empty input queue */
		for (UnHashWorker worker : workers) {
		    wqSem.release();
		}

		/* Print out result */
		for(WorkUnit res : resQueue) {
		    System.out.println(res);
		}
		
            } catch (FileNotFoundException e) {
                System.err.println("Input file does not exist.");
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println("Unable to open input file for read operation.");
                e.printStackTrace();
            }	    
	    
        } else {
            System.err.println("Input file does not exist. Exiting.");        	
        }

    }

    /* Entry point of the code */
    public static void main(String[] args) throws InterruptedException
    {
	/* Read path of input file */       
        String inputFile = args[0];

	/* Read number of available CPUs */
	int N = Integer.parseInt(args[1]);

	/* If it exists, read in timeout, default to 10 seconds otherwise */
	int timeoutMillis = 100000;
	if (args.length > 2) {
	    timeoutMillis = Integer.parseInt(args[2]);
	}

	/* Construct the dispatcher with all the necessary parameters */
        Dispatcher theDispatcher = new Dispatcher(inputFile, N, timeoutMillis);

	/* Start the work */
        theDispatcher.dispatch();
    }
}

/* END -- Q1BSR1QgUmVuYXRvIE1hbmN1c28= */
