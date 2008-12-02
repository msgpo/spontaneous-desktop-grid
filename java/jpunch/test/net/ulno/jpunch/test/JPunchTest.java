package net.ulno.jpunch.test;

import net.ulno.jpunch.comm.udp.UDPTester;

public class JPunchTest {
	private UDPTester udpTester = new UDPTester();
	
	public void test() {
		udpTester.start();
		
	}
	
	public static void main(String[] args){
		JPunchTest jPunchTest = new JPunchTest();
		jPunchTest.test();
	}
}
