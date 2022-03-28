package com.company;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("input.txt");
        Scanner scannerFile = new Scanner(file);
        Scanner scannerInput = new Scanner(System.in);
        System.out.print("Byte Ordering: ");
        String byteType = scannerInput.nextLine();
        System.out.print("Floating Point Size: ");
        int flPointSize = scannerInput.nextInt();

        while(scannerFile.hasNext()){
            String number = scannerFile.nextLine();
            if(number.contains(".")){
                System.out.println("fonk hazır değil");
            }
            else {
                int[] answer = new int[16];
                integerToBit(answer, number);
                String hexa = bitToHexa(answer);
                System.out.println(hexa);
            }
        }
    }

    public static int stringToInt(String numberString) {
        return numberString.contains("u") ? Integer.parseInt(numberString.substring(0, numberString.length() - 1)) : Integer.parseInt(numberString);
    }

    public static void integerToBit(int[] answer, String numberString) {
        int number = stringToInt(numberString);
        int start = numberString.contains("u") ? 15 : 14;

        boolean negative = number < 0;
        number = Math.abs(number);

        for(int i = start; i >= 0; i--) {
            if (Math.pow(2, i) <= number) {
                number -= Math.pow(2, i);
                answer[15 - i] = 1;
            }
        }

        if (negative) translateToNegative(answer);
    }

    public static void translateToNegative(int[] answer) {
        boolean firstSeen = false;
        for(int i = 15; i >= 0; i--) {
            if (firstSeen) answer[i] = bitTranslate(answer[i]);
            if (answer[i] == 1 && !firstSeen) firstSeen = true;
        }
    }

    public static int bitTranslate(int bit) {
        return bit == 1 ? 0 : 1;
    }

    public static String bitToHexa(int[] bits) {
        StringBuilder answer = new StringBuilder("0x");
        for(int k = 1; k <= bits.length / 4; k++) {
            int hexaValue = 0;
            for(int i = 4*k - 1; i >= 4*k - 4; i--) {
                if (bits[i] == 1) hexaValue += (i >= 4) ? Math.pow(2, 3 - (i % 4)) : Math.pow(2, 3 - i);
            }
            answer.append(hexaValue < 10 ? hexaValue : String.valueOf(Character.toChars((hexaValue - 10) + 'A')));
        }
        return answer.toString();
    }
}
