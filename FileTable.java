import java.util.Vector;

public class FileTable {
   private Vector table; // the actual entity of this file table
   private Directory dir; // the root directory

   // define inode flags
   final static char UNUSED = 0; // 0 = unused
   final static char USED_r = 1; // 1 = used(r)
   final static char USED_1r = 2; // 2 = used(!r)
   final static char UNUSED_w = 3; // 3 = unused(wreg)
   final static char USED_rw = 4; // 4 = used(r,wreg)
   final static char USED_1rw = 5; // 5 = used(!r,wreg)

   public FileTable(Directory directory) { // constructor
      table = new Vector(); // instantiate a file (structure) table
      dir = directory; // receive a reference to the Director
   } // from the file system

   // IMPLEMENT
   public synchronized FileTableEntry falloc(String filename, String mode) {
      // allocate a new file (structure) table entry for this file name
      // allocate/retrieve and register the corresponding inode using dir
      // increment this inode's count
      // immediately write back this inode to the disk
      // return a reference to this file (structure) table entry

      // Egor: use flags implemented above
   }

   public synchronized boolean ffree(FileTableEntry e) {
      // receive a file table entry reference
      boolean found = false;

      if (table.removeElement(e)) { // free this file table entry
         found = true; // return true if entry was found
         Inode inode = e.inode;

         // reset this inode flag
         if (inode.flag == USED_r || inode.flag == USED_1r) {
            notify();
            inode.flag = UNUSED;
         } else if (inode.flag == USED_rw || inode.flag == USED_1rw) {
            notify();
            inode.flag = UNUSED_w;
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