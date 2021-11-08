package hw6;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Pirate {
    String fileName;
    Integer N;
    Integer timeout;

    public static ArrayList<Integer> usedBounds = new ArrayList<Integer>();

    public static ArrayList<Integer> cracked = new ArrayList<Integer>();

    ArrayList<UnHashWorker> allWorkers;
    LinkedList<WorkUnit> myWorkQueue;
    LinkedList<WorkUnit> myResQueue;
    
    Semaphore wqSem;
    Semaphore rsSem;
    Semaphore wqMutex;
    Semaphore rsMutex;

    // using the exact same code professor gave us for Dispatcher.java
    public Pirate(String fileName,Integer N,Integer timeout) {
        this.fileName = fileName;
        this.N = N;
        this.timeout = timeout;

        this.allWorkers = new ArrayList<UnHashWorker>();
        
        this.myWorkQueue = new LinkedList<WorkUnit>();
        this.myResQueue = new LinkedList<WorkUnit>();

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

    public void findTreasure() {
        Dispatcher dispatcher = new Dispatcher(fileName, N, timeout);
        try {
            dispatcher.dispatch();
        } catch (InterruptedException e) {
            System.out.println("InterruptedException, cannot dispatch");
        }
        
        LinkedList<WorkUnit> resQueue = dispatcher.getResQueue();
        LinkedList<WorkUnit> unCracked = new LinkedList<WorkUnit>();

        for (WorkUnit workUnit : resQueue) {
            if (workUnit.getResult() != null) {
                Integer value = Integer.parseInt(workUnit.getResult());
                Pirate.cracked.add(value);
            } else {
                unCracked.add(workUnit);
            }
        }
        Collections.sort(Pirate.cracked);
        System.out.println(Pirate.cracked);

        int count = 0;
        for (WorkUnit workUnit : unCracked) {
            count++;
            WorkUnit work = new WorkUnit(workUnit.getHash());

            try {
                wqMutex.acquire();
            } catch (InterruptedException e) {
                System.out.println("Interrupted acquire");
                e.printStackTrace();
            }

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

        for (int i=0;i<Pirate.cracked.size();i++) {
            System.out.println(Pirate.cracked.get(i));
        }
        System.out.println(myResQueue);
        for(WorkUnit res : myResQueue) {
            System.out.println(res);
        }
    }

    public static void main(String[] args) {
        String inputFile = args[0];

        int N = Integer.parseInt(args[1]);

        int timeoutMillis = Integer.parseInt(args[2]);

        Pirate pirate = new Pirate(inputFile, N, timeoutMillis);

        pirate.findTreasure();
    }
}
