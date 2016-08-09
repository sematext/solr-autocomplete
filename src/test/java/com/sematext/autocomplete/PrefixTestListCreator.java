package com.sematext.autocomplete;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;

public class PrefixTestListCreator {

    /**
     * @param args
     * @throws MalformedURLException
     */
    public static void main(String[] args) throws MalformedURLException {

        if (args.length < 4) {
            System.out.println("Usage: PrefixTestListCreator <inputFile> <outputFile> <minPrefixLength> <maxPrefixLength>");
            System.exit(0);
        }

        FileReader input;
        FileWriter output;

        try {
            input = new FileReader(args[0]);
            output = new FileWriter(args[1]);

            int min = Integer.parseInt(args[2]);
            int max = Integer.parseInt(args[3]);

            BufferedReader bufRead = new BufferedReader(input);
            BufferedWriter bufferedWriter = new BufferedWriter(output);

            String line = bufRead.readLine();
            while (line != null) {

                String[] words = line.split(" ");

                for (int i = 0; i < words.length; i++) {
                    for (int j = 0; j < words[i].length(); j++) {
                        String prefix = words[i].substring(0, j);
                        if (min < prefix.length() && prefix.length() < max) {
                            bufferedWriter.write(prefix + '\n');
                        }
                    }
                }
                line = bufRead.readLine();
            }

            bufRead.close();
            bufferedWriter.close();
            input.close();
            output.close();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
