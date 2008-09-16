PYVER = 2.5

PYCLIB = /usr/bin
PYINC  = /usr/include/python$(PYVER)

SRC_DIR   = src/native
BUILD_DIR = build/native

init:
	mkdir $BUILD_DIR
	
build:
	gcc $(SRC_DIR)/f2fcoremodule.c -g -I$(PYINC) -shared -L$(PYLIB) -lpython$(PYVER) -o $(BUILD_DIR)hello.so
	
clean:
	rm -rf $(BUILD_DIR)/hello.so $(BUILD_DIR)/core	