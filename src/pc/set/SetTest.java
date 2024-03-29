package pc.set;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
@SuppressWarnings("javadoc")
public class SetTest {

  public static void main(String[] args) throws Exception {
    new SetTest(args);
  }

  private static final int NUMBER_OF_SETS = 6;
  private final CyclicBarrier barrier;
  private final int n, N, OPS;
  private final Set<Integer>[] sets;
  private final Object PRINT_LOCK = new Object();
  private final AtomicInteger errors = new AtomicInteger();

  @SuppressWarnings("unchecked")
  private SetTest(String[] args) throws Exception {
    // Number of threads
    n = args.length == 0 ? 128 : Integer.parseInt(args[0]);
    // Number of elements in set per thread
    N = 16;
    // Number of operations per thread
    OPS = 1000;
    // Define the sets
    sets = (Set<Integer>[]) new Set[NUMBER_OF_SETS];
    sets[0] = new LHashSet<>(false);
    sets[1] = new LHashSet<>(true);
    sets[2] = new LHashSet2<>(false);
    sets[3] = new LHashSet2<>(true);
    sets[4] = new LHashSet3<>(false);
    sets[5] = new LHashSet3<>(true);

    barrier = new CyclicBarrier(n + 1);
    for (Set<Integer> s : sets) {
      System.out.println("Testing set: " + s.getClass().getSimpleName());
      for (int i = 0; i < n; i++) {
        final int id = i;
        new Thread(() -> run(id, s)).start();
      }
      barrier.await(); // sync on start
      barrier.await(); // sync before verification
      barrier.await(); // sync at the end
      if (errors.get() == 0) {
        System.out.println("all seems ok :)");
      } else {
        System.out.println("There were errors :(");
      }
      barrier.reset();
    }
  }
  private void run(int id, Set<Integer> set) {
    try {
      barrier.await();
      java.util.Set<Integer> mySet = new java.util.TreeSet<>();
      int min = id * N;
      int max = min + N;
      Random rng = new Random(id);
      for (int i = 0; i < OPS; i++) {
        int v = min + rng.nextInt(max - min);
        switch(rng.nextInt(10)) {
          case 0:
            set.add(v); mySet.add(v); break;
          case 1:
            set.remove(v); mySet.remove(v); break;
          default:
            set.contains(v); break;
        }
      }
      barrier.await();
      synchronized(PRINT_LOCK) {
        for (int i = min; i < max; i++) {
          if (mySet.contains(i) != set.contains(i)) {
            System.out.printf("thread %d: test failed for value %d%n", id, i);
            System.out.printf("thread %d: expects %s%n", id, mySet.toString());
            errors.incrementAndGet();
            break;
          }
        }
      }
      barrier.await();
    }
    catch(BrokenBarrierException e) {

    }
    catch(Throwable e) {
      e.printStackTrace(System.out);
      barrier.reset();
    }
  }



}
