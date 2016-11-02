package mains;

import java.io.File;

public class testDir {
public static void main(String[]args){
	String dirname="/Users/dengjiachuan/Documents/deng/ForAmerica/UVA_Coming/java/IR_Base-master2";
	File dir = new File(dirname);
	for(File f:dir.listFiles()){
		System.out.println("h");
	}
}
}
