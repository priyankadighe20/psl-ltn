package edu.umd.cs.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by Priyanka on 11/5/2016.
 */
public class DataLoader {

    public static int getVocabularyCount(String path) {
        int count = 0;
        try {
            File inFile = new File(path);
            Scanner in = new Scanner(inFile);
            while(in.hasNextLine()) {
                in.nextLine();
                count++;
            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return count;
    }

    public static int[][] getMatrix(String path) {
        ArrayList<int[]> matrixList = new ArrayList<int[]>();
        try {
            File inFile = new File(path);
            Scanner in = new Scanner(inFile);
            while(in.hasNextLine()) {
                String[] breakList = in.nextLine().replace("[", "").replace("]", "").split(", ");
                int[] count = new int[breakList.length];
                for(int i=0; i<count.length; i++) {
                    count[i] = Integer.parseInt(breakList[i]);
                }
                matrixList.add(count);
            }
            int[][] result = new int[matrixList.size()][];
            for(int i=0; i<matrixList.size(); i++){
                result[i] = matrixList.get(i);
            }

            return result;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new int[0][0];
        }

    }
}
