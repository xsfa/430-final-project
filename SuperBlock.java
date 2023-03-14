public class SuperBlock {
    public int totalBlocks; // the number of disk blocks
    public int inodeBlocks; // the number of inodes
    public int freeList; // the block number of the free list's head

    public SuperBlock(int diskSize) {
        // read the superblock from disk to buffer
        byte[] b = new byte[Disk.blockSize];
        SysLib.rawread(0, b);

        // read content of buffer to initialize superblock object
        this.totalBlocks = SysLib.bytes2int(b, 0);
        this.inodeBlocks = SysLib.bytes2int(b, 4);
        this.freeList = SysLib.bytes2int(b, 8);

        // if disk contents are invalid
        if (totalBlocks != diskSize || inodeBlocks <= 0 || freeList < 2) {
            this.totalBlocks = diskSize;

            // if total inode count is invalid
            if (inodeBlocks <= 0) {
                format(64); // format disk with max number of inodes
            } else {
                format(inodeBlocks); // format disk with given number of inodes
            }
        }
    }

    void sync() {
        // empty block buffer
        byte[] b = new byte[Disk.blockSize];

        // fill buffer with content from superblock object
        SysLib.int2bytes(totalBlocks, b, 0);
        SysLib.int2bytes(inodeBlocks , b, 4);
        SysLib.int2bytes(freeList, b, 8);

        // write buffer to disk as superblock
        SysLib.rawwrite(0, b);
    }

    void format(int files) {
        // initialize superblock contents and empty block buffer
        byte[] b = null;
        this.inodeBlocks = files;
        this.freeList = (inodeBlocks / 16) + 1;

        // initialize each inode and immediately write it back to disk
        for (short i = 0; i < inodeBlocks ; i++) {
            Inode inode = new Inode();
            inode.toDisk(i);
        }

        // initialize free blocks
        for (int i = freeList; i < totalBlocks - 2; i++) {
            b = new byte[Disk.blockSize];
            SysLib.int2bytes(i + 1, b, 0);
            SysLib.rawwrite(i, b);
        }

        // initialize last block without pointer
        SysLib.int2bytes(-1, b, 0);
        SysLib.rawwrite(totalBlocks - 1, b);

        sync(); // update superblock on disk
    }

    public int getFreeBlock() {
        // block number of free block
        int freeBlock = -1;

        // if parameters are valid
        if (freeList < totalBlocks && freeList > 0) {
            // get a new free block from the free list
            byte[] b = new byte[Disk.blockSize];
            SysLib.rawread(freeList, b);

            // update free list
            freeBlock = freeList;
            this.freeList = SysLib.bytes2int(b, 0);
            sync(); // update superblock on disk
        }

        return freeBlock;
    }

    public boolean returnBlock(int oldBlockNumber) {
        // true if block was returned, false otherwise
        boolean blockReturn = false;

        // if parameters are valid
        if (oldBlockNumber < totalBlocks || oldBlockNumber > 0) {
            // empty block buffer
            byte[] b = new byte[Disk.blockSize];

            // write back old block to disk
            SysLib.int2bytes(freeList, b, 0);
            SysLib.rawwrite(oldBlockNumber, b);

            // update free list
            this.freeList = oldBlockNumber;
            blockReturn = true;
            sync(); // update superblock on disk
        }

        return blockReturn;
    }
}