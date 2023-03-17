import java.util.Vector;

import javax.xml.namespace.QName;

public class FileTable {
   private Vector table; // the actual entity of this file table
   private Directory dir; // the root directory

   public FileTable(Directory directory) { // constructor
      table = new Vector(); // instantiate a file (structure) table
      dir = directory; // receive a reference to the Director
   } // from the file system

   public synchronized FileTableEntry falloc(String filename, String mode) {
      // allocate a new file table entry for this file name
      short iNumber = dir.namei(filename);
      Inode inode = null;

      // file does not exist
      if(iNumber == -1) {
         // trying to read empty file
         if(mode.equals("r")) return null;

         // new file
         iNumber = dir.ialloc(filename);
         inode = new Inode();
         inode.flag = 2;

      } else {
         // file exists
         inode = new Inode(iNumber);

         while(true) {
            if(mode.equals("r")) {
               // reading
               if(inode.flag == 0 || inode.flag == 1) {
                  inode.flag = 1;
                  break;

               } else {
                  try { wait(); } catch(InterruptedException e) {}
               }
            } else {
               // can write
               if(inode.flag == 0) {
                  inode.flag = 2;
                  break;

               } else {
                  try { wait(); } catch(InterruptedException e) {}
               }
            }
         }
      }
      // increment this inode's count
      inode.count += 1;

      // immediately write back this inode to the disk
      inode.toDisk(iNumber);

      // add new table entry to disk
      FileTableEntry fileNameEntry = new FileTableEntry(inode, iNumber, mode);
      table.addElement(fileNameEntry);

      // return a reference to this file table entry
      return fileNameEntry;
   }

   public synchronized boolean ffree(FileTableEntry e) {
      // receive a file table entry reference
      boolean found = false;

      if (table.removeElement(e)) { // free this file table entry
         found = true; // return true if entry was found
         Inode inode = e.inode;

         // reset this inode flag
         if (inode.flag == 1) {
            inode.flag = 0;
         } else if (inode.flag == 2) {
            inode.flag = 0;
         } else if (inode.flag == 4) {
            inode.flag = 3;
         } else if (inode.flag == 5) {
            inode.flag = 3;
         }

         // save the corresponding inode to the disk
         inode.count -= 1;
         inode.toDisk(e.iNumber);
      }

      return found;
   }

   public synchronized boolean fempty() {
      return table.isEmpty(); // return if table is empty
   } // should be called before starting a format
}