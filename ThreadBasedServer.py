#!/usr/bin/python3

import sys, os, stat  # Parameter Parsing
import socket  # Socket Programming
import enum
import struct
from threading import Thread, Lock, Timer
import logging

packet_count = 0

def worker(localaddr):
    local_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    local_socket.bind(localaddr)
    maxPktLen = 1472
    global packet_count
    HELLO = 0
    DATA = 1

    while True:
        data, remoteaddr = local_socket.recvfrom(1472)
        packet_count = packet_count +1

        _, _, msgType, seq, sessionID = struct.unpack('!HBBII', data[:12])

      #  print("Received message msgType is ",msgType)

        message = (data[12:]).decode('utf-8')

        if (msgType == HELLO):
            local_socket.sendto(struct.pack('!HBBII', 0xC356, 1, 0, 0, sessionID), remoteaddr)
        elif (msgType == DATA):
            local_socket.sendto(struct.pack('!HBBII', 0xC356, 1, 2, 0, sessionID), remoteaddr)
            print (message)


# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    localaddr = (b'0.0.0.0', int(sys.argv[1]))
    worker_thread = Thread(target=worker, args=(localaddr,), daemon=True)
    worker_thread.start()

    mode = os.fstat(0).st_mode
    for line in sys.stdin:
        line = line.replace('\n', '')
        if stat.S_ISREG(mode):
            pass
        else:
            # Check if stdin from tty and type 'q'
            if line == 'q':
                print ("packet count: ",packet_count)
                break

