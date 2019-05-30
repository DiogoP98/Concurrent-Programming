package pc.stack;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * Lock-free, array-based stack with optional exponential back-off
 * scheme.
 *
 * @param <E> Type of elements in the stack.
 */
public class AArrayStack<E> implements Stack<E> {

  private final int INITIAL_CAPACITY = 16;
  private E[] array;
  private final Backoff backoff;
  AtomicMarkableReference<Integer> mark;

  /**
   * Constructor with no arguments, disabling back-off by default.
   */
  public AArrayStack() {
    this(true);
  }

  /**
   * Constructor with explicit back-off setting.
   * @param enableBackoff Flag indicating if back-off should be used or not.
   */
  @SuppressWarnings("unchecked")
  public AArrayStack(boolean enableBackoff) {
    array = (E[]) new Object[INITIAL_CAPACITY];
    backoff = enableBackoff ? new Backoff() : null;
    mark = new AtomicMarkableReference<>(0,false);
  }

  @Override
  public int size() {
    return mark.getReference();
  }

  @Override
  public void push(E elem) {
    if (elem == null) {
      throw new IllegalArgumentException();
    }
    while(true) {
      if (mark.compareAndSet(mark.getReference(),mark.getReference(),false,true)) {
        if (mark.getReference() == array.length) {
          array = Arrays.copyOf(array, 2 * array.length);
        }
        array[mark.getReference()] = elem;
        mark.set(mark.getReference()+1,false);
        if (backoff != null)
          backoff.diminish();
        break;
      } else {
          if (backoff != null)
            backoff.delay();
      }
    }
  }

  @Override
  public E pop() {
    E elem = null;
    while(true) {
      if (mark.compareAndSet(mark.getReference(),mark.getReference(),false,true)) {
        if (mark.getReference() == 0){
          mark.set(mark.getReference(),false);
          break;
        }
        elem = array[mark.getReference() - 1];
        array[mark.getReference() - 1] = null;
        mark.set(mark.getReference()-1,false);
        if (backoff != null)
          backoff.diminish();
        break;
      } else {
          if (backoff != null)
            backoff.delay();
      }
    }
    return elem;
  }

  // For tests
  @SuppressWarnings("javadoc")
  public static class Test extends StackTest {
    @Override
    public Stack<Integer> createStack() {
      return new AArrayStack<>();
    }
  }
}
