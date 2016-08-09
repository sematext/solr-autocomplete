package com.sematext.autocomplete.tst;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class AutoCompleteService {
    private TernarySearchTree tst = new TernarySearchTree();

    public AutoCompleteService(String file) {

        FileReader input;
        BufferedReader bufRead = null;
        try {
            input = new FileReader(file);
            bufRead = new BufferedReader(input);

            String line;

            line = bufRead.readLine();

            while (line != null) {

                tst.put(line.toLowerCase(), line.toLowerCase());

                line = bufRead.readLine();
            }

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
          if (bufRead != null) {
            try {
              bufRead.close();
            } catch (IOException e) {
            }
          }
        }
    }

    public DoublyLinkedList matchPrefix(String prefix) {
        return tst.matchPrefix(prefix);
    }

    public Object get(String key) {
        return tst.get(key);
    }

}
