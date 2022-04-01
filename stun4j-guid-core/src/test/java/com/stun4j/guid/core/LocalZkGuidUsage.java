package com.stun4j.guid.core;

import com.stun4j.guid.core.LocalGuid;
import com.stun4j.guid.core.LocalZkGuid;
import com.stun4j.guid.core.utils.Utils;

public class LocalZkGuidUsage {

  public static void main(String[] args) throws Exception {
    LocalGuid guid = LocalZkGuid.init("localhost:2181");
    long id = guid.next();
    System.out.println(id);

    long id2 = LocalGuid.instance().next();
    System.out.println(id2);

    new Thread(() -> {
      while (true) {
        Utils.sleepSeconds(5);
        System.out.println("background id generating: " + LocalGuid.instance().next());
      }
    }).start();

    // keep work process running;
    Thread.sleep(Integer.MAX_VALUE);
  }

}
