package com.stun4j.guid;

public class ZkGuidNodeUsage {

  public static void main(String[] args) throws Exception {
    short[] nodeInfo = ZkGuidNode.start("localhost:2181");
    short datacenterId = nodeInfo[0];
    short workerId = nodeInfo[1];
    LocalGuid guid = LocalGuid.init(datacenterId, workerId);
    long id = guid.next();
    System.out.println(id);

    long id2 = LocalGuid.instance().next();
    System.out.println(id2);

    System.out.println(LocalGuid.uuid());

    // keep work process running;
    Thread.sleep(Integer.MAX_VALUE);
  }

}
