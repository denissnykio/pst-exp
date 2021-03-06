package org.pipoware.pst.exp;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pipoware.pst.exp.pages.NBTENTRY;

/**
 *
 * @author Franck Arnulfo
 */
public class PC {

  public static final int NAMEID_RECORD_LENGTH = 8;
  private final BTH bth;
  public final List<PCItem> items = new ArrayList<>();

  public PC(BTH aBTH, NDB ndb, NBTENTRY nbtentry) throws IOException {
    this.bth = aBTH;
    
    for(KeyData keyData : bth.keyDatas) {
      try {
        PCItem item = new PCItem(bth, keyData, ndb, nbtentry);
        items.add(item);
      } catch (UnknownPropertyDataTypeException ex) {
        Logger.getLogger(PC.class.getName()).log(Level.SEVERE, "NID 0x" + Integer.toHexString(bth.hn.nid) + ", " + keyData, ex);
      }
    }
    
  }
  
  public void handleNameToIDMap() {
    PCItem entryStream = findPCItemByPropertyTag(PropertyTag.PidTagNameidStreamEntry);
    Preconditions.checkArgument(entryStream != null);

    PCItem stringStream = findPCItemByPropertyTag(PropertyTag.PidTagNameidStreamString);
    Preconditions.checkArgument(stringStream != null);

    ByteBuffer bbEntryStream = ByteBuffer.wrap(entryStream.dataValue).order(ByteOrder.LITTLE_ENDIAN);
    ByteBuffer bbStringStream = ByteBuffer.wrap(stringStream.dataValue).order(ByteOrder.LITTLE_ENDIAN);

    int nbEntry = entryStream.dataValue.length / NAMEID_RECORD_LENGTH;
    for (int i = 0; i < nbEntry; i++) {
      int dwPropertyID = bbEntryStream.getInt();
      long test = Integer.toUnsignedLong(dwPropertyID);
      short shGuid = bbEntryStream.getShort();
      int namedPropertyIdentifierType = shGuid & 0b01;
      short wPropIdx = bbEntryStream.getShort();
      
      if (namedPropertyIdentifierType == 1) {
        // String
        int offsetInStringStream = dwPropertyID;
        bbStringStream.position(offsetInStringStream);
        int length = bbStringStream.getInt();
        byte []stringBytes = new byte[length];
        bbStringStream.get(stringBytes);
        System.out.println("S UTF_16LE:" + new String(stringBytes, StandardCharsets.UTF_16LE));
        
      } else {
        // 16 bit numerical value
      }
      
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("bth", bth)
      .add("items", items.toString())
      .toString();
  }

  public PCItem findPCItemByPropertyTag(PropertyTag propertyTag) {
    for (PCItem item : items) {
      if (PropertyTag.getPropertyTagFromIdentifier(item.propertyIdentifier, item.getPropertyDataType()) == propertyTag) {
        return item;
      }
    }
    return null;
  }
}
