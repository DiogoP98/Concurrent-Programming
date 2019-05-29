package pc.stack;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Array-based stack - buggy implementation 1.
 * 
 * @param <E> Type of elements in the stack.
 */
public class AArrayStackV1<E> implements Stack<E> {

  private final int INITIAL_CAPACITY = 16;
  private final E[] array;
  private final AtomicInteger top;

  /**
   * Constructor.
   */
  @SuppressWarnings("unchecked")
  public AArrayStackV1() {
    array = (E[]) new Object[INITIAL_CAPACITY];
    top = new AtomicInteger(0);
  }

  @Override
  public int size() {
    return top.get();
  }

  @Override
  public void push(E elem) {
    /* Qualquer uma das instrucoes seguintes e atomica:
     * - verificar se elem e null;
     * - atualizar o valor do top usando getAndIncrement();
     * - e colocar elem no array.
     * Mas o conjunto destas nao o e.
     *
     * Para corrigir este comportamento, podemos colocar o
     * metodo como synchronized, mas isto alteraria a 
     * implementacao para uma forma bloqueante.
     * A melhor maneira de corrigir este comportamento, mas
     * mantendo a forma nao-bloqueante seria alterando a
     * implementacao de modo a termos um estado que combina
     * o topo da stack e o seu tamanho num so e desse modo
     * atualizar o topo da stack seria actualizar o seu 
     * estado de forma atomica usando por exemplo a instrucao
     * compareAndSet() como na aula de laboratorio 3
     */
    if (elem == null) {
      throw new IllegalArgumentException();
    }
    int pos = top.getAndIncrement();
    array[pos] = elem;
  }

  @Override
  public E pop() {
    /* O que foi descrito no metodo push() tambem se aplica aqui. */
    if (top.get() == 0) {
      return null;
    }
    int pos = top.decrementAndGet();
    E elem = array[pos];
    array[pos] = null;
    return elem;
  }

  //For tests
  @SuppressWarnings("javadoc")
  public static class Test extends StackTest {
    @Override
    public Stack<Integer> createStack() {
      return new AArrayStackV1<>();
    }
  }
  
} 
