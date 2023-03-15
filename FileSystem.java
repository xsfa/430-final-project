// TODO: ADD COMMENTS TO FUNCTIONS
public class FileSystem {
    private Directory directory;
    private FileTable filetable;
    private SuperBlock superblock;

    public FileSystem(int diskBlocks) {
        // create superblock
        superblock = new SuperBlock(diskBlocks);
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
        if (!filetable.fempty() || files < 0)
            return -1;

        // format and initialize superblock and directory
        superblock.format(files);
        directory = new Directory(this.superblock.inodeBlocks);
        filetable = new FileTable(this.directory);

        return 0;
    }

    private boolean deallocAllBlocks(FileTableEntry fileTableEntry) {
        synchronized (fileTableEntry) {

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
        if (mode.equals("w"))
            if (this.deallocAllBlocks(fileTableEntry) == false)
                return null;

        return fileTableEntry;
    }

    public int fsize(FileTableEntry fileTableEntry) {
        // return the size of the file
        return fileTableEntry.inode.length;
    }

    public int read(FileTableEntry fileTableEntry, byte[] buffer) {
        // param check
        if (fileTableEntry == null) {
            return 0;
        }

        // empty block buffer
        byte[] b = new byte[Disk.blockSize];

        // define markers and variables
        int position = 0;
        int count = 0;
        int seekPtr = fileTableEntry.seekPtr;
        int seekDiff = fileTableEntry.inode.length - seekPtr;
        int blkNumber = fileTableEntry.inode.findTargetBlock(seekPtr);

        // set amount left to read to smallest possible
        if (seekDiff < buffer.length) {
            count = seekDiff;
        } else {
            count = buffer.length;
        }

        int finalPtr = seekPtr + count;

        synchronized (fileTableEntry) {
            while (seekPtr < finalPtr && blkNumber != -1) {
                int offset = seekPtr % Disk.blockSize;
                int countLength = 0;
                int countPosDiff = (count - position);
                int offsetDiff = (b.length - offset);

                // set amount left to read to smallest possible
                if (countPosDiff < offsetDiff) {
                    countLength = countPosDiff;
                } else {
                    countLength = offsetDiff;
                }

                // read to buffer
                System.arraycopy(b, offset, buffer, position, countLength);

                // update seek pointer
                fileTableEntry.seekPtr += countLength;
                position += countLength;
            }
        }

        return position;
    }

    public int write(FileTableEntry fileTableEntry, byte[] buffer) {
        synchronized (fileTableEntry) {
            byte[] b = null;
            Inode inode = fileTableEntry.inode;
            int bufLength = buffer.length;
            int blkNumber = 0;
            int blkSize = Disk.blockSize;
            int bytesCount = 0;
            int bytesLeft = 0;
            int bytesTotal = 0;
            int offset = 0;
            int position = 0;
            int seekPtr = fileTableEntry.seekPtr;

            while (position < bufLength) {
                // determine amount to write; find block
                b = new byte[blkSize];
                bytesCount = (blkSize - offset);
                bytesLeft = bufLength - position;
                offset = (seekPtr % blkSize);
                blkNumber = inode.findTargetBlock(offset);

                // only write to as many bytes as possible
                if (bytesCount < bytesLeft) {
                    bytesTotal = bytesCount;
                } else {
                    bytesTotal = bytesLeft;
                }

                // if block is not found
                if (blkNumber == -1) {
                    // get free block
                    blkNumber = superblock.getFreeBlock();

                    // if no space left, unable to write
                    if (blkNumber == -1) {
                        return -1;
                    }

                    // if block is already registered
                    if (inode.registerTargetBlock(seekPtr, (short) blkNumber) == -1) {
                        if (inode.registerIndexBlock((short) blkNumber) == false) {
                            return -1;
                        }

                        // get another free block
                        blkNumber = superblock.getFreeBlock();

                        // if no space left, unable to write
                        if (blkNumber == -1) {
                            return -1;
                        }

                        // if block is already registered again
                        if (inode.registerTargetBlock(seekPtr, (short) blkNumber) == -1) {
                            return -1;
                        }
                    }
                }

                // if block number is invalid
                if (blkNumber >= superblock.totalBlocks) {
                    return -1;
                }

                // finally, write from target block to disk
                SysLib.rawread(blkNumber, b);
                System.arraycopy(buffer, position, b, offset, bytesTotal);
                SysLib.rawwrite(blkNumber, b);

                // adjust pointers for loop
                position += bytesTotal;
                seekPtr += bytesTotal;
            }

            // if file was written beyond size, increase it
            if (seekPtr > inode.length) {
                inode.length = seekPtr;
            }

            // update seek pointer and save inode to disk
            seek(fileTableEntry, position, 1);
            inode.toDisk(fileTableEntry.iNumber);

            return position;
        }
    }

    // gets the file table entry for the file name removes the file
    public boolean delete(String filename) {
        // get the file table entry
        FileTableEntry fileTableEntry = open(filename, "w");
        // check if the file table entry is null
        if (fileTableEntry == null)
            return false;
        // deallocate all blocks, remove the file from the directory, and close the file
        if (!this.deallocAllBlocks(fileTableEntry) || !directory.ifree(fileTableEntry.iNumber)
                || !this.close(fileTableEntry))
            return false;
        return true;
    }

    // updates seek pointer of file in table entry
    public int seek(FileTableEntry fileTableEntry, int offset, int whence) {
        int totalOffset = 0;
        int iLength = fileTableEntry.inode.length;

        // determine total offset
        if (whence == 0) { // if seek from beginning
            totalOffset = offset;
        } else if (whence == 1) { // if seek from current
            totalOffset = (fileTableEntry.seekPtr + offset);
        } else if (whence == 2) { // if seek from end
            totalOffset = (iLength + offset);
        }

        // set offset cap to length of inode
        if (totalOffset > iLength) {
            totalOffset = iLength;
        }

        // set seek pointer
        synchronized (fileTableEntry) {
            fileTableEntry.seekPtr = totalOffset;
        }

        return totalOffset;
    }

    public boolean close(FileTableEntry fileTableEntry) {
        synchronized (fileTableEntry) {
            // decrement the thread count
            fileTableEntry.count--;

            // no threads sharing file, call ffree
            if (fileTableEntry.count == 0)
                return filetable.ffree(fileTableEntry);
        }
        return true;
    }
}
