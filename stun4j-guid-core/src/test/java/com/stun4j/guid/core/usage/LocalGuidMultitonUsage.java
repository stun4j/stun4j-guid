package com.stun4j.guid.core.usage;

import java.text.NumberFormat;

import com.stun4j.guid.core.LocalGuid;
import com.stun4j.guid.core.LocalGuidMultiton;

public class LocalGuidMultitonUsage {

  public static void main(String[] args) {
    LocalGuidMultiton._enabled = true;
//    LocalGuidMulti._auto_register_enabled = false;
    
    LocalGuid solo = LocalGuid.initWithLocalIp();
    System.out.println(NumberFormat.getInstance().format(solo.next()));
    System.out.println("Base solo got in another way,is the same: " + (LocalGuidMultiton.instance() == solo));

    LocalGuid anotherSolo = LocalGuidMultiton.instance(16, 4, 4, 5, false);
    System.out.println(NumberFormat.getInstance().format(anotherSolo.next()));
    System.out.println("Compare to the base solo,is another the same: " + (anotherSolo == solo));

    LocalGuid tryAnotherSolo = LocalGuidMultiton.instance(16, 4, 4, 5, false);
    System.out.println("Is another the same: " + (anotherSolo == tryAnotherSolo));
  }

}
