package com.stun4j.guid;

import com.stun4j.guid.utils.Pair;

public class ZkGuidNodeUsage {

  public static void main(String[] args) throws Exception {
    Pair<Integer, Integer> nodePair = ZkGuidNode.start("localhost:2181");
    LocalGuid guid = LocalGuid.init(nodePair);
    long id = guid.next();
    System.out.println(id);

    long id2 = LocalGuid.instance().next();
    System.out.println(id2);

    System.out.println(LocalGuid.uuid());

    // keep work process running;
    Thread.sleep(Integer.MAX_VALUE);
  }

}
