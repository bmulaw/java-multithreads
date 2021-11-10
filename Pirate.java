package hw6;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Pirate {
    // inspired by Dispatcher.java code given by professor
    String fileName;
    int numCPUs;
    int timeoutMillis;
    ArrayList<UnHashWorker> allWorkers;
    public static ArrayList<Integer> bounds = new ArrayList<Integer>();
    ArrayList<Integer> solved = new ArrayList<Integer>();

    LinkedList<WorkUnit> myWorkQueue;
    LinkedList<WorkUnit> myResQueue;
        Semaphore wqSem;
    Semaphore rsSem;
    Semaphore wqMutex;
    Semaphore rsMutex;

    // using the exact same code professor gave us for Dispatcher.java
    public Pirate(String fileName, int N, int timeout) {
        this.fileName = fileName;
        this.numCPUs = N;
        this.timeoutMillis = timeout;

        this.myWorkQueue = new LinkedList<WorkUnit>();
        this.myResQueue = new LinkedList<WorkUnit>();

        this.allWorkers = new ArrayList<UnHashWorker>();

        this.wqSem = new Semaphore(0);
        this.rsSem = new Semaphore(0);
        this.wqMutex = new Semaphore(1);
        this.rsMutex = new Semaphore(1);

        for (int i = 0; i < N; ++i) {
            UnHashWorker worker = new UnHashWorker(myWorkQueue, myResQueue,
                    wqSem, wqMutex,
                    rsSem, rsMutex);
            worker.setPirated();
            this.allWorkers.add(worker);

            worker.start();
        }
    }

    public void findTreasure() throws InterruptedException {
        Dispatcher dispatcher = new Dispatcher(fileName, numCPUs, timeoutMillis);
        dispatcher.dispatch();
        LinkedList<WorkUnit> newResQueue = dispatcher.getResQueue();
        LinkedList<WorkUnit> notSolved = new LinkedList<WorkUnit>();

        for (WorkUnit workUnit : newResQueue) {
            if (workUnit.getResult() != null) {
                Integer value = Integer.parseInt(workUnit.getResult());
                solved.add(value);
            } else {
                notSolved.add(workUnit);
            }
        }

        Collections.sort(solved);

        int count = 0;
        for (WorkUnit workUnit : notSolved) {
            count++;
            WorkUnit work = new WorkUnit(workUnit.getHash());
            
            wqMutex.acquire();
            myWorkQueue.add(work);
            wqSem.release();
            wqMutex.release();
        }
        
        while(count-- > 0) {
            rsSem.acquire();
            System.out.println(count);
        }

        for (UnHashWorker worker : allWorkers) {
            worker.exitWorker();
        }

        for (UnHashWorker worker : allWorkers) {
            wqSem.release();
        }

        for (int value : solved) {
            System.out.println(value);
        }
        
        System.out.println(myResQueue);
        
        for(WorkUnit res : myResQueue) {
            System.out.println(res);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        String inputFile = args[0];
        int N = Integer.parseInt(args[1]);
        int timeout = Integer.parseInt(args[2]);

        Pirate pirate = new Pirate(inputFile, N, timeout);
        pirate.findTreasure();
    }
}
