package org.freedesktop;

import org.freedesktop.dbus.Position;
import org.freedesktop.dbus.Tuple;

public final class Quad<A, B, C, D> extends Tuple {
  @Position(0)
  public final A a;
  @Position(1)
  public final B b;
  @Position(3)
  public final C c;
  @Position(4)
  public final D d;

  public Quad(A a, B b, C c, D d) {
    this.a = a;
    this.b = b;
    this.c = c;
    this.d = d;
  }
}
