// TODO: ADD COMMENTS TO FUNCTIONS
public class FileSystem {
    private Directory directory;
    private FileTable filetable;
    private SuperBlock superblock;

    public FileSystem(int totalBlocks) {
        // create superblock
        superblock = new SuperBlock(totalBlocks);
        // create directory
        directory = new Directory(this.superblock.inodeBlocks);
        // create file table
        filetable = new FileTable(this.directory);

        // rebuilding directory
        FileTableEntry dir = open("/", "r");

        if (fsize(dir) > 0) {
            byte[] dirbytes = new byte[fsize(dir)];

            read(dir, dirbytes);
            directory.bytes2directory(dirbytes);
        }
        close(dir);
    }

    public void syncDir() {
        FileTableEntry dir = open("/", "w");
        byte[] dirbytes = directory.directory2bytes();

        write(dir, dirbytes);
        close(dir);

        superblock.sync();
    }

    public int format(int files) {
        if (!filetable.fempty() || files < 0) return -1;

        // format and initialize superblock and directory
        superblock.format(files);
        directory = new Directory(this.superblock.inodeBlocks);
        filetable = new FileTable(this.directory);

        return 0;
    }

    private boolean deallocAllBlocks(FileTableEntry fileTableEntry) {
        synchronized(fileTableEntry) {

            // loop through and dealloc direct pointers
            for (int i = 0; i < fileTableEntry.inode.direct.length; i++) {
                if (fileTableEntry.inode.direct[i] != -1) {
                    superblock.returnBlock(fileTableEntry.inode.direct[i]);
                    fileTableEntry.inode.direct[i] = -1;
                }
            }

            // deallocate indirect blocks
            if (fileTableEntry.inode.indirect != -1) {
                byte[] indirectBlock = new byte[Disk.blockSize];
                SysLib.rawread(fileTableEntry.inode.indirect, indirectBlock);

                int indirectBlockNumber = SysLib.bytes2int(indirectBlock, 0);

                while (indirectBlockNumber != -1) {
                    superblock.returnBlock(indirectBlockNumber);

                    SysLib.int2bytes(-1, indirectBlock, 0);
                    SysLib.rawwrite(fileTableEntry.inode.indirect, indirectBlock);
                    SysLib.rawread(fileTableEntry.inode.indirect, indirectBlock);

                    indirectBlockNumber = SysLib.bytes2int(indirectBlock, 0);
                }

                superblock.returnBlock(fileTableEntry.inode.indirect);
                fileTableEntry.inode.indirect = -1;
            }

            // set the length to 0
            fileTableEntry.inode.length = 0;

            // write the inode back to the disk
            fileTableEntry.inode.toDisk(fileTableEntry.iNumber);
        }

        return true;
    }

    public FileTableEntry open(String filename, String mode) {
        // create a new file table entry for this file name
        FileTableEntry fileTableEntry = filetable.falloc(filename, mode);
        if ( mode.equals("w") ) if (this.deallocAllBlocks(fileTableEntry) == false) return null;

        return fileTableEntry;
    }

    public int fsize(FileTableEntry fileTableEntry) {
        // return the size of the file
        return fileTableEntry.inode.length;
    }

    // TODO: implement
    public int read(FileTableEntry fileTableEntry, byte[] buffer) {
        return 0;
    }

    // TODO: implement
    public int write(FileTableEntry fileTableEntry, byte[] buffer) {
        return 0;
    }

    // gets the file table entry for the file name removes the file
    public boolean delete(String filename) {
        // get the file table entry
        FileTableEntry fileTableEntry = open(filename, "w");
        // check if the file table entry is null
        if (fileTableEntry == null) return false;
        // deallocate all blocks, remove the file from the directory, and close the file
        if (!this.deallocAllBlocks(fileTableEntry) || !directory.ifree(fileTableEntry.iNumber) || !this.close(fileTableEntry)) return false;
        return true;
    }

    // TODO: IMPLEMENT
    public int seek(FileTableEntry fileTableEntry, int offset, int whence) {
        return 0;
    }

    public boolean close(FileTableEntry fileTableEntry) {
        synchronized(fileTableEntry) {
            // decrement the thread count
            fileTableEntry.count--;

            // no threads sharing file, call ffree
            if (fileTableEntry.count == 0) return filetable.ffree(fileTableEntry);
        }
        return true;
    }
}
