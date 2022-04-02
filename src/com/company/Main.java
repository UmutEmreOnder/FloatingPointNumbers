package com.company;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("input.txt");
        Scanner scannerFile = new Scanner(file);
        Scanner scannerInput = new Scanner(System.in);
        System.out.print("Byte Ordering: ");
        char byteType = scannerInput.nextLine().charAt(0);
        System.out.print("Floating Point Size: ");
        int flPointSize = scannerInput.nextInt();

        while(scannerFile.hasNext()){
            String number = scannerFile.nextLine();
            int[] answer = number.contains(".") ? floatToBit(Double.parseDouble(number), flPointSize) : integerToBit(number, 16, true);
            String hex = endian(bitToHex(answer), byteType);
            System.out.println(hex);
        }
    }

    public static int stringToInt(String numberString) {
        return numberString.charAt(numberString.length() - 1) == 'u' ? Integer.parseInt(numberString.substring(0, numberString.length() - 1)) : Integer.parseInt(numberString);
    }

    // fixedSize sadece fraction kismindan cagrildiysa fonksiyon false oluyor
    // int signed kisminin amaci 16 bitlik unsigned ya da signed gonderildiginde eger unsigned ise tum bitleri kullanabilir
    // ama signed ise ilk biti signe icin ayirmali o yuzden for dongusu 2. bitten (yani 1. indexten) baslatmali
    // bu dedigim fraction kismindan gelenler icin gecerli olmadigindan fixedSize booleani ekledim
    public static int[] integerToBit(String numberString, int size, boolean fixedSize) {
        int[] answer = new int[size];
        int number = stringToInt(numberString);
        int signed = numberString.charAt(numberString.length() - 1) == 'u' || !fixedSize ? 0 : 1;

        boolean negative = number < 0;
        number = Math.abs(number);

        for(int i = size - signed; i >= 0; i--) {
            if (Math.pow(2, i) <= number) {
                number -= Math.pow(2, i);
                answer[size - 1 - i] = 1;
            }
        }

        if (negative) translateToNegative(answer);
        return answer;
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

    public static String bitToHex(int[] bits) {
        StringBuilder answer = new StringBuilder();
        for(int k = 1; k <= bits.length / 4; k++) {
            int hexValue = 0;
            for(int i = 4*k - 1; i >= 4*k - 4; i--) {
                if (bits[i] == 1) hexValue += (i >= 4) ? Math.pow(2, 3 - (i % 4)) : Math.pow(2, 3 - i);
            }
            answer.append(hexValue < 10 ? hexValue : String.valueOf(Character.toChars((hexValue - 10) + 'A')));
        }
        return answer.toString();
    }

    public static String endian(String hex, char endian) {
        StringBuilder newHex = new StringBuilder();

        if (endian == 'b') {
            for (int i = 0; i < hex.length(); i++) {
                newHex.append(hex.charAt(i));
                if (i % 2 == 1) newHex.append(" ");
            }
        }
        else {
            for (int i = hex.length() - 2; i >= 0; i -= 2) {
                newHex.append(hex.charAt(i));
                newHex.append(hex.charAt(i + 1));
                newHex.append(" ");
            }
        }
        return newHex.toString();
    }

    public static int[] floatToBit(double a, int size) {
        boolean negative = a < 0;
        a = Math.abs(a);

        int fractionSize = size == 1 ? 4 : size == 2 ? 7 : size == 3 ? 13 : 19;

        int intPart = (int) (a);
        double fraction = a - intPart;
        int[] intPartArray = integerToBit(String.valueOf(intPart), (int) (Math.log(intPart) / Math.log(2) + 1), false);
        List<Integer> fractionList = fractionToBit(fraction);

        int E = intPartArray.length - 1;
        int bias = (int) Math.pow(2, (size * 8) - fractionSize - 2) - 1;
        int exp = E + bias;
        int[] expArray = integerToBit(String.valueOf(exp), (int) (Math.log(exp) / Math.log(2) + 1), false);


        int[] sum = new int[intPartArray.length + fractionList.size()];
        System.arraycopy(intPartArray, 0, sum, 0, intPartArray.length);
        for (int i = 0; i < fractionList.size(); i++) {
            sum[intPartArray.length + i] = fractionList.get(i);
        }

        int[] roundedSum = new int[fractionSize];
        round(sum, fractionSize);
        for (int i = 1; i <= fractionSize; i++) {
            if (i >= sum.length) roundedSum[i - 1] = 0;
            else roundedSum[i - 1] = sum[i];
        }

        int[] bitLast = new int[1 + expArray.length + roundedSum.length];
        bitLast[0] = negative ? 1 : 0;

        System.arraycopy(expArray, 0, bitLast, 1, expArray.length);

        int index = 0;
        for (int i = expArray.length + 1; i < bitLast.length; i++) {
            bitLast[i] = roundedSum[index++];
        }

        return bitLast;
    }

    public static void round(int[] sum, int fractionSize) {
        if (sum.length - 1 <= fractionSize || sum[fractionSize + 1] == 0) return;
        boolean repeat = false;

        for(int i = fractionSize + 2; i < sum.length; i++) {
            if (sum[i] == 1) {
                repeat = true;
                break;
            }
        }

        if (!repeat && (sum[fractionSize] == 0)) return;

        for (int i = fractionSize; i >= 0; i--) {
            if (sum[i] == 1) sum[i] = 0;
            else {
                sum[i] = 1;
                break;
            }
        }
    }

    public static List<Integer> fractionToBit(double fraction) {
        List<Integer> fractionArray = new ArrayList<>();
        for (int i = 1; i <= 24; i++) {
            if (fraction == 0) break;

            if (fraction >= Math.pow(2, -i)) {
                fractionArray.add(1);
                fraction -= Math.pow(2, -i);
            }
            else {
                fractionArray.add(0);
            }
        }
        if (fraction != 0) fractionArray.add(1);
        return fractionArray;
    }
}