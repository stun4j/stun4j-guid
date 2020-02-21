package com.stun4j.guid;

import java.util.function.Supplier;

import com.stun4j.guid.utils.Strings;

public class StringsUsage {

  public static void main(String[] args) {
    System.out.println(Strings.lenientFormat("hi %s %s", "foo", "bar"));
    System.out.println(Strings.lenientFormat("hi %s %s", 1));
    try {
      System.out.println(String.format("hi %s %s", 1));
    } catch (Exception e) {
      e.printStackTrace();// ex throw
    }
    int round = 100_0000;
    bench(() -> {
      Strings.lenientFormat("hi %s %s", "foo", "bar");
      return null;
    }, round);
    bench(() -> {
      String.format("hi %s %s", "foo", "bar");
      return null;
    }, round);
  }

  static void bench(Supplier<?> sp, int round) {
    long start = System.currentTimeMillis();
    for (int i = 0; i < round; i++) {
      sp.get();
    }
    System.out.println(System.currentTimeMillis() - start);
  }

}
