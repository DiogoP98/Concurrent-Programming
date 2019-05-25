package pc.stack;

import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Lock-free stack with optional exponential back-off scheme.
 *
 * @param <E> Type of elements in the stack.
 */
public class ALinkedStackASR<E> implements Stack<E> {


  private final AtomicStampedReference<Node<E>> ref;

  private final Backoff backoff;

  /**
   * Constructor with no arguments, disabling back-off by default.
   */
  public ALinkedStackASR() {
    this(false);
  }

  /**
   * Constructor with explicit back-off setting.
   * @param enableBackoff Flag indicating if back-off should be used or not.
   */
  public ALinkedStackASR(boolean enableBackoff) {
    ref = new AtomicStampedReference<>(null, 0);
    backoff = enableBackoff ? new Backoff() : null;
  }

  @Override
  public int size() {
    return ref.getStamp(); // use ASR stamp to store the size!
  }

  @Override
  public void push(E elem) {
    if (elem == null) {
      throw new IllegalArgumentException();
    }
    Node<E> newTop = new Node<>(elem, null);
    newTop.data = elem;
    Node<E> oldReference,newReference;
    Integer newStamp;
    int[] holder = new int[1];
    while(true) {
      oldReference = ref.get(holder);
      newTop.next = oldReference;
      newReference = newTop;
      newStamp = holder[0] + 1;
      if (ref.compareAndSet(oldReference, newReference, holder[0], newStamp)) {
        if (backoff != null) {
          backoff.diminish();
        }
        break;
      }
      if (backoff != null) {
        backoff.delay();
      }
    }
  }

  @Override
  public E pop() {
    Node<E> oldReference, newReference;
    int newStamp;
    E elem = null;
    int[] holder = new int[1];
    while(true) {
      oldReference = ref.get(holder);
      if (holder[0] == 0) {
        elem = null;
        break;
      }
      newReference = oldReference.next;
      newStamp = holder[0] - 1;
      elem = oldReference.data;
      if (ref.compareAndSet(oldReference, newReference, holder[0], newStamp)) {
        if (backoff != null) {
          backoff.diminish();
        }
        break;
      }
      if (backoff != null) {
        backoff.delay();
      }
    }
    return elem;
  }

  //For tests
  @SuppressWarnings("javadoc")
  public static class Test extends StackTest {
    @Override
    public Stack<Integer> createStack() {
      return new ALinkedStackASR<>();
    }
  }
}
