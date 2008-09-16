#!/usr/bin/python

import socket
from select import select
from string import split,strip,join
import sys,os

sys.path.insert(1, os.path.join(sys.path[0], '..'))

# Import jabber client
from jabberpy import jabber

True = 1
False = 0

#Exceptions
class NullArgumentException (Exception):
    def __init__ (self,value):
        self.value = value
    def __str__ (self):
        return repr(self)
    
#Class Contact
class Contact:
    def __init__ (self,name):
        self.name = name
    def __str__ (self):
        str = "Name : [" + self.name + "]"
        return str
    def setName(self,name):
        if name != null:
            self.name = name
        else:
            raise NullArgumentException()
    def getName(self,name):
        return self.name
        
cont = Contact("Test001")
print cont.__str__()
            
            
    