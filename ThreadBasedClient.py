#!/usr/bin/python3
import sys, os, stat # Parameter Parsing
import socket  # Socket Programming
import time
import enum
import struct
import random
from threading import Thread, Lock, Timer
import logging

def recv(local_socket):
    while 1:
        data, addr = local_socket.recvfrom(1472)

if __name__ == '__main__':
    remotehost = sys.argv[1]
    remoteport = int(sys.argv[2])

    remoteip = socket.gethostbyname(remotehost)
    local_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, 0)
    seq_num = 1

    recvThread = Thread(target=recv, args=(local_socket,), daemon=True)
    recvThread.start()

    local_socket.sendto(struct.pack('!HBBII', 0xC356, 1, 0, 0, 0), (remoteip, remoteport))

    mode = os.fstat(0).st_mode
    for line in sys.stdin:
             line = line.replace('\n','')
             header = struct.pack("!HBBII", 0xC356,1,1,seq_num, 0)
             complete_message = header + line.encode("utf-8")

             local_socket.sendto(complete_message, (remoteip, remoteport))
             seq_num = seq_num + 1

             if stat.S_ISREG(mode):
                 pass
             else:
                 if line == 'q':
                     break
    exit(0)

