# ThreadOS File System - CSS430 Final Project

This is an implementation of a working file system in Java for UW's ThreadOS. It emulates a UNIX file system with minor differences by implementing inodes, superblocks, file tables and entries, and other components. It is able to allocate and deallocate space, create a virtual directory, and handles active readers and/or writers accordingly.

More information on the project can be found here: https://courses.washington.edu/css430/prog/dimpsey/finalproject.html

## Responsibilities

### Egor Kolyshkin:

#### 1. Superblock (block #0)

Methods:

- Superblock() – constructor
- sync()
- format()
- getFreeBlock()
- returnBlock()

#### 2. FileTable

Methods:

- ffree()
  Similar to readers/writers problem

#### 3. Kernel

- initial switch cases for file system manipulation

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

#### 3. Kernel

- sear to perfection
