package Assignment1;

import java.util.Map.Entry;
import java.time.chrono.ThaiBuddhistChronology;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FieldRace {
    static AtomicBoolean isOn = new AtomicBoolean(true);
    static final int PLAYER_COUNT = 10;
    static ConcurrentHashMap<Integer, AtomicInteger> scores = new ConcurrentHashMap<>(PLAYER_COUNT);
    static List<AtomicInteger> checkpointScores = new ArrayList<>(Collections.nCopies(PLAYER_COUNT, new AtomicInteger(0)));
    static final int CHECKPOINT_COUNT = 10;
    static List<BlockingQueue<AtomicInteger>> checkpointQueues = Collections.synchronizedList(new ArrayList<BlockingQueue<AtomicInteger>>(CHECKPOINT_COUNT));
    

    public static void main(String[] args) {
        for(int i = 0; i < CHECKPOINT_COUNT; i++){
            checkpointQueues.add(new ArrayBlockingQueue<AtomicInteger>(20));
        }
        for(int i = 0; i < PLAYER_COUNT; i++){
            scores.put(i, new AtomicInteger(0));
        }
        Random random = new Random();

        ExecutorService ex = Executors.newFixedThreadPool(CHECKPOINT_COUNT + PLAYER_COUNT + 1);
        
        ex.submit(new Runnable() {
            public void run(){
                while(true){
                    synchronized(isOn){
                        if(!isOn.get()) break;
                        
                    }
                    printScores(scores);
                    sleepForMsec(1000);
                }
                
            }
        });
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run(){
                synchronized(isOn){
                    isOn.set(false);
                }

                ex.shutdownNow();
                try {
                    ex.awaitTermination(3, TimeUnit.SECONDS);
                } catch (Exception e) {
                    // TODO: handle exception
                }
                ex.shutdownNow();
                printScores(scores);
                timer.cancel();
            }
        }, 10000);
        
        for(int i = 0; i < CHECKPOINT_COUNT; i++){
            int I = i;
            ex.submit(new Runnable() {
                public void run(){
                    while(true){
                        synchronized(isOn){
                            if(!isOn.get()) break;
                        }
                        try {
                            AtomicInteger reciept = checkpointQueues.get(I).poll(2, TimeUnit.SECONDS);
                            if(reciept != null) {
                                reciept.set(getRandomBetween(10, 101));
                                // checkpointQueues.get(I).notify();
                                reciept.notify();
                            }
                        } catch (Exception e) {
                            // TODO: handle exception
                        }
                    }
                }
            });
        }
        
        for(int i = 0; i < PLAYER_COUNT; i++){
            int I = i;
            ex.submit(new Runnable() {
                public void run(){
                    while(true){
                        synchronized(isOn){
                            if(!isOn.get()) break;
                        }
                        int ind_checkpoint = getRandomBetween(0, CHECKPOINT_COUNT);
                        sleepForMsec(getRandomBetween(500, 2001));
                        synchronized(checkpointQueues){
                            try {
                                checkpointQueues.get(ind_checkpoint).add(checkpointScores.get(I));
                            } catch (Exception e) {
                                // TODO: handle exception
                            }
                        }
                        
                        do{
                            try {
                                checkpointScores.get(I).wait(3000);
                                synchronized(isOn){
                                    if(!isOn.get()) break;
                                }
                            } catch (Exception e) {
                                // TODO: handle exception
                            }
                        }while(checkpointScores.get(I).get() == 0);
                        
                        int score = checkpointScores.get(I).get();
                        System.out.println("Player " + I + " got " + score + " points at checkpoint " + ind_checkpoint);
                        scores.get(I).addAndGet(score);
                        checkpointScores.get(I).set(0);
                    }
                }
            });
        }
    }

    private static void printScores(ConcurrentHashMap<Integer, AtomicInteger> scores){
        List<Integer> list = new ArrayList<>();
        for(AtomicInteger ai : scores.values()) list.add(ai.get());
        Collections.sort(list, Comparator.reverseOrder());
        LinkedHashMap<Integer, AtomicInteger> sortedMap = new LinkedHashMap<>();
        for(int num : list){
            for(java.util.Map.Entry<Integer, AtomicInteger> entry : scores.entrySet()){
                if(entry.getValue().get() == num) sortedMap.put(entry.getKey(), entry.getValue());
            }
        }
        System.out.println("Score: " + sortedMap );
    }

    private static void sleepForMsec(int mili){
        try {
            Thread.sleep(mili);
        } catch (InterruptedException e) {
            e.getMessage();
        }
    }

    private static int getRandomBetween(int min, int max){
        Random r = new Random();
        return r.nextInt(min, max);
    }
}

