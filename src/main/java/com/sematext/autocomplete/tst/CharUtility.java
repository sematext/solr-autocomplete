package com.sematext.autocomplete.tst;

public class CharUtility {

    /**
     * Returns an int value that is negative if cCompare comes before cRef in the alphabet, zero if the two are equal,
     * and positive if cCompare comes after cRef in the alphabet.
     */
    public static int compareCharsAlphabetically(char cCompare, char cRef) {
        return (alphabetizeChar(cCompare) - alphabetizeChar(cRef));
    }

    private static int alphabetizeChar(char c) {
        if (c < 65)
            return c;
        if (c < 89)
            return (2 * c) - 65;
        if (c < 97)
            return c + 24;
        if (c < 121)
            return (2 * c) - 128;
        return c;
    }

    public static int compareWordsAlphabetically(String wCompare, String wRef) {

        int i = 0;
        int compareCharValue = -1;

        do {
            compareCharValue = compareCharsAlphabetically(wCompare.charAt(i), wRef.charAt(i));
            i++;

        } while (i < wCompare.length() && i < wRef.length() && compareCharValue == 0);

        // word is larger if it is longer (and same in prefix)
        if (compareCharValue == 0 && wCompare.length() < wRef.length()) {
            return (0 - alphabetizeChar(wRef.charAt(i++)));
        }

        // word is larger if it is longer (and same in prefix)
        if (compareCharValue == 0 && wCompare.length() > wRef.length()) {
            return alphabetizeChar(wCompare.charAt(i++));
        }

        return compareCharValue;
    }

    public static int compareWordsByHash(String wCompare, String wRef) {
        return wCompare.hashCode() - wRef.hashCode();

    }

}
