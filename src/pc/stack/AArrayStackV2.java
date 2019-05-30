package pc.stack;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Array-based stack - buggy implementation 1.
 * 
 * @param <E> Type of elements in the stack.
 */
public class AArrayStackV2<E> implements Stack<E> {

  private final int INITIAL_CAPACITY = 16;
  private final E[] array;
  private final AtomicInteger top;
  private final Backoff backoff;

  /**
   * Constructor with no arguments, disabling back-off by default.
   */
  public AArrayStackV2() {
    this(false);
  }

  /**
   * Constructor with explicit back-off setting.
   * @param enableBackoff Flag indicating if back-off should be used or not.
   */
  @SuppressWarnings("unchecked")
  public AArrayStackV2(boolean enableBackoff) {
    array = (E[]) new Object[INITIAL_CAPACITY];
    top = new AtomicInteger(0);
    backoff = enableBackoff ? new Backoff() : null;
  }

  @Override
  public int size() {
    return top.get();
  }

  @Override
  public void push(E elem) {
    if (elem == null) {
      throw new IllegalArgumentException();
    }
    while (true) {
      int n = top.get();
      array[n] = elem;
      /* Para determinar se o novo elemento foi inserido correctamente,
       * verificamos se o tamanho da stack foi incrementado por 1. Isto
       * pode acontecer mesmo que o elemento seja removido e um outro 
       * inserido no seu lugar por outra thread, entre a sua insercao e
       * o momento que o compareAndSet() e efectuado.
       * Tal como no caso do AArrayStackV1.java, este problema pode ser
       * evitado se utilizarmos um estado para representar o topo da stack.
       * Deste modo, podemos nao so verificar se o tamanho da stack foi
       * incrementado corretamente, mas tambem se o elemento no topo da stack
       * e o elemento que foi iserido - ver aula 3.
       */
      if (top.compareAndSet(n, n+1)) {
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
    E elem = null;
    while (true) {
      int n = top.get();
      if (n == 0) {
        elem = null;
        break;
      }
      elem = array[n - 1];
      array[n - 1] = null;
      /* O que foi descrito no metodo push() tambem se aplica aqui. */
      if (top.compareAndSet(n, n - 1)) {
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
      return new AArrayStackV2<>();
    }
  }
} 
