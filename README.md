# CSS 430 - Final Project

## Responsibilities
### Egor Kolyshkin:
#### 1. Superblock (block #0)
Methods:
- Superblock() – constructor
- sync()
- format()
- getFreeBlock()
- returnBlock()

Notes: 
- Freelist points to given block number (enqueuing the block at end of freelist)
- First four blocks = total # of blocks
- Next four blocks = total # of inodes
- 8+ blocks = freelist (next free block(s))
- Inodes are created starting from block #1 each occupying 32 bytes

#### 2. FileTable
Methods:
- ffree()
Similar to readers/writers problem

### Tesfa Shenkute:

#### 1. Directory
Methods:
- ialloc()
- ifree()
- namei()

#### 2. FileTable
Methods:
- falloc()
Similar to readers/writers problem
