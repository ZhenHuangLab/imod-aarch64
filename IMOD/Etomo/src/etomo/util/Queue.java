package etomo.util;

import java.util.ArrayList;
import java.util.List;

public final class Queue<E> {
  private final List<E> list = new ArrayList<E>();

  public Queue() {}

  public boolean isEmpty() {
    return list.isEmpty();
  }

  public E remove() {
    if (!list.isEmpty()) {
      return list.remove(0);
    }
    return null;
  }

  public E peek() {
    if (!list.isEmpty()) {
      return list.get(0);
    }
    return null;
  }

  public void add(final E element) {
    if (element != null) {
      list.add(element);
    }
  }

  public void clear() {
    list.clear();
  }
}